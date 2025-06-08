// In: app/src/main/java/com/spidroid/starry/viewmodels/CommunityViewModel.kt
package com.spidroid.starry.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.spidroid.starry.repositories.CommunityRepository
import kotlinx.coroutines.Dispatchers

class CommunityViewModel : ViewModel() {
    private val repository = CommunityRepository()

    fun createCommunity(name: String, description: String, isPublic: Boolean, ownerId: String) = liveData(Dispatchers.IO) {
        emit(UiState.Loading)
        val result = repository.createCommunity(name, description, isPublic, ownerId)
        result.onSuccess { communityId ->
            // <-- إصلاح: استخدام SuccessWithData لتمرير البيانات
            emit(UiState.SuccessWithData(communityId))
        }.onFailure {
            emit(UiState.Error(it.message ?: "Unknown Error"))
        }
    }
}