package com.spidroid.starry.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.repositories.UserRepository;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StoryViewModel extends ViewModel {

    private static final String TAG = "StoryViewModel";
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final UserRepository userRepository;

    // LiveData واحد لتمرير كل البيانات اللازمة للواجهة
    private final MutableLiveData<StoryFeedState> _storyFeedState = new MutableLiveData<>();
    public LiveData<StoryFeedState> getStoryFeedState() {
        return _storyFeedState;
    }

    // LiveData لبيانات المستخدم الحالي فقط
    private final MutableLiveData<UserModel> _currentUser = new MutableLiveData<>();
    public LiveData<UserModel> getCurrentUser() {
        return _currentUser;
    }


    public StoryViewModel() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();
    }

    // دالة واحدة لجلب كل ما يتعلق بالقصص
    public void fetchStoriesForCurrentUserAndFollowing() {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            _storyFeedState.setValue(new StoryFeedState(new ArrayList<>(), false, new HashSet<>()));
            return;
        }

        // 1. جلب قائمة المتابَعين
        db.collection("users").document(currentUserId).collection("following")
                .get()
                .addOnSuccessListener(followingSnapshot -> {
                    List<String> userIdsToFetch = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : followingSnapshot) {
                        userIdsToFetch.add(doc.getId());
                    }
                    userIdsToFetch.add(currentUserId); // أضف المستخدم الحالي دائماً

                    // 2. جلب القصص لهؤلاء المستخدمين
                    // ملاحظة هامة: استعلام whereIn محدود بـ 10 أو 30 عنصر.
                    // في تطبيق حقيقي، ستحتاج إلى تقسيم الطلب إلى أجزاء أو استخدام بنية بيانات مختلفة.
                    if (userIdsToFetch.isEmpty()) {
                        _storyFeedState.setValue(new StoryFeedState(new ArrayList<>(), false, new HashSet<>()));
                        return;
                    }

                    db.collection("stories")
                            .whereIn("userId", userIdsToFetch)
                            .whereGreaterThan("expiresAt", new Date()) // جلب القصص النشطة فقط
                            .orderBy("expiresAt", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(storySnapshots -> {
                                List<StoryModel> allStories = storySnapshots.toObjects(StoryModel.class);

                                // 3. فصل قصة المستخدم الحالي عن قصص الآخرين
                                boolean hasMyStory = allStories.stream().anyMatch(s -> s.getUserId().equals(currentUserId));
                                List<StoryModel> otherStories = allStories.stream()
                                        .filter(s -> !s.getUserId().equals(currentUserId))
                                        .collect(Collectors.toList());

                                // 4. جلب بيانات أصحاب القصص (الاسم والصورة) لإثرائها
                                Set<String> authorIds = otherStories.stream()
                                        .map(StoryModel::getUserId)
                                        .collect(Collectors.toSet());

                                if (authorIds.isEmpty()) {
                                    // لا توجد قصص للآخرين، فقط أرسل حالة قصة المستخدم الحالي
                                    _storyFeedState.setValue(new StoryFeedState(new ArrayList<>(), hasMyStory, new HashSet<>()));
                                } else {
                                    // جلب بيانات المستخدمين
                                    db.collection("users").whereIn("userId", new ArrayList<>(authorIds))
                                            .get()
                                            .addOnSuccessListener(userSnapshots -> {
                                                // إنشاء Map لسهولة الوصول إلى بيانات المستخدم
                                                java.util.Map<String, UserModel> userMap = new java.util.HashMap<>();
                                                for (UserModel user : userSnapshots.toObjects(UserModel.class)) {
                                                    userMap.put(user.getUserId(), user);
                                                }

                                                // إثراء كل قصة ببيانات صاحبها
                                                for (StoryModel story : otherStories) {
                                                    UserModel author = userMap.get(story.getUserId());
                                                    if (author != null) {
                                                        // story.setAuthorDisplayName(author.getDisplayName()); // يجب إضافة هذه الحقول إلى StoryModel
                                                        // story.setAuthorAvatarUrl(author.getProfileImageUrl());
                                                    }
                                                }

                                                // 5. جلب القصص المشاهدة
                                                Set<String> viewedStoryIds = new HashSet<>(); // يجب جلبها من قاعدة البيانات

                                                _storyFeedState.setValue(new StoryFeedState(otherStories, hasMyStory, viewedStoryIds));
                                            });
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error fetching stories: ", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching following list: ", e));
    }

    // دالة منفصلة لجلب المستخدم الحالي، لأنها قد تكون مطلوبة في أماكن أخرى
    public void fetchCurrentUser() {
        String currentUserId = auth.getUid();
        if (currentUserId != null) {
            // استخدام المستودع Repository لجلب المستخدم، وهي ممارسة جيدة
            // لكن لاحظ أن استخدام observeForever يتطلب إزالته في onCleared لتجنب تسريب الذاكرة
            userRepository.getUserById(currentUserId).observeForever(user -> {
                _currentUser.postValue(user);
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // إزالة المراقب (observer) لتجنب تسريب الذاكرة
        userRepository.getUserById(auth.getUid()).removeObserver(user -> {});
    }

    // فئة داخلية لتغليف حالة واجهة المستخدم
    public static class StoryFeedState {
        public final List<StoryModel> stories;
        public final boolean hasMyActiveStory;
        public final Set<String> viewedStoryIds;

        public StoryFeedState(List<StoryModel> stories, boolean hasMyActiveStory, Set<String> viewedStoryIds) {
            this.stories = stories;
            this.hasMyActiveStory = hasMyActiveStory;
            this.viewedStoryIds = viewedStoryIds;
        }
    }
}