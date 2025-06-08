package com.spidroid.starry.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.FragmentProfilePostsBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.PageViewModel

class PagePostsFragment : Fragment(), PostInteractionListener {

    private var _binding: FragmentProfilePostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PageViewModel by activityViewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfilePostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(requireContext(), this)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = postAdapter
    }

    private fun observeViewModel() {
        viewModel.pagePosts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitCombinedList(posts ?: emptyList())
            binding.tvEmptyPosts.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.tvEmptyPosts.text = "This page has no posts yet."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Implement all methods from PostInteractionListener with empty bodies ---
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
    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {}
    override fun onVideoPlayClicked(videoUrl: String?) {}
    override fun onLayoutClicked(post: PostModel?) {}
    override fun onSeeMoreClicked(post: PostModel?) {}
    override fun onTranslateClicked(post: PostModel?) {}
    override fun onShowOriginalClicked(post: PostModel?) {}
    override fun onModeratePost(post: PostModel?) {}
    override fun onUserClicked(user: UserModel?) {}
    override fun onFollowClicked(user: UserModel?) {}

    companion object {
        private const val PAGE_ID_ARG = "PAGE_ID"
        fun newInstance(pageId: String) = PagePostsFragment().apply {
            arguments = bundleOf(PAGE_ID_ARG to pageId)
        }
    }
}
