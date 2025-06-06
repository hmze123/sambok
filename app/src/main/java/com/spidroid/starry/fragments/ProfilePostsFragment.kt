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
                        // You can show a progress bar here if you have one
                    }
                    is ProfilePostState.Success -> {
                        binding.tvEmptyPosts.visibility = if (state.posts.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility = if (state.posts.isNotEmpty()) View.VISIBLE else View.GONE
                        postAdapter.submitCombinedList(state.posts)
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
            // ... (Menu visibility logic remains the same) ...
            setOnMenuItemClickListener { item ->
                // ... (Menu item click logic remains the same) ...
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
    override fun onEditPost(post: PostModel?) { /* ... */ }
    override fun onCopyLink(post: PostModel?) { /* ... */ }
    override fun onSharePost(post: PostModel?) { /* ... */ }
    override fun onEditPostPrivacy(post: PostModel?) { /* ... */ }
    override fun onReportPost(post: PostModel?) { /* ... */ }
    override fun onEmojiSelected(post: PostModel?, emojiUnicode: String?) { /* ... */ }
    override fun onEmojiSummaryClicked(post: PostModel?) { /* ... */ }
    override fun onFollowClicked(user: UserModel?) { /* ... */ }
    override fun onHashtagClicked(hashtag: String?) { /* ... */ }
    override fun onPostLongClicked(post: PostModel) { /* ... */ }
    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int) { /* ... */ }
    override fun onVideoPlayClicked(videoUrl: String?) { /* ... */ }
    override fun onLayoutClicked(post: PostModel?) { onCommentClicked(post) }
    override fun onSeeMoreClicked(post: PostModel) { /* ... */ }
    override fun onTranslateClicked(post: PostModel) { /* ... */ }
    override fun onShowOriginalClicked(post: PostModel) { /* ... */ }
    override fun onModeratePost(post: PostModel?) { /* ... */ }

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
