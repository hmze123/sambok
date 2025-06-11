package com.spidroid.starry.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.activities.ComposePostActivity
import com.spidroid.starry.activities.MediaViewerActivity
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.FragmentFeedBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.common.ReactionPickerFragment
import com.spidroid.starry.viewmodels.PostViewModel
import com.spidroid.starry.viewmodels.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowingFragment : Fragment(), PostInteractionListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val postViewModel: PostViewModel by activityViewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        // Fetch initial posts for the "Following" feed
        postViewModel.fetchFollowingPosts(20)
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(requireContext(), this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = postAdapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            postViewModel.fetchFollowingPosts(20)
        }
    }

    private fun observeViewModel() {
        postViewModel.followingPosts.observe(viewLifecycleOwner) { posts ->
            binding.swipeRefresh.isRefreshing = false
            postAdapter.submitCombinedList(posts ?: emptyList())

            binding.emptyStateLayout.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.tvNoPostsYet.text = "No posts from users you follow."
            binding.tvFollowUsersPrompt.text = "Follow more people to see their posts here."
        }

        postViewModel.followingState.observe(viewLifecycleOwner) { state ->
            when(state) {
                is UiState.Loading -> {
                    if (postAdapter.itemCount == 0) {
                        binding.progressContainer.visibility = View.VISIBLE
                    }
                }
                is UiState.Error -> {
                    binding.progressContainer.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                is UiState.Success -> {
                    binding.progressContainer.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                else -> {
                    binding.progressContainer.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- تطبيق جميع دوال الواجهة ---

    override fun onLikeClicked(post: PostModel?) {
        post?.let { postViewModel.toggleLike(it, !it.isLiked) }
    }

    override fun onBookmarkClicked(post: PostModel?) {
        post?.postId?.let { postViewModel.toggleBookmark(it, !post.isBookmarked) }
    }

    override fun onRepostClicked(post: PostModel?) {
        post?.postId?.let { postViewModel.toggleRepost(it, !post.isReposted) }
    }

    override fun onCommentClicked(post: PostModel?) {
        post?.let {
            val intent = Intent(activity, PostDetailActivity::class.java).apply {
                putExtra(PostDetailActivity.EXTRA_POST, it)
            }
            startActivity(intent)
        }
    }

    override fun onDeletePost(post: PostModel?) {
        post?.postId?.let {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete") { _, _ -> postViewModel.deletePost(it) }
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

    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {
        val urls = mediaUrls?.filterNotNull()?.let { ArrayList(it) } ?: return
        MediaViewerActivity.launch(requireActivity(), urls, position, sharedView)
    }

    override fun onQuoteRepostClicked(post: PostModel?) {
        val intent = Intent(activity, ComposePostActivity::class.java).apply {
            putExtra(ComposePostActivity.EXTRA_QUOTE_POST, post)
        }
        startActivity(intent)
    }

    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?) {
        val safePost = post ?: return
        val dialog = ReactionPickerFragment.newInstance()
        dialog.setListener(this, safePost)
        dialog.show(parentFragmentManager, ReactionPickerFragment.TAG)
    }

    override fun onReactionSelected(post: PostModel?, emojiUnicode: String) {
        val safePost = post ?: return
        postViewModel.handleEmojiSelection(safePost, emojiUnicode)
    }

    // --- بقية الدوال التي لا نحتاج لمنطق خاص بها في هذه الواجهة ---
    override fun onMenuClicked(post: PostModel?, anchorView: View?) { /* TODO */ }
    override fun onTogglePinPostClicked(post: PostModel?) { /* TODO */ }
    override fun onEditPost(post: PostModel?) { /* TODO */ }
    override fun onCopyLink(post: PostModel?) { /* TODO */ }
    override fun onSharePost(post: PostModel?) { /* TODO */ }
    override fun onEditPostPrivacy(post: PostModel?) { /* TODO */ }
    override fun onReportPost(post: PostModel?) { /* TODO */ }
    override fun onEmojiSummaryClicked(post: PostModel?) { /* TODO */ }
    override fun onHashtagClicked(hashtag: String?) { /* TODO */ }
    override fun onPostLongClicked(post: PostModel?) { /* TODO */ }
    override fun onVideoPlayClicked(videoUrl: String?) { /* TODO */ }
    override fun onLayoutClicked(post: PostModel?) { onCommentClicked(post) }
    override fun onSeeMoreClicked(post: PostModel?) { /* TODO */ }
    override fun onTranslateClicked(post: PostModel?) { /* TODO */ }
    override fun onShowOriginalClicked(post: PostModel?) { /* TODO */ }
    override fun onModeratePost(post: PostModel?) { /* TODO */ }
    override fun onFollowClicked(user: UserModel?) { /* TODO */ }
}
