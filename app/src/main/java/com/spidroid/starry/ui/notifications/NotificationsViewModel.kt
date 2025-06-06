// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/notifications/NotificationsViewModel.kt
package com.spidroid.starry.ui.notifications

import android.util.Log // ✨ تم التأكد من هذا الاستيراد
import androidx.lifecycle.LiveData // ✨ تم التأكد من هذا الاستيراد
import androidx.lifecycle.MutableLiveData // ✨ تم التأكد من هذا الاستيراد
import androidx.lifecycle.ViewModel // ✨ تم التأكد من هذا الاستيراد
import com.google.firebase.auth.FirebaseAuth // ✨ تم التأكد من هذا الاستيراد
import com.google.firebase.auth.FirebaseUser // ✨ تم التأكد من هذا الاستيراد
import com.google.firebase.firestore.FirebaseFirestore // ✨ تم التأكد من هذا الاستيراد
import com.google.firebase.firestore.ListenerRegistration // ✨ تم التأكد من هذا الاستيراد
import com.google.firebase.firestore.Query // ✨ تم التأكد من هذا الاستيراد
import com.spidroid.starry.models.NotificationModel // ✨ تم التأكد من هذا الاستيراد
import java.util.ArrayList // ✨ تم التأكد من هذا الاستيراد
import java.util.Date // ✨ تم التأكد من هذا الاستيراد


class NotificationsViewModel : ViewModel() {
    private val notificationsList: MutableLiveData<List<NotificationModel>> =
        MutableLiveData(emptyList()) // ✨ تغيير النوع إلى List<NotificationModel> وتهيئة بقائمة فارغة
    private val error: MutableLiveData<String?> = MutableLiveData() // ✨ تغيير النوع إلى String
    private val isLoading: MutableLiveData<Boolean> =
        MutableLiveData(false) // ✨ تغيير النوع إلى Boolean وتهيئة بـ false

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance() // ✨ تهيئة db
    private val auth: FirebaseAuth = FirebaseAuth.getInstance() // ✨ تهيئة auth
    private var notificationListener: ListenerRegistration? = null


    init {
        fetchNotifications()
    }

    fun getNotificationsList(): LiveData<List<NotificationModel>> { // ✨ تغيير النوع
        return notificationsList
    }

    fun getError(): LiveData<String?> { // ✨ تغيير النوع
        return error
    }

    fun getIsLoading(): LiveData<Boolean> { // ✨ تغيير النوع
        return isLoading
    }

    fun fetchNotifications() {
        val currentUser: FirebaseUser? = auth.currentUser // ✨ استخدام currentUser
        if (currentUser == null) {
            error.value = "User not logged in. Cannot fetch notifications." // ✨ استخدام .value
            notificationsList.value = emptyList() // ✨ إرسال قائمة فارغة
            Log.w(
                TAG, // ✨ استخدام TAG
                "Current user is null. Cannot fetch notifications."
            )
            return
        }

        val userId: String = currentUser.uid // ✨ استخدام .uid
        isLoading.value = true // ✨ استخدام .value

        // إلغاء تسجيل أي مستمع قديم قبل تسجيل مستمع جديد
        notificationListener?.remove() // ✨ استخدام safe call

        notificationListener = db.collection("users").document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING) // عرض الأحدث أولاً
            .limit(30) // تحديد عدد الإشعارات التي يتم جلبها مبدئيًا
            .addSnapshotListener { queryDocumentSnapshots, e -> // ✨ استخدام lambda المناسبة
                isLoading.value = false // ✨ استخدام .value
                if (e != null) {
                    error.value = "Error fetching notifications: " + e.message // ✨ استخدام .value و .message
                    Log.e(TAG, "Listen failed.", e) // ✨ استخدام TAG
                    return@addSnapshotListener
                }
                if (queryDocumentSnapshots != null) {
                    val newNotifications: MutableList<NotificationModel> =
                        ArrayList()
                    for (doc in queryDocumentSnapshots) {
                        val notification: NotificationModel =
                            doc.toObject(NotificationModel::class.java)
                        notification.notificationId = doc.id // ✨ تعيين ID الإشعار
                        newNotifications.add(notification)
                    }
                    notificationsList.value = newNotifications // ✨ استخدام .value
                    Log.d(
                        TAG, // ✨ استخدام TAG
                        "Notifications fetched/updated: " + newNotifications.size
                    )
                } else {
                    Log.d(
                        TAG, // ✨ استخدام TAG
                        "Current data: null in snapshot listener"
                    )
                    notificationsList.value = emptyList() // ✨ في حالة عدم وجود بيانات
                }
            }
    }

    // (اختياري للخطوات اللاحقة) دالة لتحديث حالة قراءة الإشعار
    fun markNotificationAsRead(notificationId: String?) {
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser == null || notificationId == null) return

        val userId: String = currentUser.uid
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .update("read", true)
            .addOnSuccessListener({
                Log.d(
                    TAG, // ✨ استخدام TAG
                    "Notification marked as read: " + notificationId
                )
            })
            .addOnFailureListener({ e ->
                Log.e(
                    TAG, // ✨ استخدام TAG
                    "Error marking notification as read",
                    e
                )
            })
    }

    // (اختياري للخطوات اللاحقة) دالة لحذف إشعار
    fun deleteNotification(notificationId: String?) {
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser == null || notificationId == null) return

        val userId: String = currentUser.uid
        db.collection("users").document(userId)
            .collection("notifications").document(notificationId)
            .delete()
            .addOnSuccessListener({
                Log.d(
                    TAG, // ✨ استخدام TAG
                    "Notification deleted: " + notificationId
                )
            })
            .addOnFailureListener({ e ->
                Log.e(
                    TAG, // ✨ استخدام TAG
                    "Error deleting notification",
                    e
                )
            })
    }


    override fun onCleared() { // ✨ إضافة override
        super.onCleared()
        // إلغاء تسجيل المستمع عند تدمير ViewModel لمنع تسرب الذاكرة
        notificationListener?.remove() // ✨ استخدام safe call
        Log.d(
            TAG, // ✨ استخدام TAG
            "NotificationsViewModel cleared and listener removed."
        )
    }

    companion object {
        private const val TAG = "NotificationsViewModel"
    }
}