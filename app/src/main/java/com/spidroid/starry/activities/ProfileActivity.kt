// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/ProfileActivity.kt
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityProfileBinding
import com.spidroid.starry.fragments.ProfileMediaFragment
import com.spidroid.starry.fragments.ProfilePostsFragment
import com.spidroid.starry.fragments.ProfileRepliesFragment
import com.spidroid.starry.models.UserModel
import java.util.Locale
import com.spidroid.starry.activities.FollowersListActivity // ✨ تم إضافة هذا الاستيراد
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var targetUserId: String? = null
    private var currentAuthUserId: String? = null
    private var isCurrentUserProfile: Boolean = false

    private var displayedUserProfile: UserModel? = null
    private var userProfileListener: ListenerRegistration? = null

    private companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetUserId = intent.getStringExtra("userId")
        if (targetUserId.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.user_id_not_provided), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentAuthUserId = auth.currentUser?.uid
        isCurrentUserProfile = targetUserId == currentAuthUserId

        setupUI()
        setupTabs()
    }

    override fun onStart() {
        super.onStart()
        attachProfileListener()
    }

    override fun onStop() {
        super.onStop()
        userProfileListener?.remove()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.btnBack.setOnClickListener { finish() }

        setupActionAndSettingsButtons()
        setupCollapsingToolbar()
        setupFollowClickListeners()
    }

    private fun setupActionAndSettingsButtons() {
        if (isCurrentUserProfile) {
            binding.btnAction.text = getString(R.string.edit_profile)
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
    }

    private fun setupCollapsingToolbar() {
        var isShow = true
        var scrollRange = -1
        binding.appbarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if (scrollRange + verticalOffset == 0) {
                binding.collapsingToolbar.title = displayedUserProfile?.displayName ?: displayedUserProfile?.username
                isShow = true
            } else if (isShow) {
                binding.collapsingToolbar.title = " " // مسافة لإخفاء العنوان عند التوسيع
                isShow = false
            }
        })
    }

    private fun setupFollowClickListeners() {
        binding.layoutFollowingInfo.setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java).apply { // ✨ تم تغيير UserListActivity إلى FollowersListActivity
                putExtra(FollowersListActivity.EXTRA_USER_ID, targetUserId) // ✨ تم تغيير UserListActivity إلى FollowersListActivity
                putExtra(FollowersListActivity.EXTRA_LIST_TYPE, "following") // ✨ تم تغيير UserListActivity إلى FollowersListActivity
            }
            startActivity(intent)
        }

        binding.layoutFollowersInfo.setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java).apply { // ✨ تم تغيير UserListActivity إلى FollowersListActivity
                putExtra(FollowersListActivity.EXTRA_USER_ID, targetUserId) // ✨ تم تغيير UserListActivity إلى FollowersListActivity
                putExtra(FollowersListActivity.EXTRA_LIST_TYPE, "followers") // ✨ تم تغيير UserListActivity إلى FollowersListActivity
            }
            startActivity(intent)
        }
    }

    private fun attachProfileListener() {
        userProfileListener?.remove()
        userProfileListener = db.collection("users").document(targetUserId!!)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    displayedUserProfile = snapshot.toObject(UserModel::class.java)?.apply { this.userId = snapshot.id }
                    displayedUserProfile?.let { updateProfileUI(it) }
                } else {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateProfileUI(user: UserModel) {
        binding.tvDisplayName.text = user.displayName ?: user.username
        binding.tvUsername.text = "@${user.username}"
        binding.tvBio.text = user.bio
        binding.tvBio.visibility = if (user.bio.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE

        Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_default_avatar).into(binding.ivAvatar)
        Glide.with(this).load(user.coverImageUrl).placeholder(R.color.md_theme_primary).into(binding.ivCover)

        binding.tvFollowersCount.text = user.followers.size.toString()
        binding.tvFollowingCount.text = user.following.size.toString()

        if (!isCurrentUserProfile) {
            val isFollowing = user.followers.containsKey(currentAuthUserId)
            updateFollowButtonState(isFollowing)
        }

        populateSocialLinks(user.socialLinks)
    }

    private fun updateFollowButtonState(isCurrentlyFollowing: Boolean) {
        if (isCurrentlyFollowing) {
            binding.btnAction.text = getString(R.string.following)
            binding.btnAction.icon = null
        } else {
            binding.btnAction.text = getString(R.string.follow)
            binding.btnAction.setIconResource(R.drawable.ic_add)
        }
        binding.btnAction.isEnabled = true
    }

    private fun toggleFollow() {
        val authId = currentAuthUserId ?: run {
            Toast.makeText(this, getString(R.string.login_to_follow), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAction.isEnabled = false // تعطيل الزر لمنع النقرات المتعددة

        val targetUserRef = db.collection("users").document(targetUserId!!)
        val currentUserRef = db.collection("users").document(authId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(targetUserRef)
            val followers = (snapshot.get("followers") as? Map<String, Boolean>) ?: emptyMap()
            val isCurrentlyFollowing = followers.containsKey(authId)

            if (isCurrentlyFollowing) {
                // Unfollow
                transaction.update(targetUserRef, "followers.$authId", FieldValue.delete())
                transaction.update(currentUserRef, "following.$targetUserId", FieldValue.delete())
            } else {
                // Follow
                transaction.update(targetUserRef, "followers.$authId", true)
                transaction.update(currentUserRef, "following.$targetUserId", true)
            }
            // لا حاجة لإعادة أي شيء
        }.addOnSuccessListener {
            Log.d(TAG, "Follow status toggled successfully.")
            // سيتم تحديث الواجهة تلقائياً بفضل الـ SnapshotListener
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to toggle follow", e)
            binding.btnAction.isEnabled = true // إعادة تفعيل الزر عند الفشل
            Toast.makeText(this, getString(R.string.failed_to_update_follow_status, e.message), Toast.LENGTH_SHORT).show()
        }
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
        // ملاحظة: يفضل استخدام أيقونات مخصصة لكل منصة بدلاً من الأيقونات العامة
        // تأكد من إضافة هذه الأيقونات إلى مجلد res/drawable في مشروعك
        return when (platform.lowercase(Locale.getDefault())) {
            UserModel.SOCIAL_TWITTER -> R.drawable.ic_link // استبدل بـ R.drawable.ic_twitter
            UserModel.SOCIAL_INSTAGRAM -> R.drawable.ic_link // استبدل بـ R.drawable.ic_instagram
            UserModel.SOCIAL_FACEBOOK -> R.drawable.ic_link // استبدل بـ R.drawable.ic_facebook
            UserModel.SOCIAL_LINKEDIN -> R.drawable.ic_link // استبدل بـ R.drawable.ic_linkedin
            else -> R.drawable.ic_link
        }
    }

    private fun openUrl(url: String) {
        try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        val pagerAdapter = ProfilePagerAdapter(this, targetUserId!!)
        binding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.tabTitles[position]
        }.attach()
    }

    private class ProfilePagerAdapter(fa: FragmentActivity, private val userId: String) : FragmentStateAdapter(fa) {
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