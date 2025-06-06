package com.spidroid.starry.ui.notifications

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.spidroid.starry.R
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.activities.SettingsActivity
import com.spidroid.starry.databinding.FragmentNotificationsBinding
import com.spidroid.starry.models.NotificationModel
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel

// ★ تطبيق الواجهة OnNotificationClickListener
class NotificationsFragment : Fragment(), NotificationAdapter.OnNotificationClickListener {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var userListener: ListenerRegistration? = null
    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        // تهيئة ViewModel
        notificationsViewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar()
        setupSwipeToRefresh()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        loadCurrentUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        userListener = null
        _binding = null // ★ مهم لتجنب تسرب الذاكرة
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(requireContext(), this)
        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }

    private fun setupToolbar() {
        binding.ivSettings.setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshNotifications.setOnRefreshListener {
            Log.d(ContentValues.TAG, "Swipe to refresh triggered.")
            notificationsViewModel.fetchNotifications()
        }
    }

    private fun loadCurrentUserData() {
        val currentUser = auth.currentUser ?: return
        userListener?.remove() // إزالة المستمع القديم
        userListener = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener // تحقق من أن الواجهة ما زالت موجودة

                if (error != null) {
                    Log.w(ContentValues.TAG, "Listen failed for user data.", error)
                    binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(UserModel::class.java)
                    if (user?.profileImageUrl != null && context != null) {
                        Glide.with(requireContext())
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.ivUserAvatar)
                    } else {
                        binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                    }

                    // النقر على الصورة الرمزية للانتقال للملف الشخصي
                    binding.ivUserAvatar.setOnClickListener {
                        val intent = Intent(activity, ProfileActivity::class.java).apply {
                            putExtra("userId", currentUser.uid)
                        }
                        startActivity(intent)
                    }
                }
            }
    }


    private fun observeViewModel() {
        notificationsViewModel.notificationsList.observe(viewLifecycleOwner) { notifications ->
            notificationAdapter.submitList(notifications)
            binding.textViewEmptyNotifications.visibility = if (notifications.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        notificationsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // إظهار/إخفاء مؤشر التحميل الرئيسي فقط إذا لم يكن السحب للتحديث نشطًا
            if (!binding.swipeRefreshNotifications.isRefreshing) {
                binding.progressBarNotifications.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
            // إيقاف مؤشر السحب للتحديث عند انتهاء التحميل
            if (!isLoading) {
                binding.swipeRefreshNotifications.isRefreshing = false
            }
        }

        notificationsViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                Log.e(ContentValues.TAG, "Error observed: $errorMsg")
            }
        }
    }

    // ★ تطبيق الواجهة OnNotificationClickListener
    override fun onNotificationClick(notification: NotificationModel) {
        // تحديث حالة الإشعار إلى مقروء
        if (!notification.isRead) {
            notificationsViewModel.markNotificationAsRead(notification.notificationId)
        }

        // تحديد الوجهة بناءً على نوع الإشعار
        when (notification.type) {
            NotificationModel.TYPE_LIKE, NotificationModel.TYPE_COMMENT -> {
                notification.postId?.let { openPostDetails(it) } ?: showMissingDataError("Post ID")
            }
            NotificationModel.TYPE_FOLLOW -> {
                notification.fromUserId?.let { openUserProfile(it) } ?: showMissingDataError("User ID")
            }
            else -> {
                Toast.makeText(context, "Notification type: ${notification.type}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPostDetails(postId: String) {
        FirebaseFirestore.getInstance().collection("posts").document(postId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val post = documentSnapshot.toObject(PostModel::class.java)?.apply {
                        this.postId = documentSnapshot.id
                    }
                    if (post != null) {
                        val intent = Intent(activity, PostDetailActivity::class.java).apply {
                            putExtra(PostDetailActivity.EXTRA_POST, post)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "Failed to load post details.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Post not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openUserProfile(userId: String) {
        val intent = Intent(activity, ProfileActivity::class.java).apply {
            putExtra("userId", userId)
        }
        startActivity(intent)
    }

    private fun showMissingDataError(missingField: String) {
        Toast.makeText(context, "$missingField is missing for this notification.", Toast.LENGTH_SHORT).show()
    }
}