package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.spidroid.starry.models.CommentModel
import com.spidroid.starry.models.PostModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // StateFlow للمنشورات
    private val _postsState = MutableStateFlow<ProfilePostState>(ProfilePostState.Loading)
    val postsState: StateFlow<ProfilePostState> = _postsState

    // StateFlow جديد للردود (التعليقات)
    private val _repliesState = MutableStateFlow<ProfileRepliesState>(ProfileRepliesState.Loading)
    val repliesState: StateFlow<ProfileRepliesState> = _repliesState

    // StateFlow جديد للوسائط
    private val _mediaState = MutableStateFlow<ProfileMediaState>(ProfileMediaState.Loading)
    val mediaState: StateFlow<ProfileMediaState> = _mediaState


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

    // دالة جديدة لجلب ردود المستخدم
    fun fetchRepliesForUser(userId: String) {
        _repliesState.value = ProfileRepliesState.Loading
        viewModelScope.launch {
            try {
                // استعلام لجلب كل التعليقات التي كتبها المستخدم
                val repliesSnapshot = db.collectionGroup("comments")
                    .whereEqualTo("authorId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50) // حد مبدئي
                    .get()
                    .await()

                val fetchedReplies = repliesSnapshot.documents.mapNotNull { it.toObject(CommentModel::class.java) }

                if (fetchedReplies.isEmpty()) {
                    _repliesState.value = ProfileRepliesState.Empty
                } else {
                    _repliesState.value = ProfileRepliesState.Success(fetchedReplies)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching replies for user $userId", e)
                _repliesState.value = ProfileRepliesState.Error("Failed to load replies: ${e.message}")
            }
        }
    }

    // دالة جديدة لجلب منشورات الوسائط
    fun fetchMediaForUser(userId: String) {
        _mediaState.value = ProfileMediaState.Loading
        viewModelScope.launch {
            try {
                // استعلام لجلب المنشورات التي تحتوي على وسائط (صور أو فيديو)
                val mediaSnapshot = db.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .whereIn("contentType", listOf(PostModel.TYPE_IMAGE, PostModel.TYPE_VIDEO))
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val fetchedMediaPosts = mediaSnapshot.documents.mapNotNull { it.toObject(PostModel::class.java) }

                if (fetchedMediaPosts.isEmpty()) {
                    _mediaState.value = ProfileMediaState.Empty
                } else {
                    _mediaState.value = ProfileMediaState.Success(fetchedMediaPosts)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching media for user $userId", e)
                _mediaState.value = ProfileMediaState.Error("Failed to load media: ${e.message}")
            }
        }
    }
}

// Sealed classes جديدة لحالات الردود والوسائط
sealed class ProfileRepliesState {
    object Loading : ProfileRepliesState()
    data class Success(val replies: List<CommentModel>) : ProfileRepliesState()
    data class Error(val message: String) : ProfileRepliesState()
    object Empty : ProfileRepliesState()
}

sealed class ProfileMediaState {
    object Loading : ProfileMediaState()
    data class Success(val mediaPosts: List<PostModel>) : ProfileMediaState()
    data class Error(val message: String) : ProfileMediaState()
    object Empty : ProfileMediaState()
}