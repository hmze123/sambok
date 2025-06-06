package com.spidroid.starry.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.spidroid.starry.R
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.common.ReactionPickerFragment
import com.spidroid.starry.viewmodels.PostViewModel

class ProfilePostsFragment : Fragment(), PostInteractionListener, ReactionPickerFragment.ReactionListener {
    private var userId: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private lateinit var tvEmptyPosts: TextView
    private val postViewModel: PostViewModel by viewModels({ requireActivity() })
    private var currentAuthUserId: String? = null
    private var currentPostForReaction: PostModel? = null

    companion object {
        private const val TAG = "ProfilePostsFragment"
        private const val USER_ID_ARG = "USER_ID"

        @JvmStatic
        fun newInstance(userId: String) = ProfilePostsFragment().apply {
            arguments = bundleOf(USER_ID_ARG to userId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(USER_ID_ARG)
        }
        currentAuthUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: Log.w(TAG, "Current user is not authenticated.")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_posts, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmptyPosts = view.findViewById(R.id.tv_empty_posts)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(requireContext(), this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        postViewModel.postUpdatedEvent.observe(viewLifecycleOwner) { updatedPostId ->
            if (updatedPostId != null && userId != null) {
                Log.d(TAG, "Received post update event for $updatedPostId, reloading posts for profile: $userId")
                loadPosts()
            }
        }

        postViewModel.errorLiveData.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null && context != null) {
                Log.e(TAG, "Error LiveData observed in ProfilePostsFragment: $errorMessage")
            }
        }
    }

    private fun loadPosts() {
        val currentUserId = userId
        if (currentUserId.isNullOrEmpty()) {
            tvEmptyPosts.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            adapter.submitCombinedList(emptyList())
            return
        }
        Log.d(TAG, "Loading posts for user: $currentUserId")

        FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorId", currentUserId)
            .orderBy("isPinned", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty) {
                    Log.d(TAG, "No posts found for user: $currentUserId")
                    tvEmptyPosts.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    adapter.submitCombinedList(emptyList())
                } else {
                    Log.d(TAG, "Found ${queryDocumentSnapshots.size()} posts for user: $currentUserId")
                    val posts = queryDocumentSnapshots.documents.mapNotNull { doc ->
                        doc.toObject(PostModel::class.java)?.apply {
                            postId = doc.id
                            currentAuthUserId?.let { authId ->
                                isLiked = likes?.containsKey(authId) == true
                                isBookmarked = bookmarks?.containsKey(authId) == true
                                isReposted = reposts?.containsKey(authId) == true
                            }
                        }
                    }
                    adapter.submitCombinedList(posts)
                    tvEmptyPosts.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load posts for user $currentUserId", e)
                context?.let {
                    Toast.makeText(it, "Failed to load posts: ${e.message}", Toast.LENGTH_LONG).show()
                }
                tvEmptyPosts.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                adapter.submitCombinedList(emptyList())
            }
    }

    // --- PostInteractionListener Implementations ---

    override fun onLikeClicked(post: PostModel) {
        if (post.postId != null && post.authorId != null) {
            postViewModel.toggleLike(post, post.isLiked)
        } else {
            Log.e(TAG, "Cannot toggle like, data missing in ProfilePostsFragment")
        }
    }

    override fun onCommentClicked(post: PostModel) {
        val intent = Intent(activity, PostDetailActivity::class.java).apply {
            putExtra(PostDetailActivity.EXTRA_POST, post)
        }
        startActivity(intent)
    }

    override fun onBookmarkClicked(post: PostModel) {
        post.postId?.let { postViewModel.toggleBookmark(it, post.isBookmarked) }
    }

    override fun onRepostClicked(post: PostModel) {
        post.postId?.let { postViewModel.toggleRepost(it, post.isReposted) }
    }

    override fun onMenuClicked(post: PostModel, anchorView: View) {
        val authId = currentAuthUserId ?: return
        Log.d(TAG, "Menu clicked for post: ${post.postId}")

        PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.post_options_menu, menu)
            val isAuthor = authId == post.authorId

