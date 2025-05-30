package com.spidroid.starry.repositories;

import androidx.lifecycle.LiveData; // استيراد LiveData
import androidx.lifecycle.MutableLiveData; // استيراد MutableLiveData
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.spidroid.starry.models.UserModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log; // لاستخدام Log

public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");

    public Task<List<UserModel>> searchUsers(String query) {
        return usersRef.orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .continueWith(task -> task.getResult().toObjects(UserModel.class));
    }

    public Task<Void> toggleFollow(String currentUserId, String targetUserId) {
        DocumentReference currentUserRef = usersRef.document(currentUserId);
        DocumentReference targetUserRef = usersRef.document(targetUserId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot currentUserSnap = transaction.get(currentUserRef);
            DocumentSnapshot targetUserSnap = transaction.get(targetUserRef);

            Map<String, Boolean> following = (Map<String, Boolean>) currentUserSnap.get("following");
            Map<String, Boolean> followers = (Map<String, Boolean>) targetUserSnap.get("followers");

            following = following != null ? new HashMap<>(following) : new HashMap<>();
            followers = followers != null ? new HashMap<>(followers) : new HashMap<>();

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

    // الدالة الجديدة: getUserById
    public LiveData<UserModel> getUserById(String userId) {
        MutableLiveData<UserModel> userLiveData = new MutableLiveData<>();
        usersRef.document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        userLiveData.setValue(user);
                    } else {
                        userLiveData.setValue(null); // المستخدم غير موجود
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Error getting user by ID: " + e.getMessage());
                    userLiveData.setValue(null); // حدوث خطأ
                });
        return userLiveData;
    }
}