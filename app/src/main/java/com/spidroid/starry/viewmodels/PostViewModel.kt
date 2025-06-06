package com.spidroid.starry.viewmodels

// ★ إضافة إذا لم يكن موجودًا
import android.util.Log
import androidx.lifecycle.Observer
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

class PostViewModel : ViewModel() {
    private val postRepository: PostRepository = PostRepository()
    private val userRepository: UserRepository = UserRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val posts: MutableLiveData<MutableList<PostModel?>?> =
        MutableLiveData<MutableList<PostModel?>?>()
    private val error: MutableLiveData<String?> = MutableLiveData<String?>()
    private val suggestions: MutableLiveData<MutableList<UserModel?>?> =
        MutableLiveData<MutableList<UserModel?>?>()
    private val combinedFeed: MediatorLiveData<MutableList<Any?>?> =
        MediatorLiveData<MutableList<Any?>?>()
    private val currentUserData: MutableLiveData<UserModel?> = MutableLiveData<UserModel?>()

    private val _postUpdatedEvent: MutableLiveData<String?> = MutableLiveData<String?>()
    val postUpdatedEvent: LiveData<String?>
        get() = _postUpdatedEvent

    private val _postInteractionErrorEvent: MutableLiveData<String?> = MutableLiveData<String?>()
    val postInteractionErrorEvent: LiveData<String?>
        get() = _postInteractionErrorEvent


    init {
        combinedFeed.addSource<MutableList<PostModel?>?>(
            posts,
            Observer { postsList: MutableList<PostModel?>? ->
                combineData(
                    postsList,
                    suggestions.getValue()
                )
            })
        combinedFeed.addSource<MutableList<UserModel?>?>(
            suggestions,
            Observer { users: MutableList<UserModel?>? -> combineData(posts.getValue(), users) })
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        if (firebaseUser != null) {
            userRepository.getUserById(firebaseUser.getUid())
                .observeForever(Observer { userModel: UserModel? ->
                    if (userModel != null) {
                        currentUserData.postValue(userModel)
                        Log.d(
                            PostViewModel.Companion.TAG,
                            "Current user data loaded: " + userModel.getUsername()
                        )
                    } else {
                        _postInteractionErrorEvent.postValue("Failed to load current user data from repository.")
                        Log.e(
                            PostViewModel.Companion.TAG,
                            "Current user data is null from repository for UID: " + firebaseUser.getUid()
                        )
                    }
                })
        } else {
            _postInteractionErrorEvent.postValue("User not authenticated.")
            Log.e(
                PostViewModel.Companion.TAG,
                "FirebaseUser is null, cannot fetch current user data."
            )
        }
    }

    fun getCombinedFeed(): LiveData<MutableList<Any?>?> {
        return combinedFeed
    }

    val errorLiveData: LiveData<String?>
        get() = error // يستخدم للأخطاء العامة لجلب الخلاصات
    val currentUserLiveData: LiveData<UserModel?>
        get() = currentUserData

