package com.spidroid.starry.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import kotlinx.coroutines.tasks.await


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

    fun getPostsForFollowing(followingIds: List<String>, limit: Int): Task<QuerySnapshot> {
        if (followingIds.isEmpty()) {
            // إذا كان المستخدم لا يتابع أحدًا، أرجع مهمة فارغة وناجحة
            return Tasks.forResult(null)
        }
        Log.d(TAG, "Executing Firestore query for following feed. Following count: ${followingIds.size}")
        // جلب المنشورات حيث يكون authorId موجودًا في قائمة المتابعين
        return postsCollection
            .whereIn("authorId", followingIds)
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
            // إذا كان المستخدم يلغي الإعجاب، قم بإزالة تفاعله أيضًا
            val reactionUpdate = if (newLikedState) "❤️" else FieldValue.delete()


            transaction.update(postRef, "likes.$currentUserId", likeUpdate)
            transaction.update(postRef, "reactions.$currentUserId", reactionUpdate)
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
    suspend fun createPost(
        authorId: String,
        text: String?,
        mediaUris: List<Uri>,
        context: Context,
        communityId: String? = null, // الحقل المضاف
        communityName: String? = null // الحقل المضاف
    ): PostModel {
        // ... منطقك الحالي لرفع الصور ...

        val post = PostModel(
            // ... الحقول الحالية ...
            communityId = communityId, // احفظه هنا
            communityName = communityName // وهنا
        )

        // ... احفظ المنشور في Firestore
        return post
    }


    fun addOrUpdateReaction(postId: String, reactingUserId: String, emoji: String, postAuthorId: String, reactorDetails: UserModel): Task<Void> {
        val postRef = postsCollection.document(postId)
        return db.runTransaction { transaction ->
            val postSnapshot = transaction.get(postRef)
            val likesMap = postSnapshot.get("likes") as? Map<String, Boolean> ?: emptyMap()

            // تحقق إذا كان المستخدم قد قام بالإعجاب بالفعل
            val wasAlreadyLiked = likesMap.containsKey(reactingUserId)

            // إضافة التفاعل
            transaction.update(postRef, "reactions.$reactingUserId", emoji)

            // إذا لم يكن قد قام بالإعجاب مسبقًا، قم بتحديث الإعجاب والعدد
            if (!wasAlreadyLiked) {
                transaction.update(postRef, "likes.$reactingUserId", true)
                transaction.update(postRef, "likeCount", FieldValue.increment(1))
            }

            // إرسال إشعار
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
                transaction.set(notificationRef, notificationData)
            }
            null
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

    suspend fun setPostPinnedStatus(postIdToUpdate: String, authorId: String, newPinnedState: Boolean) {
        val oldPinnedQuery = postsCollection
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("isPinned", true)

        val currentlyPinnedPostsSnapshot = oldPinnedQuery.get().await()

        db.runTransaction { transaction ->
            val postToUpdateRef = postsCollection.document(postIdToUpdate)

            for (oldPostDoc in currentlyPinnedPostsSnapshot.documents) {
                if (oldPostDoc.id != postIdToUpdate) {
                    transaction.update(oldPostDoc.reference, "isPinned", false)
                }
            }

            transaction.update(postToUpdateRef, "isPinned", newPinnedState)
            null
        }.await()
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
    fun getPostsPaginated(limit: Int, lastPost: PostModel?): Task<QuerySnapshot> {
        // بناء الاستعلام الأساسي
        var query: Query = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        // إذا كان هناك منشور أخير (cursor)، ابدأ الجلب من بعده
        if (lastPost != null) {
            query = query.startAfter(lastPost.createdAt)
        }

        Log.d(TAG, "Executing paginated posts query. Starting after: ${lastPost?.createdAt}")
        return query.get()
    }
    fun submitReport(reportData: Map<String, Any>): Task<DocumentReference> {
        return db.collection("reports").add(reportData)
    }
    fun searchPostsByContent(query: String, limit: Int = 20): Task<QuerySnapshot> {
        if (query.isBlank()) {
            return Tasks.forResult(null)
        }
        return postsCollection
            .whereGreaterThanOrEqualTo("content", query)
            .whereLessThanOrEqualTo("content", query + '\uf8ff')
            .orderBy("content")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
    }
    fun getBookmarkedPosts(userId: String, limit: Int = 30): Task<QuerySnapshot> {
        return postsCollection
            .whereEqualTo("bookmarks.$userId", true) // البحث عن المنشورات التي تحتوي على ID المستخدم في خريطة الحفظ
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
    }
    fun searchPostsByHashtag(hashtag: String, limit: Int = 20): Task<QuerySnapshot> {
        if (hashtag.isBlank()) {
            return Tasks.forResult(null)
        }
        return postsCollection
            .whereArrayContains("hashtags", hashtag.lowercase())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
    }

    companion object {
        private const val TAG = "PostRepository"
    }
}