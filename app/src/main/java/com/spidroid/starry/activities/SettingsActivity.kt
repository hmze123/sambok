// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/SettingsActivity.kt
package com.spidroid.starry.activities

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText // ✨ تم إضافة هذا الاستيراد
import android.widget.Toast // ✨ تم إضافة هذا الاستيراد
import androidx.appcompat.app.AlertDialog // ✨ تم إضافة هذا الاستيراد
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider // ✨ تم إضافة هذا الاستيراد
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.auth.LoginActivity
import com.spidroid.starry.databinding.ActivitySettingsBinding
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        Preference.OnPreferenceClickListener,
        Preference.OnPreferenceChangeListener {

        private val auth: FirebaseAuth by lazy { Firebase.auth }
        private val db: FirebaseFirestore by lazy { Firebase.firestore }
        private var currentUser: FirebaseUser? = null
        private var userProfileListener: ListenerRegistration? = null
        private var currentUserModel: UserModel? = null

        private lateinit var userViewModel: UserViewModel

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<Preference>("bookmarks")?.onPreferenceClickListener = this
            findPreference<Preference>("create_page")?.onPreferenceClickListener = this
            currentUser = auth.currentUser
            userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

            setupListeners()
            loadUserProfile()
        }

        override fun onResume() {
            super.onResume()
            updateEmailVerificationUI()
        }

        override fun onDestroy() {
            super.onDestroy()
            userProfileListener?.remove()
        }

        private fun setupListeners() {
            findPreference<Preference>("email_verification")?.onPreferenceClickListener = this
            findPreference<Preference>("change_password")?.onPreferenceClickListener = this
            findPreference<Preference>("logout")?.onPreferenceClickListener = this
            // Add other click listeners here...
            findPreference<Preference>("bug report")?.onPreferenceClickListener = this // ✨ إضافة مستمع لـ "Report a bug"

            findPreference<SwitchPreference>("private_account")?.onPreferenceChangeListener = this
            findPreference<SwitchPreference>("show_activity")?.onPreferenceChangeListener = this
            findPreference<ListPreference>("app_theme")?.onPreferenceChangeListener = this // ✨ إضافة مستمع لـ "app_theme"
            // Add other change listeners here...
        }

        private fun loadUserProfile() {
            val userId = currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "Please log in to manage settings.", Toast.LENGTH_SHORT).show()
                preferenceScreen.isEnabled = false
                return
            }

            userProfileListener = db.collection("users").document(userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        currentUserModel = snapshot.toObject(UserModel::class.java)
                        currentUserModel?.let { updateAllPreferences(it) }
                    }
                }
        }

        private fun updateAllPreferences(user: UserModel) {
            findPreference<Preference>("username")?.summary = user.username
            findPreference<Preference>("email")?.summary = user.email
            findPreference<Preference>("phone_number")?.summary = user.phoneNumber ?: "Not set" // ✨ عرض رقم الهاتف
            findPreference<SwitchPreference>("private_account")?.isChecked = user.privacySettings.privateAccount
            findPreference<SwitchPreference>("show_activity")?.isChecked = user.privacySettings.showActivityStatus
            findPreference<SwitchPreference>("allow_dms")?.isChecked = user.privacySettings.allowDMsFromEveryone // ✨ عرض حالة السماح بالرسائل
            findPreference<ListPreference>("app_theme")?.value = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("app_theme", "bluesky") // ✨ عرض السمة الحالية
        }

        private fun updateEmailVerificationUI() {
            val emailPref = findPreference<Preference>("email_verification") ?: return
            currentUser?.reload()?.addOnCompleteListener {
                if (isAdded) { // Ensure fragment is still attached
                    val isVerified = auth.currentUser?.isEmailVerified ?: false
                    emailPref.isEnabled = !isVerified
                    emailPref.title = if (isVerified) "Email Verified" else "Verify Email"
                    emailPref.summary = if (isVerified) "Your email address is verified." else "Tap to send verification email."
                }
            }
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            if (currentUser == null) {
                Toast.makeText(context, "Please log in.", Toast.LENGTH_SHORT).show()
                return true
            }

            when (preference.key) {
                "bookmarks" -> startActivity(Intent(requireContext(), BookmarksActivity::class.java)) // --- السطر الجديد ---
                "email_verification" -> sendVerificationEmail()
                "change_password" -> showChangePasswordDialog()
                "logout" -> showLogoutConfirmation()
                "bug report" -> sendBugReportEmail() // ✨ معالجة النقر على "Report a bug"
                "social_connections" -> Toast.makeText(context, "Social connections feature coming soon!", Toast.LENGTH_SHORT).show() // ✨ معالجة النقر على "Social connections"
                "trusted_devices" -> Toast.makeText(context, "Trusted devices feature coming soon!", Toast.LENGTH_SHORT).show() // ✨ معالجة النقر على "Trusted devices"
                "blocked_users" -> Toast.makeText(context, "Blocked users feature coming soon!", Toast.LENGTH_SHORT).show() // ✨ معالجة النقر على "Blocked users"
                "about" -> Toast.makeText(context, "About Starry: Version 1.0", Toast.LENGTH_SHORT).show() // ✨ معالجة النقر على "About Starry"
                "two_factor" -> Toast.makeText(context, "Two-Factor Authentication feature coming soon!", Toast.LENGTH_SHORT).show() // ✨ معالجة النقر على "Two-Factor Authentication"
                "notify_messages" -> Toast.makeText(context, "Notification settings coming soon!", Toast.LENGTH_SHORT).show()
                "notify_comments" -> Toast.makeText(context, "Notification settings coming soon!", Toast.LENGTH_SHORT).show()
                "notify_reposts" -> Toast.makeText(context, "Notification settings coming soon!", Toast.LENGTH_SHORT).show()
                "create_page" -> {
                    startActivity(Intent(requireContext(), CreatePageActivity::class.java))
                    true
                }
                // Handle other clicks...
            }
            return true
        }

        override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
            val userId = currentUser?.uid ?: return false
            val key = preference.key
            val updateData = mutableMapOf<String, Any>()

            when (key) {
                "private_account" -> updateData["privacySettings.privateAccount"] = newValue
                "show_activity" -> updateData["privacySettings.showActivityStatus"] = newValue
                "allow_dms" -> updateData["privacySettings.allowDMsFromEveryone"] = newValue // ✨ تحديث إعداد السماح بالرسائل المباشرة
                "app_theme" -> {
                    val selectedTheme = newValue as String
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                        .putString("app_theme", selectedTheme)
                        .apply()
                    Toast.makeText(context,"Theme changed. Restart app to apply.", Toast.LENGTH_LONG).show()
                    activity?.recreate() // ✨ إعادة إنشاء النشاط لتطبيق السمة على الفور
                    return true // No Firestore update needed, so we return early
                }
                else -> return false
            }

            if (updateData.isNotEmpty()) {
                db.collection("users").document(userId).update(updateData)
                    .addOnSuccessListener { Log.d(TAG, "Setting '$key' updated.") }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to update setting.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to update setting '$key'", e)
                    }
            }
            return true
        }

        private fun sendVerificationEmail() {
            currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Verification email sent.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to send email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun showChangePasswordDialog() {
            val newPasswordInput = EditText(requireContext()).apply {
                hint = "New Password"
                inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Add padding to the EditText
            val container = android.widget.FrameLayout(requireContext())
            val params = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val margin = (20 * resources.displayMetrics.density).toInt()
            params.setMargins(margin, 0, margin, 0)
            newPasswordInput.layoutParams = params
            container.addView(newPasswordInput)

            AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(container)
                .setPositiveButton("Change") { _, _ ->
                    val newPassword = newPasswordInput.text.toString().trim()
                    if (newPassword.length >= 6) {
                        currentUser?.updatePassword(newPassword)?.addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(context, "Password updated successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed: ${it.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun showLogoutConfirmation() {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ -> performLogout() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun performLogout() {
            userViewModel.clearUserData()
            auth.signOut()
            db.clearPersistence().addOnCompleteListener {
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                activity?.finish()
            }
        }

        private fun sendBugReportEmail() {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Only email apps should handle this.
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@starry.com")) // Replace with your support email
                putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Starry App")
                putExtra(Intent.EXTRA_TEXT, "Please describe the bug:\n\nApp Version: 1.0\nDevice: ${Build.MODEL} (${Build.VERSION.RELEASE})\n\n")
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
            }
        }

        companion object {
            private const val TAG = "SettingsFragment"
        }
    }
}