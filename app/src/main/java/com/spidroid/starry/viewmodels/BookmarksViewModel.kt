package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.repositories.PostRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // --- هذا السطر مهم جدًا ---

class BookmarksViewModel : ViewModel() {

    private val postRepository = PostRepository()
    private val auth = Firebase.auth

    private val _bookmarkedPosts = MutableLiveData<List<PostModel>>()
    val bookmarkedPosts: LiveData<List<PostModel>> = _bookmarkedPosts

    private val _uiState = MutableLiveData<UiState<Nothing>>()
    val uiState: LiveData<UiState<Nothing>> = _uiState

    fun fetchBookmarkedPosts() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = UiState.Error("User not authenticated")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                // await() ستعمل الآن بسبب وجود السطر import kotlinx.coroutines.tasks.await
                val snapshot = postRepository.getBookmarkedPosts(userId).await()

                // toObject() ستعمل الآن لأن snapshot أصبح من النوع الصحيح
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PostModel::class.java)?.apply {
                        // id و postId سيعملان الآن
                        postId = doc.id
                    }
                }
                _bookmarkedPosts.postValue(posts)
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                Log.e("BookmarksViewModel", "Failed to fetch bookmarks", e)
                _uiState.value = UiState.Error("Failed to load bookmarks: ${e.message}")
            }
        }
    }
}