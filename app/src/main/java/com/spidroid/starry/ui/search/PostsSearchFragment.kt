package com.spidroid.starry.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.spidroid.starry.activities.MediaViewerActivity
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.FragmentSearchResultListBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.PostViewModel
import com.spidroid.starry.viewmodels.SearchViewModel

class PostsSearchFragment : Fragment(), PostInteractionListener {
    private var _binding: FragmentSearchResultListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by activityViewModels()
    private val postInteractionViewModel: PostViewModel by activityViewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchResultListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(requireContext(), this)
        binding.recyclerViewResults.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewResults.adapter = postAdapter
    }

    private fun observeViewModel() {
        viewModel.postResults.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitCombinedList(posts ?: emptyList())
            binding.textEmptyResults.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (postAdapter.itemCount == 0) {
                binding.progressBarResults.visibility = if (isLoading) View.VISIBLE else View.GONE
            } else {
                binding.progressBarResults.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- الدالة التي تم إصلاحها ---
    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {
        val urls = mediaUrls?.filterNotNull()?.let { ArrayList(it) } ?: return
        // بما أننا في قائمة بحث، سنفتح عارض الصور بدون أنيميشن معقد
        val intent = Intent(activity, MediaViewerActivity::class.java).apply {
            putStringArrayListExtra("media_urls", urls)
            putExtra("position", position)
        }
        startActivity(intent)
    }

    // --- بقية دوال الواجهة ---
    override fun onLikeClicked(post: PostModel?) { post?.let { postInteractionViewModel.toggleLike(it, !it.isLiked) } }
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
            val intent = Intent(activity, ProfileActivity::class.java).apply {
                putExtra("userId", it)
            }
            startActivity(intent)
        }
    }
    override fun onRepostClicked(post: PostModel?) { /* ... */ }
    override fun onBookmarkClicked(post: PostModel?) { /* ... */ }
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