package com.spidroid.starry.auth;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View;
import android.widget.TextView;
import android.util.Patterns;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.MainActivity;
import com.spidroid.starry.auth.CircularProgressBar;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog; // استيراد AlertDialog

public class LoginActivity extends AppCompatActivity {

  private FirebaseAuth mAuth;
  private TextInputLayout emailLayout, passwordLayout;
  private TextInputEditText emailInput, passwordInput;
  private CardView progressContainer;
  private CircularProgressBar progressBar;
  private MaterialButton loginButton;
  private SharedPreferences sharedPreferences;
  private TextView forgotPasswordPrompt; // إضافة TextView الجديد

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    mAuth = FirebaseAuth.getInstance();
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    FirebaseUser currentUser = mAuth.getCurrentUser();
    if (currentUser != null) {
      startMainActivity();
      return;
    }

    initializeViews();
    setupListeners();
    loadSavedEmail();
  }

  private void initializeViews() {
    progressContainer = findViewById(R.id.progressContainer);
    progressBar = findViewById(R.id.progressBar);
    emailLayout = findViewById(R.id.emailLayout);
    passwordLayout = findViewById(R.id.passwordLayout);
    emailInput = findViewById(R.id.emailInput);
    passwordInput = findViewById(R.id.passwordInput);
    loginButton = findViewById(R.id.loginButton);
    TextView signUpPrompt = findViewById(R.id.signUpPrompt);
    forgotPasswordPrompt = findViewById(R.id.forgotPasswordPrompt); // تهيئة TextView الجديد

    loginButton.setOnClickListener(v -> attemptLogin());
    signUpPrompt.setOnClickListener(v -> startSignUp());
    forgotPasswordPrompt.setOnClickListener(v -> showForgotPasswordDialog()); // <--- إضافة مستمع النقر
  }

  private void setupListeners() {
    emailInput.addTextChangedListener(new ClearErrorTextWatcher(emailLayout));
    passwordInput.addTextChangedListener(new ClearErrorTextWatcher(passwordLayout));
  }

  private void loadSavedEmail() {
    String savedEmail = sharedPreferences.getString("user_email", "");
    if (!savedEmail.isEmpty()) {
      emailInput.setText(savedEmail);
      emailInput.setSelection(savedEmail.length());
    }
  }

  private void attemptLogin() {
    loginButton.setEnabled(false);
    String email = emailInput.getText().toString().trim();
    String password = passwordInput.getText().toString().trim();

    if (!validateLoginForm(email, password)) {
      loginButton.setEnabled(true);
      return;
    }

    showProgress(true);
    mAuth
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(
                    this,
                    task -> {
                      showProgress(false);
                      loginButton.setEnabled(true);

                      if (task.isSuccessful()) {
                        sharedPreferences.edit().putString("user_email", email).apply();
                        startMainActivity();
                      } else {
                        handleLoginError(task.getException());
                      }
                    });
  }

  private boolean validateLoginForm(String email, String password) {
    boolean isValid = true;

    if (TextUtils.isEmpty(email)) {
      emailLayout.setError("Email is required");
      isValid = false;
    } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      emailLayout.setError("Valid email required");
      isValid = false;
    }

    if (TextUtils.isEmpty(password)) {
      passwordLayout.setError("Password is required");
      isValid = false;
    } else if (password.length() < 6) {
      passwordLayout.setError("Minimum 6 characters");
      isValid = false;
    }

    return isValid;
  }

  private void handleLoginError(Exception exception) {
    String error = "Authentication failed";
    if (exception != null) {
      error = exception.getMessage();
    }
    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
  }

  // New: Show Forgot Password Dialog
  private void showForgotPasswordDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Reset Password");
    builder.setMessage("Enter your email to receive a password reset link.");

    final TextInputEditText resetEmailInput = new TextInputEditText(this);
    resetEmailInput.setHint("Email");
    resetEmailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    // يمكنك تهيئة حقل الإيميل بقيمة الإيميل الموجودة في حقل الدخول
    if (emailInput.getText() != null && !emailInput.getText().toString().isEmpty()) {
      resetEmailInput.setText(emailInput.getText().toString());
    }
    builder.setView(resetEmailInput);

    builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
      String email = resetEmailInput.getText().toString().trim();
      if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
        return;
      }
      sendPasswordResetEmail(email);
    });
    builder.setNegativeButton("Cancel", null);
    builder.show();
  }

  // New: Send Password Reset Email
  private void sendPasswordResetEmail(String email) {
    showProgress(true); // إظهار مؤشر التقدم
    mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
              showProgress(false); // إخفاء مؤشر التقدم
              if (task.isSuccessful()) {
                Toast.makeText(this, "Password reset link sent to " + email, Toast.LENGTH_LONG).show();
              } else {
                String error = "Failed to send reset link.";
                if (task.getException() != null) {
                  error += " " + task.getException().getMessage();
                }
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
              }
            });
  }


  private void startMainActivity() {
    startActivity(new Intent(this, MainActivity.class));
    finish();
  }

  private void startSignUp() {
    startActivity(new Intent(this, SignUpActivity.class));
  }

  private void showProgress(boolean show) {
    progressContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    if (show) {
      ValueAnimator animator = ValueAnimator.ofFloat(0, 100);
      animator.setDuration(2000);
      animator.addUpdateListener(
              animation -> {
                float progress = (float) animation.getAnimatedValue();
                progressBar.setProgress(progress);
              });
      animator.start();
    } else {
      progressBar.setProgress(0);
    }
  }

  private static class ClearErrorTextWatcher implements TextWatcher {
    private final TextInputLayout layout;

    ClearErrorTextWatcher(TextInputLayout layout) {
      this.layout = layout;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
      layout.setError(null);
    }
  }
}