package com.spidroid.starry.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.activities.MainActivity
import com.spidroid.starry.databinding.ActivityLoginBinding
import com.spidroid.starry.utils.Resource
import com.spidroid.starry.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        setupListeners()
        loadSavedEmail()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.signUpPrompt.setOnClickListener { startSignUp() }
        binding.forgotPasswordPrompt.setOnClickListener { showForgotPasswordDialog() }

        binding.emailInput.addTextChangedListener(ClearErrorTextWatcher(binding.emailLayout))
        binding.passwordInput.addTextChangedListener(ClearErrorTextWatcher(binding.passwordLayout))
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { resource ->
            showProgress(resource.status == Resource.Status.LOADING)
            when (resource.status) {
                Resource.Status.SUCCESS -> {
                    sharedPreferences.edit { putString("user_email", binding.emailInput.text.toString().trim()) }
                    startMainActivity()
                }
                Resource.Status.ERROR -> {
                    handleLoginError(resource.message)
                }
                else -> { /* No-op for other states */ }
            }
        }

        viewModel.resetPasswordResult.observe(this) { resource ->
            showProgress(resource.status == Resource.Status.LOADING)
            when (resource.status) {
                Resource.Status.SUCCESS -> {
                    Toast.makeText(this, "Password reset link sent.", Toast.LENGTH_LONG).show()
                }
                Resource.Status.ERROR -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
                else -> { /* No-op for other states */ }
            }
        }
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
        if (validateLoginForm(email, password)) {
            viewModel.loginUser(email, password)
        }
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        var isValid = true

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_invalid_email)
            isValid = false
        }
        if (password.length < 6) {
            binding.passwordLayout.error = getString(R.string.error_password_min_chars)
            isValid = false
        }
        return isValid
    }

    private fun handleLoginError(message: String?) {
        val error = message ?: "Authentication failed. Please check your credentials."
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
                    viewModel.sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        // **[تم التعديل هنا]**
        binding.progressContainer.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
    }

    private class ClearErrorTextWatcher(private val layout: TextInputLayout) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { layout.error = null }
        override fun afterTextChanged(s: Editable?) {}
    }
}