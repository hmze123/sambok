// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/PostDetailActivity.kt
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

        // استلام بيانات المنشور بطريقة آمنة
        post = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_POST, PostModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_POST)
        }


        // التحقق من صحة بيانات المنشور
        if (post?.postId.isNullOrEmpty()) {
            Log.e(TAG, "Post or Post ID received is null or empty.")
            Toast.makeText(this, getString(R.string.error_post_data_missing), Toast.LENGTH_LONG).show()
            finish()
            return
        }

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
        binding.tvAppName.text = getString(R.string.post)
    }

    private fun bindPostData() {
        val currentPost = post ?: return
        with(binding.includedPostLayout) {
            tvAuthorName.text = currentPost.authorDisplayName ?: currentPost.authorUsername
            tvUsername.text = "@${currentPost.authorUsername ?: "unknown"}"
            tvPostContent.text = currentPost.content ?: ""
            ivVerified.visibility = if (currentPost.isAuthorVerified) View.VISIBLE else View.GONE
            // إخفاء منطقة التفاعل الخاصة بالمنشور الرئيسي لأنها غير ضرورية في هذه الشاشة
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
                    // يمكنك إظهار مؤشر تحميل هنا إذا أردت
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
                    handleProfileLoadError(R.string.user_profile_not_found)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load current user profile", e)
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

            // إخفاء الأزرار غير المستخدمة في هذه الشاشة
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
            Toast.makeText(this, getString(R.string.error_user_data_not_loaded_wait), Toast.LENGTH_SHORT).show()
            return
        }
        if (currentPost?.postId == null) {
            Toast.makeText(this, getString(R.string.error_invalid_post_for_comment), Toast.LENGTH_SHORT).show()
            return
        }

        val commentData: Map<String, Any> = mapOf( // ✨ تم التصريح صراحةً بنوع الخريطة
            "content" to content,
            "authorId" to user.userId,
            "authorDisplayName" to (user.displayName ?: ""),
            "authorUsername" to user.username,
            "authorAvatarUrl" to (user.profileImageUrl ?: ""),
            "authorVerified" to user.isVerified,
            "timestamp" to FieldValue.serverTimestamp() as Any,
            "likeCount" to 0,
            "repliesCount" to 0,
            "parentPostId" to currentPost.postId!! // ✨ تم التأكيد على عدم كون القيمة null
        )

        commentViewModel.addComment(currentPost.postId, commentData)

        // مسح حقل الإدخال وإخفاء لوحة المفاتيح
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
                Toast.makeText(this@PostDetailActivity, getString(R.string.error_delete_own_comment_only), Toast.LENGTH_SHORT).show()
                return
            }
            androidx.appcompat.app.AlertDialog.Builder(this@PostDetailActivity)
                .setTitle(getString(R.string.delete_comment_title))
                .setMessage(getString(R.string.delete_comment_confirmation))
                .setPositiveButton(getString(R.string.delete_button)) { _, _ ->
                    commentViewModel.deleteComment(post?.postId, comment)
                }
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show()
        }

        override fun onReportComment(comment: CommentModel) {
            Toast.makeText(this@PostDetailActivity, getString(R.string.comment_reported_toast, comment.commentId), Toast.LENGTH_SHORT).show()
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