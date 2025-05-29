package com.spidroid.starry.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.spidroid.starry.models.UserModel;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<UserModel> userData = new MutableLiveData<>();

    public void clearUserData() {
        userData.setValue(null);
    }

    public LiveData<UserModel> getUserData() {
        return userData;
    }
}