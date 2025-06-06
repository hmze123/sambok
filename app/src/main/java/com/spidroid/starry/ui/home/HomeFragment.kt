package com.spidroid.starry.ui.home

import com.google.firebase.auth.FirebaseAuth

class HomeFragment : androidx.fragment.app.Fragment(), OnStoryClickListener {
    private var binding: com.spidroid.starry.databinding.FragmentHomeBinding? = null
    private var storyViewModel: StoryViewModel? = null
    private var storyAdapter: StoryAdapter? = null
    private var auth: FirebaseAuth? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        binding =
            com.spidroid.starry.databinding.FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        storyViewModel = ViewModelProvider(this).get<StoryViewModel>(StoryViewModel::class.java)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        // جلب البيانات عند بدء الـ fragment
        storyViewModel.fetchCurrentUser()
        storyViewModel.fetchStoriesForCurrentUserAndFollowing()
    }

    private fun setupUI() {
        binding!!.fabCompose.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            startActivity(
                Intent(getActivity(), ComposePostActivity::class.java)
            )
        })

        setupViewPager()
        setupStoriesRecyclerView()
        setupToolbar()
    }

    private fun setupStoriesRecyclerView() {
        val currentUserId: kotlin.String? =
            if (auth.getCurrentUser() != null) auth.getCurrentUser().getUid() else ""
        storyAdapter = StoryAdapter(requireContext(), currentUserId, this)
        binding!!.rvStories.setLayoutManager(
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        binding!!.rvStories.setAdapter(storyAdapter)
    }

    private fun setupObservers() {
        // مراقبة بيانات المستخدم الحالي لتحديث صورة الأفاتار في الشريط العلوي
        storyViewModel.getCurrentUser()
            .observe(getViewLifecycleOwner(), androidx.lifecycle.Observer { user: UserModel? ->
                if (user != null) {
                    storyAdapter.setCurrentUser(user)
                    updateToolbarAvatar(user.getProfileImageUrl())
                }
            })

        // هذا هو المراقب الرئيسي الذي يستقبل كل بيانات القصص الجاهزة
        storyViewModel.getStoryFeedState().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { state: StoryFeedState? ->
                if (state != null) {
                    // تمرير البيانات المجهزة إلى الـ Adapter
                    storyAdapter.setStories(state.stories, state.hasMyActiveStory)
                    storyAdapter.setViewedStories(state.viewedStoryIds)
                }
            })
    }

    private fun setupToolbar() {
        binding!!.ivUserAvatar.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            if (auth.getCurrentUser() != null) {
                val intent: Intent = Intent(getActivity(), ProfileActivity::class.java)
                intent.putExtra("userId", auth.getCurrentUser().getUid())
                startActivity(intent)
            }
        })
    }

    private fun updateToolbarAvatar(imageUrl: kotlin.String?) {
        if (isAdded() && imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding!!.ivUserAvatar)
        }
    }

    // --- تنفيذ واجهات OnStoryClickListener ---
    override fun onAddStoryClicked() {
        startActivity(Intent(getActivity(), CreateStoryActivity::class.java))
    }

    override fun onViewMyStoryClicked() {
        if (auth.getCurrentUser() != null) {
            val intent: Intent = Intent(getActivity(), StoryViewerActivity::class.java)
            intent.putExtra("userId", auth.getCurrentUser().getUid())
            startActivity(intent)
        }
    }

    override fun onStoryPreviewClicked(story: StoryModel) {
        val intent: Intent = Intent(getActivity(), StoryViewerActivity::class.java)
        intent.putExtra("userId", story.getUserId())
        startActivity(intent)
    }

    // --- ViewPager and other Fragment methods ---
    private fun setupViewPager() {
        binding!!.viewPager.setAdapter(
            com.spidroid.starry.ui.home.HomeFragment.ViewPagerAdapter(
                this
            )
        )
        TabLayoutMediator(
            binding!!.tabLayout, binding!!.viewPager,
            TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int -> tab.setText(if (position == 0) "For You" else "Following") })
            .attach()
    }

    private class ViewPagerAdapter(fragment: androidx.fragment.app.Fragment) :
        androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return if (position == 0) ForYouFragment() else FollowingFragment()
        }

        override fun getItemCount(): Int {
            return 2
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}