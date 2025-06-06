package com.spidroid.starry.ui.notifications

// ★ استيراد Log
// ★ استيراد ProgressBar
// ★ استيراد TextView
// ★ استيراد Nullable
// ★ استيراد LinearLayoutManager
// ★ استيراد RecyclerView
// ★ استيراد SwipeRefreshLayout
// ★ استيراد TabLayoutMediator (إذا كنت لا تزال تستخدمه)
// ★ استيراد PostDetailActivity
// ★ استيراد SettingsActivity
// ★ استيراد NotificationModel
// ★ استيراد ArrayList
import com.google.firebase.auth.FirebaseAuth

class NotificationsFragment : androidx.fragment.app.Fragment(), OnNotificationClickListener {
    private var binding: com.spidroid.starry.databinding.FragmentNotificationsBinding? = null
    private var auth: FirebaseAuth? = null
    private var userListener: ListenerRegistration? = null
    private var notificationsViewModel: NotificationsViewModel? = null // ★ إضافة ViewModel
    private var notificationAdapter: NotificationAdapter? = null // ★ إضافة Adapter

    // ★ لم نعد بحاجة لـ ViewPager و TabLayout هنا إذا كانت هذه الشاشة فقط للإشعارات
    // private ViewPager2 viewPager;
    // private TabLayout tabLayout;
    private var notificationsRecyclerView: RecyclerView? = null // ★ إضافة RecyclerView للإشعارات
    private var loadingIndicator: ProgressBar? = null // ★ إضافة ProgressBar
    private var emptyNotificationsTextView: TextView? = null // ★ إضافة TextView لحالة الفراغ
    private var swipeRefreshLayout: SwipeRefreshLayout? = null // ★ إضافة SwipeRefreshLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): android.view.View {
        // استخدام ViewModelProvider للحصول على مثيل من NotificationsViewModel
        notificationsViewModel =
            ViewModelProvider(this).get<NotificationsViewModel>(NotificationsViewModel::class.java)

        binding = com.spidroid.starry.databinding.FragmentNotificationsBinding.inflate(
            inflater,
            container,
            false
        )
        val root: android.view.View = binding!!.getRoot()
        auth = FirebaseAuth.getInstance()

        // تهيئة عناصر واجهة المستخدم الجديدة
        notificationsRecyclerView =
            root.findViewById<RecyclerView?>(R.id.recycler_view_notifications) // ستحتاج لإضافة هذا ID في XML
        loadingIndicator =
            root.findViewById<ProgressBar?>(R.id.progress_bar_notifications) // ستحتاج لإضافة هذا ID في XML
        emptyNotificationsTextView =
            root.findViewById<TextView?>(R.id.text_view_empty_notifications) // ستحتاج لإضافة هذا ID في XML
        swipeRefreshLayout =
            root.findViewById<SwipeRefreshLayout?>(R.id.swipe_refresh_notifications) // ستحتاج لإضافة هذا ID في XML


        // إعداد RecyclerView
        setupRecyclerView()
        setupSwipeToRefresh() // إعداد السحب للتحديث

