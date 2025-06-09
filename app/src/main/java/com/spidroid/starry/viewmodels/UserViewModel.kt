package com.spidroid.starry.viewmodels



import androidx.lifecycle.LiveData

import androidx.lifecycle.MutableLiveData

import androidx.lifecycle.ViewModel

import com.spidroid.starry.models.UserModel



class UserViewModel : ViewModel() {

    private val userData = MutableLiveData<UserModel?>()



    fun clearUserData() {

        userData.setValue(null)

    }



    fun getUserData(): LiveData<UserModel?> {

        return userData

    }

}