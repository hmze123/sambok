package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.PostRepository
import com.spidroid.starry.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// لا حاجة لتعريف UiState هنا، سنستخدم LiveData مباشرة للحالات

class SearchViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val postRepository = PostRepository()
    private val auth = Firebase.auth

    // LiveData لنتائج بحث المستخدمين
    private val _userResults = MutableLiveData<List<UserModel>>()
    val userResults: LiveData<List<UserModel>> = _userResults

    // LiveData لنتائج بحث المنشورات
    private val _postResults = MutableLiveData<List<PostModel>>()
    val postResults: LiveData<List<PostModel>> = _postResults

    // LiveData لحالة التحميل
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun performSearch(query: String) {
        if (query.isBlank()) {
            _userResults.value = emptyList()
            _postResults.value = emptyList()
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // البحث عن المستخدمين والمنشورات في نفس الوقت
                val usersTask = userRepository.searchUsers(query)
                val postsTask = postRepository.searchPostsByContent(query)
                // يمكن إضافة بحث الهاشتاجات هنا بنفس الطريقة

                val users = usersTask.await()
                val posts = postsTask.await()?.toObjects(PostModel::class.java)

                _userResults.postValue(users)
                _postResults.postValue(posts ?: emptyList())

            } catch (e: Exception) {
                Log.e("SearchViewModel", "Search failed", e)
                _error.postValue("Search failed: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}