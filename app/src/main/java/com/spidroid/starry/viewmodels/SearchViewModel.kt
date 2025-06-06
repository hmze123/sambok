package com.spidroid.starry.viewmodels

import com.google.firebase.auth.FirebaseAuth

class SearchViewModel : ViewModel() {
    private val userRepo: UserRepository = UserRepository()
    private val searchResults: MutableLiveData<kotlin.collections.MutableList<UserModel?>?> =
        MutableLiveData<kotlin.collections.MutableList<UserModel?>?>()
    private val errorMessage: MutableLiveData<kotlin.String?> = MutableLiveData<kotlin.String?>()
    private var lastQuery = ""

    fun searchUsers(query: kotlin.String) {
        if (query.isEmpty()) return
        lastQuery = query
        userRepo.searchUsers(query)
            .addOnSuccessListener(OnSuccessListener { value: kotlin.collections.MutableList<UserModel?>? ->
                searchResults.postValue(
                    value
                )
            })
            .addOnFailureListener(OnFailureListener { e: java.lang.Exception? ->
                errorMessage.postValue(
                    e!!.message
                )
            })
    }

    fun toggleFollowStatus(user: UserModel): com.google.android.gms.tasks.Task<java.lang.Void?>? {
        return userRepo.toggleFollow(
            FirebaseAuth.getInstance().getCurrentUser().getUid(),
            user.getUserId()
        )
    }

    fun retryLastSearch() {
        if (!lastQuery.isEmpty()) searchUsers(lastQuery)
    }

    fun getSearchResults(): LiveData<kotlin.collections.MutableList<UserModel?>?> {
        return searchResults
    }

    fun getErrorMessage(): LiveData<kotlin.String?> {
        return errorMessage
    }
}