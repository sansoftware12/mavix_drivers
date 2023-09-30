package com.example.mavix_drivers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class Sign_up : AppCompatActivity() {
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        emailEditText = findViewById(R.id.edtSignInEmail)
        passwordEditText = findViewById(R.id.edtSignInPassword)
        confirmPasswordEditText = findViewById(R.id.confirmpass)
        phoneNumberEditText = findViewById(R.id.phone)
        signUpButton = findViewById(R.id.btnSignIn)
        progressBar = findViewById(R.id.signinProgressBar)

        signUpButton.setOnClickListener {
            signUpUser()
        }
    }

    private fun signUpUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = ProgressBar.VISIBLE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, OnCompleteListener<AuthResult> { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToFirebase(user, phoneNumber)
                } else {
                    handleSignUpError(task.exception)
                }
            })
    }

    private fun saveUserToFirebase(user: FirebaseUser?, phoneNumber: String) {
        val userId = user?.uid
        if (userId != null) {
            val userRef = database.getReference("driver_locations").child(userId)
            userRef.child("email").setValue(user.email)
            userRef.child("phoneNumber").setValue(phoneNumber)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "User registered and data saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.visibility = ProgressBar.INVISIBLE
                        navigateToMainActivity()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to save user data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun handleSignUpError(exception: Exception?) {
        progressBar.visibility = ProgressBar.INVISIBLE

        when (exception) {
            is FirebaseAuthWeakPasswordException -> {
                Toast.makeText(this, "Weak password", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthException -> {
                Toast.makeText(this, "Sign up failed: ${exception.message}", Toast.LENGTH_SHORT)
                    .show()
            }
            else -> {
                Toast.makeText(this, "Sign up failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}