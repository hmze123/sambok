package com.spidroid.starry.repositories

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// --- ١. تمت إضافة هذه التعليقات التوضيحية ---
@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val postsCollection = firestore.collection("posts")

    fun getCommentsForPost(postId: String): Task<QuerySnapshot> {
        return postsCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
    }

    fun addComment(postId: String, commentData: Map<String, Any>): Task<Void> {
        val commentRef = postsCollection.document(postId).collection("comments").document()
        return commentRef.set(commentData)
    }

    fun toggleCommentLike(postId: String, commentId: String, userId: String, isLiked: Boolean): Task<Void> {
        val commentRef = postsCollection.document(postId).collection("comments").document(commentId)
        val updateData = mapOf("likes.$userId" to isLiked)
        return commentRef.update(updateData)
    }

    fun deleteComment(postId: String, commentId: String): Task<Void> {
        return postsCollection.document(postId).collection("comments").document(commentId).delete()
    }

    fun updatePostReplyCount(postId: String, increment: Long): Task<Void> {
        val postRef = postsCollection.document(postId)
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val newReplyCount = (snapshot.getLong("replyCount") ?: 0) + increment
            transaction.update(postRef, "replyCount", newReplyCount)
            null
        }
    }
}