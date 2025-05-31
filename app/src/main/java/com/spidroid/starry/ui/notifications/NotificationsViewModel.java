package com.spidroid.starry.ui.notifications;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.spidroid.starry.models.NotificationModel; // ★ استيراد NotificationModel

import java.util.ArrayList;
import java.util.List;

public class NotificationsViewModel extends ViewModel {

    private static final String TAG = "NotificationsViewModel";

    private final MutableLiveData<List<NotificationModel>> notificationsList = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);


    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;

    public NotificationsViewModel() {
        // كان الكود القديم هنا:
        // mText = new MutableLiveData<>();
        // mText.setValue("This is notifications fragment");
        // سنقوم بإزالته واستبداله بمنطق الإشعارات.

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fetchNotifications();
    }

    public LiveData<List<NotificationModel>> getNotificationsList() {
        return notificationsList;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void fetchNotifications() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            error.setValue("User not logged in. Cannot fetch notifications.");
            notificationsList.setValue(new ArrayList<>()); // إرسال قائمة فارغة
            Log.w(TAG, "Current user is null. Cannot fetch notifications.");
            return;
        }

        String userId = currentUser.getUid();
        isLoading.setValue(true);

        // إلغاء تسجيل أي مستمع قديم قبل تسجيل مستمع جديد
        if (notificationListener != null) {
            notificationListener.remove();
        }

        notificationListener = db.collection("users").document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING) // عرض الأحدث أولاً
                .limit(30) // تحديد عدد الإشعارات التي يتم جلبها مبدئيًا
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    isLoading.setValue(false);
                    if (e != null) {
                        error.setValue("Error fetching notifications: " + e.getMessage());
                        Log.e(TAG, "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        List<NotificationModel> newNotifications = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            NotificationModel notification = doc.toObject(NotificationModel.class);
                            notification.setNotificationId(doc.getId()); // تعيين ID الإشعار
                            newNotifications.add(notification);
                        }
                        notificationsList.setValue(newNotifications);
                        Log.d(TAG, "Notifications fetched/updated: " + newNotifications.size());
                    } else {
                        Log.d(TAG, "Current data: null in snapshot listener");
                        notificationsList.setValue(new ArrayList<>()); // في حالة عدم وجود بيانات
                    }
                });
    }

    // (اختياري للخطوات اللاحقة) دالة لتحديث حالة قراءة الإشعار
    public void markNotificationAsRead(String notificationId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || notificationId == null) return;

        String userId = currentUser.getUid();
        db.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + notificationId))
                .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as read", e));
    }

    // (اختياري للخطوات اللاحقة) دالة لحذف إشعار
    public void deleteNotification(String notificationId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || notificationId == null) return;

        String userId = currentUser.getUid();
        db.collection("users").document(userId)
                .collection("notifications").document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted: " + notificationId);
                    // يمكنك هنا إعادة تحميل القائمة أو إزالة العنصر يدويًا من LiveData
                    // fetchNotifications(); // أبسط طريقة هي إعادة التحميل
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting notification", e));
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        // إلغاء تسجيل المستمع عند تدمير ViewModel لمنع تسرب الذاكرة
        if (notificationListener != null) {
            notificationListener.remove();
        }
        Log.d(TAG, "NotificationsViewModel cleared and listener removed.");
    }
}