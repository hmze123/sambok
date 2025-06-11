package com.spidroid.starry.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _currentUser = MutableLiveData<UserModel?>()
    val currentUser: LiveData<UserModel?> = _currentUser

    fun fetchCurrentUser() {
        // جلب بيانات المستخدم الحالي عند الطلب
        auth.currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                // استخدام الدالة الصحيحة من الـ Repository
                val user = userRepository.getUser(userId)
                _currentUser.postValue(user)
            }
        }
    }
}