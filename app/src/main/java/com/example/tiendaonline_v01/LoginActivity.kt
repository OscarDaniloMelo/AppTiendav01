package com.example.tiendaonline_v01 // ASEGÚRATE QUE ESTE ES TU PAQUETE

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button // ¡Importar Button para el login local!
import android.widget.EditText // ¡Importar EditText para el login local!
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton // Para el botón de Google
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity(), AuthListener { // Implementa AuthListener

    private lateinit var authenticationManager: AuthenticationManager // Instancia de AuthenticationManager

    // googleSignInLauncher se queda aquí porque es una ActivityResultLauncher,
    // y debe ser propiedad de una Activity/Fragment.
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private val TAG = "LoginActivity" // Mejor TAG para esta clase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializar AuthenticationManager.
        // auth y googleSignInClient ahora se inicializan dentro de AuthenticationManager.
        authenticationManager = AuthenticationManager(this, this) // 'this' para el Context y para el AuthListener

        // 2. Verificar estado de sesión con SharedPreferences (a través de AuthenticationManager)
        if (authenticationManager.isLoggedIn()) {
            // Si SharedPreferences indica que está logueado, navegar directamente a HomeActivity
            navigateToHome()
            return // Salir de onCreate para no cargar la UI de login
        }

        // Si no está logueado, cargar el layout de login
        setContentView(R.layout.activity_login) // Asegúrate que este sea tu layout de login

        // --- Configuración de Google Sign-In Button (existente en tu código funcional) ---
        // Registrar el ActivityResultLauncher para el flujo de Google Sign-In
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Delegar el manejo del resultado a AuthenticationManager
            authenticationManager.handleGoogleSignInResult(result.data)
        }

        val googleSignInButton = findViewById<SignInButton>(R.id.sign_in_button_google) // Asegúrate que este ID exista en activity_login.xml
        googleSignInButton?.setOnClickListener { // Usar ?. para seguridad si el botón no se encuentra
            authenticationManager.startGoogleSignInFlow() // Delegar el inicio del flujo de Google a AuthenticationManager
        }

        // --- Configuración de Login Local (NUEVO) ---
        val editEmail = findViewById<EditText>(R.id.editEmail) // Asegúrate que este ID exista en activity_login.xml
        val editPassword = findViewById<EditText>(R.id.editPassword) // Asegúrate que este ID exista en activity_login.xml
        val btnLogin = findViewById<Button>(R.id.btnLogin) // Asegúrate que este ID exista en activity_login.xml

        btnLogin?.setOnClickListener { // Usar ?. para seguridad
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa correo y contraseña.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Delegar la autenticación de correo/contraseña a AuthenticationManager
            authenticationManager.signInWithEmail(email, password)
        }

        // Opcional: Esto ya no es necesario aquí si AuthenticationManager maneja Firebase.auth
        // auth = Firebase.auth
        // googleSignInClient = GoogleSignIn.getClient(this, gso) // Esto también lo hace AuthenticationManager
    }

    // --- Implementación de la interfaz AuthListener ---
    override fun onAuthSuccess(user: FirebaseUser?) {
        val welcomeMessage = if (user?.displayName != null && user.displayName!!.isNotEmpty()) {
            "Bienvenido, ${user.displayName}"
        } else if (user?.email != null && user.email!!.isNotEmpty()) {
            "Bienvenido, ${user.email}"
        } else {
            "Inicio de sesión exitoso."
        }
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
        navigateToHome()
    }

    override fun onAuthFailure(errorMessage: String) {
        Toast.makeText(this, "Autenticación fallida: $errorMessage", Toast.LENGTH_LONG).show()
    }

    override fun onGoogleSignInStart(signInIntent: Intent) {
        // Este método es llamado por AuthenticationManager para que LoginActivity lance la Intent
        googleSignInLauncher.launch(signInIntent)
    }

    // --- Función auxiliar para navegar a la pantalla principal de la app ---
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java) // ¡Ahora navega a HomeActivity!
        startActivity(intent)
        finish() // Cierra la Activity de Login
    }
}