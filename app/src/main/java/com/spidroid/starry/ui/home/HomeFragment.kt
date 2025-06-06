package com.spidroid.starry.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.activities.ComposePostActivity
import com.spidroid.starry.activities.CreateStoryActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.activities.StoryViewerActivity
import com.spidroid.starry.adapters.OnStoryClickListener
import com.spidroid.starry.adapters.StoryAdapter
import com.spidroid.starry.databinding.FragmentHomeBinding
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.StoryFeedState
import com.spidroid.starry.viewmodels.StoryViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), OnStoryClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // تهيئة الـ ViewModel بشكل آمن ومناسب للـ Fragment
    private val storyViewModel: StoryViewModel by viewModels()
    private lateinit var storyAdapter: StoryAdapter
    private val auth by lazy { Firebase.auth }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        // إعادة تحميل البيانات عند عودة المستخدم للواجهة لضمان حداثتها
        // ViewModel سيمنع التحميل المتكرر إذا لم يكن هناك حاجة لذلك
        storyViewModel.fetchStoriesForCurrentUserAndFollowing()
    }

    private fun setupUI() {
        setupStoriesRecyclerView()
        setupViewPager()
        setupToolbar()
        binding.fabCompose.setOnClickListener {
            startActivity(Intent(activity, ComposePostActivity::class.java))
        }
    }

    private fun setupStoriesRecyclerView() {
        // التأكد من أن السياق (Context) متاح قبل إنشاء الـ Adapter
        storyAdapter = StoryAdapter(requireContext(), auth.currentUser?.uid, this)
        binding.rvStories.adapter = storyAdapter
        // يمكنك أيضاً إضافة LayoutManager هنا إذا لم يكن محدداً في الـ XML
        // binding.rvStories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupObservers() {
        // مراقبة بيانات المستخدم الحالي لتحديث الأفاتار
        storyViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                storyAdapter.setCurrentUser(it)
                updateToolbarAvatar(it.profileImageUrl)
            }
        }

        // استخدام Coroutine لمراقبة StateFlow بشكل آمن ومتوافق مع دورة الحياة
        viewLifecycleOwner.lifecycleScope.launch {
            storyViewModel.storyFeedState.collectLatest { state ->
                when (state) {
                    is StoryFeedState.Loading -> {
                        // يمكنك إضافة مؤشر تحميل هنا لشريط القصص إذا أردت
                        Log.d("HomeFragment", "Loading stories...")
                    }
                    is StoryFeedState.Success -> {
                        // تحديث الـ Adapter بالبيانات الجديدة
                        storyAdapter.setStories(state.storyPreviews, state.hasMyActiveStory)
                        Log.d("HomeFragment", "Stories loaded successfully: ${state.storyPreviews.size} previews.")
                    }
                    is StoryFeedState.Error -> {
                        Log.e("HomeFragment", "Error loading stories: ${state.message}")
                        // يمكنك عرض رسالة خطأ للمستخدم هنا
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.ivUserAvatar.setOnClickListener {
            auth.currentUser?.uid?.let { userId ->
                val intent = Intent(activity, ProfileActivity::class.java).apply {
                    putExtra("userId", userId)
                }
                startActivity(intent)
            }
        }
    }

    private fun updateToolbarAvatar(imageUrl: String?) {
        // التحقق من أن الـ Fragment ما زال مضافًا وأن السياق متاح
        if (!isAdded) return

        Glide.with(requireContext())
            .load(imageUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivUserAvatar)
    }

    // --- OnStoryClickListener Implementation ---
    override fun onAddStoryClicked() {
        startActivity(Intent(activity, CreateStoryActivity::class.java))
    }

    override fun onViewMyStoryClicked() {
        auth.currentUser?.uid?.let {
            val intent = Intent(activity, StoryViewerActivity::class.java).apply {
                putExtra(StoryViewerActivity.EXTRA_USER_ID, it)
            }
            startActivity(intent)
        }
    }

    override fun onStoryPreviewClicked(user: UserModel) {
        user.userId.let {
            val intent = Intent(activity, StoryViewerActivity::class.java).apply {
                putExtra(StoryViewerActivity.EXTRA_USER_ID, it)
            }
            startActivity(intent)
        }
    }
    // --- End OnStoryClickListener ---

    private fun setupViewPager() {
        binding.viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.title_home) else getString(R.string.following)
        }.attach()
    }

    private class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            // استخدام ForYouFragment و FollowingFragment
            return if (position == 0) ForYouFragment() else FollowingFragment()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // تجنب تسريب الذاكرة
    }
}