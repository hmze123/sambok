package com.spidroid.starry.ui.notifications

// ★ استيراد NotificationModel
import com.google.firebase.auth.FirebaseAuth

class NotificationsViewModel : ViewModel() {
    private val notificationsList: MutableLiveData<kotlin.collections.MutableList<NotificationModel?>?> =
        MutableLiveData<kotlin.collections.MutableList<NotificationModel?>?>()
    private val error: MutableLiveData<kotlin.String?> = MutableLiveData<kotlin.String?>()
    private val isLoading: MutableLiveData<kotlin.Boolean?> =
        MutableLiveData<kotlin.Boolean?>(false)


    private val db: FirebaseFirestore
    private val auth: FirebaseAuth
    private var notificationListener: ListenerRegistration? = null

    init {
        // كان الكود القديم هنا:
        // mText = new MutableLiveData<>();
        // mText.setValue("This is notifications fragment");
        // سنقوم بإزالته واستبداله بمنطق الإشعارات.

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fetchNotifications()
    }

    fun getNotificationsList(): LiveData<kotlin.collections.MutableList<NotificationModel?>?> {
        return notificationsList
    }

    fun getError(): LiveData<kotlin.String?> {
        return error
    }

    fun getIsLoading(): LiveData<kotlin.Boolean?> {
        return isLoading
    }

    fun fetchNotifications() {
        val currentUser: FirebaseUser? = auth.getCurrentUser()
        if (currentUser == null) {
            error.setValue("User not logged in. Cannot fetch notifications.")
            notificationsList.setValue(java.util.ArrayList<NotificationModel?>()) // إرسال قائمة فارغة
            android.util.Log.w(
                NotificationsViewModel.Companion.TAG,
                "Current user is null. Cannot fetch notifications."
            )
            return
        }

        val userId: kotlin.String? = currentUser.getUid()
        isLoading.setValue(true)

        // إلغاء تسجيل أي مستمع قديم قبل تسجيل مستمع جديد
        if (notificationListener != null) {
            notificationListener.remove()
        }

        notificationListener = db.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING) // عرض الأحدث أولاً
            .limit(30) // تحديد عدد الإشعارات التي يتم جلبها مبدئيًا
            .addSnapshotListener({ queryDocumentSnapshots, e ->
                isLoading.setValue(false)
                if (e != null) {
                    error.setValue("Error fetching notifications: " + e.getMessage())
                    android.util.Log.e(NotificationsViewModel.Companion.TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (queryDocumentSnapshots != null) {
                    val newNotifications: kotlin.collections.MutableList<NotificationModel?> =
                        java.util.ArrayList<NotificationModel?>()
                    for (doc in queryDocumentSnapshots) {
                        val notification: NotificationModel =
                            doc.toObject(NotificationModel::class.java)
                        notification.setNotificationId(doc.getId()) // تعيين ID الإشعار
                        newNotifications.add(notification)
                    }
                    notificationsList.setValue(newNotifications)
                    android.util.Log.d(
                        NotificationsViewModel.Companion.TAG,
                        "Notifications fetched/updated: " + newNotifications.size
                    )
                } else {
                    android.util.Log.d(
                        NotificationsViewModel.Companion.TAG,
                        "Current data: null in snapshot listener"
                    )
                    notificationsList.setValue(java.util.ArrayList<NotificationModel?>()) // في حالة عدم وجود بيانات
                }
            })
    }

    // (اختياري للخطوات اللاحقة) دالة لتحديث حالة قراءة الإشعار
    fun markNotificationAsRead(notificationId: kotlin.String?) {
        val currentUser: FirebaseUser? = auth.getCurrentUser()
        if (currentUser == null || notificationId == null) return

        val userId: kotlin.String? = currentUser.getUid()
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("read", true)
            .addOnSuccessListener({ aVoid ->
                android.util.Log.d(
                    NotificationsViewModel.Companion.TAG,
                    "Notification marked as read: " + notificationId
                )
            })
            .addOnFailureListener({ e ->
                android.util.Log.e(
                    NotificationsViewModel.Companion.TAG,
                    "Error marking notification as read",
                    e
                )
            })
    }

    // (اختياري للخطوات اللاحقة) دالة لحذف إشعار
    fun deleteNotification(notificationId: kotlin.String?) {
        val currentUser: FirebaseUser? = auth.getCurrentUser()
        if (currentUser == null || notificationId == null) return

        val userId: kotlin.String? = currentUser.getUid()
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .delete()
            .addOnSuccessListener({ aVoid ->
                android.util.Log.d(
                    NotificationsViewModel.Companion.TAG,
                    "Notification deleted: " + notificationId
                )
            })
            .addOnFailureListener({ e ->
                android.util.Log.e(
                    NotificationsViewModel.Companion.TAG,
                    "Error deleting notification",
                    e
                )
            })
    }


    override fun onCleared() {
        super.onCleared()
        // إلغاء تسجيل المستمع عند تدمير ViewModel لمنع تسرب الذاكرة
        if (notificationListener != null) {
            notificationListener.remove()
        }
        android.util.Log.d(
            NotificationsViewModel.Companion.TAG,
            "NotificationsViewModel cleared and listener removed."
        )
    }

    companion object {
        private const val TAG = "NotificationsViewModel"
    }
}