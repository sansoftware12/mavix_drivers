package com.example.mavix_drivers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var signUpText: TextView


    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.edtSignInEmail)
        passwordEditText = findViewById(R.id.edtSignInPassword)
        signInButton = findViewById(R.id.btnSignIn)
        progressBar = findViewById(R.id.signInProgressBar)
        signUpText = findViewById(R.id.txtSignUp)

        signInButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Sign in success
                            val user = auth.currentUser
                            showToast("Sign in successful")
                            // Example: Start the main activity after successful sign in
                            startActivity(Intent(this, MapsActivity::class.java))
                            finish() // Finish the sign-in activity
                        } else {
                            // Sign in failed
                            showToast("Sign in failed: ${task.exception?.message}")
                        }

                        progressBar.visibility = View.GONE
                    }
            } else {
                showToast("Please enter email and password")
            }
        }
        signUpText.setOnClickListener {
            navigateToSignUp()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToSignUp() {
        val intent = Intent(this, Sign_up::class.java)
        startActivity(intent)
    }
}



