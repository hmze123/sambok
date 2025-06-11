package com.spidroid.starry.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.repositories.UserRepository
import com.spidroid.starry.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _loginResult = MutableLiveData<Resource<AuthResult?>>()
    val loginResult: LiveData<Resource<AuthResult?>> = _loginResult

    private val _resetPasswordResult = MutableLiveData<Resource<Unit?>>()
    val resetPasswordResult: LiveData<Resource<Unit?>> = _resetPasswordResult

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            // ١. استخدام الطريقة الصحيحة لإنشاء الحالات
            _loginResult.value = Resource.loading(null)
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _loginResult.value = Resource.success(result)
            } catch (e: Exception) {
                _loginResult.value = Resource.error(e.message ?: "An unknown error occurred.", null)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _resetPasswordResult.value = Resource.loading(null)
            try {
                auth.sendPasswordResetEmail(email).await()
                _resetPasswordResult.value = Resource.success(Unit)
            } catch (e: Exception) {
                _resetPasswordResult.value = Resource.error(e.message ?: "Failed to send reset link.", null)
            }
        }
    }
}