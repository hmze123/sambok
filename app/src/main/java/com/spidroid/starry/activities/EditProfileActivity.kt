package com.spidroid.starry.activities

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
import java.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

    private companion object {
        private const val TAG = "EditProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            Toast.makeText(this, getString(R.string.toast_authentication_required), Toast.LENGTH_SHORT).show()
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
        binding.switchPrivate.setOnCheckedChangeListener { _, _ -> hasChanges = true }
        binding.switchActivity.setOnCheckedChangeListener { _, _ -> hasChanges = true }
    }

    private fun loadUserData() {
        binding.progressContainer.root.visibility = View.VISIBLE
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        originalUser = document.toObject(UserModel::class.java)
                        originalUser?.let { populateFields(it) }
                    } else {
                        Toast.makeText(this, getString(R.string.toast_user_profile_not_found), Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.toast_failed_to_load_profile, e.message), Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    binding.progressContainer.root.visibility = View.GONE
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
        setupTextWatchers()
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
            UserModel.SOCIAL_TWITTER, UserModel.SOCIAL_INSTAGRAM, UserModel.SOCIAL_FACEBOOK, UserModel.SOCIAL_LINKEDIN -> R.drawable.ic_link
            else -> R.drawable.ic_link
        }
    }

    private val textWatcher = object : TextWatcher {
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
        val platforms = arrayOf(UserModel.SOCIAL_TWITTER.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            UserModel.SOCIAL_INSTAGRAM.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            UserModel.SOCIAL_FACEBOOK.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            UserModel.SOCIAL_LINKEDIN.replaceFirstChar { it.titlecase(Locale.getDefault()) })
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_social_link))
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
            .setTitle(getString(R.string.dialog_discard_changes_title))
            .setMessage(getString(R.string.dialog_discard_changes_message))
            .setPositiveButton(getString(R.string.dialog_button_discard)) { _, _ -> finish() }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun saveProfile() {
        if (!validateInputs()) return
        binding.progressContainer.root.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        val profileImageTask = profileImageUri?.let { uploadImage(it, "profile") }
        val coverImageTask = coverImageUri?.let { uploadImage(it, "cover") }
        val allTasks = listOfNotNull(profileImageTask, coverImageTask)
        Tasks.whenAllSuccess<Uri>(allTasks).addOnSuccessListener { uris ->
            var currentUriIndex = 0
            val profileUrl = if (profileImageTask != null) uris[currentUriIndex++].toString() else null
            val coverUrl = if (coverImageTask != null) uris[currentUriIndex].toString() else null
            updateFirestore(profileUrl, coverUrl)
        }.addOnFailureListener { e ->
            binding.progressContainer.root.visibility = View.GONE
            binding.btnSave.isEnabled = true
            Toast.makeText(this, getString(R.string.toast_image_upload_failed, e.message), Toast.LENGTH_SHORT).show()
        }
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
                if (android.util.Patterns.WEB_URL.matcher(url).matches()) {
                    socialLinks[platform] = url
                } else {
                    Toast.makeText(this, getString(R.string.error_invalid_url, platform, url), Toast.LENGTH_SHORT).show()
                    binding.progressContainer.root.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    return
                }
            }
        }
        user.socialLinks = socialLinks
        db.collection("users").document(userId).set(user, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.toast_profile_updated), Toast.LENGTH_SHORT).show()
                updateUserPosts(user)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.toast_update_failed, e.message), Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                binding.progressContainer.root.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
    }

    private fun validateInputs(): Boolean {
        val username = binding.etUsername.text.toString().trim()
        if (username.length < 4) {
            binding.etUsername.error = getString(R.string.toast_username_min_chars)
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
                    Log.e(TAG, getString(R.string.log_error_update_posts_failed), e)
                }
            }
    }
}