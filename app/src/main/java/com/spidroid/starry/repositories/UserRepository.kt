package com.spidroid.starry.repositories

import com.google.firebase.firestore.CollectionReference

class UserRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersRef: CollectionReference = db.collection("users")

    fun searchUsers(query: kotlin.String?): com.google.android.gms.tasks.Task<kotlin.collections.MutableList<UserModel?>?> {
        val searchByUsername: com.google.android.gms.tasks.Task<kotlin.collections.MutableList<UserModel?>?>? =
            usersRef.orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(10)
                .get()
                .continueWith({ task -> task.getResult().toObjects(UserModel::class.java) })

        val searchByDisplayName: com.google.android.gms.tasks.Task<kotlin.collections.MutableList<UserModel?>?>? =
            usersRef.orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(10)
                .get()
                .continueWith({ task -> task.getResult().toObjects(UserModel::class.java) })

        return Tasks.whenAllSuccess<kotlin.Any?>(searchByUsername, searchByDisplayName)
            .continueWith<kotlin.collections.MutableList<UserModel?>?>(com.google.android.gms.tasks.Continuation { task: com.google.android.gms.tasks.Task<kotlin.collections.MutableList<kotlin.Any?>?>? ->
                val usernameResults: kotlin.collections.MutableList<UserModel> =
                    task!!.getResult()!!
                        .get(0) as kotlin.collections.MutableList<UserModel>
                val displayNameResults: kotlin.collections.MutableList<UserModel> =
                    task.getResult()!!.get(1) as kotlin.collections.MutableList<UserModel>
                val combinedResultsMap: kotlin.collections.MutableMap<kotlin.String?, UserModel?> =
                    java.util.HashMap<kotlin.String?, UserModel?>()
                for (user in usernameResults) {
                    combinedResultsMap.put(user.getUserId(), user)
                }
                for (user in displayNameResults) {
                    combinedResultsMap.put(user.getUserId(), user)
                }
                java.util.ArrayList<UserModel?>(combinedResultsMap.values)
            })
    }

    fun toggleFollow(
        currentUserId: kotlin.String?,
        targetUserId: kotlin.String?
    ): com.google.android.gms.tasks.Task<java.lang.Void?> {
        val currentUserRef: DocumentReference? = usersRef.document(currentUserId)
        val targetUserRef: DocumentReference? = usersRef.document(targetUserId)

        return db.runTransaction({ transaction ->
            val currentUserSnap: DocumentSnapshot = transaction.get(currentUserRef)
            val targetUserSnap: DocumentSnapshot = transaction.get(targetUserRef)

            var following = currentUserSnap.get("following")
            if (following == null) following = java.util.HashMap<kotlin.String?, kotlin.Any?>()

            var followers = targetUserSnap.get("followers")
            if (followers == null) followers = java.util.HashMap<kotlin.String?, kotlin.Any?>()

            if (following.containsKey(targetUserId)) {
                following.remove(targetUserId)
                followers.remove(currentUserId)
            } else {
                following.put(targetUserId, true)
                followers.put(currentUserId, true)
            }

            transaction.update(currentUserRef, "following", following)
            transaction.update(targetUserRef, "followers", followers)
            null
        })
    }

    fun getUserById(userId: kotlin.String?): LiveData<UserModel?> {
        val userLiveData: MutableLiveData<UserModel?> = MutableLiveData<UserModel?>()
        usersRef.document(userId).get()
            .addOnSuccessListener({ documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user: UserModel? = documentSnapshot.toObject(UserModel::class.java)
                    userLiveData.setValue(user)
                } else {
                    userLiveData.setValue(null)
                }
            })
            .addOnFailureListener({ e ->
                android.util.Log.e("UserRepository", "Error getting user by ID: " + e.getMessage())
                userLiveData.setValue(null)
            })

        // *** هذا هو السطر الذي كان ناقصاً على الأغلب ***
        return userLiveData
    }
}