package com.spidroid.starry.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.activities.ComposePostActivity
import com.spidroid.starry.activities.CreateStoryActivity
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.StoryViewerActivity
import com.spidroid.starry.adapters.OnStoryClickListener
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.adapters.StoryAdapter
import com.spidroid.starry.databinding.FragmentHomeBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(), OnStoryClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val storyViewModel: StoryViewModel by viewModels()
    private val postViewModel: PostViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModels()
    }

    override fun onResume() {
        super.onResume()
        // ١. إضافة قيمة للـ limit
        postViewModel.fetchInitialPosts(10)
        storyViewModel.fetchStoriesForCurrentUserAndFollowing()
    }

    private fun setupUI() {
        setupRecyclerViews()
        setupComposeBar()
        binding.swipeRefreshLayout.setOnRefreshListener {
            // ١. إضافة قيمة للـ limit
            postViewModel.fetchInitialPosts(10)
            storyViewModel.fetchStoriesForCurrentUserAndFollowing()
            binding.swipeRefreshLayout.isRefreshing = false // إيقاف التحديث
        }
    }

    private fun setupRecyclerViews() {
        storyAdapter = StoryAdapter(requireContext(), mainViewModel.currentUser.value?.userId, this)
        binding.rvStories.adapter = storyAdapter

        // التأكد من تطبيق جميع الدوال في Listener
        postAdapter = PostAdapter(requireContext(), object : PostInteractionListener {
            override fun onUserClicked(user: UserModel?) {}
            override fun onLikeClicked(post: PostModel?) {}
            override fun onCommentClicked(post: PostModel?) {}
            override fun onRepostClicked(post: PostModel?) {}
            override fun onBookmarkClicked(post: PostModel?) {}
            override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {}
            override fun onQuoteRepostClicked(post: PostModel?) {}
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
            override fun onVideoPlayClicked(videoUrl: String?) {}
            override fun onSeeMoreClicked(post: PostModel?) {}
            override fun onTranslateClicked(post: PostModel?) {}
            override fun onShowOriginalClicked(post: PostModel?) {}
            override fun onFollowClicked(user: UserModel?) {}
            override fun onModeratePost(post: PostModel?) {}
            override fun onLayoutClicked(post: PostModel?) {
                post?.postId?.let {
                    val intent = Intent(activity, PostDetailActivity::class.java).apply {
                        putExtra("POST_ID", it)
                    }
                    startActivity(intent)
                }
            }
        })
        binding.feedRecyclerView.adapter = postAdapter
        binding.feedRecyclerView.isNestedScrollingEnabled = false
    }

    private fun setupComposeBar() {
        binding.composeBar.root.setOnClickListener {
            startActivity(Intent(activity, ComposePostActivity::class.java))
        }
    }

    private fun observeViewModels() {
        mainViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                Glide.with(this)
                    .load(it.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_outline)
                    .into(binding.composeBar.composeBarProfileImage)
            }
        }

        // ٢. استخدام LiveData الصحيح: combinedFeed
        postViewModel.combinedFeed.observe(viewLifecycleOwner) { feedItems ->
            // ٣. فلترة القائمة للحصول على المنشورات فقط
            val posts = feedItems.filterIsInstance<PostModel>()
            postAdapter.submitList(posts)
        }

        // ٤. مراقبة LiveData الخاص بالخطأ
        postViewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAddStoryClicked() {
        startActivity(Intent(activity, CreateStoryActivity::class.java))
    }

    override fun onViewMyStoryClicked() {
        mainViewModel.currentUser.value?.userId?.let {
            startActivity(Intent(activity, StoryViewerActivity::class.java).apply {
                putExtra(StoryViewerActivity.EXTRA_USER_ID, it)
            })
        }
    }

    override fun onStoryPreviewClicked(user: UserModel) {
        startActivity(Intent(activity, StoryViewerActivity::class.java).apply {
            putExtra(StoryViewerActivity.EXTRA_USER_ID, user.userId)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.feedRecyclerView.adapter = null
        binding.rvStories.adapter = null
        _binding = null
    }
}