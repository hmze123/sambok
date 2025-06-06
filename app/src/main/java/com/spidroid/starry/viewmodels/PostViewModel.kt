package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.PostRepository
import com.spidroid.starry.repositories.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// تم حذف تعريف UiState من هنا

class PostViewModel : ViewModel() {

    private val postRepository = PostRepository()
    private val userRepository = UserRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance() // تم إضافة هذا السطر

    // For "For You" feed
    private val _posts = MutableLiveData<List<PostModel>>()
    private val _suggestions = MutableLiveData<List<UserModel>>()
    private val _error = MutableLiveData<String?>()

    // For "Following" feed
    private val _followingPosts = MutableLiveData<List<PostModel>>()
    val followingPosts: LiveData<List<PostModel>> get() = _followingPosts

    private val _followingState = MutableLiveData<UiState<Nothing>>()
    val followingState: LiveData<UiState<Nothing>> get() = _followingState

    private val _currentUserData = MutableLiveData<UserModel?>()
    val currentUserLiveData: LiveData<UserModel?> get() = _currentUserData

    private val _postUpdatedEvent = MutableLiveData<String?>()
    val postUpdatedEvent: LiveData<String?> get() = _postUpdatedEvent

    private val _postInteractionErrorEvent = MutableLiveData<String?>()
    val postInteractionErrorEvent: LiveData<String?> get() = _postInteractionErrorEvent

    val errorLiveData: LiveData<String?> get() = _error

    val combinedFeed = MediatorLiveData<List<Any>>().apply {
        addSource(_posts) { posts ->
            value = combineFeeds(posts, _suggestions.value)
        }
        addSource(_suggestions) { suggestions ->
            value = combineFeeds(_posts.value, suggestions)
        }
    }

    private companion object {
        private const val TAG = "PostViewModel"
    }

    init {
        fetchCurrentUser()
    }

    private fun combineFeeds(posts: List<PostModel>?, suggestions: List<UserModel>?): List<Any> {
        val combinedList = mutableListOf<Any>()
        posts?.let { combinedList.addAll(it) }
        suggestions?.let { if (it.isNotEmpty()) combinedList.add(it) }
        return combinedList
    }

    private fun fetchCurrentUser() {
        auth.currentUser?.uid?.let { userId ->
            userRepository.getUserById(userId).observeForever { userModel ->
                if (userModel != null) {
                    _currentUserData.postValue(userModel)
                    Log.d(TAG, "Current user data loaded: ${userModel.username}")
                } else {
                    _postInteractionErrorEvent.postValue("Failed to load current user data.")
                    Log.e(TAG, "Current user data is null from repository for UID: $userId")
                }
            }
        } ?: run {
            _postInteractionErrorEvent.postValue("User not authenticated.")
            Log.e(TAG, "FirebaseUser is null, cannot fetch current user data.")
        }
    }

