package com.spidroid.starry.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.spidroid.starry.R;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.repositories.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchViewModel extends ViewModel {
    private final UserRepository userRepo = new UserRepository();
    private final MutableLiveData<List<UserModel>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private String lastQuery = "";

    public void searchUsers(String query) {
        if (query.isEmpty()) return;
        lastQuery = query;
        userRepo.searchUsers(query)
            .addOnSuccessListener(searchResults::postValue)
            .addOnFailureListener(e -> errorMessage.postValue(e.getMessage()));
    }

    public Task<Void> toggleFollowStatus(UserModel user) {
        return userRepo.toggleFollow(
            FirebaseAuth.getInstance().getCurrentUser().getUid(),
            user.getUserId()
        );
    }

    public void retryLastSearch() {
        if (!lastQuery.isEmpty()) searchUsers(lastQuery);
    }

    public LiveData<List<UserModel>> getSearchResults() { return searchResults; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
}