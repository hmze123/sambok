package com.spidroid.starry.viewmodels

// Sealed class to represent UI states
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    object Success : UiState<Nothing>()
    data class SuccessWithData<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}