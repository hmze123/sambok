package com.spidroid.starry.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.PostRepository
import com.spidroid.starry.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

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
                // Note: The repository methods searchUsers and searchPostsByContent
                // might need adjustments to fit the refactored repositories.
                // This code assumes they return Tasks as in the original ViewModel.
                val usersTask = userRepository.searchUsers(query)
                val postsTask = postRepository.searchPostsByContent(query)

                // انتظار نتائج المهمتين
                val users = usersTask.await().toObjects(UserModel::class.java) // Assuming searchUsers returns a Task<QuerySnapshot>
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