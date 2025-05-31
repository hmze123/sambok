package com.spidroid.starry.repositories;

import android.util.Log; // ★ إضافة استيراد Log

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostRepository {
    private static final String TAG = "PostRepository"; // ★ إضافة TAG للـ Log
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    // دالة لجلب المنشورات
    public Task<QuerySnapshot> getPosts(int limit) {
        return db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    // دالة لجلب اقتراحات المستخدمين
    public Task<QuerySnapshot> getUserSuggestions() {
        return db.collection("users").limit(5).get();
    }

    // دالة لمعالجة الإعجاب (تم تعديلها سابقًا)
    public Task<Void> toggleLike(String postId, boolean isLiked, String postAuthorId, UserModel likerUser) {
        String currentUserId = auth.getUid();
        if (currentUserId == null || postAuthorId == null || likerUser == null) {
            return Tasks.forException(new IllegalArgumentException("User IDs or likerUser cannot be null for toggleLike."));
        }

        DocumentReference postRef = db.collection("posts").document(postId);

        if (currentUserId.equals(postAuthorId)) {
            Map<String, Object> likeUpdates = new HashMap<>();
            likeUpdates.put("likeCount", FieldValue.increment(isLiked ? 1 : -1));
            likeUpdates.put("likes." + currentUserId, isLiked ? true : FieldValue.delete());
            return postRef.update(likeUpdates);
        }

        DocumentReference notificationRef = db.collection("users").document(postAuthorId)
                .collection("notifications").document();

        return db.runTransaction(transaction -> {
            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("likeCount", FieldValue.increment(isLiked ? 1 : -1));
            postUpdates.put("likes." + currentUserId, isLiked ? true : FieldValue.delete());
            transaction.update(postRef, postUpdates);

            if (isLiked) {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "like");
                notificationData.put("fromUserId", currentUserId);
                notificationData.put("fromUsername", likerUser.getUsername() != null ? likerUser.getUsername() : "Unknown User");
                notificationData.put("fromUserAvatarUrl", likerUser.getProfileImageUrl() != null ? likerUser.getProfileImageUrl() : "");
                notificationData.put("postId", postId);
                notificationData.put("timestamp", FieldValue.serverTimestamp());
                notificationData.put("read", false);
                transaction.set(notificationRef, notificationData);
            }
            return null;
        });
    }

    // ★★★ دالة جديدة لإضافة أو تحديث الريأكشن على منشور ★★★
    public Task<Void> addOrUpdateReaction(String postId, String reactingUserId, String emoji, String postAuthorId, UserModel reactorDetails) {
        if (postId == null || reactingUserId == null) {
            Log.e(TAG, "Post ID or Reacting User ID is null for addOrUpdateReaction.");
            return Tasks.forException(new IllegalArgumentException("Post ID or Reacting User ID cannot be null."));
        }
        if (postAuthorId == null || reactorDetails == null) {
            Log.e(TAG, "Post Author ID or Reactor Details are null, cannot create notification for reaction.");
            // سنستمر في تحديث الريأكشن ولكن لن نتمكن من إنشاء إشعار
        }


        DocumentReference postRef = db.collection("posts").document(postId);
        Map<String, Object> updates = new HashMap<>();

        // إذا كان emoji هو null أو فارغ، فهذا يعني إزالة الريأكشن
        if (emoji == null || emoji.isEmpty()) {
            updates.put("reactions." + reactingUserId, FieldValue.delete());
        } else {
            updates.put("reactions." + reactingUserId, emoji);
        }

        // (اختياري) إذا كنت تريد عدادًا عامًا للريأكشنات
        // ستحتاج إلى منطق أكثر تعقيدًا هنا إذا كنت تريد حساب الزيادة/النقصان بدقة
        // بناءً على ما إذا كان المستخدم يغير الريأكشن أو يضيفه لأول مرة أو يزيله.
        // للتبسيط الآن، لن نضيف عدادًا عامًا.

        // الجزء الخاص بإنشاء الإشعار للريأكشن (مشابه لإشعار الإعجاب)
        // لا نرسل إشعارًا إذا كان المستخدم يتفاعل مع منشوره الخاص
        // أو إذا كان postAuthorId أو reactorDetails غير متوفرين
        if (postAuthorId != null && reactorDetails != null && !reactingUserId.equals(postAuthorId) && emoji != null && !emoji.isEmpty()) {
            DocumentReference notificationRef = db.collection("users").document(postAuthorId)
                    .collection("notifications").document(); // ID تلقائي للإشعار

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "reaction"); // نوع إشعار جديد
            notificationData.put("fromUserId", reactingUserId);
            notificationData.put("fromUsername", reactorDetails.getUsername() != null ? reactorDetails.getUsername() : "Someone");
            notificationData.put("fromUserAvatarUrl", reactorDetails.getProfileImageUrl() != null ? reactorDetails.getProfileImageUrl() : "");
            notificationData.put("postId", postId);
            // يمكنك إضافة معاينة للمنشور أو الـ emoji المستخدم
            notificationData.put("postContentPreview", "reacted with " + emoji); // مثال بسيط
            notificationData.put("timestamp", FieldValue.serverTimestamp());
            notificationData.put("read", false);

            // يمكنك تنفيذ هذا كجزء من transaction إذا كنت تريد ضمان التناسق
            // ولكن لتحديث بسيط، يمكننا استدعاؤه بشكل منفصل أو ضمن transaction إذا كان تحديث الريأكشن يتم عبر transaction
            // حاليًا، سنقوم بتحديث الريأكشن ثم محاولة إضافة الإشعار.
            // لضمان التناسق التام، يجب أن يكونا معًا في transaction واحد.
            // لكن للتسهيل، سنقوم بتحديث المنشور أولاً.
            return postRef.update(updates).continueWithTask(task -> {
                if (task.isSuccessful()) {
                    // بعد نجاح تحديث الريأكشن، قم بإنشاء الإشعار
                    return notificationRef.set(notificationData);
                } else {
                    // إذا فشل تحديث الريأكشن، لا تقم بإنشاء الإشعار وأرجع الخطأ
                    return Tasks.forException(task.getException());
                }
            });
        } else {
            // إذا كان المستخدم يتفاعل مع منشوره الخاص، أو لا يمكن إنشاء إشعار، فقط قم بتحديث الريأكشن
            return postRef.update(updates);
        }
    }


    public Task<Void> toggleBookmark(String postId, boolean isBookmarked) {
        String userId = auth.getUid();
        if (userId == null) return Tasks.forException(new IllegalArgumentException("User not authenticated for toggleBookmark."));

        DocumentReference postRef = db.collection("posts").document(postId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("bookmarkCount", FieldValue.increment(isBookmarked ? 1 : -1));
        updates.put("bookmarks." + userId, isBookmarked ? true : FieldValue.delete());
        return postRef.update(updates);
    }

    public Task<Void> toggleRepost(String postId, boolean isReposted) {
        String userId = auth.getUid();
        if (userId == null) return Tasks.forException(new IllegalArgumentException("User not authenticated for toggleRepost."));

        DocumentReference postRef = db.collection("posts").document(postId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("repostCount", FieldValue.increment(isReposted ? 1 : -1));
        updates.put("reposts." + userId, isReposted ? true : FieldValue.delete());
        return postRef.update(updates);
    }

    public Task<Void> deletePost(String postId) {
        return db.collection("posts").document(postId).delete();
    }
}