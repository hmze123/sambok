// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/fragments/ProfilePostsFragment.kt
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.spidroid.starry.R
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.activities.ReportActivity
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.FragmentProfilePostsBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.common.ReactionPickerFragment
import com.spidroid.starry.viewmodels.PostViewModel
import com.spidroid.starry.viewmodels.ProfilePostState
import com.spidroid.starry.viewmodels.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth // ✨ تم إضافة هذا الاستيراد

class ProfilePostsFragment : Fragment(), PostInteractionListener, ReactionPickerFragment.ReactionListener {

    private var _binding: FragmentProfilePostsBinding? = null
    private val binding get() = _binding!!

    private var userId: String? = null
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val postInteractionViewModel: PostViewModel by activityViewModels()

    private lateinit var postAdapter: PostAdapter
    private var currentPostForReaction: PostModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(USER_ID_ARG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        userId?.let {
            profileViewModel.fetchPostsForUser(it)
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(requireContext(), this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.postsState.collectLatest { state ->
                when (state) {
                    is ProfilePostState.Loading -> {
                        binding.tvEmptyPosts.visibility = View.GONE
                        // يمكنك إظهار مؤشر تحميل هنا إذا أردت
                    }
                    is ProfilePostState.Success -> {
                        binding.tvEmptyPosts.visibility = if (state.posts.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility = if (state.posts.isNotEmpty()) View.VISIBLE else View.GONE
                        postAdapter.submitCombinedList(state.posts)
                    }
                    is ProfilePostState.Empty -> { // ✨ تم إضافة هذا الفرع
                        binding.recyclerView.visibility = View.GONE
                        binding.tvEmptyPosts.visibility = View.VISIBLE
                        binding.tvEmptyPosts.text = getString(R.string.no_posts_yet_profile)
                    }
                    is ProfilePostState.Error -> {
                        binding.recyclerView.visibility = View.GONE
                        binding.tvEmptyPosts.visibility = View.VISIBLE
                        binding.tvEmptyPosts.text = state.message
                    }
                }
            }
        }

        // Observe interaction events from the shared PostViewModel
        postInteractionViewModel.postUpdatedEvent.observe(viewLifecycleOwner) { updatedPostId ->
            if (updatedPostId != null) {
                userId?.let { profileViewModel.fetchPostsForUser(it) }
            }
        }
    }

    // --- PostInteractionListener Implementation ---
    // All interactions are delegated to the shared PostViewModel

    override fun onLikeClicked(post: PostModel?) {
        post?.let { postInteractionViewModel.toggleLike(it, it.isLiked) }
    }

    override fun onBookmarkClicked(post: PostModel?) {
        post?.postId?.let { postInteractionViewModel.toggleBookmark(it, post.isBookmarked) }
    }

    override fun onRepostClicked(post: PostModel?) {
        post?.postId?.let { postInteractionViewModel.toggleRepost(it, post.isReposted) }
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

        PopupMenu(requireContext(), safeAnchor).apply {
            menuInflater.inflate(R.menu.post_options_menu, menu)
            val currentAuthUserId = FirebaseAuth.getInstance().currentUser?.uid // ✨ تم الوصول إلى FirebaseAuth مباشرة
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
        post?.let { postInteractionViewModel.togglePostPinStatus(it) }
    }

    override fun onDeletePost(post: PostModel?) {
        post?.postId?.let {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete") { _, _ -> postInteractionViewModel.deletePost(it) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onUserClicked(user: UserModel?) {
        user?.userId?.let {
            val intent = Intent(activity, ProfileActivity::class.java)
            intent.putExtra("userId", it)
            startActivity(intent)
        }
    }

    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?) {
        currentPostForReaction = post
        ReactionPickerFragment().apply {
            setReactionListener(this@ProfilePostsFragment)
        }.show(parentFragmentManager, ReactionPickerFragment.TAG)
    }

    override fun onReactionSelected(emojiUnicode: String?) {
        val post = currentPostForReaction ?: return
        val emoji = emojiUnicode ?: return
        postInteractionViewModel.handleEmojiSelection(post, emoji)
    }

    // --- Other interaction stubs ---
    override fun onEditPost(post: PostModel?) { Toast.makeText(context, "Edit feature is not implemented yet.", Toast.LENGTH_SHORT).show() }
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
    override fun onEditPostPrivacy(post: PostModel?) { Toast.makeText(context, "Edit privacy feature is not implemented yet.", Toast.LENGTH_SHORT).show() }
    override fun onReportPost(post: PostModel?) {
        val safePost = post ?: return
        val intent = Intent(activity, ReportActivity::class.java).apply {
            putExtra(ReportActivity.EXTRA_REPORTED_ITEM_ID, safePost.postId)
            putExtra(ReportActivity.EXTRA_REPORT_TYPE, "post")
            putExtra(ReportActivity.EXTRA_REPORTED_AUTHOR_ID, safePost.authorId)
        }
        startActivity(intent)
    }
    override fun onEmojiSelected(post: PostModel?, emojiUnicode: String?) {
        val safePost = post ?: return
        val safeEmoji = emojiUnicode ?: return
        postInteractionViewModel.handleEmojiSelection(safePost, safeEmoji)
    }
    override fun onEmojiSummaryClicked(post: PostModel?) { Toast.makeText(context, "Emoji summary coming soon.", Toast.LENGTH_SHORT).show() }
    override fun onFollowClicked(user: UserModel?) { /* Not implemented directly in this fragment */ }
    override fun onHashtagClicked(hashtag: String?) { /* Not implemented */ }
    override fun onPostLongClicked(post: PostModel?) { /* Not implemented */ }
    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int) { /* Not implemented */ }
    override fun onVideoPlayClicked(videoUrl: String?) { /* Not implemented */ }
    override fun onLayoutClicked(post: PostModel?) { onCommentClicked(post) }
    override fun onSeeMoreClicked(post: PostModel?) { /* Not implemented */ }
    override fun onTranslateClicked(post: PostModel?) { /* Not implemented */ }
    override fun onShowOriginalClicked(post: PostModel?) { /* Not implemented */ }
    override fun onModeratePost(post: PostModel?) { /* Not implemented */ }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfilePostsFragment"
        private const val USER_ID_ARG = "USER_ID"

        @JvmStatic
        fun newInstance(userId: String) = ProfilePostsFragment().apply {
            arguments = bundleOf(USER_ID_ARG to userId)
        }
    }
}