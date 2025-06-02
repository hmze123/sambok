package com.spidroid.starry.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference; // ★ إضافة إذا لم يكن موجودًا
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.repositories.PostRepository;
import com.spidroid.starry.repositories.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostViewModel extends ViewModel {

    private static final String TAG = "PostViewModel";
    private final PostRepository postRepository = new PostRepository();
    private final UserRepository userRepository = new UserRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<PostModel>> posts = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<UserModel>> suggestions = new MutableLiveData<>();
    private final MediatorLiveData<List<Object>> combinedFeed = new MediatorLiveData<>();
    private final MutableLiveData<UserModel> currentUserData = new MutableLiveData<>();

    private final MutableLiveData<String> _postUpdatedEvent = new MutableLiveData<>();
    public LiveData<String> getPostUpdatedEvent() { return _postUpdatedEvent; }

    private final MutableLiveData<String> _postInteractionErrorEvent = new MutableLiveData<>();
    public LiveData<String> getPostInteractionErrorEvent() { return _postInteractionErrorEvent; }


    public PostViewModel() {
        combinedFeed.addSource(posts, postsList -> combineData(postsList, suggestions.getValue()));
        combinedFeed.addSource(suggestions, users -> combineData(posts.getValue(), users));
        fetchCurrentUser();
    }

    private void fetchCurrentUser() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            userRepository.getUserById(firebaseUser.getUid()).observeForever(userModel -> {
                if (userModel != null) {
                    currentUserData.postValue(userModel);
                    Log.d(TAG, "Current user data loaded: " + userModel.getUsername());
                } else {
                    _postInteractionErrorEvent.postValue("Failed to load current user data from repository.");
                    Log.e(TAG, "Current user data is null from repository for UID: " + firebaseUser.getUid());
                }
            });
        } else {
            _postInteractionErrorEvent.postValue("User not authenticated.");
            Log.e(TAG, "FirebaseUser is null, cannot fetch current user data.");
        }
    }

    public LiveData<List<Object>> getCombinedFeed() { return combinedFeed; }
    public LiveData<String> getErrorLiveData() { return error; } // يستخدم للأخطاء العامة لجلب الخلاصات
    public LiveData<UserModel> getCurrentUserLiveData() { return currentUserData; }

    public void fetchPosts(int limit) {
        Log.d(TAG, "Attempting to fetch posts for main feed. Limit: " + limit);
        postRepository.getPosts(limit).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<PostModel> postList = new ArrayList<>();
                Log.d(TAG, "Fetched " + task.getResult().size() + " documents from Firestore for main feed.");
                for (DocumentSnapshot doc : task.getResult()) {
                    PostModel post = doc.toObject(PostModel.class);
                    if (post != null) {
                        post.setPostId(doc.getId());
                        updateUserInteractions(post);
                        postList.add(post);
                        Log.d(TAG, "Added post to list: " + post.getPostId() + " with content (first 20 chars): " + (post.getContent() != null && post.getContent().length() > 20 ? post.getContent().substring(0, 20) : post.getContent()));
                    } else {
                        Log.w(TAG, "Document " + doc.getId() + " converted to null PostModel for main feed.");
                    }
                }
                posts.postValue(postList);
                Log.d(TAG, "posts LiveData updated with " + postList.size() + " posts for main feed.");
                if (postList.isEmpty()) {
                    Log.w(TAG, "Post list IS EMPTY after successful fetch for main feed.");
                    error.postValue("No posts found."); // إرسال رسالة إذا كانت القائمة فارغة
                }
            } else {
                String errorMsg = "Failed to load posts for main feed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error");
                error.postValue(errorMsg);
                Log.e(TAG, errorMsg, task.getException());
                posts.postValue(new ArrayList<>());
            }
        });
    }

    public void fetchUserSuggestions() {
        postRepository.getUserSuggestions().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                suggestions.postValue(task.getResult().toObjects(UserModel.class));
            } else {
                Log.e(TAG, "Failed to fetch user suggestions", task.getException());
            }
        });
    }

    public void toggleLike(PostModel postToUpdate, boolean newLikedState) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        UserModel liker = currentUserData.getValue();
        if (firebaseUser == null || liker == null || postToUpdate == null || postToUpdate.getPostId() == null || postToUpdate.getAuthorId() == null) {
            handleInteractionError("toggleLike", "Authentication or data error.");
            return;
        }
        postRepository.toggleLike(postToUpdate.getPostId(), newLikedState, postToUpdate.getAuthorId(), liker)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Like status updated in Firestore for " + postToUpdate.getPostId());
                    updateLocalPostInteraction(postToUpdate.getPostId(), "like", newLikedState, newLikedState ? 1 : -1);
                    _postUpdatedEvent.postValue(postToUpdate.getPostId());
                })
                .addOnFailureListener(e -> handleInteractionError("toggleLike", e.getMessage()));
    }

    public void toggleBookmark(String postId, boolean newBookmarkedState) {
        if (auth.getUid() == null || postId == null) {
            handleInteractionError("toggleBookmark", "Authentication or Post ID error.");
            return;
        }
        postRepository.toggleBookmark(postId, newBookmarkedState)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bookmark status updated in Firestore for " + postId);
                    updateLocalPostInteraction(postId, "bookmark", newBookmarkedState, newBookmarkedState ? 1 : -1);
                    _postUpdatedEvent.postValue(postId);
                })
                .addOnFailureListener(e -> handleInteractionError("toggleBookmark", e.getMessage()));
    }

    public void toggleRepost(String postId, boolean newRepostedState) {
        if (auth.getUid() == null || postId == null) {
            handleInteractionError("toggleRepost", "Authentication or Post ID error.");
            return;
        }
        postRepository.toggleRepost(postId, newRepostedState)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Repost status updated in Firestore for " + postId);
                    updateLocalPostInteraction(postId, "repost", newRepostedState, newRepostedState ? 1 : -1);
                    _postUpdatedEvent.postValue(postId);
                })
                .addOnFailureListener(e -> handleInteractionError("toggleRepost", e.getMessage()));
    }

    public void deletePost(String postId) {
        if (postId == null || postId.isEmpty()) {
            handleInteractionError("deletePost", "Post ID cannot be null or empty.");
            return;
        }
        postRepository.deletePost(postId)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post " + postId + " deleted successfully from Firestore.");
                    List<PostModel> currentPostsList = posts.getValue();
                    if (currentPostsList != null) {
                        boolean removed = currentPostsList.removeIf(p -> p.getPostId().equals(postId));
                        if (removed) {
                            posts.postValue(new ArrayList<>(currentPostsList));
                            _postUpdatedEvent.postValue(postId); // إرسال حدث أن المنشور تم حذفه
                        }
                    }
                })
                .addOnFailureListener(e -> handleInteractionError("deletePost", "Failed to delete post: " + e.getMessage()));
    }

    public void handleEmojiSelection(PostModel post, String emoji) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        UserModel reactor = currentUserData.getValue();
        if (firebaseUser == null || reactor == null || post == null || post.getPostId() == null || post.getAuthorId() == null) {
            handleInteractionError("handleEmojiSelection", "Authentication or data error.");
            return;
        }
        postRepository.addOrUpdateReaction(post.getPostId(), reactor.getUserId(), emoji, post.getAuthorId(), reactor)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reaction '" + emoji + "' successfully processed for post " + post.getPostId());
                    _postUpdatedEvent.postValue(post.getPostId());
                })
                .addOnFailureListener(e -> handleInteractionError("handleEmojiSelection", "Failed to add/update reaction: " + e.getMessage()));
    }

    public void togglePostPinStatus(PostModel postToToggle) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || postToToggle == null || postToToggle.getPostId() == null || postToToggle.getAuthorId() == null) {
            handleInteractionError("togglePostPinStatus", "Authentication or data error.");
            return;
        }
        if (!firebaseUser.getUid().equals(postToToggle.getAuthorId())) {
            handleInteractionError("togglePostPinStatus", "User not authorized to pin this post.");
            return;
        }
        boolean newPinnedState = !postToToggle.isPinned();
        postRepository.setPostPinnedStatus(postToToggle.getPostId(), postToToggle.getAuthorId(), newPinnedState)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Pin status updated in Firestore for " + postToToggle.getPostId() + " to " + newPinnedState);
                    _postUpdatedEvent.postValue(postToToggle.getPostId()); // للإشارة إلى أن القائمة يجب أن تُعاد تحميلها في ProfilePostsFragment
                })
                .addOnFailureListener(e -> handleInteractionError("togglePostPinStatus", e.getMessage()));
    }

    public void editPostContent(String postId, String newContent) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || postId == null || newContent == null) {
            handleInteractionError("editPostContent", "Authentication, Post ID, or content error.");
            return;
        }
        // يمكنك إضافة تحقق من ملكية المنشور هنا إذا أردت
        postRepository.updatePostContent(postId, newContent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post " + postId + " content updated.");
                    _postUpdatedEvent.postValue(postId);
                })
                .addOnFailureListener(e -> handleInteractionError("editPostContent", e.getMessage()));
    }

    public void setPostPrivacy(String postId, String newPrivacyLevel) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || postId == null || newPrivacyLevel == null) {
            handleInteractionError("setPostPrivacy", "Authentication, Post ID, or privacy level error.");
            return;
        }
        // يمكنك إضافة تحقق من ملكية المنشور هنا
        postRepository.updatePostPrivacy(postId, newPrivacyLevel)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Post " + postId + " privacy updated to " + newPrivacyLevel);
                    _postUpdatedEvent.postValue(postId);
                })
                .addOnFailureListener(e -> handleInteractionError("setPostPrivacy", e.getMessage()));
    }

    public void submitReportForPost(String postId, String reason, String reportedAuthorId) { // ★ إضافة reportedAuthorId
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || postId == null || reason == null || reason.trim().isEmpty() || reportedAuthorId == null) {
            handleInteractionError("submitReportForPost", "Authentication, Post ID, reason, or author ID error.");
            return;
        }
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reportedPostId", postId);
        reportData.put("reportedAuthorId", reportedAuthorId); // ★ إضافة معرّف صاحب المنشور
        reportData.put("reportingUserId", firebaseUser.getUid());
        reportData.put("reason", reason);
        reportData.put("timestamp", FieldValue.serverTimestamp());
        reportData.put("status", "pending");

        postRepository.submitReport(reportData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Report submitted for post " + postId + " with ID: " + documentReference.getId());
                    // يمكنك إرسال LiveData event هنا إذا أردت إظهار رسالة نجاح في الواجهة
                    // _reportSubmittedEvent.postValue(true);
                })
                .addOnFailureListener(e -> handleInteractionError("submitReportForPost", e.getMessage()));
    }

    private void updateLocalPostInteraction(String postId, String interactionType, boolean newState, long countChange) {
        List<PostModel> currentPostsList = posts.getValue();
        if (currentPostsList == null) return;

        boolean listChanged = false;
        List<PostModel> tempList = new ArrayList<>(currentPostsList); // اعمل على نسخة لتجنب ConcurrentModificationException

        for (PostModel p : tempList) {
            if (p.getPostId().equals(postId)) {
                switch (interactionType) {
                    case "like":
                        if (p.isLiked() != newState) {
                            p.setLiked(newState);
                            p.setLikeCount(p.getLikeCount() + countChange);
                            if (p.getLikeCount() < 0) p.setLikeCount(0);
                            listChanged = true;
                        }
                        break;
                    case "bookmark":
                        if (p.isBookmarked() != newState) {
                            p.setBookmarked(newState);
                            p.setBookmarkCount(p.getBookmarkCount() + countChange);
                            if (p.getBookmarkCount() < 0) p.setBookmarkCount(0);
                            listChanged = true;
                        }
                        break;
                    case "repost":
                        if (p.isReposted() != newState) {
                            p.setReposted(newState);
                            p.setRepostCount(p.getRepostCount() + countChange);
                            if (p.getRepostCount() < 0) p.setRepostCount(0);
                            listChanged = true;
                        }
                        break;
                }
                break;
            }
        }
        if (listChanged) {
            posts.postValue(tempList);
        }
    }

    private void combineData(List<PostModel> postsList, List<UserModel> users) {
        List<Object> combinedList = new ArrayList<>();
        if (postsList != null) {
            combinedList.addAll(postsList);
        }
        // ... (منطق دمج اقتراحات المستخدمين إذا لزم الأمر) ...
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
        if (post.getLikes() != null) {
            post.setLiked(post.getLikes().containsKey(currentUserId));
        } else {
            post.setLiked(false);
        }
        if (post.getBookmarks() != null) {
            post.setBookmarked(post.getBookmarks().containsKey(currentUserId));
        } else {
            post.setBookmarked(false);
        }
        if (post.getReposts() != null) {
            post.setReposted(post.getReposts().containsKey(currentUserId));
        } else {
            post.setReposted(false);
        }
    }

    private void handleInteractionError(String operation, String errorMessage) {
        String fullError = "Failed to " + operation + ": " + errorMessage;
        _postInteractionErrorEvent.postValue(fullError); // استخدام LiveData جديد لأخطاء التفاعل
        Log.e(TAG, fullError);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // ... (منطق إزالة المراقبين إذا لزم الأمر)
    }
}