            menu.findItem(R.id.action_pin_post)?.apply {
                isVisible = isAuthor
                title = if (post.isPinned) "Unpin Post" else "Pin Post"
            }
            menu.findItem(R.id.action_edit_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_delete_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_edit_privacy)?.isVisible = isAuthor
            menu.findItem(R.id.action_report_post)?.isVisible = !isAuthor
            menu.findItem(R.id.action_save_post)?.title = if (post.isBookmarked) "Unsave Post" else "Save Post"

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_pin_post -> onTogglePinPostClicked(post)
                    R.id.action_edit_post -> onEditPost(post)
                    R.id.action_delete_post -> onDeletePost(post)
                    R.id.action_copy_link -> onCopyLink(post)
                    R.id.action_share_post -> onSharePost(post)
                    R.id.action_save_post -> onBookmarkClicked(post)
                    R.id.action_edit_privacy -> onEditPostPrivacy(post)
                    R.id.action_report_post -> onReportPost(post)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
        }.show()
    }

    private fun showReactionPickerForPost(post: PostModel) {
        this.currentPostForReaction = post
        ReactionPickerFragment().apply {
            setReactionListener(this@ProfilePostsFragment)
        }.show(parentFragmentManager, ReactionPickerFragment.TAG)
    }

    override fun onReactionSelected(emojiUnicode: String) {
        val post = currentPostForReaction ?: return
        Log.d(TAG, "Emoji selected: $emojiUnicode for post: ${post.postId}")
        postViewModel.handleEmojiSelection(post, emojiUnicode)
        currentPostForReaction = null
    }

    override fun onTogglePinPostClicked(post: PostModel) {
        if (post.postId != null && post.authorId != null) {
            Log.d(TAG, "Toggle pin clicked for post: ${post.postId}")
            postViewModel.togglePostPinStatus(post)
        } else {
            context?.let { Toast.makeText(it, "Error processing pin/unpin action.", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onEditPost(post: PostModel) {
        if (currentAuthUserId == post.authorId) {
            Toast.makeText(context, "TODO: Open ComposePostActivity in edit mode", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "You can only edit your own posts.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeletePost(post: PostModel) {
        if (currentAuthUserId == post.authorId) {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ -> post.postId?.let { postViewModel.deletePost(it) } }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(context, "You can only delete your own posts.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCopyLink(post: PostModel) {
        val postId = post.postId ?: return
        val postUrl = "https://starry.app/post/$postId" // Replace with your actual domain
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Post URL", postUrl)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
    }

    override fun onSharePost(post: PostModel) {
        val postId = post.postId ?: return
        val postUrl = "https://starry.app/post/$postId" // Replace with your actual domain
        val postContent = post.content?.take(70)?.let { "$it..." } ?: ""
        val shareText = "Check out this post on Starry: $postContent\n$postUrl"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Post from Starry")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share post via"))
    }

    override fun onEditPostPrivacy(post: PostModel) {
        Toast.makeText(context, "TODO: Implement Edit Post Privacy UI", Toast.LENGTH_SHORT).show()
    }

    override fun onReportPost(post: PostModel) {
        Toast.makeText(context, "TODO: Open ReportActivity", Toast.LENGTH_SHORT).show()
    }

    override fun onLikeButtonLongClicked(post: PostModel, anchorView: View) {
        showReactionPickerForPost(post)
    }

    override fun onEmojiSelected(post: PostModel, emojiUnicode: String) {
        postViewModel.handleEmojiSelection(post, emojiUnicode)
    }

    override fun onEmojiSummaryClicked(post: PostModel) {
        Toast.makeText(context, "TODO: Show emoji summary", Toast.LENGTH_SHORT).show()
    }

    override fun onUserClicked(user: UserModel) {
        if (activity != null) {
            val intent = Intent(activity, ProfileActivity::class.java).apply {
                putExtra("userId", user.userId)
            }
            startActivity(intent)
        }
    }

    // --- Other unused listener methods ---
    override fun onHashtagClicked(hashtag: String) { /* Not implemented */ }
    override fun onPostLongClicked(post: PostModel) { /* Not implemented */ }
    override fun onMediaClicked(mediaUrls: List<String>, position: Int) { /* Not implemented */ }
    override fun onVideoPlayClicked(videoUrl: String) { /* Not implemented */ }
    override fun onLayoutClicked(post: PostModel) { onCommentClicked(post) }
    override fun onSeeMoreClicked(post: PostModel) { /* Not implemented */ }
    override fun onTranslateClicked(post: PostModel) { /* Not implemented */ }
    override fun onShowOriginalClicked(post: PostModel) { /* Not implemented */ }
    override fun onFollowClicked(user: UserModel) { /* Not implemented */ }
    override fun onModeratePost(post: PostModel) { /* Not implemented */ }
}
