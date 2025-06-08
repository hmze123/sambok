package com.spidroid.starry.activities

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
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
    private val urlPattern: Pattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)")
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

    private fun setupUI() {
        setupMediaPreviewRecyclerView()
        setupListeners()
        binding.btnPost.isEnabled = false

        if (postToQuote != null) {
            showQuotedPostPreview(postToQuote!!)
        }
    }

    private fun showQuotedPostPreview(post: PostModel) {
        // الوصول إلى الـ views من خلال كائن الربط الخاص بالـ include
        val quotedBinding = binding.layoutQuotedPost
        quotedBinding.root.visibility = View.VISIBLE
        quotedBinding.tvQuotedAuthorName.text = post.authorDisplayName ?: post.authorUsername
        quotedBinding.tvQuotedContent.text = post.content
        quotedBinding.ivQuotedVerified.visibility = if (post.isAuthorVerified) View.VISIBLE else View.GONE
        Glide.with(this).load(post.authorAvatarUrl).placeholder(R.drawable.ic_default_avatar).into(quotedBinding.ivQuotedAuthorAvatar)
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
            hashtags = hashtags,
            quotedPost = quotedPostData,
            createdAt = Date()
        )
        db.collection("posts").add(post).addOnSuccessListener { docRef ->
            docRef.update("postId", docRef.id, "createdAt", FieldValue.serverTimestamp())
            Toast.makeText(this, R.string.post_success, Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error adding post", e)
        }.addOnCompleteListener { showProgress(false) }
    }

    private fun initializeFirebase() { /* ... */ }
    private fun initializeMediaPicker() { /* ... */ }
    private fun loadCurrentUser() { /* ... */ }
    private fun handleUserLoadError(errorMessage: String) { /* ... */ }
    private fun setupListeners() { /* ... */ }
    private fun hasVideo(): Boolean { return false }
    private fun setupMediaPreviewRecyclerView() { /* ... */ }
    private fun handleMediaSelection(uris: List<Uri>) { /* ... */ }
    override fun onMediaRemoved(item: MediaPreviewAdapter.MediaItem, position: Int) { /* ... */ }
    override fun onMediaClicked(item: MediaPreviewAdapter.MediaItem, position: Int) { /* ... */ }
    private fun updateCharCount(count: Int) { /* ... */ }
    private fun checkAndFetchLinkPreview(content: String) { /* ... */ }
    private fun showLinkPreviewLoading() { /* ... */ }
    private fun bindLinkPreview(preview: PostModel.LinkPreview) { /* ... */ }
    private fun onLinkPreviewError() { /* ... */ }
    private fun removeLinkPreview() { /* ... */ }
    private fun updatePostButtonState() { /* ... */ }
    private fun createNewPost() { /* ... */ }
    private fun uploadMediaAndCreatePost(content: String) { /* ... */ }
    private fun extractMentions(content: String): List<String> { return emptyList() }
    private fun extractHashtags(content: String): List<String> { return emptyList() }
    private fun showProgress(show: Boolean) { /* ... */ }
    private fun handleUploadFailure(e: Exception) { /* ... */ }

    companion object {
        const val EXTRA_QUOTE_POST = "EXTRA_QUOTE_POST"
        private const val TAG = "ComposePostActivity"
    }
}
