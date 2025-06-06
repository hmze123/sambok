package com.spidroid.starry.repositories

import com.google.firebase.firestore.DocumentReference

class CommentRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getCommentsForPost(postId: kotlin.String?): com.google.android.gms.tasks.Task<QuerySnapshot?> {
        return db.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
    }

    // ... (داخل كلاس CommentRepository)
    fun addComment(
        postId: kotlin.String?,
        commentData: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?>?
    ): com.google.android.gms.tasks.Task<DocumentReference?> {
        return db.collection("posts")
            .document(postId)
            .collection("comments")
            .add(commentData)
    }

    fun toggleCommentLike(
        postId: kotlin.String?,
        commentId: kotlin.String?,
        userId: kotlin.String?,
        isLiked: kotlin.Boolean
    ): com.google.android.gms.tasks.Task<java.lang.Void?> {
        val commentRef: DocumentReference =
            db.collection("posts").document(postId).collection("comments").document(commentId)
        val updates: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?> =
            java.util.HashMap<kotlin.String?, kotlin.Any?>()
        updates.put("likeCount", FieldValue.increment(if (isLiked) 1 else -1))
        updates.put("likes." + userId, if (isLiked) true else FieldValue.delete())
        return commentRef.update(updates)
    }

    fun deleteComment(
        postId: kotlin.String?,
        commentId: kotlin.String?
    ): com.google.android.gms.tasks.Task<java.lang.Void?> {
        return db.collection("posts")
            .document(postId)
            .collection("comments")
            .document(commentId)
            .delete()
    }

    fun updatePostReplyCount(
        postId: kotlin.String?,
        increment: Int
    ): com.google.android.gms.tasks.Task<java.lang.Void?> {
        return db.collection("posts")
            .document(postId)
            .update("replyCount", FieldValue.increment(increment))
    } // يمكنك إضافة دوال أخرى هنا لاحقًا (مثل إضافة تعليق، حذفه، إلخ)
}