    fun fetchPosts(limit: Int) {
        Log.d(
            PostViewModel.Companion.TAG,
            "Attempting to fetch posts for main feed. Limit: " + limit
        )
        postRepository.getPosts(limit)
            .addOnCompleteListener(OnCompleteListener { task: Task<QuerySnapshot?>? ->
                if (task!!.isSuccessful() && task.getResult() != null) {
                    val postList: MutableList<PostModel?> = ArrayList<PostModel?>()
                    Log.d(
                        PostViewModel.Companion.TAG,
                        "Fetched " + task.getResult()
                            .size() + " documents from Firestore for main feed."
                    )
                    for (doc in task.getResult()) {
                        val post: PostModel? = doc.toObject(PostModel::class.java)
                        if (post != null) {
                            post.setPostId(doc.getId())
                            updateUserInteractions(post)
                            postList.add(post)
                            Log.d(
                                PostViewModel.Companion.TAG,
                                "Added post to list: " + post.getPostId() + " with content (first 20 chars): " + (if (post.getContent() != null && post.getContent().length > 20) post.getContent()
                                    .substring(0, 20) else post.getContent())
                            )
                        } else {
                            Log.w(
                                PostViewModel.Companion.TAG,
                                "Document " + doc.getId() + " converted to null PostModel for main feed."
                            )
                        }
                    }
                    posts.postValue(postList)
                    Log.d(
                        PostViewModel.Companion.TAG,
                        "posts LiveData updated with " + postList.size + " posts for main feed."
                    )
                    if (postList.isEmpty()) {
                        Log.w(
                            PostViewModel.Companion.TAG,
                            "Post list IS EMPTY after successful fetch for main feed."
                        )
                        error.postValue("No posts found.") // إرسال رسالة إذا كانت القائمة فارغة
                    }
                } else {
                    val errorMsg =
                        "Failed to load posts for main feed: " + (if (task.getException() != null) task.getException()!!.message else "Unknown error")
                    error.postValue(errorMsg)
                    Log.e(PostViewModel.Companion.TAG, errorMsg, task.getException())
                    posts.postValue(ArrayList<PostModel?>())
                }
            })
    }

    fun fetchUserSuggestions() {
        postRepository.getUserSuggestions()
            .addOnCompleteListener(OnCompleteListener { task: Task<QuerySnapshot?>? ->
                if (task!!.isSuccessful() && task.getResult() != null) {
                    suggestions.postValue(task.getResult().toObjects(UserModel::class.java))
                } else {
                    Log.e(
                        PostViewModel.Companion.TAG,
                        "Failed to fetch user suggestions",
                        task.getException()
                    )
                }
            })
    }

