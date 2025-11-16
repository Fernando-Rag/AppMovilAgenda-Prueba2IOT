package com.example.appmovilagenda

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Regex para los dominios permitidos
    // Solo acepta: gmail.cl, gmail.com, hotmail.com
    private val allowedEmailRegex =
        Regex("^[A-Za-z0-9._%+-]+@(gmail\\.cl|gmail\\.com|hotmail\\.com)$")

    // Password: mínimo 8 caracteres, al menos 1 mayúscula y 1 número
    // Puedes añadir más símbolos si quieres; aquí permitimos letras, dígitos y algunos símbolos.
    private val passwordRegex =
        Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@\$!%*?&._-]{8,}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // Validación en tiempo real opcional (marca el error mientras escribe)
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { /* no-op */ }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */ }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = s?.toString()?.trim() ?: ""
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    emailEditText.error = "Correo inválido o dominio no permitido"
                }
            }
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { /* no-op */ }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */ }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s?.toString() ?: ""
                if (pass.isNotEmpty() && !isValidPassword(pass)) {
                    passwordEditText.error = "Min 8 caracteres, 1 mayúscula y 1 número"
                }
            }
        })

        confirmPasswordEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { /* no-op */ }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */ }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = passwordEditText.text.toString()
                val confirm = s?.toString() ?: ""
                if (confirm.isNotEmpty() && pass.isNotEmpty() && pass != confirm) {
                    confirmPasswordEditText.error = "La contraseña no coincide"
                }
            }
        })

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirm = confirmPasswordEditText.text.toString()

            // Validaciones por orden. Se marca el campo específico con error y se detiene.
            if (email.isEmpty()) {
                emailEditText.error = "Ingresa tu correo"
                emailEditText.requestFocus()
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                emailEditText.error = "Correo inválido (usa @gmail.cl, @gmail.com o @hotmail.com)"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Ingresa una contraseña"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }
            if (!isValidPassword(password)) {
                passwordEditText.error = "Min 8 caracteres, 1 mayúscula y 1 número"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            if (confirm.isEmpty()) {
                confirmPasswordEditText.error = "Confirma la contraseña"
                confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }
            if (password != confirm) {
                confirmPasswordEditText.error = "La contraseña no coincide"
                confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }

            // Crear el usuario en Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Cuenta creada. Inicia sesión con tus credenciales.",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        val mensaje = task.exception?.localizedMessage ?: "No se pudo registrar. Intenta de nuevo."
                        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        // Verifica formato general y dominio permitido
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false
        return allowedEmailRegex.matches(email)
    }

    private fun isValidPassword(password: String): Boolean {
        // Verifica regex de requisitos
        return passwordRegex.matches(password)
    }
}