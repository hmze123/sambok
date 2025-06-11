package com.spidroid.starry.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.spidroid.starry.adapters.PostAdapter
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.ActivityBookmarksBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.BookmarksViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarksBinding
    private val viewModel: BookmarksViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadBookmarkedPosts()
    }

    private fun setupToolbar() {
        binding.toolbarBookmarks.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(this, object : PostInteractionListener {
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

            // --- ١. تم تصحيح اسم الدالة هنا ---
            override fun onLayoutClicked(post: PostModel?) {
                post?.postId?.let {
                    val intent = Intent(this@BookmarksActivity, PostDetailActivity::class.java)
                    intent.putExtra("POST_ID", it)
                    startActivity(intent)
                }
            }
        })

        binding.recyclerViewBookmarks.apply {
            layoutManager = LinearLayoutManager(this@BookmarksActivity)
            adapter = postAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarBookmarks.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                binding.textEmptyBookmarks.visibility = View.VISIBLE
                binding.textEmptyBookmarks.text = it
                binding.recyclerViewBookmarks.visibility = View.GONE
            } ?: run {
                binding.textEmptyBookmarks.visibility = View.GONE
            }
        }

        viewModel.bookmarkedPosts.observe(this) { posts ->
            if (posts.isNullOrEmpty()) {
                binding.textEmptyBookmarks.visibility = View.VISIBLE
                binding.recyclerViewBookmarks.visibility = View.GONE
            } else {
                binding.textEmptyBookmarks.visibility = View.GONE
                binding.recyclerViewBookmarks.visibility = View.VISIBLE
                postAdapter.submitList(posts)
            }
        }
    }
}