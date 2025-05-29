package com.spidroid.starry.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostViewModel extends ViewModel {
  private final FirebaseFirestore db = FirebaseFirestore.getInstance();
  private final FirebaseAuth auth = FirebaseAuth.getInstance();

  private final MutableLiveData<List<PostModel>> posts = new MutableLiveData<>();
  private final MutableLiveData<String> error = new MutableLiveData<>();
  private final MutableLiveData<List<UserModel>> suggestions = new MutableLiveData<>();
  private final MediatorLiveData<List<Object>> combinedFeed = new MediatorLiveData<>();

  public LiveData<List<Object>> getCombinedFeed() {
    return combinedFeed;
  }

  public PostViewModel() {
    combinedFeed.addSource(posts, posts -> combineData(posts, suggestions.getValue()));
    combinedFeed.addSource(suggestions, users -> combineData(posts.getValue(), users));
  }

  private void combineData(List<PostModel> posts, List<UserModel> users) {
    List<Object> merged = new ArrayList<>();
    int userIndex = 0;
    if (posts != null) {
      for (int i = 0; i < posts.size(); i++) {
        merged.add(posts.get(i));
        if (i % 5 == 4 && users != null && userIndex < users.size()) {
          merged.add(users.get(userIndex++));
        }
      }
    }
    combinedFeed.postValue(merged);
  }

  public LiveData<String> getErrorLiveData() {
    return error;
  }

  public LiveData<List<PostModel>> getPosts() {
    return posts;
  }

  public void fetchPosts(int limit) {
    db.collection("posts")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit)
        .get()
        .addOnCompleteListener(
            task -> {
              if (task.isSuccessful()) {
                List<PostModel> postList = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult()) {
                  PostModel post = doc.toObject(PostModel.class);
                  if (post != null) {
                    post.setPostId(doc.getId());
                    updateUserInteractions(post);
                    postList.add(post);
                  }
                }
                posts.postValue(postList); // Maintain separate posts list
                combineData(postList, suggestions.getValue());
              } else {
                error.postValue("Failed to load posts: " + task.getException());
              }
            });
  }

  public void fetchUserSuggestions() {
    db.collection("users")
        .limit(5)
        .get()
        .addOnCompleteListener(
            task -> {
              if (task.isSuccessful()) {
                List<UserModel> users = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult()) {
                  UserModel user = doc.toObject(UserModel.class);
                  if (user != null) users.add(user);
                }
                suggestions.postValue(users);
              }
            });
  }

  private void updateUserInteractions(PostModel post) {
    String userId = auth.getUid();
    if (userId == null) return;

    post.setLiked(post.getLikes() != null && post.getLikes().containsKey(userId));
    post.setBookmarked(post.getBookmarks() != null && post.getBookmarks().containsKey(userId));
    post.setReposted(post.getReposts() != null && post.getReposts().containsKey(userId));
  }

  public void toggleLike(String postId, boolean isLiked) {
    String userId = auth.getUid();
    if (userId == null) {
      error.postValue("You must be logged in to like posts");
      return;
    }

    DocumentReference postRef = db.collection("posts").document(postId);
    db.runTransaction(
            transaction -> {
              DocumentSnapshot snapshot = transaction.get(postRef);
              long newCount = snapshot.getLong("likeCount") + (isLiked ? 1 : -1);

              // Update like count
              transaction.update(postRef, "likeCount", newCount);

              // Update likes map
              Map<String, Boolean> likes = (Map<String, Boolean>) snapshot.get("likes");
              if (isLiked) {
                likes.put(userId, true);
              } else {
                likes.remove(userId);
              }
              transaction.update(postRef, "likes", likes);

              return null;
            })
        .addOnFailureListener(e -> error.postValue("Like failed: " + e.getMessage()));
  }

  public void toggleBookmark(String postId, boolean isBookmarked) {
    String userId = auth.getUid();
    if (userId == null) {
      error.postValue("You must be logged in to bookmark posts");
      return;
    }

    DocumentReference postRef = db.collection("posts").document(postId);
    db.runTransaction(
            transaction -> {
              DocumentSnapshot snapshot = transaction.get(postRef);
              long newCount = snapshot.getLong("bookmarkCount") + (isBookmarked ? 1 : -1);

              transaction.update(postRef, "bookmarkCount", newCount);

              Map<String, Boolean> bookmarks = (Map<String, Boolean>) snapshot.get("bookmarks");
              if (isBookmarked) {
                bookmarks.put(userId, true);
              } else {
                bookmarks.remove(userId);
              }
              transaction.update(postRef, "bookmarks", bookmarks);

              return null;
            })
        .addOnFailureListener(e -> error.postValue("Bookmark failed: " + e.getMessage()));
  }

  public void toggleRepost(String postId, boolean isReposted) {
    String userId = auth.getUid();
    if (userId == null) {
      error.postValue("You must be logged in to repost");
      return;
    }

    DocumentReference postRef = db.collection("posts").document(postId);
    db.runTransaction(
            transaction -> {
              DocumentSnapshot snapshot = transaction.get(postRef);
              long newCount = snapshot.getLong("repostCount") + (isReposted ? 1 : -1);

              transaction.update(postRef, "repostCount", newCount);

              Map<String, Boolean> reposts = (Map<String, Boolean>) snapshot.get("reposts");
              if (isReposted) {
                reposts.put(userId, true);
              } else {
                reposts.remove(userId);
              }
              transaction.update(postRef, "reposts", reposts);

              return null;
            })
        .addOnFailureListener(e -> error.postValue("Repost failed: " + e.getMessage()));
  }

  public void reportPost(String postId) {
    // Implement your reporting logic
    db.collection("reports")
        .document(postId)
        .set(
            new HashMap<String, Object>() {
              {
                put("reportedAt", FieldValue.serverTimestamp());
                put("postId", postId);
              }
            })
        .addOnSuccessListener(aVoid -> error.postValue("Post reported successfully"))
        .addOnFailureListener(e -> error.postValue("Report failed: " + e.getMessage()));
  }

  public void deletePost(String postId) {
    db.collection("posts")
        .document(postId)
        .delete()
        .addOnSuccessListener(
            aVoid -> {
              // Remove from local list
              List<PostModel> currentPosts = posts.getValue();
              if (currentPosts != null) {
                currentPosts.removeIf(post -> post.getPostId().equals(postId));
                posts.postValue(currentPosts);
              }
            })
        .addOnFailureListener(e -> error.postValue("Delete failed: " + e.getMessage()));
  }

  public void resetErrorState() {
    error.postValue(null);
  }
}
