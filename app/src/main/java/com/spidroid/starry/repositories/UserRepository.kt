package com.spidroid.starry.repositories

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.spidroid.starry.models.UserModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    private val usersCollection = firestore.collection("users")

    suspend fun createUser(uid: String, email: String, username: String, name: String) {
        val user = UserModel(
            userId = uid, // <-- ١. تم التعديل من 'uid' إلى 'userId'
            username = username,
            displayName = name, // <-- ٢. تم التعديل من 'name' إلى 'displayName'
            email = email
        )
        usersCollection.document(uid).set(user).await()
    }

    suspend fun getUser(uid: String): UserModel? {
        val document = usersCollection.document(uid).get().await()
        return document.toObject(UserModel::class.java)
    }

    suspend fun updateUserProfile(uid: String, name: String, bio: String, link: String) {
        val updates = mapOf(
            "displayName" to name, // <-- التأكد من استخدام الاسم الصحيح هنا أيضًا
            "bio" to bio,
            "link" to link
        )
        usersCollection.document(uid).update(updates).await()
    }

    suspend fun uploadProfileImage(uid: String, imageUri: Uri): String {
        val storageRef = storage.reference.child("profile_images/$uid")
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun followUser(currentUserId: String, targetUserId: String) {
        usersCollection.document(currentUserId).update("following.$targetUserId", true).await()
        usersCollection.document(targetUserId).update("followers.$currentUserId", true).await()
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        usersCollection.document(currentUserId).update("following.$targetUserId", FieldValue.delete()).await()
        usersCollection.document(targetUserId).update("followers.$currentUserId", FieldValue.delete()).await()
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val doc = usersCollection.document(currentUserId).get().await()
            val user = doc.toObject(UserModel::class.java)
            user?.following?.containsKey(targetUserId) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun searchUsers(query: String): com.google.android.gms.tasks.Task<QuerySnapshot> {
        return usersCollection
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + '\uf8ff')
            .limit(20)
            .get()
    }
}