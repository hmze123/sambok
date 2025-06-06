// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/messages/MessagesFragment.kt
package com.spidroid.starry.ui.messages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.spidroid.starry.R
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.databinding.FragmentMessagesBinding
import com.spidroid.starry.models.UserModel


class MessagesFragment : Fragment() {
    private var binding: FragmentMessagesBinding? = null
    private var auth: FirebaseAuth? = null
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagesBinding.inflate(
            inflater,
            container,
            false
        )
        val root: View = binding!!.root
        auth = FirebaseAuth.getInstance()
        val fab: FloatingActionButton = binding!!.fabCreateNew
        fab.setOnClickListener { v: View? -> showNewChatDialog() }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
    }

    override fun onStart() {
        super.onStart()
        setupToolbar()
    }

    private fun setupToolbar() {
        val ivAvatar = binding!!.ivUserAvatar
        val currentUser: FirebaseUser? = auth?.currentUser

        // Set default image first
        ivAvatar.setImageResource(R.drawable.ic_default_avatar)

        if (currentUser != null) {
            // Remove any existing listener to prevent duplicates
            userListener?.remove()

            // Real-time listener to Firestore user document
            userListener =
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .addSnapshotListener(
                        { documentSnapshot, error ->
                            if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                                return@addSnapshotListener
                            }
                            val user: UserModel? = documentSnapshot.toObject(UserModel::class.java)
                            if (user != null && user.profileImageUrl != null && !user.profileImageUrl.isNullOrEmpty()
                            ) {
                                Glide.with(requireContext())
                                    .load(user.profileImageUrl)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(ivAvatar)
                            }
                        })
        }

        ivAvatar.setOnClickListener(
            View.OnClickListener { v: View? ->
                if (currentUser != null) {
                    val profileIntent: Intent = Intent(activity, ProfileActivity::class.java)
                    profileIntent.putExtra("userId", currentUser.uid)
                    startActivity(profileIntent)
                } else {
                    Toast.makeText(requireContext(), "Please login to view profile", Toast.LENGTH_SHORT) // ✨ تم تغيير context إلى requireContext()
                        .show()
                }
            })
    }

    private fun setupViewPager() {
        binding!!.viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(
            binding!!.tabLayout,
            binding!!.viewPager,
            { tab: TabLayout.Tab, position: Int ->
                when (position) {
                    0 -> tab.text = "Chats"
                    1 -> tab.text = "Groups"
                    2 -> tab.text = "Communities"
                }
            })
            .attach()
    }

    private inner class ViewPagerAdapter(fragment: Fragment) :
        FragmentStateAdapter(fragment) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return ChatsFragment()
                1 -> return GroupsFragment()
                2 -> return CommunitiesFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }

        override fun getItemCount(): Int {
            return 3
        }
    }

    private fun showNewChatDialog() {
        val dialog = NewChatBottomSheetDialog()
        dialog.show(parentFragmentManager, "NewChatBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        userListener = null
        binding = null
    }
}