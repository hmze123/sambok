package com.spidroid.starry.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.spidroid.starry.R
import com.spidroid.starry.adapters.MediaPreviewAdapter
import com.spidroid.starry.databinding.ActivityComposePostBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.utils.LinkPreviewFetcher
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Pattern

class ComposePostActivity : AppCompatActivity(), MediaPreviewAdapter.OnMediaInteraction {

    private lateinit var binding: ActivityComposePostBinding
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val storage: FirebaseStorage by lazy { Firebase.storage }

    private var currentUserModel: UserModel? = null
    private lateinit var mediaPreviewAdapter: MediaPreviewAdapter
    private val selectedMediaUris = mutableListOf<MediaPreviewAdapter.MediaItem>()
    private var postToQuote: PostModel? = null

    private val urlPattern: Pattern =
        Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)")
    private var currentLinkPreview: PostModel.LinkPreview? = null

    private lateinit var pickMultipleMedia: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComposePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postToQuote = intent.getParcelableExtra(EXTRA_QUOTE_POST)

        initializeMediaPicker()
        initializeFirebase()
        setupUI()
        loadCurrentUser()
    }

    private fun initializeFirebase() {
        if (auth.currentUser == null) {
            Toast.makeText(this, getString(R.string.user_not_authenticated_error), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeMediaPicker() {
        pickMultipleMedia = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                handleMediaSelection(uris)
            }
        }
    }

    private fun setupUI() {
        setupMediaPreviewRecyclerView()
        setupListeners()
        binding.btnPost.isEnabled = false // Disable initially

        if (postToQuote != null) {
            showQuotedPostPreview(postToQuote!!)
        }
    }

    private fun showQuotedPostPreview(post: PostModel) {
        val quotedBinding = binding.layoutQuotedPost
        quotedBinding.root.visibility = View.VISIBLE
        quotedBinding.tvQuotedAuthorName.text = post.authorDisplayName ?: post.authorUsername
        quotedBinding.tvQuotedContent.text = post.content
        quotedBinding.ivQuotedVerified.visibility = if (post.isAuthorVerified) View.VISIBLE else View.GONE

        Glide.with(this)
            .load(post.authorAvatarUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .into(quotedBinding.ivQuotedAuthorAvatar)
    }

    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        binding.btnPost.text = getString(R.string.loading_text)
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserModel = document.toObject(UserModel::class.java)
                    updatePostButtonState()
                } else {
                    handleUserLoadError("User profile not found.")
                }
            }
            .addOnFailureListener { e ->
                handleUserLoadError(e.message ?: "Failed to load profile.")
            }
    }

    private fun handleUserLoadError(errorMessage: String) {
        Log.e(TAG, errorMessage)
        Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show()
        binding.btnPost.text = getString(R.string.error_button_text)
        binding.btnPost.isEnabled = false
    }

    private fun setupListeners() {
        binding.btnClose.setOnClickListener { finish() }
        binding.btnPost.setOnClickListener { createNewPost() }

        binding.btnAddPhoto.setOnClickListener { pickMultipleMedia.launch(arrayOf("image/*")) }
        binding.btnAddVideo.setOnClickListener {
            if (hasVideo()) {
                Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show()
            } else {
                pickMultipleMedia.launch(arrayOf("video/*"))
            }
        }

        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                updateCharCount(s.length)
                checkAndFetchLinkPreview(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {
                updatePostButtonState()
            }
        })

        binding.layoutLinkPreview.btnRemoveLink.setOnClickListener { removeLinkPreview() }
    }

    private fun hasVideo(): Boolean {
        return selectedMediaUris.any { item ->
            when (item) {
                is MediaPreviewAdapter.MediaItem.New -> contentResolver.getType(item.uri)?.startsWith("video/") == true
                is MediaPreviewAdapter.MediaItem.Existing -> PostModel.VIDEO_EXTENSIONS.any { item.url.endsWith(it, true) }
            }
        }
    }

    private fun setupMediaPreviewRecyclerView() {
        mediaPreviewAdapter = MediaPreviewAdapter(this)
        binding.rvMediaPreview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvMediaPreview.adapter = mediaPreviewAdapter
    }

    private fun handleMediaSelection(uris: List<Uri>) {
        if (selectedMediaUris.size + uris.size > 4) {
            Toast.makeText(this, getString(R.string.max_media_limit, 4), Toast.LENGTH_SHORT).show()
            return
        }

        for (uri in uris) {
            val mimeType = contentResolver.getType(uri)
            if (mimeType?.startsWith("video/") == true && hasVideo()) {
                Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show()
                continue
            }
            selectedMediaUris.add(MediaPreviewAdapter.MediaItem.New(uri))
        }
        mediaPreviewAdapter.submitList(selectedMediaUris.toMutableList())
        binding.rvMediaPreview.visibility = if (selectedMediaUris.isNotEmpty()) View.VISIBLE else View.GONE
        updatePostButtonState()
    }

    override fun onMediaRemoved(item: MediaPreviewAdapter.MediaItem, position: Int) {
        if (position < selectedMediaUris.size) {
            selectedMediaUris.removeAt(position)
            mediaPreviewAdapter.submitList(selectedMediaUris.toMutableList())
            if (selectedMediaUris.isEmpty()) {
                binding.rvMediaPreview.visibility = View.GONE
            }
        }
        updatePostButtonState()
    }

    override fun onMediaClicked(item: MediaPreviewAdapter.MediaItem, position: Int) {
        Toast.makeText(this, "Media preview clicked.", Toast.LENGTH_SHORT).show()
    }

    private fun updateCharCount(count: Int) {
        binding.tvCharCount.text = getString(R.string.char_count_format, count, PostModel.MAX_CONTENT_LENGTH)
        val color = if (count > PostModel.MAX_CONTENT_LENGTH) R.color.error_red else R.color.text_secondary
        binding.tvCharCount.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun checkAndFetchLinkPreview(content: String) {
        val url = urlPattern.matcher(content).takeIf { it.find() }?.group(0)
        if (url != null) {
            if (currentLinkPreview?.url != url) {
                showLinkPreviewLoading()
                lifecycleScope.launch {
                    val result = LinkPreviewFetcher.fetch(url)
                    result.onSuccess { preview ->
                        if (preview.title != null) {
                            currentLinkPreview = preview
                            bindLinkPreview(preview)
                        } else {
                            onLinkPreviewError()
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "Link preview fetch failed: ${e.message}", e)
                        onLinkPreviewError()
                    }
                }
            }
        } else {
            removeLinkPreview()
        }
    }

    private fun showLinkPreviewLoading() {
        binding.layoutLinkPreview.root.visibility = View.VISIBLE
        binding.layoutLinkPreview.tvLinkTitle.text = getString(R.string.loading_preview)
        binding.layoutLinkPreview.tvLinkDescription.text = ""
        binding.layoutLinkPreview.tvLinkDomain.text = ""
        binding.layoutLinkPreview.ivLinkImage.setImageResource(R.drawable.ic_cover_placeholder)
    }

    private fun bindLinkPreview(preview: PostModel.LinkPreview) {
        binding.layoutLinkPreview.tvLinkTitle.text = preview.title
        binding.layoutLinkPreview.tvLinkDescription.text = preview.description
        binding.layoutLinkPreview.tvLinkDomain.text = preview.url?.let { Uri.parse(it).host }
        Glide.with(this).load(preview.imageUrl).placeholder(R.drawable.ic_cover_placeholder).into(binding.layoutLinkPreview.ivLinkImage)
    }

    private fun onLinkPreviewError() { removeLinkPreview() }
    private fun removeLinkPreview() {
        currentLinkPreview = null
        binding.layoutLinkPreview.root.visibility = View.GONE
    }

    private fun updatePostButtonState() {
        val hasContent = binding.etContent.text.trim().isNotEmpty()
        val hasMedia = selectedMediaUris.isNotEmpty()
        val contentWithinLimit = binding.etContent.text.length <= PostModel.MAX_CONTENT_LENGTH
        val canPost = (hasContent || hasMedia || postToQuote != null) && contentWithinLimit && currentUserModel != null
        binding.btnPost.isEnabled = canPost
        binding.btnPost.text = getString(if (currentUserModel != null) R.string.post else R.string.loading_text)
    }

    private fun createNewPost() {
        val content = binding.etContent.text.toString().trim()
        if (content.isEmpty() && selectedMediaUris.isEmpty() && currentLinkPreview == null && postToQuote == null) {
            Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show()
            return
        }
        showProgress(true)
        if (selectedMediaUris.isNotEmpty()) {
            uploadMediaAndCreatePost(content)
        } else {
            savePostToFirestore(content, emptyList())
        }
    }

    private fun uploadMediaAndCreatePost(content: String) {
        val uploadTasks = selectedMediaUris.mapNotNull { mediaItem ->
            (mediaItem as? MediaPreviewAdapter.MediaItem.New)?.uri?.let { uri ->
                val userId = currentUserModel?.userId ?: "unknown_user"
                val fileExtension = contentResolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: "jpg"
                val ref = storage.reference.child("post_media/$userId/${UUID.randomUUID()}.$fileExtension")
                ref.putFile(uri).continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    ref.downloadUrl
                }
            }
        }
        Tasks.whenAllSuccess<Uri>(uploadTasks).addOnSuccessListener { uris ->
            val mediaUrls = uris.map { it.toString() }
            savePostToFirestore(content, mediaUrls)
        }.addOnFailureListener { e -> handleUploadFailure(e) }
    }

    private fun extractHashtags(content: String): List<String> {
        val hashtags = mutableListOf<String>()
        val pattern = Pattern.compile("#(\\w+)")
        val matcher = pattern.matcher(content)
        while (matcher.find()) {
            matcher.group(1)?.let { hashtags.add(it.lowercase()) }
        }
        return hashtags
    }

    private fun savePostToFirestore(content: String, mediaUrls: List<String>) {
        val user = currentUserModel ?: return
        val hashtags = extractHashtags(content)
        val quotedPostData = postToQuote?.let {
            PostModel.QuotedPost(
                postId = it.postId,
                authorId = it.authorId,
                authorUsername = it.authorUsername,
                authorDisplayName = it.authorDisplayName,
                authorAvatarUrl = it.authorAvatarUrl,
                isAuthorVerified = it.isAuthorVerified,
                content = it.content,
                createdAt = it.createdAt
            )
        }

        val post = PostModel(
            authorId = user.userId,
            content = content,
            authorUsername = user.username,
            authorDisplayName = user.displayName,
            authorAvatarUrl = user.profileImageUrl,
            isAuthorVerified = user.isVerified,
            mediaUrls = mediaUrls.toMutableList(),
            linkPreviews = currentLinkPreview?.let { mutableListOf(it) } ?: mutableListOf(),
            hashtags = hashtags,
            mentions = mutableListOf(),
            quotedPost = quotedPostData,
            createdAt = Date()
        ).apply {
            contentType = if (mediaUrls.isNotEmpty()) {
                if (hasVideo()) PostModel.TYPE_VIDEO else PostModel.TYPE_IMAGE
            } else {
                PostModel.TYPE_TEXT
            }
        }

        db.collection("posts").add(post)
            .addOnSuccessListener { docRef ->
                docRef.update("postId", docRef.id, "createdAt", FieldValue.serverTimestamp())
                Toast.makeText(this, R.string.post_success, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding post to Firestore", e)
                Toast.makeText(this, "Post failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener { showProgress(false) }
    }

    private fun showProgress(show: Boolean) {
        binding.progressContainer.root.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnPost.isEnabled = !show
    }

    private fun handleUploadFailure(e: Exception) {
        showProgress(false)
        Toast.makeText(this, "Media upload failed: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e(TAG, "Media upload failed", e)
    }

    companion object {
        const val EXTRA_QUOTE_POST = "EXTRA_QUOTE_POST"
        private const val TAG = "ComposePostActivity"
    }
}
