package com.spidroid.starry.repositories;

import com.google.android.gms.tasks.Task;
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
        // يمكنك تطوير هذا المنطق لاحقاً ليكون أذكى
        return db.collection("users").limit(5).get();
    }

    // دالة لمعالجة الإعجاب
    public Task<Void> toggleLike(String postId, boolean isLiked) {
        String userId = auth.getUid();
        if (userId == null) return null; // يجب معالجة هذه الحالة في الـ ViewModel

        DocumentReference postRef = db.collection("posts").document(postId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("likeCount", FieldValue.increment(isLiked ? 1 : -1));
        updates.put("likes." + userId, isLiked ? true : FieldValue.delete());

        return postRef.update(updates);
    }

    // دالة لمعالجة الحفظ
    public Task<Void> toggleBookmark(String postId, boolean isBookmarked) {
        String userId = auth.getUid();
        if (userId == null) return null;

        DocumentReference postRef = db.collection("posts").document(postId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("bookmarkCount", FieldValue.increment(isBookmarked ? 1 : -1));
        updates.put("bookmarks." + userId, isBookmarked ? true : FieldValue.delete());

        return postRef.update(updates);
    }

    // دالة لمعالجة إعادة النشر
    public Task<Void> toggleRepost(String postId, boolean isReposted) {
        String userId = auth.getUid();
        if (userId == null) return null;

        DocumentReference postRef = db.collection("posts").document(postId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("repostCount", FieldValue.increment(isReposted ? 1 : -1));
        updates.put("reposts." + userId, isReposted ? true : FieldValue.delete());

        return postRef.update(updates);
    }

    // دالة لحذف المنشور
    public Task<Void> deletePost(String postId) {
        return db.collection("posts").document(postId).delete();
    }
}