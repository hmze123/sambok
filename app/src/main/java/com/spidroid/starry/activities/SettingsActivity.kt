package com.spidroid.starry.activities

import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
            getSupportActionBar().setDisplayShowHomeEnabled(true)
            getSupportActionBar().setTitle("Settings")
        }

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    override fun onSupportNavigateUp(): kotlin.Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        androidx.preference.Preference.OnPreferenceClickListener,
        androidx.preference.Preference.OnPreferenceChangeListener {
        private var userViewModel: UserViewModel? = null
        private var auth: FirebaseAuth? = null
        private var db: FirebaseFirestore? = null
        private var currentUser: FirebaseUser? = null
        private var userProfileListener: ListenerRegistration? = null
        private var currentUserModel: UserModel? = null

        // Preferences
        private var usernamePreference: androidx.preference.Preference? = null
        private var emailPreference: androidx.preference.Preference? = null
        private var emailVerificationPreference: androidx.preference.Preference? = null
        private var phoneNumberPreference: androidx.preference.Preference? = null
        private var socialConnectionsPreference: androidx.preference.Preference? = null
        private var privateAccountSwitch: androidx.preference.SwitchPreference? = null
        private var showActivitySwitch: androidx.preference.SwitchPreference? = null
        private var allowDmsSwitch: androidx.preference.SwitchPreference? = null
        private var blockedUsersPreference: androidx.preference.Preference? = null
        private var notifyMessagesSwitch: androidx.preference.SwitchPreference? = null
        private var notifyCommentsSwitch: androidx.preference.SwitchPreference? = null
        private var notifyRepostsSwitch: androidx.preference.SwitchPreference? = null
        private var twoFactorPreference: androidx.preference.Preference? = null
        private var trustedDevicesPreference: androidx.preference.Preference? = null
        private var changePasswordPreference: androidx.preference.Preference? = null
        private var appThemePreference: androidx.preference.ListPreference? = null
        private var bugReportPreference: androidx.preference.Preference? = null
        private var logoutPreference: androidx.preference.Preference? = null
        private var aboutPreference: androidx.preference.Preference? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: kotlin.String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            currentUser = auth.getCurrentUser()
            userViewModel =
                ViewModelProvider(requireActivity()).get<UserViewModel>(UserViewModel::class.java)

            findPreferences()
            setupListeners()
            loadUserProfileAndObserve()
        }

        override fun onResume() {
            super.onResume()
            updateEmailVerificationUI()
        }

        override fun onDestroy() {
            super.onDestroy()
            if (userProfileListener != null) {
                userProfileListener.remove()
            }
        }

        private fun findPreferences() {
            usernamePreference = findPreference<androidx.preference.Preference?>("username")
            emailPreference = findPreference<androidx.preference.Preference?>("email")
            emailVerificationPreference =
                findPreference<androidx.preference.Preference?>("email_verification")
            phoneNumberPreference = findPreference<androidx.preference.Preference?>("phone_number")
            socialConnectionsPreference =
                findPreference<androidx.preference.Preference?>("social_connections")
            privateAccountSwitch =
                findPreference<androidx.preference.SwitchPreference?>("private_account")
            showActivitySwitch =
                findPreference<androidx.preference.SwitchPreference?>("show_activity")
            allowDmsSwitch = findPreference<androidx.preference.SwitchPreference?>("allow_dms")
            blockedUsersPreference =
                findPreference<androidx.preference.Preference?>("blocked_users")
            notifyMessagesSwitch =
                findPreference<androidx.preference.SwitchPreference?>("notify_messages")
            notifyCommentsSwitch =
                findPreference<androidx.preference.SwitchPreference?>("notify_comments")
            notifyRepostsSwitch =
                findPreference<androidx.preference.SwitchPreference?>("notify_reposts")
            twoFactorPreference = findPreference<androidx.preference.Preference?>("two_factor")
            trustedDevicesPreference =
                findPreference<androidx.preference.Preference?>("trusted_devices")
            changePasswordPreference =
                findPreference<androidx.preference.Preference?>("change_password")
            appThemePreference = findPreference<androidx.preference.ListPreference?>("app_theme")
            bugReportPreference = findPreference<androidx.preference.Preference?>("bug report")
            logoutPreference = findPreference<androidx.preference.Preference?>("logout")
            aboutPreference = findPreference<androidx.preference.Preference?>("about")
        }

        private fun setupListeners() {
            if (emailVerificationPreference != null) emailVerificationPreference!!.setOnPreferenceClickListener(
                this
            )
            if (socialConnectionsPreference != null) socialConnectionsPreference!!.setOnPreferenceClickListener(
                this
            )
            if (blockedUsersPreference != null) blockedUsersPreference!!.setOnPreferenceClickListener(
                this
            )
            if (twoFactorPreference != null) twoFactorPreference!!.setOnPreferenceClickListener(this)
            if (trustedDevicesPreference != null) trustedDevicesPreference!!.setOnPreferenceClickListener(
                this
            )
            if (changePasswordPreference != null) changePasswordPreference!!.setOnPreferenceClickListener(
                this
            )
            if (bugReportPreference != null) bugReportPreference!!.setOnPreferenceClickListener(this)
            if (logoutPreference != null) logoutPreference!!.setOnPreferenceClickListener(this)
            if (aboutPreference != null) aboutPreference!!.setOnPreferenceClickListener(this)

            if (privateAccountSwitch != null) privateAccountSwitch!!.setOnPreferenceChangeListener(
                this
            )
            if (showActivitySwitch != null) showActivitySwitch!!.setOnPreferenceChangeListener(this)
            if (allowDmsSwitch != null) allowDmsSwitch!!.setOnPreferenceChangeListener(this)
            if (notifyMessagesSwitch != null) notifyMessagesSwitch!!.setOnPreferenceChangeListener(
                this
            )
            if (notifyCommentsSwitch != null) notifyCommentsSwitch!!.setOnPreferenceChangeListener(
                this
            )
            if (notifyRepostsSwitch != null) notifyRepostsSwitch!!.setOnPreferenceChangeListener(
                this
            )

            if (appThemePreference != null) appThemePreference!!.setOnPreferenceChangeListener(this)
        }

        private fun loadUserProfileAndObserve() {
            if (currentUser == null) {
                Toast.makeText(
                    getContext(),
                    "Please log in to manage settings.",
                    Toast.LENGTH_SHORT
                ).show()
                disableAllPreferences()
                return
            }

            userProfileListener = db.collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener({ documentSnapshot, e ->
                    if (e != null) {
                        android.util.Log.w("SettingsFragment", "Listen failed.", e)
                        Toast.makeText(
                            getContext(),
                            "Failed to load profile data.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addSnapshotListener
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        currentUserModel = documentSnapshot.toObject(UserModel::class.java)
                        if (currentUserModel != null) {
                            updateAccountSection(currentUserModel)
                            updatePrivacySection(currentUserModel)
                            updateNotificationSection(currentUserModel)
                            updateEmailVerificationUI()
                        }
                    }
                })
        }

        private fun updateAccountSection(user: UserModel) {
            if (usernamePreference != null) usernamePreference.setSummary(user.getUsername())
            if (emailPreference != null) emailPreference.setSummary(user.getEmail())
            if (phoneNumberPreference != null) {
                if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                    phoneNumberPreference.setSummary(user.getPhoneNumber())
                } else {
                    phoneNumberPreference!!.setSummary("Not set")
                }
            }
        }

        private fun updatePrivacySection(user: UserModel) {
            if (privateAccountSwitch != null) privateAccountSwitch!!.setChecked(
                user.getPrivacySettings().isPrivateAccount()
            )
            if (showActivitySwitch != null) showActivitySwitch!!.setChecked(
                user.getPrivacySettings().isShowActivityStatus()
            )
            if (allowDmsSwitch != null) allowDmsSwitch!!.setChecked(
                user.getPrivacySettings().isAllowDMsFromEveryone()
            )
        }

        private fun updateNotificationSection(user: UserModel) {
            if (user.getNotificationPreferences() == null) {
                user.setNotificationPreferences(java.util.HashMap<kotlin.String?, kotlin.Boolean?>())
            }
            if (notifyMessagesSwitch != null) notifyMessagesSwitch!!.setChecked(
                user.getNotificationPreferences().getOrDefault("messages", true)
            )
            if (notifyCommentsSwitch != null) notifyCommentsSwitch!!.setChecked(
                user.getNotificationPreferences().getOrDefault("comments", true)
            )
            if (notifyRepostsSwitch != null) notifyRepostsSwitch!!.setChecked(
                user.getNotificationPreferences().getOrDefault("reposts", true)
            )
        }

        private fun updateEmailVerificationUI() {
            if (currentUser != null && emailVerificationPreference != null) {
                currentUser.reload().addOnCompleteListener({ task ->
                    if (task.isSuccessful()) {
                        currentUser = FirebaseAuth.getInstance().getCurrentUser()
                        if (currentUser != null && currentUser.isEmailVerified()) {
                            emailVerificationPreference!!.setTitle("Email Verified")
                            emailVerificationPreference!!.setSummary("Your email address is verified.")
                            emailVerificationPreference!!.setEnabled(false)
                        } else {
                            emailVerificationPreference!!.setTitle("Verify Email")
                            emailVerificationPreference!!.setSummary("Your email is not verified. Tap to send verification email.")
                            emailVerificationPreference!!.setEnabled(true)
                        }
                    } else {
                        android.util.Log.e(
                            "SettingsFragment",
                            "Failed to reload user email verification status: " + task.getException()
                                .getMessage()
                        )
                        emailVerificationPreference!!.setEnabled(false)
                        emailVerificationPreference!!.setSummary("Failed to load email verification status.")
                    }
                })
            } else if (emailVerificationPreference != null) {
                emailVerificationPreference!!.setEnabled(false)
                emailVerificationPreference!!.setSummary("Log in to verify your email.")
            }
        }

        private fun disableAllPreferences() {
            if (usernamePreference != null) usernamePreference!!.setEnabled(false)
            if (emailPreference != null) emailPreference!!.setEnabled(false)
            if (emailVerificationPreference != null) emailVerificationPreference!!.setEnabled(false)
            if (phoneNumberPreference != null) phoneNumberPreference!!.setEnabled(false)
            if (socialConnectionsPreference != null) socialConnectionsPreference!!.setEnabled(false)
            if (privateAccountSwitch != null) privateAccountSwitch!!.setEnabled(false)
            if (showActivitySwitch != null) showActivitySwitch!!.setEnabled(false)
            if (allowDmsSwitch != null) allowDmsSwitch!!.setEnabled(false)
            if (blockedUsersPreference != null) blockedUsersPreference!!.setEnabled(false)
            if (notifyMessagesSwitch != null) notifyMessagesSwitch!!.setEnabled(false)
            if (notifyCommentsSwitch != null) notifyCommentsSwitch!!.setEnabled(false)
            if (notifyRepostsSwitch != null) notifyRepostsSwitch!!.setEnabled(false)
            if (twoFactorPreference != null) twoFactorPreference!!.setEnabled(false)
            if (trustedDevicesPreference != null) trustedDevicesPreference!!.setEnabled(false)
            if (changePasswordPreference != null) changePasswordPreference!!.setEnabled(false)
            if (appThemePreference != null) appThemePreference!!.setEnabled(false)
            if (bugReportPreference != null) bugReportPreference!!.setEnabled(false)
        }

        override fun onPreferenceClick(preference: androidx.preference.Preference): kotlin.Boolean {
            val key = preference.getKey()
            if (key == null) return false

            if (currentUser == null) {
                Toast.makeText(
                    getContext(),
                    "Please log in to use this feature.",
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }

            when (key) {
                "email_verification" -> {
                    sendVerificationEmail()
                    return true
                }

                "social_connections" -> {
                    Toast.makeText(
                        getContext(),
                        "Social connections coming soon!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                "blocked_users" -> {
                    Toast.makeText(
                        getContext(),
                        "Blocked users management coming soon!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                "two_factor" -> {
                    Toast.makeText(
                        getContext(),
                        "Two-Factor Authentication setup coming soon!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                "trusted_devices" -> {
                    Toast.makeText(
                        getContext(),
                        "Trusted devices management coming soon!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }

                "change_password" -> {
                    showChangePasswordDialog()
                    return true
                }

                "bug report" -> {
                    sendBugReportEmail()
                    return true
                }

                "logout" -> {
                    showLogoutConfirmation()
                    return true
                }

                "about" -> {
                    showAboutDialog()
                    return true
                }

                else -> return false
            }
        }

        override fun onPreferenceChange(
            preference: androidx.preference.Preference,
            newValue: kotlin.Any?
        ): kotlin.Boolean {
            val key = preference.getKey()
            if (key == null || currentUserModel == null || currentUser == null) {
                Toast.makeText(
                    getContext(),
                    "Cannot save changes. Please log in.",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            var success = false
            val updates: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?> =
                java.util.HashMap<kotlin.String?, kotlin.Any?>()

            // تحديث إعدادات الخصوصية
            if ("private_account" == key) {
                currentUserModel.getPrivacySettings()
                    .setPrivateAccount(newValue as kotlin.Boolean) // التعديل هنا
                updates.put("privacySettings.privateAccount", newValue)
                success = true
            } else if ("show_activity" == key) {
                currentUserModel.getPrivacySettings()
                    .setShowActivityStatus(newValue as kotlin.Boolean)
                updates.put("privacySettings.showActivityStatus", newValue)
                success = true
            } else if ("allow_dms" == key) {
                currentUserModel.getPrivacySettings()
                    .setAllowDMsFromEveryone(newValue as kotlin.Boolean) // التعديل هنا
                updates.put("privacySettings.allowDMsFromEveryone", newValue)
                success = true
            } else if ("notify_messages" == key) {
                currentUserModel.getNotificationPreferences()
                    .put("messages", newValue as kotlin.Boolean?)
                updates.put("notificationPreferences.messages", newValue)
                success = true
            } else if ("notify_comments" == key) {
                currentUserModel.getNotificationPreferences()
                    .put("comments", newValue as kotlin.Boolean?)
                updates.put("notificationPreferences.comments", newValue)
                success = true
            } else if ("notify_reposts" == key) {
                currentUserModel.getNotificationPreferences()
                    .put("reposts", newValue as kotlin.Boolean?)
                updates.put("notificationPreferences.reposts", newValue)
                success = true
            } else if ("app_theme" == key) {
                val selectedTheme = newValue as kotlin.String?
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit()
                    .putString("app_theme", selectedTheme)
                    .apply()
                Toast.makeText(
                    getContext(),
                    "Theme changed. Restart app to apply.",
                    Toast.LENGTH_LONG
                ).show()
                success = true
            }

            if (success && !updates.isEmpty()) {
                db.collection("users").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener({ aVoid ->
                        Toast.makeText(
                            getContext(),
                            "Settings updated.",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                    .addOnFailureListener({ e ->
                        android.util.Log.e(
                            "SettingsFragment",
                            "Failed to update settings: " + e.getMessage()
                        )
                        Toast.makeText(
                            getContext(),
                            "Failed to save settings: " + e.getMessage(),
                            Toast.LENGTH_LONG
                        ).show()
                    })
                return true
            }
            return false
        }

        private fun sendVerificationEmail() {
            if (currentUser == null) {
                Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
                return
            }

            if (currentUser.isEmailVerified()) {
                Toast.makeText(getContext(), "Your email is already verified!", Toast.LENGTH_SHORT)
                    .show()
                updateEmailVerificationUI()
                return
            }

            currentUser.sendEmailVerification()
                .addOnCompleteListener({ task ->
                    if (task.isSuccessful()) {
                        Toast.makeText(
                            getContext(),
                            "Verification email sent to " + currentUser.getEmail(),
                            Toast.LENGTH_LONG
                        ).show()
                        if (emailVerificationPreference != null) {
                            emailVerificationPreference!!.setEnabled(false)
                            emailVerificationPreference!!.setSummary("Verification email sent. Please check your inbox and refresh.")
                        }
                    } else {
                        Toast.makeText(
                            getContext(),
                            "Failed to send verification email: " + task.getException()
                                .getMessage(),
                            Toast.LENGTH_LONG
                        ).show()
                        android.util.Log.e(
                            "SettingsFragment",
                            "sendEmailVerification failed",
                            task.getException()
                        )
                    }
                })
        }

        private fun showChangePasswordDialog() {
            if (currentUser == null) return
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Change Password")

            val newPasswordInput: EditText = EditText(requireContext())
            newPasswordInput.setHint("New Password")
            newPasswordInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
            builder.setView(newPasswordInput)

            builder.setPositiveButton(
                "Change",
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    val newPassword = newPasswordInput.getText().toString().trim { it <= ' ' }
                    if (newPassword.length < 6) {
                        Toast.makeText(
                            getContext(),
                            "Password must be at least 6 characters.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setPositiveButton
                    }
                    currentUser.updatePassword(newPassword)
                        .addOnCompleteListener({ task ->
                            if (task.isSuccessful()) {
                                Toast.makeText(
                                    getContext(),
                                    "Password changed successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    getContext(),
                                    "Failed to change password: " + task.getException()
                                        .getMessage(),
                                    Toast.LENGTH_LONG
                                ).show()
                                android.util.Log.e(
                                    "SettingsFragment",
                                    "Password change failed",
                                    task.getException()
                                )
                            }
                        })
                })
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }

        private fun sendBugReportEmail() {
            val intent: Intent = Intent(Intent.ACTION_SENDTO)
            intent.setData(android.net.Uri.parse("mailto:support@yourdomain.com"))
            intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Starry App")
            intent.putExtra(
                Intent.EXTRA_TEXT,
                "Device: Android\nApp Version: 1.0\n\nDescription of the bug:\n\nSteps to reproduce:\n\n"
            )
            try {
                startActivity(Intent.createChooser(intent, "Send bug report via..."))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(getContext(), "No email app found.", Toast.LENGTH_SHORT).show()
            }
        }

        private fun showAboutDialog() {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("About Starry")
            builder.setMessage("Starry App\nVersion 1.0\n\nDeveloped by [Your Name/Company]\n© 2023 All rights reserved.")
            builder.setPositiveButton("OK", null)
            builder.show()
        }

        private fun showLogoutConfirmation() {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton(
                    "Yes",
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> performLogout() })
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun performLogout() {
            auth.signOut()

            db.clearPersistence()
                .addOnCompleteListener(
                    { task ->
                        if (task.isSuccessful()) {
                            android.util.Log.d("Logout", "Firestore persistence cleared.")
                        } else {
                            android.util.Log.w("Logout", "Failed to clear Firestore persistence.")
                        }
                    })

            clearSharedPreferences()

            userViewModel.clearUserData()

            val intent: Intent = Intent(requireContext(), LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

            requireActivity().finish()
        }

        private fun clearSharedPreferences() {
            val prefs: SharedPreferences =
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.remove("user_id")
            editor.remove("last_login")
            editor.apply()
        }
    }
}