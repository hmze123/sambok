package com.spidroid.starry.ui.home

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.activities.ComposePostActivity
import com.spidroid.starry.activities.MediaViewerActivity
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForYouFragment : Fragment(), PostInteractionListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val postViewModel: PostViewModel by activityViewModels()
    private lateinit var postAdapter: PostAdapter
    private var currentAuthUserId: String? = null
    private var currentPostForReaction: PostModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentAuthUserId = FirebaseAuth.getInstance().currentUser?.uid
    }
    override fun onQuoteRepostClicked(post: PostModel?) {
        val safePost = post ?: return
        val intent = Intent(activity, ComposePostActivity::class.java).apply {
            putExtra(ComposePostActivity.EXTRA_QUOTE_POST, safePost)
        }
        startActivity(intent)
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
        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        loadInitialData()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(requireContext(), this)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = postAdapter

        binding.recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 4 && firstVisibleItemPosition >= 0) {
                        postViewModel.fetchMorePosts(10)
                    }
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            postViewModel.fetchInitialPosts(15)
            postViewModel.fetchUserSuggestions()
        }
    }

    private fun loadInitialData() {
        if (postAdapter.itemCount == 0) {
            binding.progressContainer.visibility = View.VISIBLE
            postViewModel.fetchInitialPosts(15)
            postViewModel.fetchUserSuggestions()
        }
    }

    private fun setupObservers() {
        postViewModel.combinedFeed.observe(viewLifecycleOwner) { items ->
            binding.progressContainer.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false
            postAdapter.submitCombinedList(items ?: emptyList())
            binding.emptyStateLayout.visibility = if (items.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        postViewModel.errorLiveData.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
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

    // --- تطبيق دوال PostInteractionListener ---

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

    override fun onUserClicked(user: UserModel?) {
        user?.userId?.let {
            val intent = Intent(activity, ProfileActivity::class.java)
            intent.putExtra("userId", it)
            startActivity(intent)
        }
    }

    // --- الدالة التي تم إصلاحها ---
    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {
        val urls = mediaUrls?.filterNotNull()?.let { ArrayList(it) } ?: return
        MediaViewerActivity.launch(requireActivity(), urls, position, sharedView)
    }

    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?) {
        val safePost = post ?: return
        currentPostForReaction = safePost
        val dialog = ReactionPickerFragment.newInstance()
        dialog.setListener(this, safePost)
        dialog.show(parentFragmentManager, ReactionPickerFragment.TAG)
    }

    override fun onReactionSelected(post: PostModel?, emojiUnicode: String) {
        val safePost = post ?: return
        postViewModel.handleEmojiSelection(safePost, emojiUnicode)
    }

    override fun onMenuClicked(post: PostModel?, anchorView: View?) {
        val safePost = post ?: return
        val safeAnchor = anchorView ?: return

        PopupMenu(requireContext(), safeAnchor).apply {
            menuInflater.inflate(R.menu.post_options_menu, menu)
            // ... (الكود الحالي للتحكم في ظهور الخيارات)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_quote_repost -> onQuoteRepostClicked(safePost) // --- السطر الجديد ---
                    R.id.action_pin_post -> onTogglePinPostClicked(safePost)
                    // ... (بقية الحالات)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
        }.show()
    }

    // --- بقية الدوال ---
    override fun onTogglePinPostClicked(post: PostModel?) { /* ... */ }
    override fun onEditPost(post: PostModel?) { /* ... */ }
    override fun onDeletePost(post: PostModel?) { /* ... */ }
    override fun onCopyLink(post: PostModel?) { /* ... */ }
    override fun onSharePost(post: PostModel?) { /* ... */ }
    override fun onEditPostPrivacy(post: PostModel?) { /* ... */ }
    override fun onReportPost(post: PostModel?) { /* ... */ }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}