// المسار: app/src/main/java/com/spidroid/starry/viewmodels/BookmarksViewModel.kt

package com.spidroid.starry.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.repositories.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _bookmarkedPosts = MutableLiveData<List<PostModel>>()
    val bookmarkedPosts: LiveData<List<PostModel>> = _bookmarkedPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadBookmarkedPosts() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // استدعاء الدالة الجديدة التي سنضيفها إلى الـ Repository
                val result = postRepository.getBookmarkedPosts(userId)
                _bookmarkedPosts.postValue(result)
            } catch (e: Exception) {
                _error.postValue("Failed to load bookmarks: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}