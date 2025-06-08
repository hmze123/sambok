package com.spidroid.starry.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spidroid.starry.models.PageModel
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.repositories.PageRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PageViewModel : ViewModel() {

    private val pageRepository = PageRepository()

    private val _pageDetails = MutableLiveData<PageModel>()
    val pageDetails: LiveData<PageModel> = _pageDetails

    private val _pagePosts = MutableLiveData<List<PostModel>>()
    val pagePosts: LiveData<List<PostModel>> = _pagePosts

    private val _uiState = MutableLiveData<UiState<PageModel>>()
    val uiState: LiveData<UiState<PageModel>> = _uiState

    fun fetchPageData(pageId: String) {
        if (pageId.isBlank()) {
            _uiState.value = UiState.Error("Invalid Page ID")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val pageDetailsTask = pageRepository.getPageById(pageId)
                val pagePostsTask = pageRepository.getPostsForPage(pageId)

                val page = pageDetailsTask.await().toObject(PageModel::class.java)
                val posts = pagePostsTask.await().toObjects(PostModel::class.java)

                if (page != null) {
                    _pageDetails.postValue(page!!)
                    _pagePosts.postValue(posts)
                    _uiState.postValue(UiState.SuccessWithData(page))
                } else {
                    _uiState.postValue(UiState.Error("Page not found."))
                }
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error(e.message ?: "Failed to load page data."))
            }
        }
    }
}