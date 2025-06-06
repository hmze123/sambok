package com.spidroid.starry.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.spidroid.starry.models.UserModel

class UserRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersRef: CollectionReference = db.collection("users")

    fun searchUsers(query: String): Task<List<UserModel>> {
        if (query.isBlank()) {
            return Tasks.forResult(emptyList())
        }
        // A simple search by username. You can expand this to search by displayName as well.
        return usersRef.orderBy("username")
            .startAt(query)
            .endAt(query + '\uf8ff')
            .limit(20)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Unknown Firestore error")
                }
                task.result?.toObjects(UserModel::class.java) ?: emptyList()
            }
    }

    fun toggleFollow(currentUserId: String, targetUserId: String): Task<Void> {
        val currentUserRef = usersRef.document(currentUserId)
        val targetUserRef = usersRef.document(targetUserId)

        return db.runTransaction { transaction ->
            val targetUserDoc = transaction.get(targetUserRef)
            val currentFollowers = (targetUserDoc.get("followers") as? Map<String, Boolean>) ?: emptyMap()

            // Decide whether to follow or unfollow
            if (currentFollowers.containsKey(currentUserId)) {
                // Unfollow
                transaction.update(targetUserRef, "followers.$currentUserId", FieldValue.delete())
                transaction.update(currentUserRef, "following.$targetUserId", FieldValue.delete())
            } else {
                // Follow
                transaction.update(targetUserRef, "followers.$currentUserId", true)
                transaction.update(currentUserRef, "following.$targetUserId", true)
            }
            null // Transaction must return null in Kotlin
        }
    }

    fun getUserById(userId: String): LiveData<UserModel?> {
        val userLiveData = MutableLiveData<UserModel?>()
        if (userId.isBlank()) {
            userLiveData.value = null
            return userLiveData
        }
        usersRef.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserRepository", "Listen failed for user ID: $userId", error)
                userLiveData.value = null
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(UserModel::class.java)
                user?.userId = snapshot.id // Ensure the document ID is set on the model
                userLiveData.value = user
            } else {
                Log.d("UserRepository", "User document does not exist for ID: $userId")
                userLiveData.value = null
            }
        }
        return userLiveData
    }
}