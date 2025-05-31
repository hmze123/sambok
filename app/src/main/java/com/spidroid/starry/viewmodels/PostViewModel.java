package com.spidroid.starry.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.repositories.PostRepository;
import com.spidroid.starry.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostViewModel extends ViewModel {

    private static final String TAG = "PostViewModel"; // ★ إضافة TAG للـ Log
    private final PostRepository postRepository = new PostRepository();
    private final UserRepository userRepository = new UserRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<PostModel>> posts = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<UserModel>> suggestions = new MutableLiveData<>();
    private final MediatorLiveData<List<Object>> combinedFeed = new MediatorLiveData<>();

    private final MutableLiveData<UserModel> currentUserData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public PostViewModel() {
        combinedFeed.addSource(posts, postsList -> combineData(postsList, suggestions.getValue()));
        combinedFeed.addSource(suggestions, users -> combineData(posts.getValue(), users));
        fetchCurrentUser();
    }

    private void fetchCurrentUser() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            // استخدام LiveData من UserRepository مباشرة
            userRepository.getUserById(firebaseUser.getUid()).observeForever(userModel -> {
                if (userModel != null) {
                    currentUserData.postValue(userModel);
                    Log.d(TAG, "Current user data loaded: " + userModel.getUsername());
                } else {
                    error.postValue("Failed to load current user data from repository.");
                    Log.e(TAG, "Current user data is null from repository for UID: " + firebaseUser.getUid());
                }
            });
        } else {
            error.postValue("User not authenticated.");
            Log.e(TAG, "FirebaseUser is null, cannot fetch current user data.");
        }
    }


    public LiveData<List<Object>> getCombinedFeed() {
        return combinedFeed;
    }

    public LiveData<String> getErrorLiveData() {
        return error;
    }

    public LiveData<UserModel> getCurrentUserLiveData() { // ★ إضافة Getter لـ currentUserData
        return currentUserData;
    }

    public void fetchPosts(int limit) {
        postRepository.getPosts(limit).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<PostModel> postList = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult()) {
                    Log.d("PostViewModel_Debug", "Firestore Document Data: " + doc.getData());
                    PostModel post = doc.toObject(PostModel.class);
                    if (post != null) {
                        Log.d("PostViewModel_Debug", "Converted Post Object, Content: " + post.getContent() + ", PostId: " + doc.getId());
                        if (post.getContent() != null && !post.getContent().isEmpty()) {
                            post.setPostId(doc.getId()); // ★ التأكد من تعيين postId
                            updateUserInteractions(post);
                            postList.add(post);
                        } else {
                            Log.w("PostViewModel_Debug", "Skipping post with null or empty content. ID: " + doc.getId());
                        }
                    } else {
                        Log.w("PostViewModel_Debug", "Skipping null PostModel object. Document ID: " + doc.getId());
                    }
                }
                posts.postValue(postList);
            } else {
                error.postValue("Failed to load posts: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                Log.e("PostViewModel_Debug", "Failed to fetch posts", task.getException());
            }
        });
    }

    public void fetchUserSuggestions() {
        postRepository.getUserSuggestions().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                suggestions.postValue(task.getResult().toObjects(UserModel.class));
            } else {
                Log.e("PostViewModel_Debug", "Failed to fetch user suggestions", task.getException());
            }
        });
    }

    public void toggleLike(PostModel postToUpdate, boolean isLiked) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        UserModel liker = currentUserData.getValue();

        if (firebaseUser == null) {
            error.postValue("User not authenticated to like posts.");
            Log.e(TAG, "User not authenticated for toggleLike.");
            return;
        }
        if (liker == null) {
            error.postValue("Current user data not loaded yet for like. Please try again.");
            Log.e(TAG, "Liker (current user) data is null in toggleLike.");
            fetchCurrentUser(); // محاولة جلب المستخدم مرة أخرى
            return;
        }
        if (postToUpdate == null || postToUpdate.getPostId() == null || postToUpdate.getAuthorId() == null) {
            error.postValue("Invalid post data for liking.");
            Log.e(TAG, "Post data is invalid for toggleLike. PostID: " + (postToUpdate != null ? postToUpdate.getPostId() : "null") + ", AuthorID: " + (postToUpdate != null ? postToUpdate.getAuthorId() : "null"));
            return;
        }

        Task<Void> likeTask = postRepository.toggleLike(postToUpdate.getPostId(), isLiked, postToUpdate.getAuthorId(), liker);

        if (likeTask != null) {
            likeTask.addOnFailureListener(e -> {
                error.postValue("Failed to toggle like: " + e.getMessage());
                Log.e(TAG, "Failed to toggle like in repository for post " + postToUpdate.getPostId(), e);
            });
        } else {
            Log.e(TAG, "PostRepository.toggleLike returned null task for post " + postToUpdate.getPostId());
        }
    }


    public void toggleBookmark(String postId, boolean isBookmarked) {
        if (auth.getUid() == null) {
            error.postValue("User not authenticated to bookmark posts.");
            return;
        }
        Task<Void> bookmarkTask = postRepository.toggleBookmark(postId, isBookmarked);
        if (bookmarkTask != null) {
            bookmarkTask.addOnFailureListener(e -> error.postValue("Failed to toggle bookmark: " + e.getMessage()));
        }
    }

    public void toggleRepost(String postId, boolean isReposted) {
        if (auth.getUid() == null) {
            error.postValue("User not authenticated to repost.");
            return;
        }
        Task<Void> repostTask = postRepository.toggleRepost(postId, isReposted);
        if (repostTask != null) {
            repostTask.addOnFailureListener(e -> error.postValue("Failed to toggle repost: " + e.getMessage()));
        }
    }

    public void deletePost(String postId) {
        postRepository.deletePost(postId)
                .addOnSuccessListener(aVoid -> {
                    fetchPosts(15); // Or remove locally for faster UI update
                })
                .addOnFailureListener(e -> error.postValue("Failed to delete post: " + e.getMessage()));
    }

    // ★★★ تحديث دالة handleEmojiSelection لاستدعاء PostRepository ★★★
    public void handleEmojiSelection(PostModel post, String emoji) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        UserModel reactor = currentUserData.getValue(); // المستخدم الذي يقوم بالريأكشن

        if (firebaseUser == null) {
            error.postValue("User not authenticated to react to posts.");
            Log.e(TAG, "User not authenticated for handleEmojiSelection.");
            return;
        }

        if (reactor == null) {
            error.postValue("Current user data not loaded yet for reaction. Please try again.");
            Log.e(TAG, "Reactor (current user) data is null in handleEmojiSelection.");
            fetchCurrentUser(); // محاولة جلب المستخدم مرة أخرى
            return;
        }

        if (post == null || post.getPostId() == null || post.getAuthorId() == null) {
            error.postValue("Invalid post data for reaction.");
            Log.e(TAG, "Post data is invalid for handleEmojiSelection. PostID: " +
                    (post != null ? post.getPostId() : "null") + ", AuthorID: " +
                    (post != null ? post.getAuthorId() : "null"));
            return;
        }

        Log.d(TAG, "Handling emoji '" + emoji + "' for post: " + post.getPostId() + " by user: " + reactor.getUserId());

        // استدعاء الدالة في PostRepository
        Task<Void> reactionTask = postRepository.addOrUpdateReaction(post.getPostId(), reactor.getUserId(), emoji, post.getAuthorId(), reactor);

        if (reactionTask != null) {
            reactionTask
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reaction '" + emoji + "' successfully processed for post " + post.getPostId());
                        // يمكنك هنا تحديث الـ LiveData الخاص بالمنشورات إذا أردت أن يظهر التغيير فورًا
                        // أو الاعتماد على SnapshotListener إذا كان لديك واحد للمنشورات الفردية
                        // أو إعادة تحميل المنشورات (أقل كفاءة)
                        // fetchPosts(15); // مثال لإعادة التحميل
                    })
                    .addOnFailureListener(e -> {
                        error.postValue("Failed to add/update reaction: " + e.getMessage());
                        Log.e(TAG, "Failed to add/update reaction for post " + post.getPostId(), e);
                    });
        } else {
            Log.e(TAG, "PostRepository.addOrUpdateReaction returned null task for post " + post.getPostId());
        }
    }


    private void combineData(List<PostModel> postsList, List<UserModel> users) {
        List<Object> combinedList = new ArrayList<>();
        if (postsList != null) {
            combinedList.addAll(postsList);
        }
        if (users != null && !users.isEmpty()) {
            if (combinedList.size() >= 3) {
                combinedList.add(3, users.get(0));
            } else if (!combinedList.isEmpty()) {
                combinedList.add(users.get(0));
            }
            // يمكنك إضافة المزيد من الاقتراحات في مواضع أخرى إذا أردت
        }
        combinedFeed.postValue(combinedList);
    }

    private void updateUserInteractions(PostModel post) {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            post.setLiked(false);
            post.setBookmarked(false);
            post.setReposted(false);
            return;
        }

        // تحديث حالة الإعجاب
        if (post.getLikes() != null) {
            post.setLiked(post.getLikes().containsKey(currentUserId));
        } else {
            post.setLiked(false);
        }

        // تحديث حالة الحفظ
        if (post.getBookmarks() != null) {
            post.setBookmarked(post.getBookmarks().containsKey(currentUserId));
        } else {
            post.setBookmarked(false);
        }

        // تحديث حالة إعادة النشر
        if (post.getReposts() != null) {
            post.setReposted(post.getReposts().containsKey(currentUserId));
        } else {
            post.setReposted(false);
        }
        // ملاحظة: تحديث الريأكشنات الخاصة بالمستخدم الحالي على المنشور يمكن أن يتم هنا أيضًا
        // إذا كنت تريد عرض الريأكشن الحالي للمستخدم بشكل مختلف (مثلاً، تلوين الأيقونة)
        // String userReaction = post.getUserReaction(currentUserId);
        // if (userReaction != null) { /* ... تحديث واجهة المستخدم بناءً على الريأكشن ... */ }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        // إزالة المراقبين لتجنب تسرب الذاكرة
        // تأكد من أنك تزيل نفس الـ observer الذي تم تسجيله
        // الطريقة التالية هي طريقة عامة لإزالة المراقب إذا لم تحتفظ بمرجع له
        if (currentUserData.hasObservers()) {
            // لا توجد طريقة مباشرة لإزالة كل المراقبين بدون مرجع إليهم
            // لكن في ViewModel، سيتم مسح المراقبين تلقائيًا عند onCleared إذا كانوا مرتبطين بدورة حياة LifecycleOwner
            // الـ observeForever هو الذي يحتاج إلى إزالة يدوية
            Log.d(TAG, "ViewModel cleared. Remember to remove any 'observeForever' observers if used directly.");
        }
        // إذا كنت تستخدم userRepository.getUserById(firebaseUser.getUid()).observeForever(...)
        // فيجب أن يكون لديك طريقة لإلغاء تسجيل هذا المراقب بالتحديد
        // على سبيل المثال، إذا كان لديك مرجع للمستخدم الحالي:
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            // userRepository.getUserById(firebaseUser.getUid()).removeObserver(theObserverInstance);
            // هذا يتطلب الاحتفاظ بمرجع لـ theObserverInstance
        }
    }
}