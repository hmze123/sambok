package com.spidroid.starry.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.spidroid.starry.R;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.viewmodels.SearchViewModel;
import java.util.Collections;

public class SearchFragment extends Fragment
    implements UserResultAdapter.OnUserInteractionListener {

  private SearchViewModel viewModel;
  private UserResultAdapter adapter;
  private Handler searchHandler = new Handler();
  private Runnable searchRunnable;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_search, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
    setupViews(view);
    setupObservers();
  }

  private void setupViews(View view) {
    EditText searchInput = view.findViewById(R.id.et_search);
    RecyclerView resultsRecycler = view.findViewById(R.id.rv_results);
    ImageView clearButton = view.findViewById(R.id.iv_clear);

    adapter = new UserResultAdapter(getCurrentUserId(), this);
    resultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    resultsRecycler.setAdapter(adapter);

    searchInput.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
          }

          @Override
          public void afterTextChanged(Editable s) {
            searchHandler.removeCallbacks(searchRunnable);
            searchRunnable = () -> viewModel.searchUsers(s.toString().trim());
            searchHandler.postDelayed(searchRunnable, 400);
          }
        });

    clearButton.setOnClickListener(
        v -> {
          searchInput.setText("");
          adapter.updateUsers(Collections.emptyList());
        });
  }

  private void setupObservers() {
    viewModel
        .getSearchResults()
        .observe(
            getViewLifecycleOwner(),
            users -> {
              if (users.isEmpty()) showEmptyState();
              else adapter.updateUsers(users);
            });

    viewModel
        .getErrorMessage()
        .observe(
            getViewLifecycleOwner(),
            error -> {
              if (error != null) showError(error);
            });
  }

  // Implement interface methods
  @Override
  public void onFollowClicked(UserModel user, int position) {
    viewModel
        .toggleFollowStatus(user)
        .addOnCompleteListener(
            task -> {
              if (!task.isSuccessful()) {
                showError("Failed to update follow status");
                adapter.notifyItemChanged(position);
              }
            });
  }

  @Override
  public void onUserClicked(UserModel user) {
    navigateToProfile(user);
  }

  @Override
  public void onMoreClicked(UserModel user, View anchor) {
    showContextMenu(user, anchor);
  }

  private String getCurrentUserId() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    return user != null ? user.getUid() : "";
  }

  private void showError(String message) {
    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        .setAction("Retry", v -> viewModel.retryLastSearch())
        .show();
  }

  private void navigateToProfile(UserModel user) {}

  private void showContextMenu(UserModel user, View anchor) {
    // Implement context menu logic
  }

  private void showEmptyState() {
    // Example implementation
    View view = getView();
    if (view != null) {
      TextView emptyText = view.findViewById(R.id.tv_empty_state);
      RecyclerView resultsList = view.findViewById(R.id.rv_results);

      if (emptyText != null && resultsList != null) {
        emptyText.setVisibility(View.VISIBLE);
        resultsList.setVisibility(View.GONE);
      }
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    searchHandler.removeCallbacks(searchRunnable);
  }
}
