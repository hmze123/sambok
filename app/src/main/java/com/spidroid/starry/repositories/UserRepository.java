package com.spidroid.starry.repositories;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.models.UserModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");

    public Task<List<UserModel>> searchUsers(String query) {
        Task<List<UserModel>> searchByUsername = usersRef.orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(10)
                .get()
                .continueWith(task -> task.getResult().toObjects(UserModel.class));

        Task<List<UserModel>> searchByDisplayName = usersRef.orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(10)
                .get()
                .continueWith(task -> task.getResult().toObjects(UserModel.class));

        return Tasks.whenAllSuccess(searchByUsername, searchByDisplayName).continueWith(task -> {
            List<UserModel> usernameResults = (List<UserModel>) task.getResult().get(0);
            List<UserModel> displayNameResults = (List<UserModel>) task.getResult().get(1);
            Map<String, UserModel> combinedResultsMap = new HashMap<>();
            for (UserModel user : usernameResults) {
                combinedResultsMap.put(user.getUserId(), user);
            }
            for (UserModel user : displayNameResults) {
                combinedResultsMap.put(user.getUserId(), user);
            }
            return new ArrayList<>(combinedResultsMap.values());
        });
    }

    public Task<Void> toggleFollow(String currentUserId, String targetUserId) {
        DocumentReference currentUserRef = usersRef.document(currentUserId);
        DocumentReference targetUserRef = usersRef.document(targetUserId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot currentUserSnap = transaction.get(currentUserRef);
            DocumentSnapshot targetUserSnap = transaction.get(targetUserRef);

            Map<String, Object> following = (Map<String, Object>) currentUserSnap.get("following");
            if (following == null) following = new HashMap<>();

            Map<String, Object> followers = (Map<String, Object>) targetUserSnap.get("followers");
            if (followers == null) followers = new HashMap<>();

            if (following.containsKey(targetUserId)) {
                following.remove(targetUserId);
                followers.remove(currentUserId);
            } else {
                following.put(targetUserId, true);
                followers.put(currentUserId, true);
            }

            transaction.update(currentUserRef, "following", following);
            transaction.update(targetUserRef, "followers", followers);
            return null;
        });
    }

    public LiveData<UserModel> getUserById(String userId) {
        MutableLiveData<UserModel> userLiveData = new MutableLiveData<>();
        usersRef.document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        userLiveData.setValue(user);
                    } else {
                        userLiveData.setValue(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Error getting user by ID: " + e.getMessage());
                    userLiveData.setValue(null);
                });

        // *** هذا هو السطر الذي كان ناقصاً على الأغلب ***
        return userLiveData;
    }
}