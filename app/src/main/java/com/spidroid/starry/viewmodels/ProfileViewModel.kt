// app/src/main/java/com/spidroid/starry/viewmodels/ProfileViewModel.kt
package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.spidroid.starry.models.PostModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _postsState = MutableStateFlow<ProfilePostState>(ProfilePostState.Loading)
    val postsState: StateFlow<ProfilePostState> = _postsState

    private companion object {
        private const val TAG = "ProfileViewModel"
    }

    fun fetchPostsForUser(userId: String) {
        _postsState.value = ProfilePostState.Loading
        viewModelScope.launch {
            try {
                val postsSnapshot = db.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val fetchedPosts = postsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(PostModel::class.java)?.apply {
                        // Manually set postId and local interaction states
                        postId = doc.id
                        val currentAuthUserId = auth.currentUser?.uid
                        isLiked = currentAuthUserId != null && likes.containsKey(currentAuthUserId)
                        isBookmarked = currentAuthUserId != null && bookmarks.containsKey(currentAuthUserId)
                        isReposted = currentAuthUserId != null && reposts.containsKey(currentAuthUserId)
                    }
                }

                if (fetchedPosts.isEmpty()) {
                    _postsState.value = ProfilePostState.Empty
                } else {
                    _postsState.value = ProfilePostState.Success(fetchedPosts)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching posts for user $userId", e)
                _postsState.value = ProfilePostState.Error("Failed to load posts: ${e.message}")
            }
        }
    }
}