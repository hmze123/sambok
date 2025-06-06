package com.spidroid.starry.activities

import com.google.firebase.auth.FirebaseAuth

class EditProfileActivity : AppCompatActivity() {
    private var binding: com.spidroid.starry.databinding.ActivityEditProfileBinding? = null
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var storage: FirebaseStorage? = null
    private var originalUser: UserModel? = null
    private var profileImageUri: android.net.Uri? = null
    private var coverImageUri: android.net.Uri? = null
    private var hasChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            com.spidroid.starry.databinding.ActivityEditProfileBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupUI()
        loadUserData()
        setupTextWatchers()
    }

    private fun setupUI() {
        binding!!.btnBack.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> onBackPressed() })
        binding!!.btnSave.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> saveProfile() })
        binding!!.btnAddSocial.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> showAddSocialDialog() })
        binding!!.btnChangePhoto.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            openImagePicker(
                "profile"
            )
        })
        binding!!.btnChangeCover.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            openImagePicker(
                "cover"
            )
        })
    }

    private fun loadUserData() {
        db.collection("users")
            .document(auth.getCurrentUser().getUid())
            .get()
            .addOnSuccessListener(
                { documentSnapshot ->
                    originalUser = documentSnapshot.toObject(UserModel::class.java)
                    if (originalUser != null) {
                        populateFields(originalUser)
                    }
                })
    }

    private fun populateFields(user: UserModel) {
        // Profile Image
        Glide.with(this)
            .load(user.getProfileImageUrl())
            .placeholder(R.drawable.ic_default_avatar)
            .into(binding!!.ivProfile)

        // Cover Image
        Glide.with(this)
            .load(user.getCoverImageUrl())
            .placeholder(R.drawable.ic_cover_placeholder)
            .into(binding!!.ivCover)

        // Text Fields
        binding!!.etDisplayName.setText(user.getDisplayName())
        binding!!.etUsername.setText(user.getUsername())
        binding!!.etBio.setText(user.getBio())

        // Privacy Settings
        binding!!.switchPrivate.setChecked(user.getPrivacySettings().isPrivateAccount())
        binding!!.switchActivity.setChecked(user.getPrivacySettings().isShowActivityStatus())

        // Social Links
        updateSocialLinksUI(user.getSocialLinks())
    }

    private fun updateSocialLinksUI(socialLinks: kotlin.collections.MutableMap<kotlin.String?, kotlin.String?>) {
        binding!!.layoutSocialLinks.removeAllViews()
        for (entry in socialLinks.entries) {
            addSocialLinkView(entry.key!!, entry.value)
        }
    }

    private fun addSocialLinkView(platform: kotlin.String, url: kotlin.String?) {
        val linkView: android.view.View =
            LayoutInflater.from(this).inflate(R.layout.item_social_link, null)
        linkView.setTag(platform)

        val icon = linkView.findViewById<android.widget.ImageView>(R.id.ivPlatform)
        val etUrl: EditText = linkView.findViewById<EditText>(R.id.etUrl)
        val btnRemove: ImageButton = linkView.findViewById<ImageButton>(R.id.btnRemove)

        icon.setImageResource(getPlatformIcon(platform))
        etUrl.setText(url)

        btnRemove.setOnClickListener(
            android.view.View.OnClickListener { v: android.view.View? ->
                binding!!.layoutSocialLinks.removeView(linkView)
                hasChanges = true
            })

        etUrl.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    hasChanges = true
                }

                override fun beforeTextChanged(
                    s: kotlin.CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: kotlin.CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }
            })

        binding!!.layoutSocialLinks.addView(linkView)
    }

    private fun getPlatformIcon(platform: kotlin.String): Int {
        when (platform.lowercase(java.util.Locale.getDefault())) {
            else -> return R.drawable.ic_link
        }
    }

    private fun showAddSocialDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Social Link")
            .setItems(
                kotlin.arrayOf<kotlin.String>("Twitter", "Instagram", "Facebook", "LinkedIn"),
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    var platform = ""
                    when (which) {
                        0 -> platform = "twitter"
                        1 -> platform = "instagram"
                        2 -> platform = "facebook"
                        3 -> platform = "linkedin"
                    }
                    addSocialLinkView(platform, "")
                    hasChanges = true
                })
            .show()
    }

    private fun openImagePicker(type: kotlin.String) {
        val intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        startActivityForResult(intent, if (type == "profile") 100 else 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri: android.net.Uri? = data.getData()
            if (requestCode == 100) {
                binding!!.ivProfile.setImageURI(uri)
                profileImageUri = uri
            } else if (requestCode == 101) {
                binding!!.ivCover.setImageURI(uri)
                coverImageUri = uri
            }
            hasChanges = true
        }
    }

    private fun setupTextWatchers() {
        val watcher: TextWatcher =
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    hasChanges = true
                }

                override fun beforeTextChanged(
                    s: kotlin.CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: kotlin.CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }
            }

        binding!!.etDisplayName.addTextChangedListener(watcher)
        binding!!.etUsername.addTextChangedListener(watcher)
        binding!!.etBio.addTextChangedListener(watcher)
    }

    override fun onBackPressed() {
        if (hasChanges) {
            showDiscardDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showDiscardDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton(
                "Discard",
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> finish() })
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfile() {
        if (!validateInputs()) return

        binding!!.progressBar.setVisibility(android.view.View.VISIBLE)

        // Upload images first if changed
        val uploadTasks: kotlin.collections.MutableList<com.google.android.gms.tasks.Task<android.net.Uri?>?> =
            java.util.ArrayList<com.google.android.gms.tasks.Task<android.net.Uri?>?>()
        if (profileImageUri != null) {
            uploadTasks.add(uploadImage(profileImageUri, "profile"))
        }
        if (coverImageUri != null) {
            uploadTasks.add(uploadImage(coverImageUri, "cover"))
        }

        // Proceed after all uploads succeed
        Tasks.whenAllSuccess<kotlin.Any?>(uploadTasks)
            .addOnSuccessListener(
                OnSuccessListener { urls: kotlin.collections.MutableList<kotlin.Any?>? ->
                    var profileImageUrl: kotlin.String? = null
                    var coverImageUrl: kotlin.String? = null
                    var index = 0

                    // Extract URLs based on upload order
                    if (profileImageUri != null && !urls!!.isEmpty()) {
                        profileImageUrl = (urls.get(index) as android.net.Uri).toString()
                        index++
                    }
                    if (coverImageUri != null && !urls!!.isEmpty() && index < urls.size) {
                        coverImageUrl = (urls.get(index) as android.net.Uri).toString()
                    }

                    // Update originalUser with new image URLs
                    if (profileImageUrl != null) {
                        originalUser.setProfileImageUrl(profileImageUrl)
                    }
                    if (coverImageUrl != null) {
                        originalUser.setCoverImageUrl(coverImageUrl)
                    }

                    // Update other fields from UI
                    updateOriginalUserFields()
                    updateUserPosts(originalUser)

                    // Save changes to Firestore with merge to avoid overwriting
                    db.collection("users")
                        .document(originalUser.getUserId())
                        .set(originalUser, SetOptions.merge())
                        .addOnSuccessListener(
                            { aVoid ->
                                Toast.makeText(
                                    this,
                                    "Profile updated",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                finish()
                            })
                        .addOnFailureListener(
                            { e ->
                                binding!!.progressBar.setVisibility(android.view.View.GONE)
                                Toast.makeText(
                                    this,
                                    "Update failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            })
                })
            .addOnFailureListener(
                OnFailureListener { e: java.lang.Exception? ->
                    binding!!.progressBar.setVisibility(android.view.View.GONE)
                    Toast.makeText(
                        this,
                        "Image upload failed: " + e!!.message,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                })
    }

    private fun updateOriginalUserFields() {
        // Update username, display name, and bio
        originalUser.setUsername(binding!!.etUsername.getText().toString().trim { it <= ' ' })
        originalUser.setDisplayName(binding!!.etDisplayName.getText().toString().trim { it <= ' ' })
        originalUser.setBio(binding!!.etBio.getText().toString().trim { it <= ' ' })

        // Update social links
        val socialLinks: kotlin.collections.MutableMap<kotlin.String?, kotlin.String?> =
            java.util.HashMap<kotlin.String?, kotlin.String?>()
        for (i in 0..<binding!!.layoutSocialLinks.getChildCount()) {
            val view = binding!!.layoutSocialLinks.getChildAt(i)
            val etUrl: EditText = view.findViewById<EditText>(R.id.etUrl)
            val platform =
                view.getTag() as kotlin.String? // Ensure platform is set as view tag when adding
            val url = etUrl.getText().toString().trim { it <= ' ' }
            if (!url.isEmpty()) {
                socialLinks.put(platform, url)
            }
        }
        originalUser.setSocialLinks(socialLinks)

        // Update privacy settings
        val privacySettings: PrivacySettings = originalUser.getPrivacySettings()
        privacySettings.setPrivateAccount(binding!!.switchPrivate.isChecked())
        privacySettings.setShowActivityStatus(binding!!.switchActivity.isChecked())
        originalUser.setPrivacySettings(privacySettings)
    }

    private fun uploadImage(
        uri: android.net.Uri?,
        type: kotlin.String?
    ): com.google.android.gms.tasks.Task<android.net.Uri?> {
        val ref: StorageReference =
            storage.getReference()
                .child(type + "_images")
                .child(auth.getCurrentUser().getUid())
                .child(java.lang.System.currentTimeMillis().toString() + ".jpg")

        return ref.putFile(uri).continueWithTask({ task -> ref.getDownloadUrl() })
    }

    private fun createUpdatedUser(): UserModel {
        val user: UserModel =
            UserModel(
                auth.getCurrentUser().getUid(),
                binding!!.etUsername.getText().toString(),
                originalUser.getEmail()
            )

        // Basic info
        user.setDisplayName(binding!!.etDisplayName.getText().toString())
        user.setBio(binding!!.etBio.getText().toString())

        // Social links
        val socialLinks: kotlin.collections.MutableMap<kotlin.String?, kotlin.String?> =
            java.util.HashMap<kotlin.String?, kotlin.String?>()
        for (i in 0..<binding!!.layoutSocialLinks.getChildCount()) {
            val view = binding!!.layoutSocialLinks.getChildAt(i)
            val etUrl: EditText = view.findViewById<EditText>(R.id.etUrl)
            val platform = view.getTag() as kotlin.String?
            if (!etUrl.getText().toString().isEmpty()) {
                socialLinks.put(platform, etUrl.getText().toString())
            }
        }
        user.setSocialLinks(socialLinks)

        // Privacy settings
        val privacy: PrivacySettings = originalUser.getPrivacySettings()
        privacy.setPrivateAccount(binding!!.switchPrivate.isChecked())
        privacy.setShowActivityStatus(binding!!.switchActivity.isChecked())
        user.setPrivacySettings(privacy)

        return user
    }

    private fun validateInputs(): kotlin.Boolean {
        val username = binding!!.etUsername.getText().toString().trim { it <= ' ' }
        if (username.isEmpty() || username.length < 4) {
            binding!!.etUsername.setError("Invalid username")
            return false
        }
        // Add more validation as needed
        return true
    }

    private fun updateUserPosts(updatedUser: UserModel) {
        db.collection("posts")
            .whereEqualTo("authorId", updatedUser.getUserId())
            .get()
            .addOnSuccessListener(
                { queryDocumentSnapshots ->
                    for (document in queryDocumentSnapshots.getDocuments()) {
                        val updates: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?> =
                            java.util.HashMap<kotlin.String?, kotlin.Any?>()
                        updates.put("authorUsername", updatedUser.getUsername())
                        updates.put("authorDisplayName", updatedUser.getDisplayName())
                        updates.put("authorAvatarUrl", updatedUser.getProfileImageUrl())

                        document.getReference().update(updates)
                    }
                })
    }
}
