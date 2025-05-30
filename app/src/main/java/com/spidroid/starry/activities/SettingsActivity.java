package com.spidroid.starry.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.ListPreference;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.spidroid.starry.R;
import com.spidroid.starry.auth.LoginActivity;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.viewmodels.UserViewModel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setDisplayShowHomeEnabled(true);
      getSupportActionBar().setTitle("Settings");
    }

    getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new SettingsFragment())
            .commit();
  }

  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }

  public static class SettingsFragment extends PreferenceFragmentCompat
          implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private UserViewModel userViewModel;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration userProfileListener;
    private UserModel currentUserModel;

    // Preferences
    private Preference usernamePreference;
    private Preference emailPreference;
    private Preference emailVerificationPreference;
    private Preference phoneNumberPreference;
    private Preference socialConnectionsPreference;
    private SwitchPreference privateAccountSwitch;
    private SwitchPreference showActivitySwitch;
    private SwitchPreference allowDmsSwitch;
    private Preference blockedUsersPreference;
    private SwitchPreference notifyMessagesSwitch;
    private SwitchPreference notifyCommentsSwitch;
    private SwitchPreference notifyRepostsSwitch;
    private Preference twoFactorPreference;
    private Preference trustedDevicesPreference;
    private Preference changePasswordPreference;
    private ListPreference appThemePreference;
    private Preference bugReportPreference;
    private Preference logoutPreference;
    private Preference aboutPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.root_preferences, rootKey);
      auth = FirebaseAuth.getInstance();
      db = FirebaseFirestore.getInstance();
      currentUser = auth.getCurrentUser();
      userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

      findPreferences();
      setupListeners();
      loadUserProfileAndObserve();
    }

    @Override
    public void onResume() {
      super.onResume();
      updateEmailVerificationUI();
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
      if (userProfileListener != null) {
        userProfileListener.remove();
      }
    }

    private void findPreferences() {
      usernamePreference = findPreference("username");
      emailPreference = findPreference("email");
      emailVerificationPreference = findPreference("email_verification");
      phoneNumberPreference = findPreference("phone_number");
      socialConnectionsPreference = findPreference("social_connections");
      privateAccountSwitch = findPreference("private_account");
      showActivitySwitch = findPreference("show_activity");
      allowDmsSwitch = findPreference("allow_dms");
      blockedUsersPreference = findPreference("blocked_users");
      notifyMessagesSwitch = findPreference("notify_messages");
      notifyCommentsSwitch = findPreference("notify_comments");
      notifyRepostsSwitch = findPreference("notify_reposts");
      twoFactorPreference = findPreference("two_factor");
      trustedDevicesPreference = findPreference("trusted_devices");
      changePasswordPreference = findPreference("change_password");
      appThemePreference = findPreference("app_theme");
      bugReportPreference = findPreference("bug report");
      logoutPreference = findPreference("logout");
      aboutPreference = findPreference("about");
    }

    private void setupListeners() {
      if (emailVerificationPreference != null) emailVerificationPreference.setOnPreferenceClickListener(this);
      if (socialConnectionsPreference != null) socialConnectionsPreference.setOnPreferenceClickListener(this);
      if (blockedUsersPreference != null) blockedUsersPreference.setOnPreferenceClickListener(this);
      if (twoFactorPreference != null) twoFactorPreference.setOnPreferenceClickListener(this);
      if (trustedDevicesPreference != null) trustedDevicesPreference.setOnPreferenceClickListener(this);
      if (changePasswordPreference != null) changePasswordPreference.setOnPreferenceClickListener(this);
      if (bugReportPreference != null) bugReportPreference.setOnPreferenceClickListener(this);
      if (logoutPreference != null) logoutPreference.setOnPreferenceClickListener(this);
      if (aboutPreference != null) aboutPreference.setOnPreferenceClickListener(this);

      if (privateAccountSwitch != null) privateAccountSwitch.setOnPreferenceChangeListener(this);
      if (showActivitySwitch != null) showActivitySwitch.setOnPreferenceChangeListener(this);
      if (allowDmsSwitch != null) allowDmsSwitch.setOnPreferenceChangeListener(this);
      if (notifyMessagesSwitch != null) notifyMessagesSwitch.setOnPreferenceChangeListener(this);
      if (notifyCommentsSwitch != null) notifyCommentsSwitch.setOnPreferenceChangeListener(this);
      if (notifyRepostsSwitch != null) notifyRepostsSwitch.setOnPreferenceChangeListener(this);

      if (appThemePreference != null) appThemePreference.setOnPreferenceChangeListener(this);
    }

    private void loadUserProfileAndObserve() {
      if (currentUser == null) {
        Toast.makeText(getContext(), "Please log in to manage settings.", Toast.LENGTH_SHORT).show();
        disableAllPreferences();
        return;
      }

      userProfileListener = db.collection("users")
              .document(currentUser.getUid())
              .addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) {
                  Log.w("SettingsFragment", "Listen failed.", e);
                  Toast.makeText(getContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                  return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                  currentUserModel = documentSnapshot.toObject(UserModel.class);
                  if (currentUserModel != null) {
                    updateAccountSection(currentUserModel);
                    updatePrivacySection(currentUserModel);
                    updateNotificationSection(currentUserModel);
                    updateEmailVerificationUI();
                  }
                }
              });
    }

    private void updateAccountSection(UserModel user) {
      if (usernamePreference != null) usernamePreference.setSummary(user.getUsername());
      if (emailPreference != null) emailPreference.setSummary(user.getEmail());
      if (phoneNumberPreference != null) {
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
          phoneNumberPreference.setSummary(user.getPhoneNumber());
        } else {
          phoneNumberPreference.setSummary("Not set");
        }
      }
    }

    private void updatePrivacySection(UserModel user) {
      if (privateAccountSwitch != null) privateAccountSwitch.setChecked(user.getPrivacySettings().isPrivateAccount());
      if (showActivitySwitch != null) showActivitySwitch.setChecked(user.getPrivacySettings().isShowActivityStatus());
      if (allowDmsSwitch != null) allowDmsSwitch.setChecked(user.getPrivacySettings().isAllowDMsFromEveryone());
    }

    private void updateNotificationSection(UserModel user) {
      if (user.getNotificationPreferences() == null) {
        user.setNotificationPreferences(new HashMap<>());
      }
      if (notifyMessagesSwitch != null) notifyMessagesSwitch.setChecked(user.getNotificationPreferences().getOrDefault("messages", true));
      if (notifyCommentsSwitch != null) notifyCommentsSwitch.setChecked(user.getNotificationPreferences().getOrDefault("comments", true));
      if (notifyRepostsSwitch != null) notifyRepostsSwitch.setChecked(user.getNotificationPreferences().getOrDefault("reposts", true));
    }

    private void updateEmailVerificationUI() {
      if (currentUser != null && emailVerificationPreference != null) {
        currentUser.reload().addOnCompleteListener(task -> {
          if (task.isSuccessful()) {
            currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
              emailVerificationPreference.setTitle("Email Verified");
              emailVerificationPreference.setSummary("Your email address is verified.");
              emailVerificationPreference.setEnabled(false);
            } else {
              emailVerificationPreference.setTitle("Verify Email");
              emailVerificationPreference.setSummary("Your email is not verified. Tap to send verification email.");
              emailVerificationPreference.setEnabled(true);
            }
          } else {
            Log.e("SettingsFragment", "Failed to reload user email verification status: " + task.getException().getMessage());
            emailVerificationPreference.setEnabled(false);
            emailVerificationPreference.setSummary("Failed to load email verification status.");
          }
        });
      } else if (emailVerificationPreference != null) {
        emailVerificationPreference.setEnabled(false);
        emailVerificationPreference.setSummary("Log in to verify your email.");
      }
    }

    private void disableAllPreferences() {
      if (usernamePreference != null) usernamePreference.setEnabled(false);
      if (emailPreference != null) emailPreference.setEnabled(false);
      if (emailVerificationPreference != null) emailVerificationPreference.setEnabled(false);
      if (phoneNumberPreference != null) phoneNumberPreference.setEnabled(false);
      if (socialConnectionsPreference != null) socialConnectionsPreference.setEnabled(false);
      if (privateAccountSwitch != null) privateAccountSwitch.setEnabled(false);
      if (showActivitySwitch != null) showActivitySwitch.setEnabled(false);
      if (allowDmsSwitch != null) allowDmsSwitch.setEnabled(false);
      if (blockedUsersPreference != null) blockedUsersPreference.setEnabled(false);
      if (notifyMessagesSwitch != null) notifyMessagesSwitch.setEnabled(false);
      if (notifyCommentsSwitch != null) notifyCommentsSwitch.setEnabled(false);
      if (notifyRepostsSwitch != null) notifyRepostsSwitch.setEnabled(false);
      if (twoFactorPreference != null) twoFactorPreference.setEnabled(false);
      if (trustedDevicesPreference != null) trustedDevicesPreference.setEnabled(false);
      if (changePasswordPreference != null) changePasswordPreference.setEnabled(false);
      if (appThemePreference != null) appThemePreference.setEnabled(false);
      if (bugReportPreference != null) bugReportPreference.setEnabled(false);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      String key = preference.getKey();
      if (key == null) return false;

      if (currentUser == null) {
        Toast.makeText(getContext(), "Please log in to use this feature.", Toast.LENGTH_SHORT).show();
        return true;
      }

      switch (key) {
        case "email_verification":
          sendVerificationEmail();
          return true;
        case "social_connections":
          Toast.makeText(getContext(), "Social connections coming soon!", Toast.LENGTH_SHORT).show();
          return true;
        case "blocked_users":
          Toast.makeText(getContext(), "Blocked users management coming soon!", Toast.LENGTH_SHORT).show();
          return true;
        case "two_factor":
          Toast.makeText(getContext(), "Two-Factor Authentication setup coming soon!", Toast.LENGTH_SHORT).show();
          return true;
        case "trusted_devices":
          Toast.makeText(getContext(), "Trusted devices management coming soon!", Toast.LENGTH_SHORT).show();
          return true;
        case "change_password":
          showChangePasswordDialog();
          return true;
        case "bug report":
          sendBugReportEmail();
          return true;
        case "logout":
          showLogoutConfirmation();
          return true;
        case "about":
          showAboutDialog();
          return true;
        default:
          return false;
      }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
      String key = preference.getKey();
      if (key == null || currentUserModel == null || currentUser == null) {
        Toast.makeText(getContext(), "Cannot save changes. Please log in.", Toast.LENGTH_SHORT).show();
        return false;
      }

      boolean success = false;
      Map<String, Object> updates = new HashMap<>();

      // تحديث إعدادات الخصوصية
      if ("private_account".equals(key)) {
        currentUserModel.getPrivacySettings().setPrivateAccount((boolean) newValue); // التعديل هنا
        updates.put("privacySettings.privateAccount", (boolean) newValue);
        success = true;
      } else if ("show_activity".equals(key)) {
        currentUserModel.getPrivacySettings().setShowActivityStatus((boolean) newValue);
        updates.put("privacySettings.showActivityStatus", (boolean) newValue);
        success = true;
      } else if ("allow_dms".equals(key)) {
        currentUserModel.getPrivacySettings().setAllowDMsFromEveryone((boolean) newValue); // التعديل هنا
        updates.put("privacySettings.allowDMsFromEveryone", (boolean) newValue);
        success = true;
      }
      // تحديث إعدادات الإشعارات
      else if ("notify_messages".equals(key)) {
        currentUserModel.getNotificationPreferences().put("messages", (Boolean) newValue);
        updates.put("notificationPreferences.messages", (Boolean) newValue);
        success = true;
      } else if ("notify_comments".equals(key)) {
        currentUserModel.getNotificationPreferences().put("comments", (Boolean) newValue);
        updates.put("notificationPreferences.comments", (Boolean) newValue);
        success = true;
      } else if ("notify_reposts".equals(key)) {
        currentUserModel.getNotificationPreferences().put("reposts", (Boolean) newValue);
        updates.put("notificationPreferences.reposts", (Boolean) newValue);
        success = true;
      }
      // تغيير الثيم
      else if ("app_theme".equals(key)) {
        String selectedTheme = (String) newValue;
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit()
                .putString("app_theme", selectedTheme)
                .apply();
        Toast.makeText(getContext(), "Theme changed. Restart app to apply.", Toast.LENGTH_LONG).show();
        success = true;
      }

      if (success && !updates.isEmpty()) {
        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Settings updated.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                  Log.e("SettingsFragment", "Failed to update settings: " + e.getMessage());
                  Toast.makeText(getContext(), "Failed to save settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        return true;
      }
      return false;
    }

    private void sendVerificationEmail() {
      if (currentUser == null) {
        Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
        return;
      }

      if (currentUser.isEmailVerified()) {
        Toast.makeText(getContext(), "Your email is already verified!", Toast.LENGTH_SHORT).show();
        updateEmailVerificationUI();
        return;
      }

      currentUser.sendEmailVerification()
              .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                  Toast.makeText(getContext(), "Verification email sent to " + currentUser.getEmail(), Toast.LENGTH_LONG).show();
                  if (emailVerificationPreference != null) {
                    emailVerificationPreference.setEnabled(false);
                    emailVerificationPreference.setSummary("Verification email sent. Please check your inbox and refresh.");
                  }
                } else {
                  Toast.makeText(getContext(), "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                  Log.e("SettingsFragment", "sendEmailVerification failed", task.getException());
                }
              });
    }

    private void showChangePasswordDialog() {
      if (currentUser == null) return;
      AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
      builder.setTitle("Change Password");

      final EditText newPasswordInput = new EditText(requireContext());
      newPasswordInput.setHint("New Password");
      newPasswordInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
      builder.setView(newPasswordInput);

      builder.setPositiveButton("Change", (dialog, which) -> {
        String newPassword = newPasswordInput.getText().toString().trim();
        if (newPassword.length() < 6) {
          Toast.makeText(getContext(), "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
          return;
        }
        currentUser.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                  if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
                  } else {
                    Toast.makeText(getContext(), "Failed to change password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SettingsFragment", "Password change failed", task.getException());
                  }
                });
      });
      builder.setNegativeButton("Cancel", null);
      builder.show();
    }

    private void sendBugReportEmail() {
      Intent intent = new Intent(Intent.ACTION_SENDTO);
      intent.setData(Uri.parse("mailto:support@yourdomain.com"));
      intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Starry App");
      intent.putExtra(Intent.EXTRA_TEXT, "Device: Android\nApp Version: 1.0\n\nDescription of the bug:\n\nSteps to reproduce:\n\n");
      try {
        startActivity(Intent.createChooser(intent, "Send bug report via..."));
      } catch (android.content.ActivityNotFoundException ex) {
        Toast.makeText(getContext(), "No email app found.", Toast.LENGTH_SHORT).show();
      }
    }

    private void showAboutDialog() {
      AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
      builder.setTitle("About Starry");
      builder.setMessage("Starry App\nVersion 1.0\n\nDeveloped by [Your Name/Company]\n© 2023 All rights reserved.");
      builder.setPositiveButton("OK", null);
      builder.show();
    }

    private void showLogoutConfirmation() {
      new AlertDialog.Builder(requireContext())
              .setTitle("Logout")
              .setMessage("Are you sure you want to log out?")
              .setPositiveButton("Yes", (dialog, which) -> performLogout())
              .setNegativeButton("Cancel", null)
              .show();
    }

    private void performLogout() {
      auth.signOut();

      db.clearPersistence()
              .addOnCompleteListener(
                      task -> {
                        if (task.isSuccessful()) {
                          Log.d("Logout", "Firestore persistence cleared.");
                        } else {
                          Log.w("Logout", "Failed to clear Firestore persistence.");
                        }
                      });

      clearSharedPreferences();

      userViewModel.clearUserData();

      Intent intent = new Intent(requireContext(), LoginActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);

      requireActivity().finish();
    }

    private void clearSharedPreferences() {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
      SharedPreferences.Editor editor = prefs.edit();
      editor.remove("user_id");
      editor.remove("last_login");
      editor.apply();
    }
  }
}