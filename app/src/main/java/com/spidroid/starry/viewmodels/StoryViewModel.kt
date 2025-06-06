package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.Observer
import com.google.firebase.auth.FirebaseAuth
import java.util.Date
import java.util.stream.Collectors
import kotlin.Boolean
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.setValue
import kotlin.setValue

class StoryViewModel : ViewModel() {
    private val auth: FirebaseAuth
    private val db: FirebaseFirestore
    private val userRepository: UserRepository

    // LiveData واحد لتمرير كل البيانات اللازمة للواجهة
    private val _storyFeedState: MutableLiveData<StoryFeedState?> =
        MutableLiveData<StoryFeedState?>()
    val storyFeedState: LiveData<StoryFeedState?>
        get() = _storyFeedState

    // LiveData لبيانات المستخدم الحالي فقط
    private val _currentUser: MutableLiveData<UserModel?> = MutableLiveData<UserModel?>()
    val currentUser: LiveData<UserModel?>
        get() = _currentUser


    init {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userRepository = UserRepository()
    }

    // دالة واحدة لجلب كل ما يتعلق بالقصص
    fun fetchStoriesForCurrentUserAndFollowing() {
        val currentUserId: String? = auth.getUid()
        if (currentUserId == null) {
            _storyFeedState.setValue(
                StoryFeedState(
                    ArrayList<StoryModel?>(),
                    false,
                    HashSet<String?>()
                )
            )
            return
        }

        // 1. جلب قائمة المتابَعين
        db.collection("users").document(currentUserId).collection("following")
            .get()
            .addOnSuccessListener({ followingSnapshot ->
                val userIdsToFetch: MutableList<String?> = ArrayList<String?>()
                for (doc in followingSnapshot) {
                    userIdsToFetch.add(doc.getId())
                }
                userIdsToFetch.add(currentUserId) // أضف المستخدم الحالي دائماً

                // 2. جلب القصص لهؤلاء المستخدمين
                // ملاحظة هامة: استعلام whereIn محدود بـ 10 أو 30 عنصر.
                // في تطبيق حقيقي، ستحتاج إلى تقسيم الطلب إلى أجزاء أو استخدام بنية بيانات مختلفة.
                if (userIdsToFetch.isEmpty()) {
                    _storyFeedState.setValue(
                        StoryFeedState(
                            ArrayList<StoryModel?>(),
                            false,
                            HashSet<String?>()
                        )
                    )
                    return@addOnSuccessListener
                }
                db.collection("stories")
                    .whereIn("userId", userIdsToFetch)
                    .whereGreaterThan("expiresAt", Date()) // جلب القصص النشطة فقط
                    .orderBy("expiresAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener({ storySnapshots ->
                        val allStories: MutableList<StoryModel?> =
                            storySnapshots.toObjects(StoryModel::class.java)
                        // 3. فصل قصة المستخدم الحالي عن قصص الآخرين
                        val hasMyStory = allStories.stream()
                            .anyMatch { s: StoryModel? -> s.getUserId() == currentUserId }
                        val otherStories: MutableList<StoryModel> = allStories.stream()
                            .filter { s: StoryModel? -> s.getUserId() != currentUserId }
                            .collect(Collectors.toList())

                        // 4. جلب بيانات أصحاب القصص (الاسم والصورة) لإثرائها
                        val authorIds = otherStories.stream()
                            .map<String?> { obj: StoryModel? -> obj.getUserId() }
                            .collect(Collectors.toSet())
                        if (authorIds.isEmpty()) {
                            // لا توجد قصص للآخرين، فقط أرسل حالة قصة المستخدم الحالي
                            _storyFeedState.setValue(
                                StoryFeedState(
                                    ArrayList<StoryModel?>(),
                                    hasMyStory,
                                    HashSet<String?>()
                                )
                            )
                        } else {
                            // جلب بيانات المستخدمين
                            db.collection("users").whereIn("userId", ArrayList<E?>(authorIds))
                                .get()
                                .addOnSuccessListener({ userSnapshots ->
                                    // إنشاء Map لسهولة الوصول إلى بيانات المستخدم
                                    val userMap: MutableMap<String?, UserModel?> =
                                        HashMap<String?, UserModel?>()
                                    for (user in userSnapshots.toObjects(UserModel::class.java)) {
                                        userMap.put(user.getUserId(), user)
                                    }

                                    // إثراء كل قصة ببيانات صاحبها
                                    for (story in otherStories) {
                                        val author: UserModel? = userMap.get(story.getUserId())
                                        if (author != null) {
                                            // story.setAuthorDisplayName(author.getDisplayName()); // يجب إضافة هذه الحقول إلى StoryModel
                                            // story.setAuthorAvatarUrl(author.getProfileImageUrl());
                                        }
                                    }

                                    // 5. جلب القصص المشاهدة
                                    val viewedStoryIds: MutableSet<String?> =
                                        HashSet<String?>() // يجب جلبها من قاعدة البيانات
                                    _storyFeedState.setValue(
                                        StoryFeedState(
                                            otherStories,
                                            hasMyStory,
                                            viewedStoryIds
                                        )
                                    )
                                })
                        }
                    })
                    .addOnFailureListener({ e ->
                        Log.e(
                            StoryViewModel.Companion.TAG,
                            "Error fetching stories: ",
                            e
                        )
                    })
            })
            .addOnFailureListener({ e ->
                Log.e(
                    StoryViewModel.Companion.TAG,
                    "Error fetching following list: ",
                    e
                )
            })
    }

    // دالة منفصلة لجلب المستخدم الحالي، لأنها قد تكون مطلوبة في أماكن أخرى
    fun fetchCurrentUser() {
        val currentUserId: String? = auth.getUid()
        if (currentUserId != null) {
            // استخدام المستودع Repository لجلب المستخدم، وهي ممارسة جيدة
            // لكن لاحظ أن استخدام observeForever يتطلب إزالته في onCleared لتجنب تسريب الذاكرة
            userRepository.getUserById(currentUserId).observeForever(Observer { user: UserModel? ->
                _currentUser.postValue(user)
            })
        }
    }

    override fun onCleared() {
        super.onCleared()
        // إزالة المراقب (observer) لتجنب تسريب الذاكرة
        userRepository.getUserById(auth.getUid()).removeObserver(Observer { user: UserModel? -> })
    }

    // فئة داخلية لتغليف حالة واجهة المستخدم
    class StoryFeedState(
        stories: MutableList<StoryModel>?,
        hasMyActiveStory: Boolean,
        viewedStoryIds: MutableSet<String?>?
    ) {
        val stories: MutableList<StoryModel>?
        val hasMyActiveStory: Boolean
        val viewedStoryIds: MutableSet<String?>?

        init {
            this.stories = stories
            this.hasMyActiveStory = hasMyActiveStory
            this.viewedStoryIds = viewedStoryIds
        }
    }

    companion object {
        private const val TAG = "StoryViewModel"
    }
}