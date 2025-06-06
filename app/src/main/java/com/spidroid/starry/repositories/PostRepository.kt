// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/repositories/PostRepository.kt
package com.spidroid.starry.repositories

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import kotlinx.coroutines.tasks.await // ✨ إضافة هذا الاستيراد


class PostRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val postsCollection: CollectionReference = db.collection("posts")

    fun getPosts(limit: Int): Task<QuerySnapshot> {
        Log.d(TAG, "Executing Firestore query for main feed: orderBy 'createdAt' DESC, limit $limit")
        return postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
    }

    val userSuggestions: Task<QuerySnapshot>
        get() = db.collection("users").limit(5).get()

    fun toggleLike(postId: String, newLikedState: Boolean, postAuthorId: String, likerUser: UserModel): Task<Void> {
        val currentUserId = auth.currentUser?.uid ?: return Tasks.forException(Exception("User not authenticated."))
        val postRef = postsCollection.document(postId)

        return db.runTransaction { transaction ->
            val postSnapshot = transaction.get(postRef)
            val currentLikes = (postSnapshot.getLong("likeCount") ?: 0L)
            val newLikeCount = if (newLikedState) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)

            val likeUpdate = if (newLikedState) true else FieldValue.delete()
            transaction.update(postRef, "likes.$currentUserId", likeUpdate)
            transaction.update(postRef, "likeCount", newLikeCount)

            // Send notification only if someone else likes the post
            if (newLikedState && currentUserId != postAuthorId) {
                val notificationRef = db.collection("users").document(postAuthorId).collection("notifications").document()
                val notificationData = hashMapOf(
                    "type" to "like",
                    "fromUserId" to currentUserId,
                    "fromUsername" to (likerUser.username),
                    "fromUserAvatarUrl" to (likerUser.profileImageUrl ?: ""),
                    "postId" to postId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "read" to false
                )
                transaction.set(notificationRef, notificationData)
            }
            null // Transaction must return null
        }
    }

    fun toggleBookmark(postId: String, newBookmarkedState: Boolean): Task<Void> {
        val userId = auth.currentUser?.uid ?: return Tasks.forException(Exception("User not authenticated."))
        val postRef = postsCollection.document(postId)
        val updateData = mapOf(
            "bookmarkCount" to FieldValue.increment(if (newBookmarkedState) 1 else -1),
            "bookmarks.$userId" to if (newBookmarkedState) true else FieldValue.delete()
        )
        return postRef.update(updateData)
    }

    fun toggleRepost(postId: String, newRepostedState: Boolean): Task<Void> {
        val userId = auth.currentUser?.uid ?: return Tasks.forException(Exception("User not authenticated."))
        val postRef = postsCollection.document(postId)
        val updateData = mapOf(
            "repostCount" to FieldValue.increment(if (newRepostedState) 1 else -1),
            "reposts.$userId" to if (newRepostedState) true else FieldValue.delete()
        )
        return postRef.update(updateData)
    }

    fun addOrUpdateReaction(postId: String, reactingUserId: String, emoji: String, postAuthorId: String, reactorDetails: UserModel): Task<Void> {
        val postRef = postsCollection.document(postId)
        val reactionUpdate = mapOf("reactions.$reactingUserId" to emoji)

        // This part can also be a transaction if you want to ensure the notification is sent only if the reaction is updated.
        postRef.update(reactionUpdate)

        // Send notification logic
        if (reactingUserId != postAuthorId) {
            val notificationRef = db.collection("users").document(postAuthorId).collection("notifications").document()
            val notificationData = hashMapOf(
                "type" to "reaction",
                "fromUserId" to reactingUserId,
                "fromUsername" to reactorDetails.username,
                "fromUserAvatarUrl" to (reactorDetails.profileImageUrl ?: ""),
                "postId" to postId,
                "postContentPreview" to "reacted with $emoji",
                "timestamp" to FieldValue.serverTimestamp(),
                "read" to false
            )
            return notificationRef.set(notificationData) // Returns the task for setting the notification
        }

        return Tasks.forResult(null) // Return a completed task if no notification is sent
    }

    // ✨ تم تحويل الدالة إلى suspend fun
    suspend fun setPostPinnedStatus(postIdToUpdate: String, authorId: String, newPinnedState: Boolean) {
        // خطوة 1: جلب أي منشورات مثبتة حاليًا للمؤلف
        // يجب أن يتم هذا داخل coroutine لأننا نستخدم await()
        val oldPinnedQuery = postsCollection
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("isPinned", true)

        val currentlyPinnedPostsSnapshot = oldPinnedQuery.get().await() // ✨ استخدام await() هنا

        // خطوة 2: تشغيل المعاملة
        // db.runTransaction تُرجع Task<Void>، لذا يمكن انتظارها أيضًا
        db.runTransaction { transaction ->
            val postToUpdateRef = postsCollection.document(postIdToUpdate)

            // إلغاء تثبيت أي منشورات أخرى كانت مثبتة
            for (oldPostDoc in currentlyPinnedPostsSnapshot.documents) {
                if (oldPostDoc.id != postIdToUpdate) {
                    transaction.update(oldPostDoc.reference, "isPinned", false)
                }
            }

            // تثبيت أو إلغاء تثبيت المنشور الحالي
            transaction.update(postToUpdateRef, "isPinned", newPinnedState)
            null // المعاملة يجب أن تُرجع null
        }.await() // ✨ انتظار اكتمال المعاملة
    }


    fun deletePost(postId: String): Task<Void> {
        return postsCollection.document(postId).delete()
    }

    fun updatePostContent(postId: String, newContent: String): Task<Void> {
        val updates = mapOf(
            "content" to newContent,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return postsCollection.document(postId).update(updates)
    }

    fun updatePostPrivacy(postId: String, newPrivacyLevel: String): Task<Void> {
        val updates = mapOf(
            "privacyLevel" to newPrivacyLevel,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return postsCollection.document(postId).update(updates)
    }

    fun submitReport(reportData: Map<String, Any>): Task<DocumentReference> {
        return db.collection("reports").add(reportData)
    }

    companion object {
        private const val TAG = "PostRepository"
    }
}