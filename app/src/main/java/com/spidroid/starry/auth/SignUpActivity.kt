package com.spidroid.starry.auth

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.spidroid.starry.R
import com.spidroid.starry.activities.MainActivity
import com.spidroid.starry.models.UserModel

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var usernameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var signUpButton: Button
    private lateinit var progressContainer: CardView
    private lateinit var progressBar: CircularProgressBar
    private var progressAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupProgressAnimator()
    }

    private fun initializeViews() {
        progressContainer = findViewById(R.id.progressContainer)
        progressBar = findViewById(R.id.progressBar)
        usernameLayout = findViewById(R.id.usernameLayout)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        usernameInput = findViewById(R.id.usernameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        signUpButton = findViewById(R.id.signUpButton)
        val loginPrompt: TextView = findViewById(R.id.loginPrompt)
        val termsCheckBox: android.widget.CheckBox = findViewById(R.id.termsCheckBox)
        val termsText: TextView = findViewById(R.id.termsText)

        signUpButton.setOnClickListener { attemptSignUp() }
        loginPrompt.setOnClickListener { startLogin() }

        addTextWatchers()
        setupTermsText(termsText)

        termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            signUpButton.isEnabled = isChecked
        }
        signUpButton.isEnabled = false // الحالة الأولية للزر
    }

    private fun setupProgressAnimator() {
        progressAnimator = ValueAnimator.ofFloat(0f, 100f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                progressBar.setProgress(animation.animatedValue as Float)
            }
        }
    }

    private fun addTextWatchers() {
        usernameInput.addTextChangedListener(createErrorClearingWatcher(usernameLayout))
        emailInput.addTextChangedListener(createErrorClearingWatcher(emailLayout))
        passwordInput.addTextChangedListener(createErrorClearingWatcher(passwordLayout))
    }

    private fun createErrorClearingWatcher(layout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                layout.error = null
            }
        }
    }

    private fun setupTermsText(termsText: TextView) {
        val fullText = getString(R.string.terms_and_conditions_prompt) // "I agree to the Terms of Service and Privacy Policy"

        // استخدام KTX لجعل الكود أنظف
        val spannable = buildSpannedString {
            append("I agree to the ")
            // Terms of Service span
            inSpans(
                StyleSpan(Typeface.BOLD),
                ForegroundColorSpan(ContextCompat.getColor(this@SignUpActivity, R.color.primary)),
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        showTermsBottomSheet("terms")
                    }
                }
            ) {
                append("Terms of Service")
            }
            append(" and ")
            // Privacy Policy span
            inSpans(
                StyleSpan(Typeface.BOLD),
                ForegroundColorSpan(ContextCompat.getColor(this@SignUpActivity, R.color.primary)),
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        showTermsBottomSheet("privacy")
                    }
                }
            ) {
                append("Privacy Policy")
            }
        }

        termsText.text = spannable
        termsText.movementMethod = LinkMovementMethod.getInstance() // لجعل الروابط قابلة للنقر
        termsText.highlightColor = android.graphics.Color.TRANSPARENT // لإزالة لون التمييز عند النقر
    }

    private fun showTermsBottomSheet(termsType: String) {
        val bottomSheet = TermsBottomSheetFragment.newInstance(termsType)
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }

    private fun attemptSignUp() {
        signUpButton.isEnabled = false
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (!validateSignUpForm(username, email, password)) {
            signUpButton.isEnabled = true
            return
        }

        showProgress(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    createUserProfile(username, email)
                } else {
                    signUpButton.isEnabled = true
                    showProgress(false)
                    handleSignUpError(task.exception)
                }
            }
    }

    private fun validateSignUpForm(username: String, email: String, password: String): Boolean {
        var isValid = true

        if (username.isBlank()) {
            usernameLayout.error = "Username required"
            isValid = false
        } else if (username.length < 3) {
            usernameLayout.error = "Minimum 3 characters"
            isValid = false
        }

        if (email.isBlank()) {
            emailLayout.error = "Email required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Valid email required"
            isValid = false
        }

        if (password.isBlank()) {
            passwordLayout.error = "Password required"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Minimum 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun createUserProfile(username: String, email: String) {
        val firebaseUser = auth.currentUser ?: run {
            showProgress(false)
            signUpButton.isEnabled = true
            Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show()
            return
        }

        val user = UserModel(firebaseUser.uid, username, email)

        db.collection("users")
            .document(firebaseUser.uid)
            .set(user)
            .addOnCompleteListener { task ->
                showProgress(false)
                signUpButton.isEnabled = true

                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to create profile: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun handleSignUpError(exception: Exception?) {
        val errorMessage = exception?.message ?: "Registration failed"
        if (errorMessage.contains("email address is already in use", ignoreCase = true)) {
            emailLayout.error = "Email already registered"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showProgress(show: Boolean) {
        progressContainer.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            progressAnimator?.start()
        } else {
            progressAnimator?.cancel()
            progressBar.setProgress(0f)
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // استخدام finish() بدلاً من finishAffinity() في معظم الحالات
    }

    private fun startLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        progressAnimator?.cancel() // التأكد من إلغاء الـ animator لتجنب تسريب الذاكرة
    }
}
