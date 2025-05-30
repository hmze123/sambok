package com.spidroid.starry.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class CommentRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<QuerySnapshot> getCommentsForPost(String postId) {
        return db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get();
    }
// ... (داخل كلاس CommentRepository)

    public Task<DocumentReference> addComment(String postId, Map<String, Object> commentData) {
        return db.collection("posts")
                .document(postId)
                .collection("comments")
                .add(commentData);
    }

    public Task<Void> toggleCommentLike(String postId, String commentId, String userId, boolean isLiked) {
        DocumentReference commentRef = db.collection("posts").document(postId).collection("comments").document(commentId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("likeCount", FieldValue.increment(isLiked ? 1 : -1));
        updates.put("likes." + userId, isLiked ? true : FieldValue.delete());
        return commentRef.update(updates);
    }

    public Task<Void> deleteComment(String postId, String commentId) {
        return db.collection("posts")
                .document(postId)
                .collection("comments")
                .document(commentId)
                .delete();
    }

    public Task<Void> updatePostReplyCount(String postId, int increment) {
        return db.collection("posts")
                .document(postId)
                .update("replyCount", FieldValue.increment(increment));
    }
    // يمكنك إضافة دوال أخرى هنا لاحقًا (مثل إضافة تعليق، حذفه، إلخ)
}