    fun toggleLike(postToUpdate: PostModel?, newLikedState: Boolean) {
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        val liker: UserModel? = currentUserData.getValue()
        if (firebaseUser == null || liker == null || postToUpdate == null || postToUpdate.getPostId() == null || postToUpdate.getAuthorId() == null) {
            handleInteractionError("toggleLike", "Authentication or data error.")
            return
        }
        postRepository.toggleLike(
            postToUpdate.getPostId(),
            newLikedState,
            postToUpdate.getAuthorId(),
            liker
        )
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Like status updated in Firestore for " + postToUpdate.getPostId()
                )
                updateLocalPostInteraction(
                    postToUpdate.getPostId(),
                    "like",
                    newLikedState,
                    (if (newLikedState) 1 else -1).toLong()
                )
                _postUpdatedEvent.postValue(postToUpdate.getPostId())
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "toggleLike",
                    e!!.message
                )
            })
    }

    fun toggleBookmark(postId: String?, newBookmarkedState: Boolean) {
        if (auth.getUid() == null || postId == null) {
            handleInteractionError("toggleBookmark", "Authentication or Post ID error.")
            return
        }
        postRepository.toggleBookmark(postId, newBookmarkedState)
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Bookmark status updated in Firestore for " + postId
                )
                updateLocalPostInteraction(
                    postId,
                    "bookmark",
                    newBookmarkedState,
                    (if (newBookmarkedState) 1 else -1).toLong()
                )
                _postUpdatedEvent.postValue(postId)
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "toggleBookmark",
                    e!!.message
                )
            })
    }

    fun toggleRepost(postId: String?, newRepostedState: Boolean) {
        if (auth.getUid() == null || postId == null) {
            handleInteractionError("toggleRepost", "Authentication or Post ID error.")
            return
        }
        postRepository.toggleRepost(postId, newRepostedState)
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Repost status updated in Firestore for " + postId
                )
                updateLocalPostInteraction(
                    postId,
                    "repost",
                    newRepostedState,
                    (if (newRepostedState) 1 else -1).toLong()
                )
                _postUpdatedEvent.postValue(postId)
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "toggleRepost",
                    e!!.message
                )
            })
    }

    fun deletePost(postId: String?) {
        if (postId == null || postId.isEmpty()) {
            handleInteractionError("deletePost", "Post ID cannot be null or empty.")
            return
        }
        postRepository.deletePost(postId)
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Post " + postId + " deleted successfully from Firestore."
                )
                val currentPostsList: MutableList<PostModel?>? = posts.getValue()
                if (currentPostsList != null) {
                    val removed =
                        currentPostsList.removeIf { p: PostModel? -> p.getPostId() == postId }
                    if (removed) {
                        posts.postValue(ArrayList<PostModel?>(currentPostsList))
                        _postUpdatedEvent.postValue(postId) // إرسال حدث أن المنشور تم حذفه
                    }
                }
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "deletePost",
                    "Failed to delete post: " + e!!.message
                )
            })
    }

    fun handleEmojiSelection(post: PostModel?, emoji: String?) {
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        val reactor: UserModel? = currentUserData.getValue()
        if (firebaseUser == null || reactor == null || post == null || post.getPostId() == null || post.getAuthorId() == null) {
            handleInteractionError("handleEmojiSelection", "Authentication or data error.")
            return
        }
        postRepository.addOrUpdateReaction(
            post.getPostId(),
            reactor.getUserId(),
            emoji,
            post.getAuthorId(),
            reactor
        )
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Reaction '" + emoji + "' successfully processed for post " + post.getPostId()
                )
                _postUpdatedEvent.postValue(post.getPostId())
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "handleEmojiSelection",
                    "Failed to add/update reaction: " + e!!.message
                )
            })
    }

    fun togglePostPinStatus(postToToggle: PostModel?) {
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        if (firebaseUser == null || postToToggle == null || postToToggle.getPostId() == null || postToToggle.getAuthorId() == null) {
            handleInteractionError("togglePostPinStatus", "Authentication or data error.")
            return
        }
        if (!firebaseUser.getUid().equals(postToToggle.getAuthorId())) {
            handleInteractionError("togglePostPinStatus", "User not authorized to pin this post.")
            return
        }
        val newPinnedState: Boolean = !postToToggle.isPinned()
        postRepository.setPostPinnedStatus(
            postToToggle.getPostId(),
            postToToggle.getAuthorId(),
            newPinnedState
        )
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Pin status updated in Firestore for " + postToToggle.getPostId() + " to " + newPinnedState
                )
                _postUpdatedEvent.postValue(postToToggle.getPostId()) // للإشارة إلى أن القائمة يجب أن تُعاد تحميلها في ProfilePostsFragment
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "togglePostPinStatus",
                    e!!.message
                )
            })
    }

    fun editPostContent(postId: String?, newContent: String?) {
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        if (firebaseUser == null || postId == null || newContent == null) {
            handleInteractionError("editPostContent", "Authentication, Post ID, or content error.")
            return
        }
        // يمكنك إضافة تحقق من ملكية المنشور هنا إذا أردت
        postRepository.updatePostContent(postId, newContent)
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(PostViewModel.Companion.TAG, "Post " + postId + " content updated.")
                _postUpdatedEvent.postValue(postId)
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "editPostContent",
                    e!!.message
                )
            })
    }

    fun setPostPrivacy(postId: String?, newPrivacyLevel: String?) {
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        if (firebaseUser == null || postId == null || newPrivacyLevel == null) {
            handleInteractionError(
                "setPostPrivacy",
                "Authentication, Post ID, or privacy level error."
            )
            return
        }
        // يمكنك إضافة تحقق من ملكية المنشور هنا
        postRepository.updatePostPrivacy(postId, newPrivacyLevel)
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Post " + postId + " privacy updated to " + newPrivacyLevel
                )
                _postUpdatedEvent.postValue(postId)
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "setPostPrivacy",
                    e!!.message
                )
            })
    }

    fun submitReportForPost(
        postId: String?,
        reason: String?,
        reportedAuthorId: String?
    ) { // ★ إضافة reportedAuthorId
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        if (firebaseUser == null || postId == null || reason == null || reason.trim { it <= ' ' }
                .isEmpty() || reportedAuthorId == null) {
            handleInteractionError(
                "submitReportForPost",
                "Authentication, Post ID, reason, or author ID error."
            )
            return
        }
        val reportData: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        reportData.put("reportedPostId", postId)
        reportData.put("reportedAuthorId", reportedAuthorId) // ★ إضافة معرّف صاحب المنشور
        reportData.put("reportingUserId", firebaseUser.getUid())
        reportData.put("reason", reason)
        reportData.put("timestamp", FieldValue.serverTimestamp())
        reportData.put("status", "pending")

        postRepository.submitReport(reportData)
            .addOnSuccessListener(OnSuccessListener { documentReference: DocumentReference? ->
                Log.d(
                    PostViewModel.Companion.TAG,
                    "Report submitted for post " + postId + " with ID: " + documentReference.getId()
                )
            })
            .addOnFailureListener(OnFailureListener { e: Exception? ->
                handleInteractionError(
                    "submitReportForPost",
                    e!!.message
                )
            })
    }

    private fun updateLocalPostInteraction(
        postId: String?,
        interactionType: String,
        newState: Boolean,
        countChange: Long
    ) {
        val currentPostsList: MutableList<PostModel?>? = posts.getValue()
        if (currentPostsList == null) return

        var listChanged = false
        val tempList: MutableList<PostModel> =
            ArrayList<PostModel>(currentPostsList) // اعمل على نسخة لتجنب ConcurrentModificationException

        for (p in tempList) {
            if (p.getPostId() == postId) {
                when (interactionType) {
                    "like" -> if (p.isLiked() != newState) {
                        p.setLiked(newState)
                        p.setLikeCount(p.getLikeCount() + countChange)
                        if (p.getLikeCount() < 0) p.setLikeCount(0)
                        listChanged = true
                    }

                    "bookmark" -> if (p.isBookmarked() != newState) {
                        p.setBookmarked(newState)
                        p.setBookmarkCount(p.getBookmarkCount() + countChange)
                        if (p.getBookmarkCount() < 0) p.setBookmarkCount(0)
                        listChanged = true
                    }

                    "repost" -> if (p.isReposted() != newState) {
                        p.setReposted(newState)
                        p.setRepostCount(p.getRepostCount() + countChange)
                        if (p.getRepostCount() < 0) p.setRepostCount(0)
                        listChanged = true
                    }
                }
                break
            }
        }
        if (listChanged) {
            posts.postValue(tempList)
        }
    }

    private fun combineData(postsList: MutableList<PostModel?>?, users: MutableList<UserModel?>?) {
        val combinedList: MutableList<Any?> = ArrayList<Any?>()
        if (postsList != null) {
            combinedList.addAll(postsList)
        }
        // ... (منطق دمج اقتراحات المستخدمين إذا لزم الأمر) ...
        combinedFeed.postValue(combinedList)
    }

    private fun updateUserInteractions(post: PostModel) {
        val currentUserId: String? = auth.getUid()
        if (currentUserId == null) {
            post.setLiked(false)
            post.setBookmarked(false)
            post.setReposted(false)
            return
        }
        if (post.getLikes() != null) {
            post.setLiked(post.getLikes().containsKey(currentUserId))
        } else {
            post.setLiked(false)
        }
        if (post.getBookmarks() != null) {
            post.setBookmarked(post.getBookmarks().containsKey(currentUserId))
        } else {
            post.setBookmarked(false)
        }
        if (post.getReposts() != null) {
            post.setReposted(post.getReposts().containsKey(currentUserId))
        } else {
            post.setReposted(false)
        }
    }

    private fun handleInteractionError(operation: String, errorMessage: String?) {
        val fullError = "Failed to " + operation + ": " + errorMessage
        _postInteractionErrorEvent.postValue(fullError) // استخدام LiveData جديد لأخطاء التفاعل
        Log.e(PostViewModel.Companion.TAG, fullError)
    }

    override fun onCleared() {
        super.onCleared()
        // ... (منطق إزالة المراقبين إذا لزم الأمر)
    }

    companion object {
        private const val TAG = "PostViewModel"
    }
}