    fun fetchPosts(limit: Int) {
        Log.d(TAG, "Attempting to fetch posts for main feed. Limit: $limit")
        postRepository.getPosts(limit).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val postList = task.result?.mapNotNull { doc ->
                    doc.toObject(PostModel::class.java).apply {
                        postId = doc.id
                        updateUserInteractions(this)
                    }
                }
                if (postList != null) {
                    _posts.postValue(postList)
                    Log.d(TAG, "posts LiveData updated with ${postList.size} posts.")
                    if (postList.isEmpty()) {
                        _error.postValue("No posts found.")
                    }
                }
            } else {
                val errorMsg = "Failed to load posts: ${task.exception?.message ?: "Unknown error"}"
                _error.postValue(errorMsg)
                Log.e(TAG, errorMsg, task.exception)
                _posts.postValue(emptyList())
            }
        }
    }

    fun fetchFollowingPosts(limit: Int) {
        _followingState.value = UiState.Loading
        val currentUserId = auth.currentUser?.uid ?: run {
            _followingState.value = UiState.Error("User not authenticated.")
            return
        }

        viewModelScope.launch {
            try {
                val userDoc = db.collection("users").document(currentUserId).get().await()
                val followingIds = (userDoc.get("following") as? Map<String, Boolean>)?.keys?.toList()

                if (followingIds.isNullOrEmpty()) {
                    _followingPosts.postValue(emptyList())
                    _followingState.value = UiState.Success
                    return@launch
                }

                val postsList = mutableListOf<PostModel>()
                // Fetch posts in chunks of 10
                followingIds.chunked(10).forEach { chunk ->
                    val snapshot = postRepository.getPostsForFollowing(chunk, limit).await()
                    snapshot?.documents?.mapNotNullTo(postsList) { doc ->
                        doc.toObject(PostModel::class.java)?.apply {
                            postId = doc.id
                            updateUserInteractions(this)
                        }
                    }
                }

                // Sort all fetched posts by date
                postsList.sortByDescending { it.createdAt }

                _followingPosts.postValue(postsList)
                _followingState.value = UiState.Success

            } catch (e: Exception) {
                val errorMsg = "Failed to fetch following feed: ${e.message}"
                Log.e(TAG, errorMsg, e)
                _followingState.value = UiState.Error(errorMsg)
            }
        }
    }

    fun fetchUserSuggestions() {
        postRepository.userSuggestions.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _suggestions.postValue(task.result?.toObjects(UserModel::class.java))
            } else {
                Log.e(TAG, "Failed to fetch user suggestions", task.exception)
            }
        }
    }

    private fun updateUserInteractions(post: PostModel) {
        val userId = auth.currentUser?.uid
        post.isLiked = userId != null && post.likes.containsKey(userId)
        post.isBookmarked = userId != null && post.bookmarks.containsKey(userId)
        post.isReposted = userId != null && post.reposts.containsKey(userId)
    }

    fun toggleLike(post: PostModel, newLikedState: Boolean) {
        val liker = _currentUserData.value ?: run {
            handleInteractionError("toggleLike", "User data not loaded.")
            return
        }
        val postId = post.postId ?: return
        val authorId = post.authorId ?: return

        postRepository.toggleLike(postId, newLikedState, authorId, liker)
            .addOnSuccessListener {
                updateLocalPostInteraction(postId, "like", newLikedState, if (newLikedState) 1 else -1)
                _postUpdatedEvent.postValue(postId)
            }
            .addOnFailureListener { e -> handleInteractionError("toggleLike", e.message) }
    }

    fun toggleBookmark(postId: String, newBookmarkedState: Boolean) {
        postRepository.toggleBookmark(postId, newBookmarkedState)
            .addOnSuccessListener {
                updateLocalPostInteraction(postId, "bookmark", newBookmarkedState, if (newBookmarkedState) 1 else -1)
                _postUpdatedEvent.postValue(postId)
            }
            .addOnFailureListener { e -> handleInteractionError("toggleBookmark", e.message) }
    }

    fun toggleRepost(postId: String, newRepostedState: Boolean) {
        postRepository.toggleRepost(postId, newRepostedState)
            .addOnSuccessListener {
                updateLocalPostInteraction(postId, "repost", newRepostedState, if (newRepostedState) 1 else -1)
                _postUpdatedEvent.postValue(postId)
            }
            .addOnFailureListener { e -> handleInteractionError("toggleRepost", e.message) }
    }

    fun deletePost(postId: String?) {
        val safePostId = postId ?: run {
            handleInteractionError("deletePost", "Post ID cannot be null.")
            return
        }
        postRepository.deletePost(safePostId)
            .addOnSuccessListener {
                // Remove from both lists
                _posts.value?.let { list -> _posts.postValue(list.filterNot { it.postId == safePostId }) }
                _followingPosts.value?.let { list -> _followingPosts.postValue(list.filterNot { it.postId == safePostId }) }
                _postUpdatedEvent.postValue(safePostId)
            }
            .addOnFailureListener { e -> handleInteractionError("deletePost", e.message) }
    }

    fun togglePostPinStatus(postToToggle: PostModel) {
        val authorId = postToToggle.authorId ?: return
        val postId = postToToggle.postId ?: return
        val newPinnedState = !postToToggle.isPinned

        viewModelScope.launch {
            try {
                postRepository.setPostPinnedStatus(postId, authorId, newPinnedState)
                _postUpdatedEvent.postValue(postId)
            } catch (e: Exception) {
                handleInteractionError("togglePostPinStatus", e.message)
            }
        }
    }

    fun handleEmojiSelection(post: PostModel, emoji: String) {
        val reactor = _currentUserData.value ?: run {
            handleInteractionError("handleEmojiSelection", "User data not loaded.")
            return
        }
        val postId = post.postId ?: return
        val authorId = post.authorId ?: return

        postRepository.addOrUpdateReaction(postId, reactor.userId, emoji, authorId, reactor)
            .addOnSuccessListener { _postUpdatedEvent.postValue(postId) }
            .addOnFailureListener { e -> handleInteractionError("handleEmojiSelection", e.message) }
    }

    private fun updateLocalPostInteraction(postId: String, interactionType: String, newState: Boolean, countChange: Long) {
        // Update the main feed (_posts)
        _posts.value?.let { currentPosts ->
            val updatedPosts = currentPosts.map { post ->
                if (post.postId == postId) {
                    updatePost(post, interactionType, newState, countChange)
                } else {
                    post
                }
            }
            _posts.postValue(updatedPosts)
        }

        // Update the following feed (_followingPosts)
        _followingPosts.value?.let { currentPosts ->
            val updatedPosts = currentPosts.map { post ->
                if (post.postId == postId) {
                    updatePost(post, interactionType, newState, countChange)
                } else {
                    post
                }
            }
            _followingPosts.postValue(updatedPosts)
        }
    }

    private fun updatePost(post: PostModel, interactionType: String, newState: Boolean, countChange: Long) : PostModel {
        return when (interactionType) {
            "like" -> post.copy(isLiked = newState, likeCount = (post.likeCount + countChange).coerceAtLeast(0))
            "bookmark" -> post.copy(isBookmarked = newState, bookmarkCount = (post.bookmarkCount + countChange).coerceAtLeast(0))
            "repost" -> post.copy(isReposted = newState, repostCount = (post.repostCount + countChange).coerceAtLeast(0))
            else -> post
        }
    }

    private fun handleInteractionError(operation: String, errorMessage: String?) {
        val fullError = "Failed to $operation: ${errorMessage ?: "Unknown error"}"
        _postInteractionErrorEvent.postValue(fullError)
        Log.e(TAG, fullError)
    }
}