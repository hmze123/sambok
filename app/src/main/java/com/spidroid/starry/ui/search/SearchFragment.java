package com.spidroid.starry.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.spidroid.starry.R;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.utils.SearchHistoryManager;
import com.spidroid.starry.viewmodels.SearchViewModel;
import java.util.Collections;
import java.util.List;

public class SearchFragment extends Fragment
        implements UserResultAdapter.OnUserInteractionListener, RecentSearchAdapter.OnHistoryInteractionListener {

    private SearchViewModel viewModel;
    private UserResultAdapter userResultAdapter;
    private RecentSearchAdapter recentSearchAdapter;
    private SearchHistoryManager searchHistoryManager;
    private EditText searchInput;
    private RecyclerView rvResults, rvRecentSearches;
    private LinearLayout layoutRecentSearches;
    private TextView tvEmptyState;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        searchHistoryManager = new SearchHistoryManager(requireContext());

        initializeViews(view);
        setupRecyclerViews();
        setupSearchInput();
        setupObservers();

        showInitialState();
    }

    private void initializeViews(View view) {
        searchInput = view.findViewById(R.id.et_search);
        rvResults = view.findViewById(R.id.rv_results);
        rvRecentSearches = view.findViewById(R.id.rv_recent_searches);
        layoutRecentSearches = view.findViewById(R.id.layout_recent_searches);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        ImageView clearButton = view.findViewById(R.id.iv_clear);
        clearButton.setOnClickListener(v -> searchInput.setText(""));
    }

    private void setupRecyclerViews() {
        // Adapter لنتائج البحث
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        userResultAdapter = new UserResultAdapter(currentUserId, this);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(userResultAdapter);

        // Adapter لسجل البحث
        rvRecentSearches.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupSearchInput() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.removeCallbacks(searchRunnable);
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    showInitialState();
                } else {
                    layoutRecentSearches.setVisibility(View.GONE);
                    rvResults.setVisibility(View.VISIBLE);

                    searchRunnable = () -> {
                        viewModel.searchUsers(query);
                        searchHistoryManager.addSearchTerm(query);
                    };
                    searchHandler.postDelayed(searchRunnable, 400); // Debounce
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), users -> {
            userResultAdapter.updateUsers(users);
            tvEmptyState.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> viewModel.retryLastSearch())
                        .show();
            }
        });
    }

    private void showInitialState() {
        List<String> history = searchHistoryManager.getSearchHistory();
        if (history.isEmpty()) {
            layoutRecentSearches.setVisibility(View.GONE);
        } else {
            recentSearchAdapter = new RecentSearchAdapter(history, this);
            rvRecentSearches.setAdapter(recentSearchAdapter);
            layoutRecentSearches.setVisibility(View.VISIBLE);
        }
        rvResults.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);
        userResultAdapter.updateUsers(Collections.emptyList());
    }

    // --- OnHistoryInteractionListener ---
    @Override
    public void onTermClicked(String term) {
        searchInput.setText(term);
        searchInput.setSelection(term.length());
    }

    @Override
    public void onRemoveClicked(String term) {
        searchHistoryManager.removeSearchTerm(term);
        showInitialState(); // Refresh history list
    }

    // --- OnUserInteractionListener ---
    @Override public void onFollowClicked(UserModel user, int position) { /* ... */ }
    @Override public void onUserClicked(UserModel user) { /* ... */ }
    @Override public void onMoreClicked(UserModel user, View anchor) { /* ... */ }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchHandler.removeCallbacks(searchRunnable);
    }
}