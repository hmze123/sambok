package com.spidroid.starry.repositories

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.utils.UriToFileConverter
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    private val postsCollection = firestore.collection("posts")

    suspend fun createPost(
        text: String?,
        mediaUris: List<String>,
        videoUris: List<String>
    ): PostModel {
        val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
        val postId = postsCollection.document().id

        val allMedia = (mediaUris + videoUris).toMutableList()

        val post = PostModel(
            postId = postId,
            authorId = currentUser.uid,
            content = text,
            mediaUrls = allMedia,
            createdAt = Date(),
            authorUsername = currentUser.displayName,
            authorDisplayName = currentUser.displayName
        )
        postsCollection.document(postId).set(post).await()
        return post
    }

    suspend fun uploadMedia(context: Context, uri: Uri): String {
        // --- ١. تم تصحيح اسم الدالة هنا ---
        val file = UriToFileConverter.toFile(context, uri)
            ?: throw IllegalArgumentException("Could not convert URI to File for: $uri")

        val storageRef = storage.reference.child("post_media/${file.name}")
        storageRef.putFile(Uri.fromFile(file)).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun getBookmarkedPosts(userId: String): List<PostModel> {
        val snapshot = postsCollection
            .whereEqualTo("bookmarks.$userId", true)
            .get()
            .await()
        return snapshot.toObjects(PostModel::class.java)
    }

    fun getPostsPaginated(limit: Int, lastVisible: Date?): Task<QuerySnapshot> {
        val query = postsCollection.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit.toLong())
        if (lastVisible != null) {
            return query.startAfter(lastVisible).get()
        }
        return query.get()
    }

    fun searchPostsByContent(query: String): Task<QuerySnapshot> {
        return postsCollection
            .whereGreaterThanOrEqualTo("content", query)
            .whereLessThanOrEqualTo("content", query + '\uf8ff')
            .limit(20)
            .get()
    }

    fun deletePost(postId: String): Task<Void> {
        return postsCollection.document(postId).delete()
    }

    fun updatePostReplyCount(postId: String, count: Long): Task<Void> {
        return postsCollection.document(postId).update("replyCount", FieldValue.increment(count))
    }
}