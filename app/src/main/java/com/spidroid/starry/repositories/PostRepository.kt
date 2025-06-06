package com.spidroid.starry.repositories

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import java.util.Objects

class PostRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val postsCollection: CollectionReference = db.collection("posts")

    // ★★★ تم تعديل هذا الاستعلام ليكون أبسط للصفحة الرئيسية ★★★
    fun getPosts(limit: Int): Task<QuerySnapshot?> {
        Log.d(
            PostRepository.Companion.TAG,
            "Executing Firestore query for main feed: orderBy 'createdAt' DESC, limit " + limit
        )
        return postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
    }

    val userSuggestions: Task<QuerySnapshot>
        get() = db.collection("users").limit(5).get()

    fun toggleLike(
        postId: String?,
        newLikedState: Boolean,
        postAuthorId: String?,
        likerUser: UserModel?
    ): Task<Void?>? {
        val currentUserId: String? = auth.getUid()
        if (currentUserId == null || postId == null || postId.isEmpty()) {
            return Tasks.forException<Void?>(IllegalArgumentException("User ID or Post ID cannot be null for toggleLike."))
        }

        val postRef: DocumentReference = postsCollection.document(postId)
        val postUpdates: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        postUpdates.put("likeCount", FieldValue.increment(if (newLikedState) 1 else -1))
        postUpdates.put("likes." + currentUserId, if (newLikedState) true else FieldValue.delete())

        if (newLikedState && postAuthorId != null && likerUser != null && (currentUserId != postAuthorId)) {
            val notificationRef: DocumentReference? = db.collection("users").document(postAuthorId)
                .collection("notifications").document()
            val notificationData: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            notificationData.put("type", "like")
            notificationData.put("fromUserId", currentUserId)
            notificationData.put(
                "fromUsername",
                if (likerUser.getUsername() != null) likerUser.getUsername() else "Someone"
            )
            notificationData.put(
                "fromUserAvatarUrl",
                if (likerUser.getProfileImageUrl() != null) likerUser.getProfileImageUrl() else ""
            )
            notificationData.put("postId", postId)
            notificationData.put("timestamp", FieldValue.serverTimestamp())
            notificationData.put("read", false)

            return db.runTransaction({ transaction ->
                transaction.update(postRef, postUpdates)
                transaction.set(notificationRef, notificationData)
                null
            })
        } else {
            return postRef.update(postUpdates)
        }
    }

    fun toggleBookmark(postId: String?, newBookmarkedState: Boolean): Task<Void?>? {
        val userId: String? = auth.getUid()
        if (userId == null || postId == null || postId.isEmpty()) {
            return Tasks.forException<Void?>(IllegalArgumentException("User ID or Post ID cannot be null for toggleBookmark."))
        }
        val postRef: DocumentReference = postsCollection.document(postId)
        val updates: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        updates.put("bookmarkCount", FieldValue.increment(if (newBookmarkedState) 1 else -1))
        updates.put("bookmarks." + userId, if (newBookmarkedState) true else FieldValue.delete())
        return postRef.update(updates)
    }

    fun toggleRepost(postId: String?, newRepostedState: Boolean): Task<Void?>? {
        val userId: String? = auth.getUid()
        if (userId == null || postId == null || postId.isEmpty()) {
            return Tasks.forException<Void?>(IllegalArgumentException("User ID or Post ID cannot be null for toggleRepost."))
        }
        val postRef: DocumentReference = postsCollection.document(postId)
        val updates: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        updates.put("repostCount", FieldValue.increment(if (newRepostedState) 1 else -1))
        updates.put("reposts." + userId, if (newRepostedState) true else FieldValue.delete())
        return postRef.update(updates)
    }

    fun addOrUpdateReaction(
        postId: String?,
        reactingUserId: String?,
        emoji: String?,
        postAuthorId: String?,
        reactorDetails: UserModel?
    ): Task<Void?>? {
        if (postId == null || reactingUserId == null) {
            Log.e(
                PostRepository.Companion.TAG,
                "Post ID or Reacting User ID is null for addOrUpdateReaction."
            )
            return Tasks.forException<Void?>(IllegalArgumentException("Post ID or Reacting User ID cannot be null."))
        }

        val postRef: DocumentReference = postsCollection.document(postId)
        val updates: MutableMap<String?, Any?> = HashMap<String?, Any?>()

        if (emoji == null || emoji.isEmpty()) {
            updates.put("reactions." + reactingUserId, FieldValue.delete())
        } else {
            updates.put("reactions." + reactingUserId, emoji)
        }

        if (postAuthorId != null && reactorDetails != null && (reactingUserId != postAuthorId) && emoji != null && !emoji.isEmpty()) {
            val notificationRef: DocumentReference? = db.collection("users").document(postAuthorId)
                .collection("notifications").document()
            val notificationData: MutableMap<String?, Any?> = HashMap<String?, Any?>()
            notificationData.put("type", "reaction")
            notificationData.put("fromUserId", reactingUserId)
            notificationData.put(
                "fromUsername",
                if (reactorDetails.getUsername() != null) reactorDetails.getUsername() else "Someone"
            )
            notificationData.put(
                "fromUserAvatarUrl",
                if (reactorDetails.getProfileImageUrl() != null) reactorDetails.getProfileImageUrl() else ""
            )
            notificationData.put("postId", postId)
            notificationData.put("postContentPreview", "reacted with " + emoji)
            notificationData.put("timestamp", FieldValue.serverTimestamp())
            notificationData.put("read", false)

            return db.runTransaction({ transaction ->
                transaction.update(postRef, updates)
                transaction.set(notificationRef, notificationData)
                null
            })
        } else {
            return postRef.update(updates)
        }
    }

    fun setPostPinnedStatus(
        postIdToUpdate: String?,
        authorId: String?,
        newPinnedState: Boolean
    ): Task<Void?>? {
        if (postIdToUpdate == null || authorId == null) {
            return Tasks.forException<Void?>(IllegalArgumentException("Post ID or Author ID cannot be null for pinning."))
        }

        val postToUpdateRef: DocumentReference = postsCollection.document(postIdToUpdate)

        if (newPinnedState) {
            return postsCollection
                .whereEqualTo("authorId", authorId)
                .whereEqualTo("isPinned", true)
                .get()
                .continueWithTask({ task ->
                    if (!task.isSuccessful()) {
                        Log.e(
                            PostRepository.Companion.TAG,
                            "Failed to query old pinned posts.",
                            task.getException()
                        )
                        throw Objects.requireNonNull<T?>(task.getException())
                    }
                    val oldPinnedDocs: MutableList<DocumentSnapshot> =
                        task.getResult().getDocuments()
                    db.runTransaction({ innerTransaction ->
                        for (oldPinnedPostDoc in oldPinnedDocs) {
                            if (!oldPinnedPostDoc.getId().equals(postIdToUpdate)) {
                                Log.d(
                                    PostRepository.Companion.TAG,
                                    "Unpinning old post: " + oldPinnedPostDoc.getId() + " inside transaction"
                                )
                                innerTransaction.update(
                                    oldPinnedPostDoc.getReference(),
                                    "isPinned",
                                    false
                                )
                            }
                        }
                        Log.d(
                            PostRepository.Companion.TAG,
                            "Pinning new post: " + postIdToUpdate + " inside transaction"
                        )
                        innerTransaction.update(postToUpdateRef, "isPinned", true)
                        null
                    })
                })
        } else {
            Log.d(PostRepository.Companion.TAG, "Unpinning post: " + postIdToUpdate)
            return postToUpdateRef.update("isPinned", false)
        }
    }

    fun deletePost(postId: String?): Task<Void?>? {
        if (postId == null || postId.isEmpty()) {
            return Tasks.forException<Void?>(IllegalArgumentException("Post ID cannot be null or empty for deletePost."))
        }
        return postsCollection.document(postId).delete()
    }

    fun updatePostContent(postId: String?, newContent: String?): Task<Void?>? {
        if (postId == null || postId.isEmpty() || newContent == null) {
            return Tasks.forException<Void?>(IllegalArgumentException("Post ID or new content cannot be null."))
        }
        return postsCollection.document(postId)
            .update("content", newContent, "updatedAt", FieldValue.serverTimestamp())
    }

    fun updatePostPrivacy(postId: String?, newPrivacyLevel: String?): Task<Void?>? {
        if (postId == null || postId.isEmpty() || newPrivacyLevel == null) {
            return Tasks.forException<Void?>(IllegalArgumentException("Post ID or new privacy level cannot be null."))
        }
        // ستحتاج لإضافة حقل "privacyLevel" إلى PostModel.java
        return postsCollection.document(postId)
            .update("privacyLevel", newPrivacyLevel, "updatedAt", FieldValue.serverTimestamp())
    }

    fun submitReport(reportData: MutableMap<String?, Any?>?): Task<DocumentReference?>? {
        if (reportData == null || reportData.isEmpty()) {
            return Tasks.forException<DocumentReference?>(IllegalArgumentException("Report data cannot be null or empty."))
        }
        return db.collection("reports").add(reportData)
    }

    companion object {
        private const val TAG = "PostRepository"
    }
}