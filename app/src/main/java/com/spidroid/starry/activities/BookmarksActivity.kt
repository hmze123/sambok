package com.spidroid.starry.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.ActivityBookmarksBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.BookmarksViewModel
import com.spidroid.starry.viewmodels.PostViewModel
import com.spidroid.starry.viewmodels.UiState

class BookmarksActivity : AppCompatActivity(), PostInteractionListener {

    private lateinit var binding: ActivityBookmarksBinding
    private val viewModel: BookmarksViewModel by viewModels()
    private val postInteractionViewModel: PostViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        viewModel.fetchBookmarkedPosts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarBookmarks)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarBookmarks.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(this, this)
        binding.recyclerViewBookmarks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewBookmarks.adapter = postAdapter
    }

    private fun observeViewModel() {
        viewModel.bookmarkedPosts.observe(this) { posts ->
            postAdapter.submitCombinedList(posts ?: emptyList())
            binding.textEmptyBookmarks.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBarBookmarks.visibility = View.VISIBLE
                    binding.textEmptyBookmarks.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBarBookmarks.visibility = View.GONE
                }
                is UiState.Error -> {
                    binding.progressBarBookmarks.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressBarBookmarks.visibility = View.GONE
                }
            }
        }

        postInteractionViewModel.postUpdatedEvent.observe(this) {
            // إعادة تحميل القائمة عند حدوث أي تغيير (مثل إلغاء الحفظ)
            viewModel.fetchBookmarkedPosts()
        }
    }

    // --- تطبيق جميع دوال PostInteractionListener ---

    override fun onLikeClicked(post: PostModel?) {
        post?.let { postInteractionViewModel.toggleLike(it, !it.isLiked) }
    }

    override fun onBookmarkClicked(post: PostModel?) {
        post?.postId?.let { postInteractionViewModel.toggleBookmark(it, !post.isBookmarked) }
    }

    override fun onCommentClicked(post: PostModel?) {
        post?.let {
            val intent = Intent(this, PostDetailActivity::class.java).apply {
                putExtra(PostDetailActivity.EXTRA_POST, it)
            }
            startActivity(intent)
        }
    }

    override fun onUserClicked(user: UserModel?) {
        user?.userId?.let {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("userId", it)
            }
            startActivity(intent)
        }
    }

    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View) {
        val urls = mediaUrls?.filterNotNull()?.let { ArrayList(it) } ?: return
        MediaViewerActivity.launch(this, urls, position, sharedView)
    }

    override fun onQuoteRepostClicked(post: PostModel?) {
        val intent = Intent(this, ComposePostActivity::class.java).apply {
            putExtra(ComposePostActivity.EXTRA_QUOTE_POST, post)
        }
        startActivity(intent)
    }

    // --- بقية الدوال التي لا نحتاج لمنطق خاص بها هنا ---
    override fun onRepostClicked(post: PostModel?) {}
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
    override fun onLayoutClicked(post: PostModel?) {}
    override fun onSeeMoreClicked(post: PostModel?) {}
    override fun onTranslateClicked(post: PostModel?) {}
    override fun onShowOriginalClicked(post: PostModel?) {}
    override fun onModeratePost(post: PostModel?) {}
    override fun onFollowClicked(user: UserModel?) {}
}
