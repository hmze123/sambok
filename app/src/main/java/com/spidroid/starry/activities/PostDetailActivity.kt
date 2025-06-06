package com.spidroid.starry.activities

// استيراد Log
// ★ استيراد FieldValue
// استيراد Objects
import com.google.firebase.auth.FirebaseAuth

class PostDetailActivity : AppCompatActivity() {
    private var binding: com.spidroid.starry.databinding.ActivityPostDetailBinding? = null
    private var commentViewModel: CommentViewModel? = null
    private var commentAdapter: CommentAdapter? = null
    private var post: PostModel? = null
    private var currentUser: FirebaseUser? = null
    private var currentUserModel: UserModel? = null // افترض أنك ستحتاج إليه لإضافة تعليقات

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            com.spidroid.starry.databinding.ActivityPostDetailBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        // --- ★★ بداية التعديل والتحقق ★★ ---
        post = getIntent().getParcelableExtra<PostModel?>(PostDetailActivity.Companion.EXTRA_POST)
        if (post == null) {
            android.util.Log.e(PostDetailActivity.Companion.TAG, "PostModel received is null.")
            // استخدم string resource بدلاً من النص المباشر
            Toast.makeText(this, getString(R.string.error_post_not_found), Toast.LENGTH_LONG).show()
            finish() // إنهاء النشاط إذا لم يتم العثور على بيانات المنشور
            return
        }

        // تحقق من أن postId ليس null قبل المتابعة
        if (post.getPostId() == null || post.getPostId().isEmpty()) {
            android.util.Log.e(
                PostDetailActivity.Companion.TAG,
                "Post ID is null or empty in received PostModel. Author: " +
                        (if (post.getAuthorUsername() != null) post.getAuthorUsername() else "N/A") +
                        ", Content: " + (if (post.getContent() != null) post.getContent() else "N/A")
            )
            // استخدم string resource
            Toast.makeText(this, getString(R.string.error_invalid_post_id), Toast.LENGTH_LONG)
                .show()
            finish() // إنهاء النشاط إذا كان معرّف المنشور غير صالح
            return
        }

        // --- ★★ نهاية التعديل والتحقق ★★ ---
        currentUser = FirebaseAuth.getInstance().getCurrentUser()
        commentViewModel =
            ViewModelProvider(this).get<CommentViewModel>(CommentViewModel::class.java)

        setupToolbar()
        bindPostData()
        setupCommentsRecyclerView()
        setupCommentObservers()
        setupInputSection()

