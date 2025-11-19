package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Dominios permitidos (mismo criterio que en Register)
    private val allowedEmailRegex =
        Regex("^[A-Za-z0-9._%+-]+@(gmail\\.cl|gmail\\.com|hotmail\\.com)$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isEmpty()) { emailEditText.error = "Ingresa tu correo"; emailEditText.requestFocus(); return@setOnClickListener }
            if (!isValidEmail(email)) { emailEditText.error = "Correo inválido (usa @gmail.cl, @gmail.com o @hotmail.com)"; emailEditText.requestFocus(); return@setOnClickListener }
            if (password.isEmpty()) { passwordEditText.error = "Ingresa tu contraseña"; passwordEditText.requestFocus(); return@setOnClickListener }

            loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    loginButton.isEnabled = true
                    if (task.isSuccessful) {
                        // Marca sesión como activa
                        SessionManager.setLoggedIn(this, true)

                        startActivity(Intent(this, InicioTareasActivity::class.java))
                        finish()
                    } else {
                        handleLoginError(task.exception, emailEditText, passwordEditText)
                    }
                }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false
        return allowedEmailRegex.matches(email)
    }

    private fun handleLoginError(
        exception: Exception?,
        emailField: EditText,
        passwordField: EditText
    ) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                if (exception.errorCode == "ERROR_USER_DISABLED") {
                    emailField.error = "La cuenta está deshabilitada"
                } else {
                    emailField.error = "Correo no registrado"
                }
                emailField.requestFocus()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                if (exception.errorCode == "ERROR_WRONG_PASSWORD") {
                    passwordField.error = "Contraseña incorrecta"
                    passwordField.requestFocus()
                } else {
                    emailField.error = "Credenciales inválidas"
                    emailField.requestFocus()
                }
            }
            is FirebaseNetworkException -> {
                showToast("Sin conexión. Revisa tu red.")
            }
            else -> {
                val msg = exception?.localizedMessage ?: "Error al iniciar sesión."
                showToast(msg)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}