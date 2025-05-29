package com.spidroid.starry.auth;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.auth.CircularProgressBar;
import com.spidroid.starry.activities.MainActivity;
import com.spidroid.starry.models.UserModel;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SignUpActivity extends AppCompatActivity {

  private FirebaseAuth mAuth;
  private FirebaseFirestore db;
  private TextInputLayout usernameLayout, emailLayout, passwordLayout;
  private TextInputEditText usernameInput, emailInput, passwordInput;
  private Button signUpButton;
  private CardView progressContainer;
  private CircularProgressBar progressBar;
  private ValueAnimator progressAnimator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_signup);

    mAuth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();

    initializeViews();
    setupProgressAnimator();
  }

  private void initializeViews() {
    progressContainer = findViewById(R.id.progressContainer);
    progressBar = findViewById(R.id.progressBar);
    usernameLayout = findViewById(R.id.usernameLayout);
    emailLayout = findViewById(R.id.emailLayout);
    passwordLayout = findViewById(R.id.passwordLayout);
    usernameInput = findViewById(R.id.usernameInput);
    emailInput = findViewById(R.id.emailInput);
    passwordInput = findViewById(R.id.passwordInput);
    signUpButton = findViewById(R.id.signUpButton);
    TextView loginPrompt = findViewById(R.id.loginPrompt);

    signUpButton.setOnClickListener(v -> attemptSignUp());
    loginPrompt.setOnClickListener(v -> startLogin());

    addTextWatchers();
    CheckBox termsCheckBox = findViewById(R.id.termsCheckBox);
    TextView termsText = findViewById(R.id.termsText);
    setupTermsText(termsText);
    termsCheckBox.setOnCheckedChangeListener(
        (buttonView, isChecked) -> {
          signUpButton.setEnabled(isChecked);
        });
    signUpButton.setEnabled(false);
  }

  private void setupProgressAnimator() {
    progressAnimator = ValueAnimator.ofFloat(0, 100);
    progressAnimator.setDuration(2000);
    progressAnimator.setRepeatCount(ValueAnimator.INFINITE);
    progressAnimator.addUpdateListener(
        animation -> {
          float progress = (float) animation.getAnimatedValue();
          progressBar.setProgress(progress);
        });
  }

  private void addTextWatchers() {
    usernameInput.addTextChangedListener(createErrorClearingWatcher(usernameLayout));
    emailInput.addTextChangedListener(createErrorClearingWatcher(emailLayout));
    passwordInput.addTextChangedListener(createErrorClearingWatcher(passwordLayout));
  }

  private TextWatcher createErrorClearingWatcher(TextInputLayout layout) {
    return new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s) {
        layout.setError(null);
      }
    };
  }

  private void setupTermsText(TextView termsText) {
    String fullText = "I agree to the Terms of Service and Privacy Policy";
    SpannableString spannable = new SpannableString(fullText);

    // Terms of Service span
    int termsStart = fullText.indexOf("Terms of Service");
    int termsEnd = termsStart + "Terms of Service".length();
    ForegroundColorSpan termsColor =
        new ForegroundColorSpan(getResources().getColor(R.color.primary));
    StyleSpan termsBold = new StyleSpan(android.graphics.Typeface.BOLD);
    spannable.setSpan(termsColor, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannable.setSpan(termsBold, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    // Privacy Policy span
    int privacyStart = fullText.indexOf("Privacy Policy");
    int privacyEnd = privacyStart + "Privacy Policy".length();
    ForegroundColorSpan privacyColor =
        new ForegroundColorSpan(getResources().getColor(R.color.primary));
    StyleSpan privacyBold = new StyleSpan(android.graphics.Typeface.BOLD);
    spannable.setSpan(privacyColor, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannable.setSpan(privacyBold, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

    termsText.setText(spannable);
    termsText.setMovementMethod(LinkMovementMethod.getInstance());
    termsText.setOnClickListener(
        v -> {
          // Determine which part was clicked
          int x = (int) v.getX();
          int y = (int) v.getY();

          Layout layout = termsText.getLayout();
          if (layout != null) {
            int line = layout.getLineForVertical(y);
            int offset = layout.getOffsetForHorizontal(line, x);

            if (offset >= termsStart && offset <= termsEnd) {
              showTermsBottomSheet("terms");
            } else if (offset >= privacyStart && offset <= privacyEnd) {
              showTermsBottomSheet("privacy");
            }
          }
        });
  }

  private void showTermsBottomSheet(String termsType) {
    BottomSheetDialogFragment bottomSheet = TermsBottomSheetFragment.newInstance(termsType);
    bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
  }

  private void attemptSignUp() {
    signUpButton.setEnabled(false);
    String username = usernameInput.getText().toString().trim();
    String email = emailInput.getText().toString().trim();
    String password = passwordInput.getText().toString().trim();

    if (!validateSignUpForm(username, email, password)) {
      signUpButton.setEnabled(true);
      return;
    }

    showProgress(true);
    mAuth
        .createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(
            task -> {
              if (task.isSuccessful()) {
                createUserProfile(username, email);
              } else {
                signUpButton.setEnabled(true);
                showProgress(false);
                handleSignUpError(task.getException());
              }
            });
  }

  private boolean validateSignUpForm(String username, String email, String password) {
    boolean isValid = true;

    if (TextUtils.isEmpty(username)) {
      usernameLayout.setError("Username required");
      isValid = false;
    } else if (username.length() < 3) {
      usernameLayout.setError("Minimum 3 characters");
      isValid = false;
    }

    if (TextUtils.isEmpty(email)) {
      emailLayout.setError("Email required");
      isValid = false;
    } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      emailLayout.setError("Valid email required");
      isValid = false;
    }

    if (TextUtils.isEmpty(password)) {
      passwordLayout.setError("Password required");
      isValid = false;
    } else if (password.length() < 6) {
      passwordLayout.setError("Minimum 6 characters");
      isValid = false;
    }

    return isValid;
  }

  private void createUserProfile(String username, String email) {
    FirebaseUser firebaseUser = mAuth.getCurrentUser();
    if (firebaseUser == null) {
      showProgress(false);
      signUpButton.setEnabled(true);
      Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show();
      return;
    }

    UserModel user = new UserModel(firebaseUser.getUid(), username, email);

    db.collection("users")
        .document(firebaseUser.getUid())
        .set(user)
        .addOnCompleteListener(
            task -> {
              showProgress(false);
              signUpButton.setEnabled(true);

              if (task.isSuccessful()) {
                startMainActivity();
              } else {
                Toast.makeText(
                        this,
                        "Failed to create profile: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG)
                    .show();
              }
            });
  }

  private void handleSignUpError(Exception exception) {
    String error = "Registration failed";
    if (exception != null) {
      error = exception.getMessage();
      if (error.contains("email address is already")) {
        emailLayout.setError("Email already registered");
      }
    }
    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
  }

  private void showProgress(boolean show) {
    progressContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    if (show) {
      progressAnimator.start();
    } else {
      progressAnimator.cancel();
      progressBar.setProgress(0);
    }
  }

  private void startMainActivity() {
    startActivity(new Intent(this, MainActivity.class));
    finishAffinity();
  }

  private void startLogin() {
    startActivity(new Intent(this, LoginActivity.class));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (progressAnimator != null) {
      progressAnimator.cancel();
    }
  }
}
