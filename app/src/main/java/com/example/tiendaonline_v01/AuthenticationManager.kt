package com.example.tiendaonline_v01 // ASEGÚRATE QUE ESTE ES TU PAQUETE

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth // Importación necesaria para Firebase.auth
import com.google.firebase.ktx.Firebase // Importación necesaria para Firebase.auth

// Definimos una interfaz para el callback, para que AuthenticationManager pueda notificar a la Activity
interface AuthListener {
    fun onAuthSuccess(user: FirebaseUser?)
    fun onAuthFailure(errorMessage: String)
    fun onGoogleSignInStart(signInIntent: Intent) // Para que la Activity lance la intención de Google
}

class AuthenticationManager(private val context: Context, private val listener: AuthListener) {

    private val auth: FirebaseAuth = Firebase.auth // Usa Firebase.auth para inicializar
    private val googleSignInClient: GoogleSignInClient

    // Constantes para SharedPreferences
    private val PREFS_NAME = "MyPrefs"
    private val KEY_LOGGED_IN = "isLoggedIn"

    init {
        // Inicializar GoogleSignInOptions. Se necesita un Context para getString
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    // --- Métodos de autenticación local (Email/Password) ---
    fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveLoginState(true)
                    listener.onAuthSuccess(user)
                } else {
                    Log.w("AuthManager", "signInWithEmail:failure", task.exception)
                    listener.onAuthFailure(task.exception?.message ?: "Error desconocido al iniciar sesión con correo.")
                }
            }
    }

    // --- Métodos de autenticación con Google ---
    fun startGoogleSignInFlow() {
        val signInIntent = googleSignInClient.signInIntent
        listener.onGoogleSignInStart(signInIntent)
    }

    fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("AuthManager", "Google sign in failed", e)
            listener.onAuthFailure("Fallo el inicio de sesión con Google: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveLoginState(true)
                    listener.onAuthSuccess(user)
                } else {
                    Log.w("AuthManager", "Firebase Auth with Google failed", task.exception)
                    listener.onAuthFailure("Fallo la autenticación con Google en Firebase: ${task.exception?.message}")
                }
            }
    }

    // --- Métodos de gestión de sesión (SharedPreferences) ---
    fun isLoggedIn(): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false)
    }

    private fun saveLoginState(isLoggedIn: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_LOGGED_IN, isLoggedIn)
        editor.apply()
    }

    fun signOut() {
        auth.signOut() // Cierra sesión en Firebase
        googleSignInClient.signOut() // Cierra sesión en Google
        saveLoginState(false) // Actualiza SharedPreferences
    }

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth.currentUser
    }
}