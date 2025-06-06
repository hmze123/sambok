package com.spidroid.starry.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

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

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

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
            binding.btnAction.setIconResource(R.drawable.ic_edit) // تأكد من وجود هذا الرسم
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

        val collapsingToolbarLayout = binding.collapsingToolbar
        binding.appbarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            var isShow = true
            var scrollRange = -1

            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if (scrollRange + verticalOffset == 0) {
                val title = when {
                    !displayedUserProfile?.displayName.isNullOrEmpty() -> displayedUserProfile?.displayName
                    !displayedUserProfile?.username.isNullOrEmpty() -> displayedUserProfile?.username
                    else -> null
                }
                collapsingToolbarLayout.title = title
                isShow = true
            } else if (isShow) {
                collapsingToolbarLayout.title = " " // مسافة فارغة لإخفاء العنوان
                isShow = false
            }
        })

        binding.layoutFollowingInfo.setOnClickListener {
            if (displayedUserProfile != null && !userId.isNullOrEmpty()) {
                val intent = Intent(this, FollowingListActivity::class.java).apply {
                    putExtra(FollowersListActivity.EXTRA_USER_ID, userId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.user_data_not_loaded_yet), Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutFollowersInfo.setOnClickListener {
            if (displayedUserProfile != null && !userId.isNullOrEmpty()) {
                val intent = Intent(this, FollowersListActivity::class.java).apply {
                    putExtra(FollowersListActivity.EXTRA_USER_ID, userId)
                    putExtra(FollowersListActivity.EXTRA_LIST_TYPE, "followers")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.user_data_not_loaded_yet), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserDataWithListener() {
        userProfileListener?.remove()
        binding.btnAction.isEnabled = false
        binding.layoutFollowingInfo.isEnabled = false
        binding.layoutFollowersInfo.isEnabled = false

        val currentUserId = userId ?: return // الخروج إذا كان userId هو null

        val userRef: DocumentReference = db.collection("users").document(currentUserId)
        userProfileListener = userRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed for user $currentUserId", e)
                Toast.makeText(this, getString(R.string.failed_to_load_profile, e.message), Toast.LENGTH_SHORT).show()
                binding.btnAction.isEnabled = true
                binding.layoutFollowingInfo.isEnabled = true
                binding.layoutFollowersInfo.isEnabled = true
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                displayedUserProfile = snapshot.toObject(UserModel::class.java)?.apply {
                    this.userId = snapshot.id // تعيين معرف المستخدم من الـ snapshot
                }
                if (displayedUserProfile != null) {
                    populateProfileData(displayedUserProfile!!)
                    if (!isCurrentUserProfile && currentAuthUserId != null) {
                        checkFollowingStatus() // سيمكن الزر بعد التحقق
                    } else if (isCurrentUserProfile) {
                        binding.btnAction.isEnabled = true
                    }
                } else {
                    Toast.makeText(this, getString(R.string.failed_to_parse_user_data), Toast.LENGTH_SHORT).show()
                    binding.btnAction.isEnabled = true
                }
            } else {
                Log.d(TAG, "User document does not exist for userId: $currentUserId")
                Toast.makeText(this, getString(R.string.user_profile_not_found), Toast.LENGTH_SHORT).show()
                binding.btnAction.isEnabled = true
            }
            binding.layoutFollowingInfo.isEnabled = true
            binding.layoutFollowersInfo.isEnabled = true
        }
    }

    private fun checkFollowingStatus() {
        val authUserId = currentAuthUserId ?: run {
            updateFollowButtonState(false)
            binding.btnAction.isEnabled = true
            return
        }
        val followersMap = displayedUserProfile?.followers ?: run {
            Log.w(TAG, "Cannot check following status: displayedUserProfile or its followers map is null")
            updateFollowButtonState(false)
            binding.btnAction.isEnabled = true
            return
        }
        isFollowing = followersMap.containsKey(authUserId)
        updateFollowButtonState(isFollowing)
        binding.btnAction.isEnabled = true
    }

    private fun updateFollowButtonState(following: Boolean) {
        if (isCurrentUserProfile) return

        isFollowing = following
        if (isFollowing) {
            binding.btnAction.setText(R.string.following)
            binding.btnAction.icon = null // لإزالة الأيقونة
            // يمكنك تخصيص الألوان هنا إذا أردت، مثال:
            // binding.btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.m3_surface_container_highest))
            // binding.btnAction.setTextColor(ContextCompat.getColor(this, R.color.m3_onSurface))
        } else {
            binding.btnAction.setText(R.string.follow)
            binding.btnAction.setIconResource(R.drawable.ic_add) // تأكد من وجود هذا الرسم
            // binding.btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.m3_primary))
            // binding.btnAction.setTextColor(ContextCompat.getColor(this, R.color.m3_onPrimary))
        }
    }

    private fun toggleFollow() {
        val authUserId = currentAuthUserId ?: run {
            Toast.makeText(this, getString(R.string.login_to_follow), Toast.LENGTH_SHORT).show()
            return
        }
        val targetUserId = userId ?: run { // استخدام userId الذي تم تمريره للـ Activity
            Toast.makeText(this, getString(R.string.cannot_follow_no_user_data), Toast.LENGTH_SHORT).show()
            return
        }
        if (displayedUserProfile == null) {
            Toast.makeText(this, getString(R.string.cannot_follow_no_user_data), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAction.isEnabled = false

        val currentUserDocRef = db.collection("users").document(authUserId)
        val targetUserDocRef = db.collection("users").document(targetUserId)

        db.runTransaction { transaction ->
            val currentUserSnap = transaction.get(currentUserDocRef)
            val targetUserSnap = transaction.get(targetUserDocRef)

            if (!currentUserSnap.exists() || !targetUserSnap.exists()) {
                throw FirebaseFirestoreException(
                    "User document not found for transaction.",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )
            }

            // الحصول على القوائم الحالية أو إنشاء قوائم جديدة إذا كانت null
            val currentUserFollowing =
                (currentUserSnap.get("following") as? Map<String, Boolean>)?.toMutableMap() ?: mutableMapOf()
            val targetUserFollowers =
                (targetUserSnap.get("followers") as? Map<String, Boolean>)?.toMutableMap() ?: mutableMapOf()

            if (isFollowing) { // إذا كان المستخدم يتابع حاليًا ويريد إلغاء المتابعة
                currentUserFollowing.remove(targetUserId)
                targetUserFollowers.remove(authUserId)
            } else { // إذا كان المستخدم لا يتابع ويريد المتابعة
                currentUserFollowing[targetUserId] = true
                targetUserFollowers[authUserId] = true
            }

            transaction.set(currentUserDocRef, mapOf("following" to currentUserFollowing), SetOptions.merge())
            transaction.set(targetUserDocRef, mapOf("followers" to targetUserFollowers), SetOptions.merge())
            null // يجب أن تعيد Transaction.Function<Void> قيمة Void (أي null في Kotlin)
        }.addOnSuccessListener {
            binding.btnAction.isEnabled = true
            Log.d(TAG, "Follow status toggled successfully for user: $targetUserId")
            // SnapshotListener سيتولى تحديث isFollowing وأعداد المتابعين
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to toggle follow for user: $targetUserId", e)
            Toast.makeText(this, getString(R.string.failed_to_update_follow_status, e.message), Toast.LENGTH_SHORT).show()
            checkFollowingStatus() // إعادة الزر إلى حالته الصحيحة
            binding.btnAction.isEnabled = true
        }
    }

    private fun populateProfileData(user: UserModel) {
        val displayName = when {
            !user.displayName.isNullOrEmpty() -> user.displayName
            !user.username.isNullOrEmpty() -> user.username
            else -> "User" // قيمة افتراضية
        }
        val usernameText = if (!user.username.isNullOrEmpty()) "@${user.username}" else "@unknown"

        binding.tvDisplayName.text = displayName
        binding.tvUsername.text = usernameText

        if (!user.bio.isNullOrEmpty()) {
            binding.tvBio.visibility = View.VISIBLE
            binding.tvBio.text = user.bio
        } else {
            binding.tvBio.visibility = View.GONE
        }

        binding.ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE

        Glide.with(this)
            .load(user.profileImageUrl)
            .placeholder(R.drawable.ic_default_avatar) // تأكد من وجود هذا الرسم
            .error(R.drawable.ic_default_avatar)
            .into(binding.ivAvatar)

        Glide.with(this)
            .load(user.coverImageUrl)
            .placeholder(R.color.m3_surfaceContainerLow) // تأكد من وجود هذا اللون
            .error(R.color.m3_surfaceContainerLow)
            .into(binding.ivCover)

        binding.tvFollowersCount.text = String.format(Locale.getDefault(), "%d", user.followers?.size ?: 0)
        binding.tvFollowingCount.text = String.format(Locale.getDefault(), "%d", user.following?.size ?: 0)

        binding.layoutSocialLinksContainer.removeAllViews()
        val socialLinks = user.socialLinks

        if (!socialLinks.isNullOrEmpty()) {
            binding.tvAboutTitle.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(this)

            socialLinks.forEach { (platform, urlValue) ->
                val url = urlValue ?: return@forEach // تجاهل إذا كان الرابط null
                if (url.trim().isEmpty()) return@forEach

                val socialLinkView = inflater.inflate(R.layout.item_profile_social_link, binding.layoutSocialLinksContainer, false)
                val ivPlatformIcon = socialLinkView.findViewById<ImageView>(R.id.ivPlatformIcon)
                val tvPlatformUrl = socialLinkView.findViewById<TextView>(R.id.tvPlatformUrl)

                val iconRes = when (platform.lowercase(Locale.getDefault())) {
                    UserModel.SOCIAL_TWITTER -> R.drawable.ic_share // استبدل بأيقونة تويتر
                    UserModel.SOCIAL_INSTAGRAM -> R.drawable.ic_add_photo // استبدل بأيقونة انستغرام
                    else -> R.drawable.ic_link // أيقونة افتراضية
                }
                ivPlatformIcon.setImageResource(iconRes)
                tvPlatformUrl.text = url

                socialLinkView.setOnClickListener {
                    try {
                        var formattedUrl = url
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            formattedUrl = "https://$url"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                        startActivity(intent)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Could not open social link: $url", ex)
                        Toast.makeText(this@ProfileActivity, "Could not open link: $url", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.layoutSocialLinksContainer.addView(socialLinkView)
            }
            if (binding.layoutSocialLinksContainer.childCount == 0) {
                binding.tvAboutTitle.visibility = View.GONE
            }
        } else {
            binding.tvAboutTitle.visibility = View.GONE
        }
    }

    private fun setupTabs() {
        val pagerAdapter = ProfilePagerAdapter(this, userId ?: "") // توفير قيمة افتراضية إذا كان userId هو null
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.tabTitles[position]
        }.attach()
    }

    private class ProfilePagerAdapter(
        fragmentActivity: FragmentActivity,
        private val userIdForFragments: String
    ) : FragmentStateAdapter(fragmentActivity) {

        val tabTitles = arrayOf("Posts", "Replies", "Media") // يمكن جعلها private val

        override fun getItemCount(): Int = tabTitles.size

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ProfilePostsFragment.newInstance(userIdForFragments)
                1 -> ProfileRepliesFragment.newInstance(userIdForFragments)
                2 -> ProfileMediaFragment.newInstance(userIdForFragments)
                else -> ProfilePostsFragment.newInstance(userIdForFragments) // حالة افتراضية
            }
        }
    }
}
