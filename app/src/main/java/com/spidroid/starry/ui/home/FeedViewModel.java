package com.spidroid.starry.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.models.PostModel;
import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends ViewModel {
  private final MutableLiveData<List<PostModel>> posts = new MutableLiveData<>();
  private final MutableLiveData<String> errors = new MutableLiveData<>();
  private boolean hasLoaded = false;

  public boolean hasPosts() {
    return posts.getValue() != null && !posts.getValue().isEmpty();
  }

  public void setPosts(List<PostModel> newPosts) {
    hasLoaded = true;
    posts.setValue(newPosts);
  }

  public void appendPosts(List<PostModel> newPosts) {
    List<PostModel> current =
        posts.getValue() != null ? new ArrayList<>(posts.getValue()) : new ArrayList<>();
    current.addAll(newPosts);
    posts.postValue(current);
  }

  public void toggleLike(String postId) {
    // Implement like toggle logic
  }

  public void toggleRepost(String postId) {
    // Implement repost toggle logic
  }

  public void toggleBookmark(String postId) {
    // Implement bookmark toggle logic
  }

  public void reportPost(String postId) {
    // Implement report logic
  }

  public void setError(String error) {
    errors.postValue(error);
  }

  // Getters
  public LiveData<List<PostModel>> getPosts() {
    return posts;
  }

  public LiveData<String> getErrors() {
    return errors;
  }

  public void deletePost(String postId) {
    FirebaseFirestore.getInstance()
        .collection("posts")
        .document(postId)
        .delete()
        .addOnSuccessListener(
            aVoid -> {
              List<PostModel> current = posts.getValue();
              if (current != null) {
                current.removeIf(post -> post.getPostId().equals(postId));
                posts.postValue(current);
              }
            })
        .addOnFailureListener(e -> errors.postValue("Failed to delete post: " + e.getMessage()));
  }

  public void resetState() {
    hasLoaded = false;
    posts.setValue(null);
  }
}
