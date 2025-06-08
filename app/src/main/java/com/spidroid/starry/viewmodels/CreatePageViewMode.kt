package com.spidroid.starry.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spidroid.starry.models.PageModel
import com.spidroid.starry.repositories.PageRepository
import kotlinx.coroutines.launch

class CreatePageViewModel : ViewModel() {

    private val pageRepository = PageRepository()

    private val _pageCreationState = MutableLiveData<UiState<PageModel>>()
    val pageCreationState: LiveData<UiState<PageModel>> = _pageCreationState

    fun createPage(
        pageName: String,
        pageUsername: String,
        category: String,
        description: String?,
        avatarUri: Uri?
    ) {
        _pageCreationState.value = UiState.Loading
        viewModelScope.launch {
            val pageModel = PageModel(
                pageName = pageName,
                pageUsername = pageUsername.lowercase(),
                category = category,
                description = description
            )
            val result = pageRepository.createPage(pageModel, avatarUri)

            result.onSuccess { createdPage ->
                _pageCreationState.postValue(UiState.SuccessWithData(createdPage))
            }.onFailure { error ->
                _pageCreationState.postValue(UiState.Error(error.message ?: "An unknown error occurred"))
            }
        }
    }
}