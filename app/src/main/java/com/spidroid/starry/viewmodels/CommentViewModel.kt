package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.models.CommentModel
import com.spidroid.starry.repositories.CommentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Sealed class لتمثيل حالات واجهة عرض التعليقات
sealed class CommentUiState {
    object Loading : CommentUiState()
    data class Success(val comments: List<CommentModel>) : CommentUiState()
    data class Error(val message: String) : CommentUiState()
}

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val repository: CommentRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _commentState = MutableLiveData<CommentUiState>()
    val commentState: LiveData<CommentUiState> = _commentState

    private var allComments: List<CommentModel> = listOf()
    private val expandedComments = mutableSetOf<String>()

    fun loadComments(postId: String?) {
        if (postId.isNullOrEmpty()) {
            _commentState.value = CommentUiState.Error("Invalid Post ID")
            return
        }

        _commentState.value = CommentUiState.Loading

        viewModelScope.launch {
            try {
                // Assuming getCommentsForPost returns a QuerySnapshot
                val commentsSnapshot = repository.getCommentsForPost(postId).await()
                val fetchedComments = commentsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(CommentModel::class.java)?.apply {
                        commentId = doc.id
                        // isLiked = likes.containsKey(auth.currentUser?.uid)
                    }
                }
                allComments = fetchedComments
                buildAndDisplayVisibleList()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading comments for post $postId", e)
                _commentState.value = CommentUiState.Error("Failed to load comments: ${e.message}")
            }
        }
    }

    fun addComment(postId: String?, commentData: Map<String, Any>) {
        if (postId.isNullOrEmpty()) return
        viewModelScope.launch {
            try {
                repository.addComment(postId, commentData).await()
                repository.updatePostReplyCount(postId, 1).await()
                loadComments(postId) // Reload comments to show the new one
            } catch (e: Exception) {
                Log.e(TAG, "Error adding comment", e)
                // Optionally, post an error event to the UI
            }
        }
    }

    fun toggleLike(postId: String?, comment: CommentModel) {
        val userId = auth.currentUser?.uid ?: return
        val commentId = comment.commentId ?: return
        val safePostId = postId ?: return

        // Optimistically update the UI
        val isNowLiked = !comment.isLiked
        comment.isLiked = isNowLiked
        comment.likeCount += if (isNowLiked) 1 else -1
        updateCommentInList(comment)

        // Update in Firestore
        viewModelScope.launch {
            try {
                repository.toggleCommentLike(safePostId, commentId, userId, isNowLiked).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle like for comment $commentId", e)
                // Revert optimistic update on failure
                comment.isLiked = !isNowLiked
                comment.likeCount += if (isNowLiked) -1 else 1
                updateCommentInList(comment)
            }
        }
    }

    fun deleteComment(postId: String?, comment: CommentModel) {
        val commentId = comment.commentId ?: return
        val safePostId = postId ?: return

        viewModelScope.launch {
            try {
                repository.deleteComment(safePostId, commentId).await()
                repository.updatePostReplyCount(safePostId, -1).await()
                loadComments(safePostId) // Reload to reflect deletion
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete comment $commentId", e)
            }
        }
    }

    fun toggleReplies(parentComment: CommentModel) {
        val parentId = parentComment.commentId ?: return
        if (expandedComments.contains(parentId)) {
            expandedComments.remove(parentId)
        } else {
            expandedComments.add(parentId)
        }
        buildAndDisplayVisibleList()
    }

    private fun buildAndDisplayVisibleList() {
        val replyMap = allComments.filter { !it.isTopLevel }.groupBy { it.parentCommentId }
        val topLevelComments = allComments.filter { it.isTopLevel }

        val visibleList = mutableListOf<CommentModel>()
        for (comment in topLevelComments) {
            visibleList.add(comment)
            if (expandedComments.contains(comment.commentId)) {
                addRepliesRecursively(comment, replyMap, visibleList, 1)
            }
        }
        _commentState.value = CommentUiState.Success(visibleList)
    }

    private fun addRepliesRecursively(
        parent: CommentModel,
        replyMap: Map<String?, List<CommentModel>>,
        list: MutableList<CommentModel>,
        depth: Int
    ) {
        val replies = replyMap[parent.commentId] ?: return
        for (reply in replies) {
            reply.depth = depth
            list.add(reply)
            if (expandedComments.contains(reply.commentId)) {
                addRepliesRecursively(reply, replyMap, list, depth + 1)
            }
        }
    }

    private fun updateCommentInList(updatedComment: CommentModel) {
        val currentState = _commentState.value
        if (currentState is CommentUiState.Success) {
            val updatedList = currentState.comments.map {
                if (it.commentId == updatedComment.commentId) updatedComment else it
            }
            _commentState.value = CommentUiState.Success(updatedList)
        }
    }

    companion object {
        private const val TAG = "CommentViewModel"
    }
}