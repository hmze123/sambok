package com.spidroid.starry.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.repositories.PostRepository;
import java.util.ArrayList;
import java.util.List;

public class PostViewModel extends ViewModel {

    private final PostRepository postRepository = new PostRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<PostModel>> posts = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<UserModel>> suggestions = new MutableLiveData<>();
    private final MediatorLiveData<List<Object>> combinedFeed = new MediatorLiveData<>();

    public PostViewModel() {
        combinedFeed.addSource(posts, postsList -> combineData(postsList, suggestions.getValue()));
        combinedFeed.addSource(suggestions, users -> combineData(posts.getValue(), users));
    }

    public LiveData<List<Object>> getCombinedFeed() {
        return combinedFeed;
    }

    public LiveData<String> getErrorLiveData() {
        return error;
    }

    public void fetchPosts(int limit) {
        postRepository.getPosts(limit).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<PostModel> postList = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult()) {

                    // *** خطوة التشخيص 1: طباعة البيانات الخام ***
                    Log.d("PostViewModel_Debug", "Firestore Document Data: " + doc.getData());

                    PostModel post = doc.toObject(PostModel.class);
                    if (post != null) {

                        // *** خطوة التشخيص 2: طباعة الكائن بعد تحويله ***
                        Log.d("PostViewModel_Debug", "Converted Post Object, Content: " + post.getContent());

                        // *** خطوة الإصلاح: تجاهل المنشورات الفارغة ***
                        if (post.getContent() != null && !post.getContent().isEmpty()) {
                            post.setPostId(doc.getId());
                            updateUserInteractions(post);
                            postList.add(post);
                        } else {
                            Log.w("PostViewModel_Debug", "Skipping post with null or empty content. ID: " + doc.getId());
                        }
                    }
                }
                posts.postValue(postList);
            } else {
                error.postValue("Failed to load posts: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    public void fetchUserSuggestions() {
        postRepository.getUserSuggestions().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                suggestions.postValue(task.getResult().toObjects(UserModel.class));
            }
        });
    }

    // ... (باقي دوال الـ ViewModel كما هي)
    public void toggleLike(String postId, boolean isLiked) { /* ... */ }
    public void toggleBookmark(String postId, boolean isBookmarked) { /* ... */ }
    public void toggleRepost(String postId, boolean isReposted) { /* ... */ }
    public void deletePost(String postId) { /* ... */ }
    private void combineData(List<PostModel> postsList, List<UserModel> users) { /* ... */ }
    private void updateUserInteractions(PostModel post) { /* ... */ }
}