package com.spidroid.starry.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.spidroid.starry.activities.MediaViewerActivity
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.FragmentFeedBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.PostViewModel
import com.spidroid.starry.viewmodels.UiState

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

    // --- تطبيق دوال الواجهة بالشكل الصحيح ---
    override fun onLikeClicked(post: PostModel?) { post?.let { postViewModel.toggleLike(it, !it.isLiked) } }
    override fun onBookmarkClicked(post: PostModel?) { post?.postId?.let { postViewModel.toggleBookmark(it, !post.isBookmarked) } }
    override fun onRepostClicked(post: PostModel?) { post?.postId?.let { postViewModel.toggleRepost(it, !post.isReposted) } }

    override fun onCommentClicked(post: PostModel?) {
        post?.let {
            val intent = Intent(activity, PostDetailActivity::class.java).apply {
                putExtra(PostDetailActivity.EXTRA_POST, it)
            }
            startActivity(intent)
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

    // --- بقية دوال الواجهة (يمكن تركها فارغة أو تنفيذها حسب الحاجة) ---
    override fun onMenuClicked(post: PostModel?, anchorView: View?) { /* ... */ }
    override fun onTogglePinPostClicked(post: PostModel?) { /* ... */ }
    override fun onEditPost(post: PostModel?) { /* ... */ }
    override fun onDeletePost(post: PostModel?) { /* ... */ }
    override fun onCopyLink(post: PostModel?) { /* ... */ }
    override fun onSharePost(post: PostModel?) { /* ... */ }
    override fun onEditPostPrivacy(post: PostModel?) { /* ... */ }
    override fun onReportPost(post: PostModel?) { /* ... */ }
    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?) { /* ... */ }
    override fun onReactionSelected(post: PostModel?, emojiUnicode: String) { /* ... */ }
    override fun onEmojiSummaryClicked(post: PostModel?) { /* ... */ }
    override fun onHashtagClicked(hashtag: String?) { /* ... */ }
    override fun onPostLongClicked(post: PostModel?) { /* ... */ }
    override fun onVideoPlayClicked(videoUrl: String?) { /* ... */ }
    override fun onLayoutClicked(post: PostModel?) { onCommentClicked(post) }
    override fun onSeeMoreClicked(post: PostModel?) { /* ... */ }
    override fun onTranslateClicked(post: PostModel?) { /* ... */ }
    override fun onShowOriginalClicked(post: PostModel?) { /* ... */ }
    override fun onModeratePost(post: PostModel?) { /* ... */ }
    override fun onFollowClicked(user: UserModel?) { /* ... */ }
}