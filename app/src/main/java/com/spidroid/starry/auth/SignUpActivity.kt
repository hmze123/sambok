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
import android.widget.CheckBox
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.activities.MainActivity
import com.spidroid.starry.models.UserModel

class SignUpActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val db: FirebaseFirestore by lazy { Firebase.firestore }

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

        initializeViews()
        setupListeners()
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

        signUpButton.isEnabled = false // Initially disabled
    }

    private fun setupListeners() {
        val loginPrompt: TextView = findViewById(R.id.loginPrompt)
        val termsCheckBox: CheckBox = findViewById(R.id.termsCheckBox)
        val termsText: TextView = findViewById(R.id.termsText)

        signUpButton.setOnClickListener { attemptSignUp() }
        loginPrompt.setOnClickListener { startLogin() }

        termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            signUpButton.isEnabled = isChecked && isFormValid()
        }

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Enable button only if checkbox is also checked
                signUpButton.isEnabled = termsCheckBox.isChecked && isFormValid()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        usernameInput.addTextChangedListener(textWatcher)
        emailInput.addTextChangedListener(textWatcher)
        passwordInput.addTextChangedListener(textWatcher)

        setupTermsText(termsText)
    }

    private fun isFormValid(): Boolean {
        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        return username.isNotBlank() && email.isNotBlank() && password.isNotBlank()
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

    private fun setupTermsText(termsText: TextView) {
        val spannable = buildSpannedString {
            append("I agree to the ")
            inSpans(
                StyleSpan(Typeface.BOLD),
                ForegroundColorSpan(ContextCompat.getColor(this@SignUpActivity, R.color.primary)),
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        showTermsBottomSheet("terms")
                    }
                }
            ) { append("Terms of Service") }
            append(" and ")
            inSpans(
                StyleSpan(Typeface.BOLD),
                ForegroundColorSpan(ContextCompat.getColor(this@SignUpActivity, R.color.primary)),
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        showTermsBottomSheet("privacy")
                    }
                }
            ) { append("Privacy Policy") }
        }

        termsText.text = spannable
        termsText.movementMethod = LinkMovementMethod.getInstance()
        termsText.highlightColor = android.graphics.Color.TRANSPARENT
    }

    private fun showTermsBottomSheet(termsType: String) {
        TermsBottomSheetFragment.newInstance(termsType).show(supportFragmentManager, termsType)
    }

    private fun attemptSignUp() {
        if (!validateInputFields()) return

        showProgress(true)
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val username = usernameInput.text.toString().trim()
                    createUserProfile(username, email)
                } else {
                    showProgress(false)
                    handleSignUpError(task.exception)
                }
            }
    }

    private fun validateInputFields(): Boolean {
        usernameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null

        val username = usernameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        var isValid = true

        if (username.length < 3) {
            usernameLayout.error = "Username must be at least 3 characters"
            isValid = false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email"
            isValid = false
        }

        if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun createUserProfile(username: String, email: String) {
        val firebaseUser = auth.currentUser ?: run {
            showProgress(false)
            Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = UserModel(
            userId = firebaseUser.uid,
            username = username,
            email = email,
            displayName = username // Initially set displayName to username
        )

        db.collection("users").document(firebaseUser.uid).set(user)
            .addOnCompleteListener { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Failed to create profile: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleSignUpError(exception: Exception?) {
        val errorMessage = exception?.message ?: "Registration failed. Please try again."
        if (errorMessage.contains("email address is already in use", ignoreCase = true)) {
            emailLayout.error = "This email is already registered"
        } else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun showProgress(show: Boolean) {
        progressContainer.visibility = if (show) View.VISIBLE else View.GONE
        signUpButton.isEnabled = !show
        if (show) {
            progressAnimator?.start()
        } else {
            progressAnimator?.cancel()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun startLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        progressAnimator?.cancel()
    }
}