        return root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // لم نعد بحاجة لإعداد ViewPager هنا إذا كانت الشاشة مخصصة فقط للإشعارات
        // setupViewPager();
        setupToolbarAvatarClick() // نقل إعداد النقر على الأفاتار هنا
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        // نقل setupToolbar إلى onStart أو onViewCreated إذا لم يكن يعتمد على بيانات متغيرة كثيرًا
        // لكن بما أنه يعتمد على بيانات المستخدم، قد يكون من الأفضل تحديثه عند توفر البيانات
        // سنقوم بتحديث الأفاتار من خلال مراقب بيانات المستخدم في UserModel إذا كان لديك
        loadCurrentUserData() // جلب بيانات المستخدم لتحديث الأفاتار
    }

    private fun loadCurrentUserData() {
        val currentUser: FirebaseUser? = auth.getCurrentUser()
        if (currentUser != null && binding != null) {
            if (userListener != null) userListener.remove()
            userListener =
                FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                    .addSnapshotListener({ documentSnapshot, error ->
                        if (binding == null) return@addSnapshotListener  // تحقق من أن binding ليس null (قد يكون Fragment تم تدميره)


                        if (error != null) {
                            android.util.Log.w(
                                ContentValues.TAG,
                                "Listen failed for user data.",
                                error
                            )
                            binding!!.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                            return@addSnapshotListener
                        }
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            val user: UserModel? = documentSnapshot.toObject(UserModel::class.java)
                            if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl()
                                    .isEmpty() && getContext() != null
                            ) {
                                Glide.with(getContext())
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(binding!!.ivUserAvatar)
                            } else {
                                binding!!.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                            }
                        } else {
                            binding!!.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                    })
        } else if (binding != null) {
            binding!!.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
        }
    }


    private fun setupToolbarAvatarClick() {
        // إعداد النقر على صورة المستخدم في الشريط العلوي
        if (binding != null) { // التأكد من أن binding ليس null
            binding!!.ivUserAvatar.setOnClickListener(
                android.view.View.OnClickListener { v: android.view.View? ->
                    val currentUser: FirebaseUser? = auth.getCurrentUser()
                    if (currentUser != null) {
                        val profileIntent: Intent =
                            Intent(getActivity(), ProfileActivity::class.java)
                        profileIntent.putExtra("userId", currentUser.getUid())
                        startActivity(profileIntent)
                    } else {
                        Toast.makeText(
                            getContext(),
                            "Please login to view profile",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            // إعداد زر الإعدادات
            binding!!.ivSettings.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
                val settingsIntent: Intent = Intent(getActivity(), SettingsActivity::class.java)
                startActivity(settingsIntent)
            })
        }
    }

    private fun setupRecyclerView() {
        if (getContext() == null || notificationsRecyclerView == null) return  // ★ حماية إضافية


        notificationAdapter = NotificationAdapter(getContext(), this)
        notificationsRecyclerView.setLayoutManager(LinearLayoutManager(getContext()))
        notificationsRecyclerView.setAdapter(notificationAdapter)
    }

    private fun setupSwipeToRefresh() {
        if (swipeRefreshLayout == null) return  // ★ حماية إضافية


        swipeRefreshLayout.setOnRefreshListener(OnRefreshListener {
            android.util.Log.d(ContentValues.TAG, "Swipe to refresh triggered.")
            notificationsViewModel!!.fetchNotifications() // إعادة جلب الإشعارات
        })
    }


    private fun observeViewModel() {
        notificationsViewModel!!.getNotificationsList().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { notifications: kotlin.collections.MutableList<NotificationModel?>? ->
                if (notificationAdapter != null) { // ★ حماية إضافية
                    notificationAdapter!!.submitList(notifications)
                    if (emptyNotificationsTextView != null) { // ★ حماية إضافية
                        emptyNotificationsTextView.setVisibility(if (notifications!!.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
                    }
                    android.util.Log.d(
                        ContentValues.TAG,
                        "Notifications list updated in Fragment: " + notifications!!.size
                    )
                }
            })

        notificationsViewModel!!.getIsLoading().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { isLoading: kotlin.Boolean? ->
                if (loadingIndicator != null) { // ★ حماية إضافية
                    // لا نظهر مؤشر التحميل الرئيسي إذا كان السحب للتحديث نشطًا
                    if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                        loadingIndicator.setVisibility(if (isLoading) android.view.View.VISIBLE else android.view.View.GONE)
                    }
                }
                if (swipeRefreshLayout != null && !isLoading!!) { // ★ حماية إضافية
                    swipeRefreshLayout.setRefreshing(false) // إيقاف مؤشر السحب للتحديث عند انتهاء التحميل
                }
                android.util.Log.d(ContentValues.TAG, "Loading state changed: " + isLoading)
            })

        notificationsViewModel!!.getError().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { errorMsg: kotlin.String? ->
                if (errorMsg != null && !errorMsg.isEmpty() && getContext() != null) { // ★ حماية إضافية
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show()
                    android.util.Log.e(ContentValues.TAG, "Error observed: " + errorMsg)
                }
            })
    }

    // تطبيق واجهة OnNotificationClickListener
    override fun onNotificationClick(notification: NotificationModel?) {
        if (getContext() == null || notification == null) return  // ★ حماية إضافية


        // تحديث حالة الإشعار إلى مقروء (اختياري، يمكنك القيام به هنا أو عند فتح الشاشة)
        if (!notification.isRead()) {
            notificationsViewModel!!.markNotificationAsRead(notification.getNotificationId())
        }

        // الانتقال إلى الوجهة المناسبة بناءً على نوع الإشعار
        if (NotificationModel.Companion.TYPE_LIKE == notification.getType() || NotificationModel.Companion.TYPE_COMMENT == notification.getType()) {
            if (notification.getPostId() != null && !notification.getPostId().isEmpty()) {
                // ملاحظة: PostDetailActivity يتوقع كائن PostModel كاملاً.
                // هنا لدينا فقط postId. ستحتاج إلى تعديل هذا الجزء إما:
                // 1. لجلب PostModel كاملاً بناءً على postId قبل الانتقال.
                // 2. أو تعديل PostDetailActivity لقبول postId فقط وجلب البيانات هناك.
                // للتبسيط الآن، سنفترض أن PostDetailActivity يمكنه التعامل مع postId فقط (وهو ليس صحيحًا حاليًا بناءً على الكود).
                // ★★ هذا الجزء يحتاج إلى مراجعة وتعديل ليتناسب مع كيفية عمل PostDetailActivity ★★
                // Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                // intent.putExtra("postId", notification.getPostId()); // هذا مثال، قد تحتاج لتمرير PostModel
                // startActivity(intent);
                Toast.makeText(
                    getContext(),
                    "Clicked on notification for post: " + notification.getPostId(),
                    Toast.LENGTH_SHORT
                ).show()
                // ★★★ لتشغيل هذا بشكل صحيح، ستحتاج لجلب بيانات المنشور أولاً ★★★
                FirebaseFirestore.getInstance().collection("posts")
                    .document(notification.getPostId()).get()
                    .addOnSuccessListener({ documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val post: PostModel? = documentSnapshot.toObject(PostModel::class.java)
                            if (post != null) {
                                post.setPostId(documentSnapshot.getId()) // التأكد من تعيين ID المنشور
                                val intent: Intent =
                                    Intent(getActivity(), PostDetailActivity::class.java)
                                intent.putExtra(PostDetailActivity.Companion.EXTRA_POST, post)
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    getContext(),
                                    "Failed to load post details.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(getContext(), "Post not found.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
                    .addOnFailureListener({ e ->
                        Toast.makeText(
                            getContext(),
                            "Error loading post: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                        ).show()
                    })
            } else {
                Toast.makeText(
                    getContext(),
                    "Post ID is missing for this notification.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (NotificationModel.Companion.TYPE_FOLLOW == notification.getType()) {
            if (notification.getFromUserId() != null && !notification.getFromUserId().isEmpty()) {
                val intent: Intent = Intent(getActivity(), ProfileActivity::class.java)
                intent.putExtra("userId", notification.getFromUserId())
                startActivity(intent)
            } else {
                Toast.makeText(
                    getContext(),
                    "User ID is missing for follow notification.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                getContext(),
                "Notification type: " + notification.getType(),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (userListener != null) {
            userListener.remove() // Clean up listener
            userListener = null
        }
        // لا تقم بإلغاء تسجيل notificationListener هنا، لأن ViewModel هو الذي يدير دورة حياته
        // notificationListener يتم إلغاء تسجيله في onCleared() داخل ViewModel
        binding = null // ★ مهم جدًا لتجنب تسرب الذاكرة
        notificationsRecyclerView = null // ★
        loadingIndicator = null // ★
        emptyNotificationsTextView = null // ★
        swipeRefreshLayout = null // ★
        notificationAdapter = null // ★
    } // لم نعد بحاجة لهذه الدالة هنا، فكل قسم له Fragment خاص به
    // private static class ViewPagerAdapter extends FragmentStateAdapter { /* ... */ }
}