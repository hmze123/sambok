package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.spidroid.starry.models.StoryModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

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

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val userRepository: UserRepository
) : ViewModel() {

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

    // --- تم تعديل هذه الدالة بالكامل ---
    fun fetchCurrentUser() {
        auth.currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                try {
                    // استدعاء الدالة الصحيحة من الـ Repository
                    val user = userRepository.getUser(userId)
                    _currentUser.postValue(user)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch current user", e)
                    _currentUser.postValue(null)
                }
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
                val followingSnapshot = db.collection("users").document(currentUserId).get().await()
                val userModel = followingSnapshot.toObject<UserModel>()
                val followingIds = userModel?.following?.keys?.toMutableList() ?: mutableListOf()

                val storyPreviews = mutableListOf<StoryPreview>()
                if (followingIds.isNotEmpty()) {
                    val chunks = followingIds.chunked(10)

                    for (chunk in chunks) {
                        val storiesSnapshot = db.collection("stories")
                            .whereIn("userId", chunk)
                            .whereGreaterThan("expiresAt", Date())
                            .get()
                            .await()

                        val storiesByUserId = storiesSnapshot.documents.mapNotNull { it.toObject<StoryModel>() }
                            .groupBy { it.userId }

                        for (userIdWithStory in storiesByUserId.keys) {
                            val userDoc = db.collection("users").document(userIdWithStory!!).get().await()
                            val user = userDoc.toObject<UserModel>()
                            user?.let {
                                val viewedStoriesSnapshot = db.collection("users").document(currentUserId)
                                    .collection("viewed_stories").get().await()
                                val viewedStoryIds = viewedStoriesSnapshot.documents.map { it.id }.toSet()

                                val hasUnseen = storiesByUserId[userIdWithStory]?.any { !viewedStoryIds.contains(it.storyId) } == true
                                storyPreviews.add(StoryPreview(user = it, hasUnseenStories = hasUnseen))
                            }
                        }
                    }
                }

                val myStoriesSnapshot = db.collection("stories")
                    .whereEqualTo("userId", currentUserId)
                    .whereGreaterThan("expiresAt", Date())
                    .limit(1)
                    .get()
                    .await()
                val hasMyStory = !myStoriesSnapshot.isEmpty

                _storyFeedState.value = StoryFeedState.Success(storyPreviews, hasMyStory)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stories feed", e)
                _storyFeedState.value = StoryFeedState.Error("Failed to load stories: ${e.message}")
            }
        }
    }
}