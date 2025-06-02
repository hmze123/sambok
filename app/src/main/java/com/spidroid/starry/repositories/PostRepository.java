package com.spidroid.starry.repositories;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PostRepository {
    private static final String TAG = "PostRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final CollectionReference postsCollection = db.collection("posts");

    // ★★★ تم تعديل هذا الاستعلام ليكون أبسط للصفحة الرئيسية ★★★
    public Task<QuerySnapshot> getPosts(int limit) {
        Log.d(TAG, "Executing Firestore query for main feed: orderBy 'createdAt' DESC, limit " + limit);
        return postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    public Task<QuerySnapshot> getUserSuggestions() {
        return db.collection("users").limit(5).get();
    }

    public Task<Void> toggleLike(String postId, boolean newLikedState, String postAuthorId, UserModel likerUser) {
        String currentUserId = auth.getUid();
        if (currentUserId == null || postId == null || postId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("User ID or Post ID cannot be null for toggleLike."));
        }

        DocumentReference postRef = postsCollection.document(postId);
        Map<String, Object> postUpdates = new HashMap<>();
        postUpdates.put("likeCount", FieldValue.increment(newLikedState ? 1 : -1));
        postUpdates.put("likes." + currentUserId, newLikedState ? true : FieldValue.delete());

        if (newLikedState && postAuthorId != null && likerUser != null && !currentUserId.equals(postAuthorId)) {
            DocumentReference notificationRef = db.collection("users").document(postAuthorId)
                    .collection("notifications").document();
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "like");
            notificationData.put("fromUserId", currentUserId);
            notificationData.put("fromUsername", likerUser.getUsername() != null ? likerUser.getUsername() : "Someone");
            notificationData.put("fromUserAvatarUrl", likerUser.getProfileImageUrl() != null ? likerUser.getProfileImageUrl() : "");
            notificationData.put("postId", postId);
            notificationData.put("timestamp", FieldValue.serverTimestamp());
            notificationData.put("read", false);

            return db.runTransaction(transaction -> {
                transaction.update(postRef, postUpdates);
                transaction.set(notificationRef, notificationData);
                return null;
            });
        } else {
            return postRef.update(postUpdates);
        }
    }

    public Task<Void> toggleBookmark(String postId, boolean newBookmarkedState) {
        String userId = auth.getUid();
        if (userId == null || postId == null || postId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("User ID or Post ID cannot be null for toggleBookmark."));
        }
        DocumentReference postRef = postsCollection.document(postId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("bookmarkCount", FieldValue.increment(newBookmarkedState ? 1 : -1));
        updates.put("bookmarks." + userId, newBookmarkedState ? true : FieldValue.delete());
        return postRef.update(updates);
    }

    public Task<Void> toggleRepost(String postId, boolean newRepostedState) {
        String userId = auth.getUid();
        if (userId == null || postId == null || postId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("User ID or Post ID cannot be null for toggleRepost."));
        }
        DocumentReference postRef = postsCollection.document(postId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("repostCount", FieldValue.increment(newRepostedState ? 1 : -1));
        updates.put("reposts." + userId, newRepostedState ? true : FieldValue.delete());
        return postRef.update(updates);
    }

    public Task<Void> addOrUpdateReaction(String postId, String reactingUserId, String emoji, String postAuthorId, UserModel reactorDetails) {
        if (postId == null || reactingUserId == null) {
            Log.e(TAG, "Post ID or Reacting User ID is null for addOrUpdateReaction.");
            return Tasks.forException(new IllegalArgumentException("Post ID or Reacting User ID cannot be null."));
        }

        DocumentReference postRef = postsCollection.document(postId);
        Map<String, Object> updates = new HashMap<>();

        if (emoji == null || emoji.isEmpty()) {
            updates.put("reactions." + reactingUserId, FieldValue.delete());
        } else {
            updates.put("reactions." + reactingUserId, emoji);
        }

        if (postAuthorId != null && reactorDetails != null && !reactingUserId.equals(postAuthorId) && emoji != null && !emoji.isEmpty()) {
            DocumentReference notificationRef = db.collection("users").document(postAuthorId)
                    .collection("notifications").document();
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "reaction");
            notificationData.put("fromUserId", reactingUserId);
            notificationData.put("fromUsername", reactorDetails.getUsername() != null ? reactorDetails.getUsername() : "Someone");
            notificationData.put("fromUserAvatarUrl", reactorDetails.getProfileImageUrl() != null ? reactorDetails.getProfileImageUrl() : "");
            notificationData.put("postId", postId);
            notificationData.put("postContentPreview", "reacted with " + emoji);
            notificationData.put("timestamp", FieldValue.serverTimestamp());
            notificationData.put("read", false);

            return db.runTransaction(transaction -> {
                transaction.update(postRef, updates);
                transaction.set(notificationRef, notificationData);
                return null;
            });
        } else {
            return postRef.update(updates);
        }
    }

    public Task<Void> setPostPinnedStatus(String postIdToUpdate, String authorId, boolean newPinnedState) {
        if (postIdToUpdate == null || authorId == null) {
            return Tasks.forException(new IllegalArgumentException("Post ID or Author ID cannot be null for pinning."));
        }

        DocumentReference postToUpdateRef = postsCollection.document(postIdToUpdate);

        if (newPinnedState) {
            return postsCollection
                    .whereEqualTo("authorId", authorId)
                    .whereEqualTo("isPinned", true)
                    .get()
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Failed to query old pinned posts.", task.getException());
                            throw Objects.requireNonNull(task.getException());
                        }
                        List<DocumentSnapshot> oldPinnedDocs = task.getResult().getDocuments();
                        return db.runTransaction(innerTransaction -> {
                            for (DocumentSnapshot oldPinnedPostDoc : oldPinnedDocs) {
                                if (!oldPinnedPostDoc.getId().equals(postIdToUpdate)) {
                                    Log.d(TAG, "Unpinning old post: " + oldPinnedPostDoc.getId() + " inside transaction");
                                    innerTransaction.update(oldPinnedPostDoc.getReference(), "isPinned", false);
                                }
                            }
                            Log.d(TAG, "Pinning new post: " + postIdToUpdate + " inside transaction");
                            innerTransaction.update(postToUpdateRef, "isPinned", true);
                            return null;
                        });
                    });
        } else {
            Log.d(TAG, "Unpinning post: " + postIdToUpdate);
            return postToUpdateRef.update("isPinned", false);
        }
    }

    public Task<Void> deletePost(String postId) {
        if (postId == null || postId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Post ID cannot be null or empty for deletePost."));
        }
        return postsCollection.document(postId).delete();
    }

    public Task<Void> updatePostContent(String postId, String newContent) {
        if (postId == null || postId.isEmpty() || newContent == null) {
            return Tasks.forException(new IllegalArgumentException("Post ID or new content cannot be null."));
        }
        return postsCollection.document(postId).update("content", newContent, "updatedAt", FieldValue.serverTimestamp());
    }

    public Task<Void> updatePostPrivacy(String postId, String newPrivacyLevel) {
        if (postId == null || postId.isEmpty() || newPrivacyLevel == null) {
            return Tasks.forException(new IllegalArgumentException("Post ID or new privacy level cannot be null."));
        }
        // ستحتاج لإضافة حقل "privacyLevel" إلى PostModel.java
        return postsCollection.document(postId).update("privacyLevel", newPrivacyLevel, "updatedAt", FieldValue.serverTimestamp());
    }

    public Task<DocumentReference> submitReport(Map<String, Object> reportData) {
        if (reportData == null || reportData.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Report data cannot be null or empty."));
        }
        return db.collection("reports").add(reportData);
    }
}