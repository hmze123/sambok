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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
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

class ForYouFragment : Fragment(), PostInteractionListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val postViewModel: PostViewModel by activityViewModels()
    private lateinit var postAdapter: PostAdapter
    private var currentAuthUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentAuthUserId = FirebaseAuth.getInstance().currentUser?.uid
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

        // --- الكود الجديد للتحميل اللامتناهي ---
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // إذا كان المستخدم قد وصل إلى ما قبل نهاية القائمة بـ 5 عناصر، ابدأ الجلب
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && firstVisibleItemPosition >= 0) {
                    postViewModel.fetchMorePosts(10) // جلب 10 عناصر إضافية
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // عند السحب للتحديث، اجلب الدفعة الأولى من جديد
            postViewModel.fetchInitialPosts(15)
            postViewModel.fetchUserSuggestions()
        }
    }



    private fun setupObservers() {
        postViewModel.combinedFeed.observe(viewLifecycleOwner) { items ->
            binding.progressContainer.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            val currentList = items ?: emptyList()
            postAdapter.submitCombinedList(currentList)

            val isEmpty = currentList.isEmpty()
            binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
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

    private fun loadInitialData() {
        if (postAdapter.itemCount == 0) {
            binding.progressContainer.visibility = View.VISIBLE
            // عند التحميل الأولي، اجلب الدفعة الأولى
            postViewModel.fetchInitialPosts(15)
            postViewModel.fetchUserSuggestions()
        }
    }
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

    // ... (بقية دوال الواجهة تبقى كما هي)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // (الكود المتبقي من ForYouFragment مثل onMenuClicked, onUserClicked, إلخ...)
    override fun onMenuClicked(post: PostModel?, anchorView: View?) {
        val safePost = post ?: return
        val safeAnchor = anchorView ?: return

        PopupMenu(requireContext(), safeAnchor).apply {
            menuInflater.inflate(R.menu.post_options_menu, menu)
            val currentAuthUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isAuthor = currentAuthUserId == safePost.authorId

            menu.findItem(R.id.action_pin_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_edit_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_delete_post)?.isVisible = isAuthor
            menu.findItem(R.id.action_edit_privacy)?.isVisible = isAuthor
            menu.findItem(R.id.action_report_post)?.isVisible = !isAuthor
            menu.findItem(R.id.action_save_post)?.title = if (safePost.isBookmarked) "Unsave" else "Save"

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_pin_post -> onTogglePinPostClicked(safePost)
                    R.id.action_edit_post -> onEditPost(safePost)
                    R.id.action_delete_post -> onDeletePost(safePost)
                    R.id.action_copy_link -> onCopyLink(safePost)
                    R.id.action_share_post -> onSharePost(safePost)
                    R.id.action_save_post -> onBookmarkClicked(safePost)
                    R.id.action_edit_privacy -> onEditPostPrivacy(safePost)
                    R.id.action_report_post -> onReportPost(safePost)
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
        }.show()
    }
    override fun onTogglePinPostClicked(post: PostModel?) { post?.let { postViewModel.togglePostPinStatus(it) } }
    override fun onEditPost(post: PostModel?) { Toast.makeText(context, "Edit feature coming soon", Toast.LENGTH_SHORT).show() }
    override fun onDeletePost(post: PostModel?) { post?.postId?.let { postViewModel.deletePost(it) } }
    override fun onCopyLink(post: PostModel?) { /* ... */ }
    override fun onSharePost(post: PostModel?) { /* ... */ }
    override fun onEditPostPrivacy(post: PostModel?) { /* ... */ }
    override fun onReportPost(post: PostModel?) { /* ... */ }
    override fun onEmojiSummaryClicked(post: PostModel?) { /* ... */ }
    override fun onHashtagClicked(hashtag: String?) { /* ... */ }
    override fun onPostLongClicked(post: PostModel?) { /* ... */ }
    override fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int) { /* ... */ }
    override fun onVideoPlayClicked(videoUrl: String?) { /* ... */ }
    override fun onLayoutClicked(post: PostModel?) { onCommentClicked(post) }
    override fun onSeeMoreClicked(post: PostModel?) { /* ... */ }
    override fun onTranslateClicked(post: PostModel?) { /* ... */ }
    override fun onShowOriginalClicked(post: PostModel?) { /* ... */ }
    override fun onModeratePost(post: PostModel?) { /* ... */ }
    override fun onUserClicked(user: UserModel?) { /* ... */ }
    override fun onFollowClicked(user: UserModel?) { /* ... */ }

}