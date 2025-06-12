package com.spidroid.starry.activities

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.spidroid.starry.R
import com.spidroid.starry.adapters.CommentAdapter
import com.spidroid.starry.databinding.ActivityPostDetailBinding
import com.spidroid.starry.models.CommentModel
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.CommentUiState
import com.spidroid.starry.viewmodels.CommentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PostDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailBinding
    private lateinit var commentViewModel: CommentViewModel
    private lateinit var commentAdapter: CommentAdapter

    private var post: PostModel? = null
    private var currentUser: FirebaseUser? = null
    private var currentUserModel: UserModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val receivedPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_POST, PostModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_POST)
        }

        val postId = intent.getStringExtra("postId")

        if (receivedPost != null) {
            this.post = receivedPost
            initializeActivity()
        } else if (!postId.isNullOrEmpty()) {
            fetchPostFromId(postId)
        } else {
            // --[ تم التعديل هنا ]--
            Log.e(TAG, getString(R.string.log_error_no_post_data))
            Toast.makeText(this, getString(R.string.error_post_data_missing), Toast.LENGTH_LONG).show()
            finish()
            return
        }
    }

    private fun fetchPostFromId(postId: String) {
        FirebaseFirestore.getInstance().collection("posts").document(postId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    this.post = document.toObject(PostModel::class.java)?.apply { this.postId = document.id }
                    initializeActivity()
                } else {
                    Toast.makeText(this, getString(R.string.toast_post_not_found), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.error_loading_post, e.message), Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun initializeActivity() {
        if (this.post == null) return

        currentUser = FirebaseAuth.getInstance().currentUser
        commentViewModel = ViewModelProvider(this)[CommentViewModel::class.java]

        setupToolbar()
        bindPostData()
        setupCommentsRecyclerView()
        setupCommentObservers()
        setupInputSection()

        loadCurrentUserProfile()
        commentViewModel.loadComments(post?.postId)
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvAppName.text = getString(R.string.post_detail_title)
    }

    private fun bindPostData() {
        val currentPost = post ?: return
        with(binding.includedPostLayout) {
            tvAuthorName.text = currentPost.authorDisplayName ?: currentPost.authorUsername
            // --[ تم التعديل هنا ]--
            tvUsername.text = "@${currentPost.authorUsername ?: getString(R.string.unknown_user)}"
            tvPostContent.text = currentPost.content ?: ""
            ivVerified.visibility = if (currentPost.isAuthorVerified) View.VISIBLE else View.GONE
            interactionContainer.visibility = View.GONE

            Glide.with(this@PostDetailActivity)
                .load(currentPost.authorAvatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAuthorAvatar)
        }
    }

    private fun setupCommentsRecyclerView() {
        val currentPost = post ?: return
        commentAdapter = CommentAdapter(this, currentPost.authorId, CommentInteractionListenerImpl())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = commentAdapter
    }

    private fun setupCommentObservers() {
        commentViewModel.commentState.observe(this) { state ->
            when (state) {
                is CommentUiState.Loading -> {
                    // يمكنك إظهار مؤشر تحميل هنا
                }
                is CommentUiState.Success -> {
                    commentAdapter.submitList(state.comments)
                    binding.commentstxt.text = getString(R.string.comments_count, state.comments.size)
                }
                is CommentUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCurrentUserProfile() {
        if (currentUser == null) {
            binding.inputSection.postInput.hint = getString(R.string.login_to_comment)
            binding.inputSection.postInput.isEnabled = false
            return
        }

        binding.inputSection.postInput.hint = getString(R.string.loading_user_info)
        FirebaseFirestore.getInstance().collection("users").document(currentUser!!.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserModel = document.toObject(UserModel::class.java)
                    binding.inputSection.postInput.hint = getString(R.string.post_your_reply)
                    updatePostButtonState()
                } else {
                    handleProfileLoadError(R.string.toast_user_profile_not_found)
                }
            }
            .addOnFailureListener { e ->
                // --[ تم التعديل هنا ]--
                Log.e(TAG, getString(R.string.log_error_load_user_profile_failed), e)
                handleProfileLoadError(R.string.error_loading_profile)
            }
    }

    private fun handleProfileLoadError(stringResId: Int) {
        Toast.makeText(this, getString(stringResId), Toast.LENGTH_SHORT).show()
        binding.inputSection.postInput.isEnabled = false
        binding.inputSection.postInput.hint = getString(R.string.error_loading_profile)
    }

    private fun setupInputSection() {
        with(binding.inputSection) {
            charCounter.text = CommentModel.MAX_CONTENT_LENGTH.toString()
            postButton.isEnabled = false

            addMedia.visibility = View.GONE
            addGif.visibility = View.GONE
            addPoll.visibility = View.GONE

            postInput.addTextChangedListener {
                updatePostButtonState()
                val remaining = CommentModel.MAX_CONTENT_LENGTH - (it?.length ?: 0)
                charCounter.text = remaining.toString()
            }

            postButton.setOnClickListener { postComment() }
        }
    }

    private fun updatePostButtonState() {
        val hasText = binding.inputSection.postInput.text.trim().isNotEmpty()
        binding.inputSection.postButton.isEnabled = hasText && currentUserModel != null
    }

    private fun postComment() {
        val content = binding.inputSection.postInput.text.toString().trim()
        val currentPost = post
        val user = currentUserModel

        if (content.isEmpty()) {
            Toast.makeText(this, getString(R.string.comment_empty_error), Toast.LENGTH_SHORT).show()
            return
        }
        if (user == null) {
            Toast.makeText(this, getString(R.string.error_user_data_not_loaded), Toast.LENGTH_SHORT).show()
            return
        }
        if (currentPost?.postId == null) {
            Toast.makeText(this, getString(R.string.error_post_data_missing), Toast.LENGTH_SHORT).show()
            return
        }

        val commentData: Map<String, Any> = mapOf(
            "content" to content,
            "authorId" to user.userId,
            "authorDisplayName" to (user.displayName ?: ""),
            "authorUsername" to user.username,
            "authorAvatarUrl" to (user.profileImageUrl ?: ""),
            "authorVerified" to user.isVerified,
            "timestamp" to FieldValue.serverTimestamp() as Any,
            "likeCount" to 0,
            "repliesCount" to 0,
            "parentPostId" to currentPost.postId!!
        )

        commentViewModel.addComment(currentPost.postId, commentData)

        binding.inputSection.postInput.text.clear()
        val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private inner class CommentInteractionListenerImpl : CommentAdapter.CommentInteractionListener {
        override fun onLikeClicked(comment: CommentModel) {
            commentViewModel.toggleLike(post?.postId, comment)
        }

        override fun onReplyClicked(comment: CommentModel) {
            binding.inputSection.postInput.hint = getString(R.string.replying_to_user, comment.authorUsername)
            binding.inputSection.postInput.requestFocus()
        }

        override fun onAuthorClicked(authorId: String) {
            val intent = Intent(this@PostDetailActivity, ProfileActivity::class.java)
            intent.putExtra("userId", authorId)
            startActivity(intent)
        }

        override fun onShowRepliesClicked(comment: CommentModel) {
            commentViewModel.toggleReplies(comment)
        }

        override fun onDeleteComment(comment: CommentModel) {
            if (currentUser?.uid != comment.authorId) {
                Toast.makeText(this@PostDetailActivity, getString(R.string.error_delete_own_comment), Toast.LENGTH_SHORT).show()
                return
            }
            androidx.appcompat.app.AlertDialog.Builder(this@PostDetailActivity)
                .setTitle(getString(R.string.delete_comment_dialog_title))
                .setMessage(getString(R.string.delete_comment_dialog_message))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    commentViewModel.deleteComment(post?.postId, comment)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        override fun onReportComment(comment: CommentModel) {
            Toast.makeText(this@PostDetailActivity, getString(R.string.toast_comment_reported), Toast.LENGTH_SHORT).show()
            val intent = Intent(this@PostDetailActivity, ReportActivity::class.java).apply {
                putExtra(ReportActivity.EXTRA_REPORTED_ITEM_ID, comment.commentId)
                putExtra(ReportActivity.EXTRA_REPORT_TYPE, "comment")
                putExtra(ReportActivity.EXTRA_REPORTED_AUTHOR_ID, comment.authorId)
                putExtra("postId", post?.postId)
            }
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_POST: String = "EXTRA_POST"
        private const val TAG = "PostDetailActivity"
    }
}