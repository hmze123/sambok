// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/repositories/CommentRepository.kt
package com.spidroid.starry.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class CommentRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun getCommentsForPost(postId: String): Task<QuerySnapshot> {
        return db.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING) // ✨ تم التغيير إلى تنازلي لعرض الأحدث أولاً
            .get()
    }

    fun addComment(postId: String, commentData: Map<String, Any>): Task<DocumentReference> {
        return db.collection("posts")
            .document(postId)
            .collection("comments")
            .add(commentData)
    }

    fun toggleCommentLike(postId: String, commentId: String, userId: String, isLiked: Boolean): Task<Void> {
        val commentRef = db.collection("posts").document(postId).collection("comments").document(commentId)
        val updates = mapOf(
            "likeCount" to FieldValue.increment(if (isLiked) 1L else -1L),
            "likes.$userId" to if (isLiked) true else FieldValue.delete()
        )
        return commentRef.update(updates)
    }

    fun deleteComment(postId: String, commentId: String): Task<Void> {
        // Here you might want to implement a soft delete instead of a hard delete
        // For example: `update("deleted", true)`
        return db.collection("posts")
            .document(postId)
            .collection("comments")
            .document(commentId)
            .delete()
    }

    fun updatePostReplyCount(postId: String, increment: Long): Task<Void> {
        return db.collection("posts")
            .document(postId)
            .update("replyCount", FieldValue.increment(increment))
    }
}