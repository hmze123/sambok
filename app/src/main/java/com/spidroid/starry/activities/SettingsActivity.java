package com.spidroid.starry.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.auth.LoginActivity;
import com.spidroid.starry.viewmodels.UserViewModel;

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
      implements Preference.OnPreferenceClickListener {

    private UserViewModel userViewModel;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.root_preferences, rootKey);
      auth = FirebaseAuth.getInstance();
      db = FirebaseFirestore.getInstance();
      userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

      setupLogoutPreference();
    }

    private void setupLogoutPreference() {
      Preference logoutPreference = findPreference("logout");
      if (logoutPreference != null) {
        logoutPreference.setOnPreferenceClickListener(this);
      }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
      if (preference.getKey().equals("logout")) {
        showLogoutConfirmation();
        return true;
      }
      return false;
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
      // Clear Firebase authentication
      auth.signOut();

      // Clear Firestore cache (with logging)
      db.clearPersistence()
          .addOnCompleteListener(
              task -> {
                if (task.isSuccessful()) {
                  Log.d("Logout", "Firestore persistence cleared.");
                } else {
                  Log.w("Logout", "Failed to clear Firestore persistence.");
                }
              });

      // Clear SharedPreferences
      clearSharedPreferences();

      // Clear ViewModel data
      userViewModel.clearUserData();

      // Redirect to login activity
      Intent intent = new Intent(requireContext(), LoginActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);

      // Finish current activity
      requireActivity().finish();
    }

    private void clearSharedPreferences() {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
      prefs.edit().remove("user_id").remove("user_email").remove("last_login").apply();
    }
  }
}
