// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/home/ForYouFragment.kt
package com.spidroid.starry.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.activities.ReportActivity
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.FragmentFeedBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.common.ReactionPickerFragment
import com.spidroid.starry.viewmodels.PostViewModel
import com.spidroid.starry.viewmodels.ProfilePostState // ✨ تم التأكد من هذا الاستيراد
import com.spidroid.starry.viewmodels.ProfileViewModel // ✨ تم التأكد من هذا الاستيراد
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.fragment.app.activityViewModels // ✨ تم التأكد من هذا الاستيراد


class ForYouFragment : Fragment(), PostInteractionListener, ReactionPickerFragment.ReactionListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: PostViewModel
    private lateinit var postAdapter: PostAdapter

    private var currentAuthUserId: String? = null
    private var currentPostForReaction: PostModel? = null
    private val profileViewModel: ProfileViewModel by activityViewModels() // ✨ تم التأكد من هذا السطر


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentAuthUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentAuthUserId == null) {
            Log.w(TAG, "Current user is not authenticated in ForYouFragment.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use requireActivity() to scope the ViewModel to the Activity
        postViewModel = ViewModelProvider(requireActivity()).get(PostViewModel::class.java)

        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        loadInitialData()
    }

    private fun setupRecyclerView() {
        // ✨ تهيئة postAdapter مباشرة باستخدام requireContext()
        postAdapter = PostAdapter(requireContext(), this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = postAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            postViewModel.fetchPosts(15)
            postViewModel.fetchUserSuggestions()
        }
    }

    private fun setupObservers() {
        postViewModel.combinedFeed.observe(viewLifecycleOwner) { items ->
            binding.progressContainer.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            val currentList = items ?: emptyList()
            postAdapter.submitCombinedList(currentList)

            val isEmpty = currentList.isEmpty()
            binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        postViewModel.errorLiveData.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Log.e(TAG, "Error LiveData observed: $errorMessage")
                binding.progressContainer.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                if (postAdapter.itemCount == 0) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                } else {
                    Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        postViewModel.postInteractionErrorEvent.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadInitialData() {
        if (postAdapter.itemCount == 0) {
            binding.progressContainer.visibility = View.VISIBLE
            postViewModel.fetchPosts(15)
            postViewModel.fetchUserSuggestions()
        }
    }

    // region PostInteractionListener Implementations
    override fun onLikeClicked(post: PostModel?) {
        val safePost = post ?: return
        postViewModel.toggleLike(safePost, safePost.isLiked)
    }

    override fun onBookmarkClicked(post: PostModel?) {
        post?.postId?.let { postViewModel.toggleBookmark(it, post.isBookmarked) }
    }

    override fun onRepostClicked(post: PostModel?) {
        post?.postId?.let { postViewModel.toggleRepost(it, post.isReposted) }
    }

    override fun onCommentClicked(post: PostModel?) {
        post?.let {
            val intent = Intent(activity, PostDetailActivity::class.java)
            intent.putExtra(PostDetailActivity.EXTRA_POST, it)
            startActivity(intent)
        }
    }

    override fun onMenuClicked(post: PostModel?, anchorView: View?) {
        val safePost = post ?: return
        val safeAnchor = anchorView ?: return
        val safeContext = context ?: return

        PopupMenu(safeContext, safeAnchor).apply {
            menuInflater.inflate(R.menu.post_options_menu, menu)
            val currentAuthUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isAuthor = currentAuthUserId == safePost.authorId

            menu.findItem(R.id.action_pin_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_edit_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_delete_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_edit_privacy)?.isVisible = isAuthor
            menu.findItem(R.id.action_report_post)?.isVisible = !isAuthor
            menu.findItem(R.id.action_save_post)?.title = if (safePost.isBookmarked) "Unsave" else "Save"

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_pin_post -> onTogglePinPostClicked(safePost)
                    R.id.action_edit_post -> onEditPost(safePost)
                    R.id.action_delete_post -> onDeletePost(safePost)
                    R.id.action_copy_link -> onCopyLink(safePost)
                    R.id.action_share_post -> onSharePost(safePost)
                    R.id.action_save_post -> onBookmarkClicked(safePost)
                    R.id.action_edit_privacy -> onEditPostPrivacy(safePost)
                    R.id.action_report_post -> onReportPost(safePost)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
        }.show()
    }

    override fun onTogglePinPostClicked(post: PostModel?) {
        val safePost = post ?: return
        postViewModel.togglePostPinStatus(safePost)
    }

    override fun onEditPost(post: PostModel?) {
        Toast.makeText(context, "Edit feature is not implemented yet.", Toast.LENGTH_SHORT).show()
    }

    override fun onDeletePost(post: PostModel?) {
        val safePost = post ?: return
        if (currentAuthUserId != safePost.authorId) {
            Toast.makeText(context, "You can only delete your own posts.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ -> postViewModel.deletePost(safePost.postId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCopyLink(post: PostModel?) {
        val postId = post?.postId ?: return
        val postUrl = "https://starry.app/post/$postId" // Replace with your domain
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Post URL", postUrl)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
    }

    override fun onSharePost(post: PostModel?) {
        val safePost = post ?: return
        val postUrl = "https://starry.app/post/${safePost.postId}" // Replace with your domain
        val shareText = "Check out this post on Starry: ${safePost.content?.take(70)}...\n$postUrl"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Post from Starry")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share post via"))
    }

    override fun onEditPostPrivacy(post: PostModel?) {
        Toast.makeText(context, "Edit privacy feature is not implemented yet.", Toast.LENGTH_SHORT).show()
    }

    override fun onReportPost(post: PostModel?) {
        val safePost = post ?: return
        val intent = Intent(activity, ReportActivity::class.java).apply {
            putExtra(ReportActivity.EXTRA_REPORTED_ITEM_ID, safePost.postId)
            putExtra(ReportActivity.EXTRA_REPORT_TYPE, "post")
            putExtra(ReportActivity.EXTRA_REPORTED_AUTHOR_ID, safePost.authorId)
        }
        startActivity(intent)
    }

    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?) {
        val safePost = post ?: return
        currentPostForReaction = safePost
        ReactionPickerFragment().apply {
            setReactionListener(this@ForYouFragment)
        }.show(parentFragmentManager, ReactionPickerFragment.TAG)
    }

    override fun onReactionSelected(emojiUnicode: String?) {
        val post = currentPostForReaction ?: return
        val emoji = emojiUnicode ?: return
        Log.d(TAG, "Emoji selected: $emoji for post: ${post.postId}")
        postViewModel.handleEmojiSelection(post, emoji)
        currentPostForReaction = null
    }

    override fun onEmojiSelected(post: PostModel?, emojiUnicode: String?) {
        val safePost = post ?: return
        val safeEmoji = emojiUnicode ?: return
        postViewModel.handleEmojiSelection(safePost, safeEmoji)
    }

    override fun onEmojiSummaryClicked(post: PostModel?) {
        Toast.makeText(context, "Emoji summary coming soon.", Toast.LENGTH_SHORT).show()
    }

    override fun onUserClicked(user: UserModel?) {
        val safeUser = user ?: return
        val intent = Intent(activity, ProfileActivity::class.java).apply {
            putExtra("userId", safeUser.userId)
        }
        startActivity(intent)
    }

    // Other listener methods...
    override fun onFollowClicked(user: UserModel?) { /* Not implemented in this fragment */ }
    override fun onHashtagClicked(hashtag: String?) { /* Not implemented */ }
    override fun onPostLongClicked(post: PostModel?) { /* Not implemented */ }
    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int) { /* Not implemented */ }
    override fun onVideoPlayClicked(videoUrl: String?) { /* Not implemented */ }
    override fun onLayoutClicked(post: PostModel?) { onCommentClicked(post) }
    override fun onSeeMoreClicked(post: PostModel?) { /* Not implemented */ }
    override fun onTranslateClicked(post: PostModel?) { /* Not implemented */ }
    override fun onShowOriginalClicked(post: PostModel?) { /* Not implemented */ }
    override fun onModeratePost(post: PostModel?) { /* Not implemented */ }
    // endregion

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference
    }

    companion object {
        private const val TAG = "ForYouFragment"
    }
}