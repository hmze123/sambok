package com.spidroid.starry.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.activities.MediaViewerActivity
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

class ProfilePostsFragment : Fragment(), PostInteractionListener {

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
        userId?.let { profileViewModel.fetchPostsForUser(it) }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(requireContext(), this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            profileViewModel.postsState.collectLatest { state ->
                when (state) {
                    is ProfilePostState.Loading -> { binding.tvEmptyPosts.visibility = View.GONE }
                    is ProfilePostState.Success -> {
                        binding.tvEmptyPosts.visibility = if (state.posts.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility = if (state.posts.isNotEmpty()) View.VISIBLE else View.GONE
                        postAdapter.submitCombinedList(state.posts)
                    }
                    is ProfilePostState.Empty -> {
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
        postInteractionViewModel.postUpdatedEvent.observe(viewLifecycleOwner) { updatedPostId ->
            if (updatedPostId != null) {
                userId?.let { profileViewModel.fetchPostsForUser(it) }
            }
        }
    }

    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {
        val urls = mediaUrls?.filterNotNull()?.let { ArrayList(it) } ?: return
        MediaViewerActivity.launch(requireActivity(), urls, position, sharedView)
    }

    // ... بقية دوال الواجهة تبقى كما هي ...
    override fun onLikeClicked(post: PostModel?) { /* ... */ }
    override fun onBookmarkClicked(post: PostModel?) { /* ... */ }
    override fun onRepostClicked(post: PostModel?) { /* ... */ }
    override fun onCommentClicked(post: PostModel?) { /* ... */ }
    override fun onMenuClicked(post: PostModel?, anchorView: View?) { /* ... */ }
    override fun onTogglePinPostClicked(post: PostModel?) { /* ... */ }
    override fun onDeletePost(post: PostModel?) { /* ... */ }
    override fun onUserClicked(user: UserModel?) { /* ... */ }
    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?) { /* ... */ }
    override fun onReactionSelected(post: PostModel?, emojiUnicode: String) { /* ... */ }
    override fun onEditPost(post: PostModel?) { /* ... */ }
    override fun onCopyLink(post: PostModel?) { /* ... */ }
    override fun onSharePost(post: PostModel?) { /* ... */ }
    override fun onEditPostPrivacy(post: PostModel?) { /* ... */ }
    override fun onReportPost(post: PostModel?) { /* ... */ }
    override fun onEmojiSummaryClicked(post: PostModel?) { /* ... */ }
    override fun onHashtagClicked(hashtag: String?) { /* ... */ }
    override fun onPostLongClicked(post: PostModel?) { /* ... */ }
    override fun onVideoPlayClicked(videoUrl: String?) { /* ... */ }
    override fun onLayoutClicked(post: PostModel?) { /* ... */ }
    override fun onSeeMoreClicked(post: PostModel?) { /* ... */ }
    override fun onTranslateClicked(post: PostModel?) { /* ... */ }
    override fun onShowOriginalClicked(post: PostModel?) { /* ... */ }
    override fun onModeratePost(post: PostModel?) { /* ... */ }
    override fun onFollowClicked(user: UserModel?) { /* ... */ }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val USER_ID_ARG = "USER_ID"
        @JvmStatic
        fun newInstance(userId: String) = ProfilePostsFragment().apply {
            arguments = bundleOf(USER_ID_ARG to userId)
        }
    }
}