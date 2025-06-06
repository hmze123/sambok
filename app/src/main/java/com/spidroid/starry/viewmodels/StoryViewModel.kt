package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.models.StoryModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

// Sealed class to represent the different states of the Story Feed UI
sealed class StoryFeedState {
    object Loading : StoryFeedState()
    data class Success(
        val storyPreviews: List<StoryPreview>,
        val hasMyActiveStory: Boolean
    ) : StoryFeedState()
    data class Error(val message: String) : StoryFeedState()
}

// Data class to hold combined data for the adapter
data class StoryPreview(
    val user: UserModel,
    val hasUnseenStories: Boolean
)

class StoryViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val userRepository = UserRepository()

    private val _storyFeedState = MutableStateFlow<StoryFeedState>(StoryFeedState.Loading)
    val storyFeedState: StateFlow<StoryFeedState> = _storyFeedState

    private val _currentUser = MutableLiveData<UserModel?>()
    val currentUser: LiveData<UserModel?> get() = _currentUser

    companion object {
        private const val TAG = "StoryViewModel"
    }

    init {
        fetchCurrentUser()
        fetchStoriesForCurrentUserAndFollowing()
    }

    fun fetchCurrentUser() {
        auth.currentUser?.uid?.let { userId ->
            userRepository.getUserById(userId).observeForever { user ->
                _currentUser.postValue(user)
            }
        }
    }

    fun fetchStoriesForCurrentUserAndFollowing() {
        val currentUserId = auth.currentUser?.uid ?: run {
            _storyFeedState.value = StoryFeedState.Error("User not authenticated")
            return
        }

        viewModelScope.launch {
            _storyFeedState.value = StoryFeedState.Loading
            try {
                // 1. Get the list of users the current user is following
                val followingSnapshot = db.collection("users").document(currentUserId).get().await()
                val followingMap = followingSnapshot.get("following") as? Map<String, Boolean> ?: emptyMap()
                val followingIds = followingMap.keys.toMutableList()

                // 2. Fetch active stories for the followed users
                // Note: Firestore 'in' query is limited. For more than 10 users, you need to chunk the list.
                val storyPreviews = if (followingIds.isNotEmpty()) {
                    val storiesSnapshot = db.collection("stories")
                        .whereIn("userId", followingIds)
                        .whereGreaterThan("expiresAt", Date())
                        .get()
                        .await()

                    // Group stories by userId
                    val storiesByUserId = storiesSnapshot.documents.mapNotNull { it.toObject<StoryModel>() }
                        .groupBy { it.userId }

                    // Fetch user details for each user who has stories
                    storiesByUserId.keys.mapNotNull { userId ->
                        val userDoc = db.collection("users").document(userId!!).get().await()
                        val user = userDoc.toObject<UserModel>()
                        user?.let {
                            // Check if at least one story is unseen
                            val viewedStoriesSnapshot = db.collection("users").document(currentUserId)
                                .collection("viewed_stories").get().await()
                            val viewedStoryIds = viewedStoriesSnapshot.documents.map { it.id }.toSet()

                            val hasUnseen = storiesByUserId[userId]?.any { !viewedStoryIds.contains(it.storyId) } == true
                            StoryPreview(user = it, hasUnseenStories = hasUnseen)
                        }
                    }
                } else {
                    emptyList()
                }

                // 3. Check if the current user has an active story
                val myStoriesSnapshot = db.collection("stories")
                    .whereEqualTo("userId", currentUserId)
                    .whereGreaterThan("expiresAt", Date())
                    .limit(1)
                    .get()
                    .await()
                val hasMyStory = !myStoriesSnapshot.isEmpty

                // 4. Update the state with the final list
                _storyFeedState.value = StoryFeedState.Success(storyPreviews, hasMyStory)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stories feed", e)
                _storyFeedState.value = StoryFeedState.Error("Failed to load stories: ${e.message}")
            }
        }
    }
}
