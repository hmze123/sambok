package com.spidroid.starry.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Sealed class لتمثيل جميع حالات واجهة البحث الممكنة
sealed class SearchUiState {
    object Idle : SearchUiState() // الحالة الأولية، عند عدم وجود بحث
    object Loading : SearchUiState() // حالة التحميل
    data class Success(val users: List<UserModel>) : SearchUiState() // حالة النجاح مع قائمة المستخدمين
    data class Error(val message: String) : SearchUiState() // حالة حدوث خطأ
    object Empty : SearchUiState() // حالة عدم العثور على نتائج
}

class SearchViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val auth = Firebase.auth

    // StateFlow لإدارة حالة الواجهة بشكل فعال
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    private var lastQuery = ""

    fun searchUsers(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        lastQuery = trimmedQuery
        _uiState.value = SearchUiState.Loading

        viewModelScope.launch {
            try {
                // استخدام await() لجعل الكود يبدو متزامنًا وأكثر نظافة
                val results = userRepository.searchUsers(trimmedQuery).await()
                if (results.isNotEmpty()) {
                    _uiState.value = SearchUiState.Success(results)
                } else {
                    _uiState.value = SearchUiState.Empty
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun toggleFollowStatus(user: UserModel) {
        val currentUserId = auth.currentUser?.uid ?: return
        val targetUserId = user.userId ?: return

        viewModelScope.launch {
            try {
                userRepository.toggleFollow(currentUserId, targetUserId).await()
                // الواجهة يمكن تحديثها بشكل متفائل في الـ Fragment
                // أو يمكننا إعادة تحميل البحث هنا إذا لزم الأمر
            } catch (e: Exception) {
                // يمكن عرض رسالة خطأ للمستخدم
                _uiState.value = SearchUiState.Error("Failed to update follow status: ${e.message}")
            }
        }
    }

    fun retryLastSearch() {
        if (lastQuery.isNotEmpty()) {
            searchUsers(lastQuery)
        }
    }

    // دالة لمسح البحث والعودة للحالة الأولية
    fun clearSearch() {
        lastQuery = ""
        _uiState.value = SearchUiState.Idle
    }
}
