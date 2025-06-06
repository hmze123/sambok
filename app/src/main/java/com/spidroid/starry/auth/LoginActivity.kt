package com.spidroid.starry.auth

import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.activities.MainActivity
import com.spidroid.starry.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redirect if user is already logged in
        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        setupListeners()
        loadSavedEmail()
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.signUpPrompt.setOnClickListener { startSignUp() }
        binding.forgotPasswordPrompt.setOnClickListener { showForgotPasswordDialog() }

        binding.emailInput.addTextChangedListener(ClearErrorTextWatcher(binding.emailLayout))
        binding.passwordInput.addTextChangedListener(ClearErrorTextWatcher(binding.passwordLayout))
    }

    private fun loadSavedEmail() {
        val savedEmail = sharedPreferences.getString("user_email", null)
        if (!savedEmail.isNullOrEmpty()) {
            binding.emailInput.setText(savedEmail)
        }
    }

    private fun attemptLogin() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (!validateLoginForm(email, password)) return

        showProgress(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    sharedPreferences.edit { putString("user_email", email) }
                    startMainActivity()
                } else {
                    handleLoginError(task.exception)
                }
            }
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        var isValid = true

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email"
            isValid = false
        }

        if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun handleLoginError(exception: Exception?) {
        val error = exception?.message ?: "Authentication failed. Please check your credentials."
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    private fun showForgotPasswordDialog() {
        val resetEmailInput = TextInputEditText(this).apply {
            hint = "Email Address"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.emailInput.text.toString())
        }

        val container = FrameLayout(this).apply {
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
            addView(resetEmailInput)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email to receive a password reset link.")
            .setView(container)
            .setPositiveButton("Send Link") { _, _ ->
                val email = resetEmailInput.text.toString().trim()
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        showProgress(true)
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset link sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    val error = task.exception?.message ?: "Failed to send reset link."
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun startSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun showProgress(show: Boolean) {
        binding.progressContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
    }

    private class ClearErrorTextWatcher(private val layout: TextInputLayout) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            layout.error = null
        }
        override fun afterTextChanged(s: Editable?) {}
    }
}