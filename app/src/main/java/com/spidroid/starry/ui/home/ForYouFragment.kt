package com.spidroid.starry.ui.home

import com.google.firebase.auth.FirebaseAuth

class ForYouFragment : androidx.fragment.app.Fragment(), PostInteractionListener, ReactionListener {
    private var binding: com.spidroid.starry.databinding.FragmentFeedBinding? = null
    private var postViewModel: PostViewModel? = null
    private var postAdapter: PostAdapter? = null

    private var currentAuthUserId: kotlin.String? = null
    private var currentPostForReaction: PostModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentAuthUserId = FirebaseAuth.getInstance().getCurrentUser().getUid()
        } else {
            android.util.Log.w(
                ForYouFragment.Companion.TAG,
                "Current user is not authenticated in ForYouFragment."
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        binding =
            com.spidroid.starry.databinding.FragmentFeedBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postViewModel =
            ViewModelProvider(requireActivity()).get<PostViewModel?>(PostViewModel::class.java)
        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        loadInitialData()
    }

    private fun setupRecyclerView() {
        if (getContext() == null) return
        postAdapter = PostAdapter(requireContext(), this)
        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
        binding!!.recyclerView.setAdapter(postAdapter)
    }

    private fun setupSwipeRefresh() {
        binding!!.swipeRefresh.setOnRefreshListener(OnRefreshListener {
            postViewModel.fetchPosts(15)
            postViewModel.fetchUserSuggestions()
        })
    }

    private fun setupObservers() {
        postViewModel.getCombinedFeed().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { items: kotlin.collections.MutableList<kotlin.Any?>? ->
                if (binding == null) {
                    android.util.Log.w(
                        ForYouFragment.Companion.TAG,
                        "Binding is null in ForYouFragment observer, skipping update."
                    )
                    return@observe
                }
                binding!!.progressContainer.setVisibility(android.view.View.GONE)
                binding!!.swipeRefresh.setRefreshing(false)

                if (items != null) {
                    android.util.Log.d(
                        ForYouFragment.Companion.TAG,
                        "ForYouFragment CombinedFeed LiveData changed. Items count: " + items.size
                    )
                    postAdapter.submitCombinedList(items)
                } else {
                    android.util.Log.w(
                        ForYouFragment.Companion.TAG,
                        "ForYouFragment Observer received null items, submitting empty list."
                    )
                    postAdapter.submitCombinedList(java.util.ArrayList<kotlin.Any?>())
                }
                val isEmpty = items == null || items.isEmpty()
                binding!!.emptyStateLayout.setVisibility(if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE)
                binding!!.recyclerView.setVisibility(if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE)
                android.util.Log.d(
                    ForYouFragment.Companion.TAG,
                    "ForYouFragment RecyclerView visibility: " + (if (isEmpty) "GONE" else "VISIBLE") + ", EmptyState visibility: " + (if (isEmpty) "VISIBLE" else "GONE")
                )
            })

        postViewModel.getErrorLiveData().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { errorMessage: kotlin.String? ->
                if (errorMessage != null && binding != null) {
                    android.util.Log.e(
                        ForYouFragment.Companion.TAG,
                        "Error LiveData observed in ForYouFragment: " + errorMessage
                    )
                    binding!!.progressContainer.setVisibility(android.view.View.GONE)
                    binding!!.swipeRefresh.setRefreshing(false)
                    // إظهار emptyStateLayout إذا كان الخطأ يعني عدم وجود بيانات
                    if (postAdapter.getItemCount() == 0) {
                        binding!!.emptyStateLayout.setVisibility(android.view.View.VISIBLE)
                        binding!!.recyclerView.setVisibility(android.view.View.GONE)
                        // يمكنك تحديث نص emptyStateLayout ليعكس الخطأ
                        // ((TextView) binding.emptyStateLayout.findViewById(R.id.tvNoPostsYet)).setText(errorMessage);
                    } else {
                        Snackbar.make(binding!!.getRoot(), errorMessage, Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            })

        postViewModel.getPostInteractionErrorEvent().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { errorMsg: kotlin.String? ->
                if (errorMsg != null && getContext() != null) {
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
            })

        postViewModel.getPostUpdatedEvent().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { postId: kotlin.String? ->
                if (postId != null) {
                    android.util.Log.d(
                        ForYouFragment.Companion.TAG,
                        "Post " + postId + " updated event received. ForYouFragment might refresh or update item if necessary."
                    )
                    // إذا كان ForYouFragment يعرض قائمة قابلة للتحديث عند تغيير منشور واحد
                    // يمكنك هنا تحديث عنصر واحد في الـ Adapter
                    // postAdapter.notifyItemChanged(findPositionByPostId(postId));
                    // أو إعادة تحميل بسيط (أقل كفاءة ولكن أسهل)
                    // postViewModel.fetchPosts(15); // كن حذرًا من الاستدعاءات المتكررة
                }
            })
    }

    private fun loadInitialData() {
        if (binding != null && postAdapter.getItemCount() == 0) {
            binding!!.progressContainer.setVisibility(android.view.View.VISIBLE)
            postViewModel.fetchPosts(15)
            postViewModel.fetchUserSuggestions()
        }
    }

    // --- تطبيقات واجهة PostInteractionListener ---
    override fun onLikeClicked(post: PostModel?) {
        if (postViewModel != null && post != null && post.getPostId() != null && post.getAuthorId() != null) {
            postViewModel.toggleLike(post, post.isLiked())
        } else {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot toggle like, data missing in ForYouFragment"
            )
            if (getContext() != null) Toast.makeText(
                getContext(),
                "Error processing like.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onBookmarkClicked(post: PostModel?) {
        if (postViewModel != null && post != null && post.getPostId() != null) {
            postViewModel.toggleBookmark(post.getPostId(), post.isBookmarked())
        } else {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot toggle bookmark, data missing in ForYouFragment"
            )
        }
    }

    override fun onRepostClicked(post: PostModel?) {
        if (postViewModel != null && post != null && post.getPostId() != null) {
            postViewModel.toggleRepost(post.getPostId(), post.isReposted())
        } else {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot toggle repost, data missing in ForYouFragment"
            )
        }
    }

    override fun onCommentClicked(post: PostModel?) {
        if (post != null && getActivity() != null) {
            val intent: Intent = Intent(getActivity(), PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.Companion.EXTRA_POST, post)
            startActivity(intent)
        }
    }

    override fun onMenuClicked(post: PostModel?, anchorView: android.view.View?) {
        if (getContext() == null || post == null || anchorView == null || currentAuthUserId == null) {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot show post options menu, data missing in ForYouFragment."
            )
            return
        }
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Menu clicked for post: " + post.getPostId() + " in ForYouFragment"
        )

        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), anchorView)
        popup.getMenuInflater().inflate(R.menu.post_options_menu, popup.getMenu())
        val isAuthor = currentAuthUserId == post.getAuthorId()

        val pinItem = popup.getMenu().findItem(R.id.action_pin_post)
        val editItem = popup.getMenu().findItem(R.id.action_edit_post)
        val deleteItem = popup.getMenu().findItem(R.id.action_delete_post)
        val privacyItem = popup.getMenu().findItem(R.id.action_edit_privacy)
        val reportItem = popup.getMenu().findItem(R.id.action_report_post)
        val saveItem = popup.getMenu().findItem(R.id.action_save_post)
        val copyLinkItem = popup.getMenu().findItem(R.id.action_copy_link)
        val shareItem = popup.getMenu().findItem(R.id.action_share_post)

        if (pinItem != null) {
            pinItem.setVisible(isAuthor)
            if (isAuthor) pinItem.setTitle(if (post.isPinned()) "إلغاء تثبيت المنشور" else "تثبيت المنشور")
        }
        if (editItem != null) editItem.setVisible(isAuthor)
        if (deleteItem != null) deleteItem.setVisible(isAuthor)
        if (privacyItem != null) privacyItem.setVisible(isAuthor)
        if (reportItem != null) reportItem.setVisible(!isAuthor)
        if (saveItem != null) saveItem.setTitle(if (post.isBookmarked()) "إلغاء حفظ المنشور" else "حفظ المنشور")
        if (copyLinkItem != null) copyLinkItem.setVisible(true)
        if (shareItem != null) shareItem.setVisible(true)

        popup.setOnMenuItemClickListener(androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener { item: android.view.MenuItem? ->
            val itemId = item!!.getItemId()
            if (itemId == R.id.action_pin_post) onTogglePinPostClicked(post)
            else if (itemId == R.id.action_edit_post) onEditPost(post)
            else if (itemId == R.id.action_delete_post) onDeletePost(post)
            else if (itemId == R.id.action_copy_link) onCopyLink(post)
            else if (itemId == R.id.action_share_post) onSharePost(post)
            else if (itemId == R.id.action_save_post) onBookmarkClicked(post)
            else if (itemId == R.id.action_edit_privacy) onEditPostPrivacy(post)
            else if (itemId == R.id.action_report_post) onReportPost(post)
            else return@setOnMenuItemClickListener false
            true
        })
        popup.show()
    }

    private fun showReactionPickerForPost(post: PostModel?) {
        if (post == null || getParentFragmentManager() == null) {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot show reaction picker, post or FragmentManager is null in ForYouFragment"
            )
            return
        }
        this.currentPostForReaction = post
        val reactionPicker: ReactionPickerFragment = ReactionPickerFragment()
        reactionPicker.setReactionListener(this)
        reactionPicker.show(getParentFragmentManager(), ReactionPickerFragment.Companion.TAG)
    }

    // تطبيق دالة الواجهة ReactionPickerFragment.ReactionListener
    override fun onReactionSelected(emojiUnicode: kotlin.String?) {
        if (currentPostForReaction != null && postViewModel != null && currentAuthUserId != null) {
            android.util.Log.d(
                ForYouFragment.Companion.TAG,
                "Emoji selected by ReactionPickerFragment: " + emojiUnicode + " for post: " + currentPostForReaction.getPostId()
            )
            postViewModel.handleEmojiSelection(currentPostForReaction, emojiUnicode)
        } else {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot handle emoji selection from ReactionPickerFragment, data missing in ForYouFragment."
            )
            if (getContext() != null) Toast.makeText(
                getContext(),
                "Error reacting to post.",
                Toast.LENGTH_SHORT
            ).show()
        }
        currentPostForReaction = null
    }

    // --- تطبيق الدوال المتبقية من PostInteractionListener ---
    override fun onTogglePinPostClicked(post: PostModel?) {
        if (postViewModel != null && post != null && post.getPostId() != null && post.getAuthorId() != null) {
            android.util.Log.d(
                ForYouFragment.Companion.TAG,
                "Toggle pin clicked for post: " + post.getPostId() + " in ForYouFragment"
            )
            postViewModel.togglePostPinStatus(post)
        } else {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot toggle pin status, data missing in ForYouFragment."
            )
            if (getContext() != null) Toast.makeText(
                getContext(),
                "Error processing pin/unpin action.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onEditPost(post: PostModel) {
        android.util.Log.d(ForYouFragment.Companion.TAG, "Edit post clicked: " + post.getPostId())
        if (getContext() != null && currentAuthUserId != null && currentAuthUserId == post.getAuthorId()) {
            // Intent intent = new Intent(getActivity(), ComposePostActivity.class);
            // intent.putExtra("EDIT_POST_ID", post.getPostId());
            // startActivity(intent);
            Toast.makeText(
                getContext(),
                "TODO: Open ComposePostActivity in edit mode for: " + post.getPostId(),
                Toast.LENGTH_SHORT
            ).show()
        } else if (getContext() != null) {
            Toast.makeText(getContext(), "You can only edit your own posts.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDeletePost(post: PostModel?) {
        if (postViewModel != null && post != null && post.getPostId() != null && currentAuthUserId != null && currentAuthUserId == post.getAuthorId()) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton(
                    "Delete",
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        postViewModel.deletePost(post.getPostId())
                    })
                .setNegativeButton("Cancel", null)
                .show()
        } else if (getContext() != null && post != null && currentAuthUserId != null && (currentAuthUserId != post.getAuthorId())) {
            Toast.makeText(getContext(), "You can only delete your own posts.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCopyLink(post: PostModel?) {
        if (post == null || post.getPostId() == null || getContext() == null) return
        val postUrl = "https://starry.app/post/" + post.getPostId() // ★ استبدل برابط تطبيقك الفعلي
        val clipboard =
            getContext()!!.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager?
        val clip: ClipData = android.content.ClipData.newPlainText("Post URL", postUrl)
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip)
            Toast.makeText(getContext(), "Link copied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSharePost(post: PostModel?) {
        if (post == null || getActivity() == null || post.getPostId() == null) return
        val postUrl = "https://starry.app/post/" + post.getPostId() // ★ استبدل برابط تطبيقك الفعلي
        val shareText = "Check out this post on Starry: " +
                (if (post.getContent() != null && post.getContent().length > 70)  // جعل النص أقصر قليلاً
                    post.getContent().substring(0, 70) + "..." else post.getContent()) +
                "\n" + postUrl
        val shareIntent: Intent = Intent(Intent.ACTION_SEND)
        shareIntent.setType("text/plain")
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Post from Starry")
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, "Share post via"))
    }

    override fun onEditPostPrivacy(post: PostModel?) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Edit post privacy for: " + (if (post != null) post.getPostId() else "null")
        )
        if (getContext() != null && post != null && currentAuthUserId != null && currentAuthUserId == post.getAuthorId()) {
            Toast.makeText(
                getContext(),
                "TODO: Implement Edit Post Privacy UI for: " + post.getPostId(),
                Toast.LENGTH_SHORT
            ).show()
        } else if (getContext() != null) {
            Toast.makeText(
                getContext(),
                "You can only edit privacy for your own posts.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ... داخل onReportPost في ForYouFragment.java أو أي مكان آخر ...
    override fun onReportPost(post: PostModel?) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Report post: " + (if (post != null) post.getPostId() else "null")
        )
        if (getContext() != null && post != null && post.getPostId() != null && post.getAuthorId() != null) { // تأكد من وجود authorId
            val intent: Intent = Intent(getActivity(), ReportActivity::class.java)
            intent.putExtra(ReportActivity.Companion.EXTRA_REPORTED_ITEM_ID, post.getPostId())
            intent.putExtra(
                ReportActivity.Companion.EXTRA_REPORT_TYPE,
                "post"
            ) // أو "comment" أو "user"
            intent.putExtra(
                ReportActivity.Companion.EXTRA_REPORTED_AUTHOR_ID,
                post.getAuthorId()
            ) // تمرير معرّف صاحب المنشور
            startActivity(intent)
        } else if (getContext() != null && post != null) {
            Toast.makeText(
                getContext(),
                "Cannot report post: missing necessary data.",
                Toast.LENGTH_SHORT
            ).show()
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot report post: postId=" + (post.getPostId() != null) + ", authorId=" + (post.getAuthorId() != null)
            )
        }
    }


    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: android.view.View?) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Like button long clicked for post: " + (if (post != null) post.getPostId() else "null")
        )
        if (post != null) showReactionPickerForPost(post)
    }

    override fun onEmojiSelected(post: PostModel?, emojiUnicode: kotlin.String?) {
        if (post != null && postViewModel != null && currentAuthUserId != null && emojiUnicode != null) {
            android.util.Log.d(
                ForYouFragment.Companion.TAG,
                "Emoji selected via PostInteractionListener: " + emojiUnicode + " for post: " + post.getPostId()
            )
            postViewModel.handleEmojiSelection(post, emojiUnicode)
        } else {
            android.util.Log.e(
                ForYouFragment.Companion.TAG,
                "Cannot handle emoji selection from PostInteractionListener in ForYouFragment, data missing."
            )
        }
    }

    override fun onEmojiSummaryClicked(post: PostModel?) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Emoji summary clicked for post: " + (if (post != null) post.getPostId() else "null")
        )
        if (post != null && getContext() != null) Toast.makeText(
            getContext(),
            "Emoji summary: " + post.getPostId(),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onHashtagClicked(hashtag: kotlin.String?) {
        android.util.Log.d(ForYouFragment.Companion.TAG, "Hashtag clicked: " + hashtag)
    }

    override fun onPostLongClicked(post: PostModel) {
        android.util.Log.d(ForYouFragment.Companion.TAG, "Post long clicked: " + post.getPostId())
    }

    override fun onMediaClicked(
        mediaUrls: kotlin.collections.MutableList<kotlin.String?>?,
        position: Int
    ) {
        android.util.Log.d(ForYouFragment.Companion.TAG, "Media clicked")
    }

    override fun onVideoPlayClicked(videoUrl: kotlin.String?) {
        android.util.Log.d(ForYouFragment.Companion.TAG, "Video play clicked: " + videoUrl)
    }

    override fun onLayoutClicked(post: PostModel?) {
        onCommentClicked(post)
    }

    override fun onSeeMoreClicked(post: PostModel) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "See more clicked for post: " + post.getPostId()
        )
    }

    override fun onTranslateClicked(post: PostModel) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Translate clicked for post: " + post.getPostId()
        )
    }

    override fun onShowOriginalClicked(post: PostModel) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Show original clicked for post: " + post.getPostId()
        )
    }

    override fun onFollowClicked(user: UserModel?) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Follow clicked for user: " + (if (user != null) user.getUserId() else "null")
        )
    }

    override fun onUserClicked(user: UserModel?) {
        if (user != null && user.getUserId() != null && getActivity() != null) {
            val intent: Intent = Intent(getActivity(), ProfileActivity::class.java)
            intent.putExtra("userId", user.getUserId())
            startActivity(intent)
        }
    }

    override fun onModeratePost(post: PostModel?) {
        android.util.Log.d(
            ForYouFragment.Companion.TAG,
            "Moderate post clicked: " + (if (post != null) post.getPostId() else "null")
        )
    }

    companion object {
        private const val TAG = "ForYouFragment"
    }
}