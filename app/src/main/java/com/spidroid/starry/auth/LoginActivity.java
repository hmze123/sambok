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
import com.google.android.material.button.MaterialButton; // هذا هو الاستيراد الصحيح
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.MainActivity;
import com.spidroid.starry.auth.CircularProgressBar;

public class LoginActivity extends AppCompatActivity {

  private FirebaseAuth mAuth;
  private TextInputLayout emailLayout, passwordLayout;
  private TextInputEditText emailInput, passwordInput;
  private CardView progressContainer;
  private CircularProgressBar progressBar;
  private MaterialButton loginButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    mAuth = FirebaseAuth.getInstance();

    FirebaseUser currentUser = mAuth.getCurrentUser();
    if (currentUser != null) {
      startMainActivity();
      return;
    }

    initializeViews();
    setupListeners();
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

    loginButton.setOnClickListener(v -> attemptLogin());
    signUpPrompt.setOnClickListener(v -> startSignUp());
  }

  private void setupListeners() {
    emailInput.addTextChangedListener(new ClearErrorTextWatcher(emailLayout));
    passwordInput.addTextChangedListener(new ClearErrorTextWatcher(passwordLayout));
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
