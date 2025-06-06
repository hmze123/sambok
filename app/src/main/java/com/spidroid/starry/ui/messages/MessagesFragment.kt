package com.spidroid.starry.ui.messages

import com.google.firebase.auth.FirebaseAuth

class MessagesFragment : androidx.fragment.app.Fragment() {
    private var binding: com.spidroid.starry.databinding.FragmentMessagesBinding? = null
    private var auth: FirebaseAuth? = null
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): android.view.View {
        binding = com.spidroid.starry.databinding.FragmentMessagesBinding.inflate(
            inflater,
            container,
            false
        )
        val root: android.view.View = binding!!.getRoot()
        auth = FirebaseAuth.getInstance()
        val fab: FloatingActionButton = binding!!.fabCreateNew
        fab.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> showNewChatDialog() })
        return root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
    }

    override fun onStart() {
        super.onStart()
        setupToolbar()
    }

    private fun setupToolbar() {
        val ivAvatar = binding!!.ivUserAvatar
        val currentUser: FirebaseUser? = auth.getCurrentUser()

        // Set default image first
        ivAvatar.setImageResource(R.drawable.ic_default_avatar)

        if (currentUser != null) {
            // Remove any existing listener to prevent duplicates
            if (userListener != null) userListener.remove()

            // Real-time listener to Firestore user document
            userListener =
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .addSnapshotListener(
                        { documentSnapshot, error ->
                            if (error != null || documentSnapshot == null || !documentSnapshot.exists()) return@addSnapshotListener
                            val user: UserModel? = documentSnapshot.toObject(UserModel::class.java)
                            if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl()
                                    .isEmpty()
                            ) {
                                Glide.with(requireContext())
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(ivAvatar)
                            }
                        })
        }

        ivAvatar.setOnClickListener(
            android.view.View.OnClickListener { v: android.view.View? ->
                if (currentUser != null) {
                    val profileIntent: Intent = Intent(getActivity(), ProfileActivity::class.java)
                    profileIntent.putExtra("userId", currentUser.getUid())
                    startActivity(profileIntent)
                } else {
                    Toast.makeText(getContext(), "Please login to view profile", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    private fun setupViewPager() {
        binding!!.viewPager.setAdapter(
            com.spidroid.starry.ui.messages.MessagesFragment.ViewPagerAdapter(
                this
            )
        )

        TabLayoutMediator(
            binding!!.tabLayout,
            binding!!.viewPager,
            TabConfigurationStrategy { tab: TabLayout.Tab?, position: Int ->
                when (position) {
                    0 -> tab.setText("Chats")
                    1 -> tab.setText("Groups")
                    2 -> tab.setText("Communities")
                }
            })
            .attach()
    }

    private inner class ViewPagerAdapter(fragment: androidx.fragment.app.Fragment) :
        androidx.viewpager2.adapter.FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            when (position) {
                0 -> return ChatsFragment()
                1 -> return GroupsFragment()
                2 -> return CommunitiesFragment()
                else -> throw java.lang.IllegalArgumentException("Invalid position")
            }
        }

        override fun getItemCount(): Int {
            return 3
        }
    }

    private fun showNewChatDialog() {
        val dialog = NewChatBottomSheetDialog()
        dialog.show(getParentFragmentManager(), "NewChatBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (userListener != null) {
            userListener.remove() // Clean up listener
            userListener = null
        }
        binding = null
    }
}
