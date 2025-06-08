package com.spidroid.starry.repositories

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.spidroid.starry.models.PageModel
import kotlinx.coroutines.tasks.await
import java.util.*

class PageRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val pagesCollection = db.collection("pages")
    private val postsCollection = db.collection("posts")
    private val usersCollection = db.collection("users")

    suspend fun createPage(pageModel: PageModel, avatarUri: Uri?): Result<PageModel> {
        return try {
            val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
            pageModel.ownerId = userId
            pageModel.admins[userId] = true

            if (avatarUri != null) {
                val avatarRef = storage.reference.child("page_avatars/${UUID.randomUUID()}")
                avatarRef.putFile(avatarUri).await()
                pageModel.pageAvatarUrl = avatarRef.downloadUrl.await().toString()
            }

            val newPageRef = pagesCollection.document()
            pageModel.pageId = newPageRef.id
            val userRef = usersCollection.document(userId)

            db.runTransaction { transaction ->
                transaction.set(newPageRef, pageModel)
                transaction.update(userRef, "managedPages.${pageModel.pageId}", true)
                null
            }.await()

            Result.success(pageModel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPageById(pageId: String): Task<DocumentSnapshot> {
        return pagesCollection.document(pageId).get()
    }

    fun getPostsForPage(pageId: String, limit: Int = 20): Task<QuerySnapshot> {
        return postsCollection
            .whereEqualTo("authorId", pageId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
    }
}