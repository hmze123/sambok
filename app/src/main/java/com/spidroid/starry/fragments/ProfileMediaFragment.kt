package com.spidroid.starry.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.spidroid.starry.activities.ComposePostActivity
import com.spidroid.starry.activities.MediaViewerActivity
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.adapters.PostMediaAdapter
import com.spidroid.starry.databinding.FragmentProfilePostsBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.ProfileMediaState
import com.spidroid.starry.viewmodels.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileMediaFragment : Fragment(), PostInteractionListener {

    private var _binding: FragmentProfilePostsBinding? = null
    private val binding get() = _binding!!

    private var userId: String? = null
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var mediaAdapter: PostMediaAdapter

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
            profileViewModel.fetchMediaForUser(it)
        }
    }

    private fun setupRecyclerView() {
        mediaAdapter = PostMediaAdapter(this)
        binding.recyclerView.layoutManager = GridLayoutManager(context, 3)
        binding.recyclerView.adapter = mediaAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            profileViewModel.mediaState.collectLatest { state ->
                binding.tvEmptyPosts.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE
                when (state) {
                    is ProfileMediaState.Loading -> { /* Handle loading state */ }
                    is ProfileMediaState.Success -> {
                        binding.recyclerView.visibility = View.VISIBLE
                        val allMediaUrls = state.mediaPosts.flatMap { it.mediaUrls }
                        mediaAdapter.submitList(allMediaUrls)
                    }
                    is ProfileMediaState.Empty -> {
                        binding.tvEmptyPosts.visibility = View.VISIBLE
                        binding.tvEmptyPosts.text = "This user hasn't posted any media yet."
                    }
                    is ProfileMediaState.Error -> {
                        binding.tvEmptyPosts.visibility = View.VISIBLE
                        binding.tvEmptyPosts.text = state.message
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val USER_ID_ARG = "USER_ID"
        @JvmStatic
        fun newInstance(userId: String) = ProfileMediaFragment().apply {
            arguments = bundleOf(USER_ID_ARG to userId)
        }
    }

    // --- تطبيق جميع دوال الواجهة ---

    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {
        val urls = mediaUrls?.filterNotNull()?.let { ArrayList(it) } ?: return
        MediaViewerActivity.launch(requireActivity(), urls, position, sharedView)
    }

    override fun onVideoPlayClicked(videoUrl: String?) {
        videoUrl?.let { onMediaClicked(mutableListOf(it), 0, binding.root) }
    }

    // --- بقية الدوال مع ترك جسمها فارغًا لأنها غير مستخدمة هنا ---
    override fun onLikeClicked(post: PostModel?) {}
    override fun onCommentClicked(post: PostModel?) {}
    override fun onRepostClicked(post: PostModel?) {}
    override fun onQuoteRepostClicked(post: PostModel?) {}
    override fun onBookmarkClicked(post: PostModel?) {}
    override fun onMenuClicked(post: PostModel?, anchorView: View?) {}
    override fun onTogglePinPostClicked(post: PostModel?) {}
    override fun onEditPost(post: PostModel?) {}
    override fun onDeletePost(post: PostModel?) {}
    override fun onCopyLink(post: PostModel?) {}
    override fun onSharePost(post: PostModel?) {}
    override fun onEditPostPrivacy(post: PostModel?) {}
    override fun onReportPost(post: PostModel?) {}
    override fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?) {}
    override fun onReactionSelected(post: PostModel?, emojiUnicode: String) {}
    override fun onEmojiSummaryClicked(post: PostModel?) {}
    override fun onHashtagClicked(hashtag: String?) {}
    override fun onPostLongClicked(post: PostModel?) {}
    override fun onLayoutClicked(post: PostModel?) {}
    override fun onSeeMoreClicked(post: PostModel?) {}
    override fun onTranslateClicked(post: PostModel?) {}
    override fun onShowOriginalClicked(post: PostModel?) {}
    override fun onModeratePost(post: PostModel?) {}
    override fun onUserClicked(user: UserModel?) {}
    override fun onFollowClicked(user: UserModel?) {}
}
