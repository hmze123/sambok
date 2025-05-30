package com.spidroid.starry.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel; // قد تحتاجها لجلب معلومات المستخدم
import com.spidroid.starry.repositories.UserRepository; // قد تحتاجها لجلب معلومات المستخدم
import android.util.Log; // لاستخدام Log

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors; // لاستخدام stream().collect()

public class StoryViewModel extends ViewModel {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private MutableLiveData<List<StoryModel>> _stories = new MutableLiveData<>();
    public LiveData<List<StoryModel>> getStories() {
        return _stories;
    }

    // LiveData لقصة المستخدم الحالي
    private MutableLiveData<StoryModel> _yourStory = new MutableLiveData<>();
    public LiveData<StoryModel> getYourStory() {
        return _yourStory;
    }

    // LiveData للمستخدم الحالي
    private MutableLiveData<UserModel> _currentUser = new MutableLiveData<>();
    public LiveData<UserModel> getCurrentUser() {
        return _currentUser;
    }

    private UserRepository userRepository; // لإدارة المستخدمين

    public StoryViewModel() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository(); // تهيئة UserRepository
    }

    public void fetchStoriesForCurrentUserAndFollowing() {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            _stories.setValue(new ArrayList<>());
            return;
        }

        // ابدأ بجلب المستخدمين الذين يتابعهم المستخدم الحالي
        db.collection("users").document(currentUserId)
                .collection("following")
                .get()
                .addOnSuccessListener(followingSnapshot -> {
                    List<String> followingIds = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : followingSnapshot.getDocuments()) {
                        followingIds.add(doc.getId());
                    }
                    followingIds.add(currentUserId); // أضف المستخدم الحالي أيضًا لجلب قصصه

                    if (followingIds.isEmpty()) {
                        _stories.setValue(new ArrayList<>());
                        return;
                    }

                    // الآن، جلب القصص من هؤلاء المستخدمين
                    db.collection("stories")
                            .whereIn("authorId", followingIds) // جلب القصص من المستخدمين الذين يتابعهم
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(storySnapshot -> {
                                List<StoryModel> fetchedStories = new ArrayList<>();
                                for (com.google.firebase.firestore.DocumentSnapshot doc : storySnapshot.getDocuments()) {
                                    StoryModel story = doc.toObject(StoryModel.class);
                                    if (story != null && (story.getExpiresAt() == null || story.getExpiresAt().after(new Date()))) {
                                        fetchedStories.add(story);
                                    }
                                }
                                _stories.setValue(fetchedStories);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("StoryViewModel", "Error fetching stories: " + e.getMessage());
                                _stories.setValue(new ArrayList<>());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("StoryViewModel", "Error fetching following list: " + e.getMessage());
                    _stories.setValue(new ArrayList<>());
                });
    }

    public void uploadStory(StoryModel story) {
        // منطق تحميل القصة إلى Firestore
        // ...
    }

    public void markStoryAsViewed(String storyId, String userId) {
        db.collection("stories").document(storyId)
                .update("viewers", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> Log.d("StoryViewModel", "Story viewed by " + userId))
                .addOnFailureListener(e -> Log.e("StoryViewModel", "Error marking story as viewed: " + e.getMessage()));
    }

    // دالة جديدة لجلب قصة المستخدم الحالي
    public void fetchYourStory() {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            _yourStory.setValue(null);
            return;
        }

        db.collection("stories")
                .whereEqualTo("authorId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1) // جلب أحدث قصة للمستخدم
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        StoryModel story = queryDocumentSnapshots.getDocuments().get(0).toObject(StoryModel.class);
                        // تحقق من انتهاء صلاحية القصة
                        if (story != null && (story.getExpiresAt() == null || story.getExpiresAt().after(new Date()))) {
                            _yourStory.setValue(story);
                        } else {
                            _yourStory.setValue(null); // القصة منتهية الصلاحية
                        }
                    } else {
                        _yourStory.setValue(null); // لا توجد قصة للمستخدم
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("StoryViewModel", "Error fetching your story: " + e.getMessage());
                    _yourStory.setValue(null);
                });
    }

    // دالة لجلب بيانات المستخدم الحالي (مثل صورة الأفاتار)
    public void fetchCurrentUser() {
        String currentUserId = auth.getUid();
        if (currentUserId != null) {
            userRepository.getUserById(currentUserId).observeForever(user -> {
                _currentUser.setValue(user);
            });
        } else {
            _currentUser.setValue(null);
        }
    }
}