package com.spidroid.starry.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityProfileBinding
import com.spidroid.starry.fragments.ProfileMediaFragment
import com.spidroid.starry.fragments.ProfilePostsFragment
import com.spidroid.starry.fragments.ProfileRepliesFragment
import com.spidroid.starry.models.UserModel
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var userId: String? = null
    private var currentAuthUserId: String? = null
    private var isCurrentUserProfile: Boolean = false
    private var isFollowing: Boolean = false
    private var displayedUserProfile: UserModel? = null
    private var userProfileListener: ListenerRegistration? = null

    private companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.user_id_not_provided), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentAuthUserId = auth.currentUser?.uid
        isCurrentUserProfile = userId == currentAuthUserId

        setupUI()
        setupTabs()
    }

    override fun onStart() {
        super.onStart()
        loadUserDataWithListener()
    }

    override fun onStop() {
        super.onStop()
        userProfileListener?.remove()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.btnBack.setOnClickListener { finish() }

        if (isCurrentUserProfile) {
            binding.btnAction.setText(R.string.edit_profile)
            binding.btnAction.setIconResource(R.drawable.ic_edit)
            binding.btnAction.setOnClickListener {
                startActivity(Intent(this, EditProfileActivity::class.java))
            }
            binding.btnSettings.visibility = View.VISIBLE
            binding.btnSettings.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        } else {
            binding.btnSettings.visibility = View.GONE
            binding.btnAction.visibility = View.VISIBLE
            binding.btnAction.setOnClickListener { toggleFollow() }
        }

        setupCollapsingToolbar()
        setupFollowClickListeners()
    }

    private fun setupCollapsingToolbar() {
        var isShow = true
        var scrollRange = -1
        binding.appbarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if (scrollRange + verticalOffset == 0) {
                val user = displayedUserProfile
                binding.collapsingToolbar.title = user?.displayName ?: user?.username
                isShow = true
            } else if (isShow) {
                binding.collapsingToolbar.title = " " // Empty space to hide title
                isShow = false
            }
        })
    }

    private fun setupFollowClickListeners() {
        binding.layoutFollowingInfo.setOnClickListener {
            val intent = Intent(this, FollowingListActivity::class.java).apply {
                putExtra(FollowersListActivity.EXTRA_USER_ID, userId)
            }
            startActivity(intent)
        }

        binding.layoutFollowersInfo.setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java).apply {
                putExtra(FollowersListActivity.EXTRA_USER_ID, userId)
                putExtra(FollowersListActivity.EXTRA_LIST_TYPE, "followers")
            }
            startActivity(intent)
        }
    }

    private fun loadUserDataWithListener() {
        userProfileListener?.remove()
        val targetUserId = userId ?: return

        userProfileListener = db.collection("users").document(targetUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    displayedUserProfile = snapshot.toObject(UserModel::class.java)?.apply { this.userId = snapshot.id }
                    displayedUserProfile?.let {
                        populateProfileData(it)
                        if (!isCurrentUserProfile) {
                            checkFollowingStatus()
                        }
                    }
                } else {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkFollowingStatus() {
        val authId = currentAuthUserId ?: return
        isFollowing = displayedUserProfile?.followers?.containsKey(authId) == true
        updateFollowButtonState(isFollowing)
    }

    private fun updateFollowButtonState(isCurrentlyFollowing: Boolean) {
        if (isCurrentUserProfile) return
        isFollowing = isCurrentlyFollowing
        if (isFollowing) {
            binding.btnAction.setText(R.string.following)
            binding.btnAction.icon = null
        } else {
            binding.btnAction.setText(R.string.follow)
            binding.btnAction.setIconResource(R.drawable.ic_add)
        }
        binding.btnAction.isEnabled = true
    }

    private fun toggleFollow() {
        val authId = currentAuthUserId ?: run {
            Toast.makeText(this, getString(R.string.login_to_follow), Toast.LENGTH_SHORT).show()
            return
        }
        val targetId = userId ?: return
        binding.btnAction.isEnabled = false

        val currentUserDocRef = db.collection("users").document(authId)
        val targetUserDocRef = db.collection("users").document(targetId)

        db.runTransaction { transaction ->
            val isCurrentlyFollowing = transaction.get(targetUserDocRef)
                .get("followers.$authId") as? Boolean ?: false

            if (isCurrentlyFollowing) {
                // Unfollow
                transaction.update(currentUserDocRef, "following.$targetId", FieldValue.delete())
                transaction.update(targetUserDocRef, "followers.$authId", FieldValue.delete())
            } else {
                // Follow
                transaction.update(currentUserDocRef, "following.$targetId", true)
                transaction.update(targetUserDocRef, "followers.$authId", true)
            }
            null
        }.addOnSuccessListener {
            Log.d(TAG, "Follow status toggled successfully.")
            // The snapshot listener will automatically update the UI
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to toggle follow", e)
            Toast.makeText(this, "Failed to update follow status: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.btnAction.isEnabled = true
        }
    }

    private fun populateProfileData(user: UserModel) {
        binding.tvDisplayName.text = user.displayName ?: user.username
        binding.tvUsername.text = "@${user.username}"
        binding.tvBio.text = user.bio
        binding.tvBio.visibility = if (user.bio.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE

        Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_default_avatar).into(binding.ivAvatar)
        Glide.with(this).load(user.coverImageUrl).placeholder(R.color.m3_surfaceContainerLow).into(binding.ivCover)

        binding.tvFollowersCount.text = (user.followers.size).toString()
        binding.tvFollowingCount.text = (user.following.size).toString()

        populateSocialLinks(user.socialLinks)
    }

    private fun populateSocialLinks(socialLinks: Map<String, String>) {
        binding.layoutSocialLinksContainer.removeAllViews()
        binding.tvAboutTitle.visibility = if (socialLinks.isNotEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(this)
        socialLinks.forEach { (platform, url) ->
            if (url.isNotBlank()) {
                val socialLinkView = inflater.inflate(R.layout.item_profile_social_link, binding.layoutSocialLinksContainer, false)
                val ivPlatformIcon = socialLinkView.findViewById<ImageView>(R.id.ivPlatformIcon)
                val tvPlatformUrl = socialLinkView.findViewById<TextView>(R.id.tvPlatformUrl)

                ivPlatformIcon.setImageResource(getPlatformIcon(platform))
                tvPlatformUrl.text = url

                socialLinkView.setOnClickListener { openUrl(url) }
                binding.layoutSocialLinksContainer.addView(socialLinkView)
            }
        }
    }

    private fun getPlatformIcon(platform: String): Int {
        return when (platform.lowercase(Locale.getDefault())) {
            UserModel.SOCIAL_TWITTER -> R.drawable.ic_share // Replace with Twitter icon
            UserModel.SOCIAL_INSTAGRAM -> R.drawable.ic_add_photo // Replace with Instagram icon
            else -> R.drawable.ic_link
        }
    }

    private fun openUrl(url: String) {
        try {
            var formattedUrl = url
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                formattedUrl = "https://$url"
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        val safeUserId = userId ?: return
        val pagerAdapter = ProfilePagerAdapter(this, safeUserId)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.tabTitles[position]
        }.attach()
    }

    private class ProfilePagerAdapter(
        fa: FragmentActivity,
        private val userId: String
    ) : FragmentStateAdapter(fa) {

        val tabTitles = arrayOf("Posts", "Replies", "Media")

        override fun getItemCount(): Int = tabTitles.size

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ProfilePostsFragment.newInstance(userId)
                1 -> ProfileRepliesFragment.newInstance(userId)
                2 -> ProfileMediaFragment.newInstance(userId)
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }
}