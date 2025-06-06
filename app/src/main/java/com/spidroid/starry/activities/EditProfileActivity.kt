package com.spidroid.starry.activities

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityEditProfileBinding
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.common.BottomSheetPostOptions.Companion.TAG
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private var originalUser: UserModel? = null
    private var profileImageUri: Uri? = null
    private var coverImageUri: Uri? = null
    private var hasChanges = false

    private lateinit var pickProfileImageLauncher: ActivityResultLauncher<String>
    private lateinit var pickCoverImageLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            Toast.makeText(this, "Authentication required.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeLaunchers()
        setupUI()
        loadUserData()
    }

    private fun initializeLaunchers() {
        pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                profileImageUri = it
                binding.ivProfile.setImageURI(it)
                hasChanges = true
            }
        }
        pickCoverImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                coverImageUri = it
                binding.ivCover.setImageURI(it)
                hasChanges = true
            }
        }
    }


    private fun setupUI() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnAddSocial.setOnClickListener { showAddSocialDialog() }
        binding.btnChangePhoto.setOnClickListener { pickProfileImageLauncher.launch("image/*") }
        binding.btnChangeCover.setOnClickListener { pickCoverImageLauncher.launch("image/*") }
    }

    private fun loadUserData() {
        binding.progressBar.visibility = View.VISIBLE
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        originalUser = document.toObject(UserModel::class.java)
                        originalUser?.let { populateFields(it) }
                    } else {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    binding.progressBar.visibility = View.GONE
                }
        }
    }

    private fun populateFields(user: UserModel) {
        Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_default_avatar).into(binding.ivProfile)
        Glide.with(this).load(user.coverImageUrl).placeholder(R.drawable.ic_cover_placeholder).into(binding.ivCover)

        binding.etDisplayName.setText(user.displayName)
        binding.etUsername.setText(user.username)
        binding.etBio.setText(user.bio)

        binding.switchPrivate.isChecked = user.privacySettings.privateAccount
        binding.switchActivity.isChecked = user.privacySettings.showActivityStatus

        updateSocialLinksUI(user.socialLinks)
        setupTextWatchers() // Setup watchers after populating fields
    }

    private fun updateSocialLinksUI(socialLinks: Map<String, String>) {
        binding.layoutSocialLinks.removeAllViews()
        socialLinks.forEach { (platform, url) ->
            addSocialLinkView(platform, url)
        }
    }

    private fun addSocialLinkView(platform: String, url: String) {
        val linkView = LayoutInflater.from(this).inflate(R.layout.item_social_link, binding.layoutSocialLinks, false)
        linkView.tag = platform

        val icon = linkView.findViewById<ImageView>(R.id.ivPlatform)
        val etUrl = linkView.findViewById<EditText>(R.id.etUrl)
        val btnRemove = linkView.findViewById<ImageButton>(R.id.btnRemove)

        icon.setImageResource(getPlatformIcon(platform))
        etUrl.setText(url)
        etUrl.addTextChangedListener(textWatcher)

        btnRemove.setOnClickListener {
            binding.layoutSocialLinks.removeView(linkView)
            hasChanges = true
        }

        binding.layoutSocialLinks.addView(linkView)
    }

    private fun getPlatformIcon(platform: String): Int {
        return when (platform.lowercase(Locale.getDefault())) {
            UserModel.SOCIAL_TWITTER -> R.drawable.ic_share // Replace with a proper Twitter icon
            UserModel.SOCIAL_INSTAGRAM -> R.drawable.ic_add_photo // Replace with a proper Instagram icon
            else -> R.drawable.ic_link
        }
    }

    private val textWatcher = object: TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            hasChanges = true
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun setupTextWatchers() {
        binding.etDisplayName.addTextChangedListener(textWatcher)
        binding.etUsername.addTextChangedListener(textWatcher)
        binding.etBio.addTextChangedListener(textWatcher)
    }

    private fun showAddSocialDialog() {
        val platforms = arrayOf("Twitter", "Instagram", "Facebook", "LinkedIn")
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Social Link")
            .setItems(platforms) { _, which ->
                val platform = platforms[which].lowercase(Locale.getDefault())
                addSocialLinkView(platform, "")
                hasChanges = true
            }
            .show()
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
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfile() {
        if (!validateInputs()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val profileImageTask = profileImageUri?.let { uploadImage(it, "profile") }
        val coverImageTask = coverImageUri?.let { uploadImage(it, "cover") }

        val allTasks = listOfNotNull(profileImageTask, coverImageTask)

        Tasks.whenAllSuccess<Uri>(allTasks).addOnSuccessListener { uris ->
            val profileUrl = if (profileImageTask != null) uris.firstOrNull()?.toString() else null
            val coverUrl = if (coverImageTask != null) uris.getOrNull(if(profileImageTask != null) 1 else 0)?.toString() else null

            updateFirestore(profileUrl, coverUrl)
        }.addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            binding.btnSave.isEnabled = true
            Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // If no new images to upload
        if (allTasks.isEmpty()){
            updateFirestore(null, null)
        }
    }

    private fun uploadImage(uri: Uri, type: String): Task<Uri> {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val ref: StorageReference = storage.reference.child("${type}_images/$userId/${System.currentTimeMillis()}.jpg")
        return ref.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            ref.downloadUrl
        }
    }

    private fun updateFirestore(newProfileUrl: String?, newCoverUrl: String?) {
        val user = originalUser ?: return
        val userId = user.userId ?: return

        user.displayName = binding.etDisplayName.text.toString().trim()
        user.username = binding.etUsername.text.toString().trim()
        user.bio = binding.etBio.text.toString().trim()

        user.privacySettings.privateAccount = binding.switchPrivate.isChecked
        user.privacySettings.showActivityStatus = binding.switchActivity.isChecked

        if (newProfileUrl != null) user.profileImageUrl = newProfileUrl
        if (newCoverUrl != null) user.coverImageUrl = newCoverUrl

        val socialLinks = mutableMapOf<String, String>()
        for (i in 0 until binding.layoutSocialLinks.childCount) {
            val view = binding.layoutSocialLinks.getChildAt(i)
            val etUrl = view.findViewById<EditText>(R.id.etUrl)
            val platform = view.tag as? String
            val url = etUrl.text.toString().trim()
            if (platform != null && url.isNotEmpty()) {
                socialLinks[platform] = url
            }
        }
        user.socialLinks = socialLinks

        db.collection("users").document(userId).set(user, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                updateUserPosts(user) // Update posts after profile is saved
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
    }

    private fun validateInputs(): Boolean {
        val username = binding.etUsername.text.toString().trim()
        if (username.length < 4) {
            binding.etUsername.error = "Username must be at least 4 characters"
            return false
        }
        return true
    }

    private fun updateUserPosts(updatedUser: UserModel) {
        val updates = mapOf(
            "authorUsername" to updatedUser.username,
            "authorDisplayName" to updatedUser.displayName,
            "authorAvatarUrl" to updatedUser.profileImageUrl
        )

        db.collection("posts").whereEqualTo("authorId", updatedUser.userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (document in querySnapshot.documents) {
                    batch.update(document.reference, updates)
                }
                batch.commit().addOnFailureListener { e->
                    Log.e(TAG, "Failed to update user's posts.", e)
                }
            }
    }
}