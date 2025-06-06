package com.spidroid.starry.auth

import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.activities.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var progressContainer: CardView
    private lateinit var progressBar: CircularProgressBar
    private lateinit var loginButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var forgotPasswordPrompt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // إذا كان المستخدم قد سجل دخوله بالفعل، انتقل إلى الشاشة الرئيسية
        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        initializeViews()
        setupListeners()
        loadSavedEmail()
    }

    private fun initializeViews() {
        progressContainer = findViewById(R.id.progressContainer)
        progressBar = findViewById(R.id.progressBar)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        val signUpPrompt: TextView = findViewById(R.id.signUpPrompt)
        forgotPasswordPrompt = findViewById(R.id.forgotPasswordPrompt)

        loginButton.setOnClickListener { attemptLogin() }
        signUpPrompt.setOnClickListener { startSignUp() }
        forgotPasswordPrompt.setOnClickListener { showForgotPasswordDialog() }
    }

    private fun setupListeners() {
        emailInput.addTextChangedListener(ClearErrorTextWatcher(emailLayout))
        passwordInput.addTextChangedListener(ClearErrorTextWatcher(passwordLayout))
    }

    private fun loadSavedEmail() {
        val savedEmail = sharedPreferences.getString("user_email", "")
        if (!savedEmail.isNullOrEmpty()) {
            emailInput.setText(savedEmail)
            emailInput.setSelection(savedEmail.length) // لنقل المؤشر إلى نهاية النص
        }
    }

    private fun attemptLogin() {
        loginButton.isEnabled = false
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateLoginForm(email, password)) {
            loginButton.isEnabled = true
            return
        }

        showProgress(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showProgress(false)
                loginButton.isEnabled = true

                if (task.isSuccessful) {
                    // استخدام KTX لجعل الكود أكثر نظافة
                    sharedPreferences.edit { putString("user_email", email) }
                    startMainActivity()
                } else {
                    handleLoginError(task.exception)
                }
            }
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        var isValid = true

        if (email.isBlank()) {
            emailLayout.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Valid email required"
            isValid = false
        }

        if (password.isBlank()) {
            passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Minimum 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun handleLoginError(exception: Exception?) {
        val error = exception?.message ?: "Authentication failed"
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    private fun showForgotPasswordDialog() {
        val resetEmailInput = TextInputEditText(this).apply {
            hint = "Email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(emailInput.text.toString()) // ملء الحقل تلقائيًا
        }

        // استخدام MaterialAlertDialogBuilder لمظهر متناسق
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email to receive a password reset link.")
            .setView(resetEmailInput, 20, 20, 20, 20) // إضافة هوامش
            .setPositiveButton("Send Reset Link") { _, _ ->
                val email = resetEmailInput.text.toString().trim()
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                } else {
                    sendPasswordResetEmail(email)
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
        startActivity(Intent(this, MainActivity::class.java))
        finish() // إنهاء شاشة تسجيل الدخول لمنع العودة إليها
    }

    private fun startSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    private fun showProgress(show: Boolean) {
        progressContainer.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            ValueAnimator.ofFloat(0f, 100f).apply {
                duration = 2000
                addUpdateListener { animation ->
                    progressBar.setProgress(animation.animatedValue as Float)
                }
                start()
            }
        } else {
            progressBar.setProgress(0f)
        }
    }

    // يمكن تبسيط TextWatcher باستخدام دوال KTX، ولكن للحفاظ على نفس الهيكل،
    // سنقوم بتحويل الكلاس الداخلي إلى Kotlin.
    private class ClearErrorTextWatcher(private val layout: TextInputLayout) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            layout.error = null
        }
    }
}