        loadCurrentUserProfile()
        commentViewModel.loadComments(post.getPostId())
    }

    private fun setupToolbar() {
        binding!!.ivBack.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> finish() })
        // يمكنك إضافة المزيد من إعدادات Toolbar هنا إذا لزم الأمر
        // مثلاً، عنوان الـ Toolbar يمكن أن يكون اسم صاحب المنشور أو "Post"
        binding!!.tvAppName.setText(if (post != null && post.getAuthorDisplayName() != null) post.getAuthorDisplayName() + "'s Post" else "Post")
    }

    private fun bindPostData() {
        if (post == null) {
            android.util.Log.e(
                PostDetailActivity.Companion.TAG,
                "PostModel is null in bindPostData. This should not happen."
            )
            return
        }

        binding!!.includedPostLayout.tvAuthorName.setText(if (post.getAuthorDisplayName() != null) post.getAuthorDisplayName() else post.getAuthorUsername())
        binding!!.includedPostLayout.tvUsername.setText("@" + (if (post.getAuthorUsername() != null) post.getAuthorUsername() else "unknown"))
        binding!!.includedPostLayout.tvPostContent.setText(if (post.getContent() != null) post.getContent() else "")

        // يمكنك إضافة عرض التاريخ والوقت هنا إذا أردت
        // binding.includedPostLayout.tvTimestamp.setText(formatTimestamp(post.getCreatedAt()));
        if (post.getAuthorAvatarUrl() != null && !post.getAuthorAvatarUrl().isEmpty()) {
            Glide.with(this)
                .load(post.getAuthorAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(binding!!.includedPostLayout.ivAuthorAvatar)
        } else {
            binding!!.includedPostLayout.ivAuthorAvatar.setImageResource(R.drawable.ic_default_avatar)
        }

        binding!!.includedPostLayout.ivVerified.setVisibility(if (post.isAuthorVerified()) android.view.View.VISIBLE else android.view.View.GONE)
        binding!!.includedPostLayout.interactionContainer.setVisibility(android.view.View.GONE)
    }

    private fun setupCommentsRecyclerView() {
        if (post == null || post.getPostId() == null) {
            android.util.Log.e(
                PostDetailActivity.Companion.TAG,
                "Cannot setup Comments RecyclerView: post or postId is null."
            )
            return
        }
        commentAdapter = CommentAdapter(this, post.getAuthorId(), CommentInteractionListenerImpl())
        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding!!.recyclerView.setAdapter(commentAdapter)
    }

    private fun setupCommentObservers() {
        commentViewModel.getVisibleComments().observe(
            this,
            androidx.lifecycle.Observer { comments: kotlin.collections.MutableList<CommentModel?>? ->
                if (comments != null && commentAdapter != null) {
                    commentAdapter.submitList(comments)
                    if (post != null) {
                        // استخدم string resource مع placeholder
                        binding!!.commentstxt.setText(
                            getString(
                                R.string.comments_count,
                                comments.size
                            )
                        )
                    }
                }
            })
    }

    private fun loadCurrentUserProfile() {
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener({ documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        currentUserModel = documentSnapshot.toObject(UserModel::class.java)
                        if (binding!!.inputSection.postButton != null && (binding!!.inputSection.postInput.getText().length > 0)) {
                            binding!!.inputSection.postButton.setEnabled(true)
                        }
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.error_loading_profile),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
                .addOnFailureListener({ e ->
                    android.util.Log.e(
                        PostDetailActivity.Companion.TAG,
                        "Failed to load current user profile",
                        e
                    )
                    Toast.makeText(
                        this,
                        getString(R.string.error_loading_profile_with_message, e.getMessage()),
                        Toast.LENGTH_SHORT
                    ).show()
                })
        } else {
            if (binding!!.inputSection.postButton != null) binding!!.inputSection.postButton.setEnabled(
                false
            )
            if (binding!!.inputSection.postInput != null) binding!!.inputSection.postInput.setHint(
                getString(R.string.login_to_comment)
            )
        }
    }

    private fun setupInputSection() {
        binding!!.inputSection.charCounter.setText(CommentModel.Companion.MAX_CONTENT_LENGTH.toString())
        binding!!.inputSection.postButton.setEnabled(false)

        if (currentUserModel == null && currentUser != null) {
            binding!!.inputSection.postInput.setHint(getString(R.string.loading_user_info))
        } else if (currentUser == null) {
            binding!!.inputSection.postInput.setHint(getString(R.string.login_to_comment))
            binding!!.inputSection.postInput.setEnabled(false)
        }


        binding!!.inputSection.postInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: kotlin.CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                binding!!.inputSection.postButton.setEnabled(
                    s.toString().trim { it <= ' ' }.length > 0 && currentUserModel != null
                )
                val remaining: Int = CommentModel.Companion.MAX_CONTENT_LENGTH - s.length
                binding!!.inputSection.charCounter.setText(remaining.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding!!.inputSection.postButton.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> postComment() })
        binding!!.inputSection.addMedia.setVisibility(android.view.View.GONE)
        binding!!.inputSection.addGif.setVisibility(android.view.View.GONE)
        binding!!.inputSection.addPoll.setVisibility(android.view.View.GONE)
    }

    private fun postComment() {
        val content = binding!!.inputSection.postInput.getText().toString().trim { it <= ' ' }
        if (content.isEmpty()) {
            Toast.makeText(this, getString(R.string.comment_empty_error), Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUser == null || currentUserModel == null) {
            Toast.makeText(this, getString(R.string.login_to_comment_action), Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (post == null || post.getPostId() == null) {
            Toast.makeText(
                this,
                getString(R.string.error_invalid_post_for_comment),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val commentData: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?> =
            java.util.HashMap<kotlin.String?, kotlin.Any?>()
        commentData.put("content", content)
        commentData.put("authorId", currentUser.getUid())
        commentData.put("authorDisplayName", currentUserModel.getDisplayName())
        commentData.put("authorUsername", currentUserModel.getUsername())
        commentData.put("authorAvatarUrl", currentUserModel.getProfileImageUrl())
        commentData.put("authorVerified", currentUserModel.isVerified())
        commentData.put("timestamp", FieldValue.serverTimestamp())
        commentData.put("likeCount", 0)
        commentData.put("repliesCount", 0)
        commentData.put("parentPostId", post.getPostId())

        commentViewModel.addComment(post.getPostId(), commentData)
        binding!!.inputSection.postInput.setText("")
        binding!!.inputSection.postInput.clearFocus()
        val imm =
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager?
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0)
        }
    }

    private inner class CommentInteractionListenerImpl : CommentInteractionListener {
        override fun onLikeClicked(comment: CommentModel?) {
            if (currentUser != null && comment != null && comment.getCommentId() != null && post != null && post.getPostId() != null) {
                commentViewModel.toggleLike(post.getPostId(), comment, currentUser.getUid())
            } else {
                android.util.Log.e(
                    PostDetailActivity.Companion.TAG,
                    "Cannot toggle like: missing data. CurrentUser: " + (currentUser != null) +
                            ", Comment: " + (if (comment != null) comment.getCommentId() else "null") +
                            ", Post: " + (if (post != null) post.getPostId() else "null")
                )
                Toast.makeText(
                    this@PostDetailActivity,
                    getString(R.string.error_liking_comment),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onReplyClicked(comment: CommentModel) {
            binding!!.inputSection.postInput.setHint(
                getString(
                    R.string.replying_to_user,
                    comment.getAuthorUsername()
                )
            )
            binding!!.inputSection.postInput.requestFocus()
            Toast.makeText(
                this@PostDetailActivity,
                getString(R.string.replying_to_user, comment.getAuthorUsername()),
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onAuthorClicked(authorId: kotlin.String?) {
            if (authorId != null && !authorId.isEmpty()) {
                val intent: Intent = Intent(this@PostDetailActivity, ProfileActivity::class.java)
                intent.putExtra("userId", authorId)
                startActivity(intent)
            }
        }

        override fun onShowRepliesClicked(comment: CommentModel?) {
            if (comment != null && comment.getCommentId() != null) {
                commentViewModel.toggleReplies(comment)
            }
        }

        override fun onDeleteComment(comment: CommentModel?) {
            if (currentUser != null && comment != null && comment.getAuthorId() != null && comment.getAuthorId() == currentUser.getUid()) {
                android.app.AlertDialog.Builder(this@PostDetailActivity)
                    .setTitle(getString(R.string.delete_comment_title))
                    .setMessage(getString(R.string.delete_comment_confirmation))
                    .setPositiveButton(
                        getString(R.string.delete_button),
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                            if (post != null && post.getPostId() != null && comment.getCommentId() != null) {
                                commentViewModel.deleteComment(post.getPostId(), comment)
                            } else {
                                android.util.Log.e(
                                    PostDetailActivity.Companion.TAG,
                                    "Cannot delete comment: missing post or comment ID."
                                )
                                Toast.makeText(
                                    this@PostDetailActivity,
                                    getString(R.string.error_deleting_comment),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    .setNegativeButton(getString(R.string.cancel_button), null)
                    .show()
            } else {
                Toast.makeText(
                    this@PostDetailActivity,
                    getString(R.string.error_delete_own_comment_only),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onReportComment(comment: CommentModel) {
            Toast.makeText(
                this@PostDetailActivity,
                getString(R.string.comment_reported_toast, comment.getCommentId()),
                Toast.LENGTH_SHORT
            ).show()
            // يمكنك هنا فتح شاشة الإبلاغ أو إرسال البلاغ مباشرة
            val intent: Intent = Intent(this@PostDetailActivity, ReportActivity::class.java)
            intent.putExtra("commentId", comment.getCommentId())
            intent.putExtra("postId", post.getPostId())
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_POST: kotlin.String = "post"
        private const val TAG = "PostDetailActivity" // لـ Log
    }
}