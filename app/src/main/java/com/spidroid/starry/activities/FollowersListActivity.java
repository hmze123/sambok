package com.spidroid.starry.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.UserAdapter;
import com.spidroid.starry.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors; // استيراد Collectors

public class FollowersListActivity extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_LIST_TYPE = "list_type"; // "followers" or "following"

    private FirebaseFirestore db;
    private String userId;
    private String listType; // نوع القائمة التي سيتم عرضها
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        listType = getIntent().getStringExtra(EXTRA_LIST_TYPE);

        if (userId == null || listType == null) {
            Toast.makeText(this, "Invalid user or list type.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchUsers();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.user_list_recycler_view);
        progressBar = findViewById(R.id.user_list_progress_bar);
        tvEmptyState = findViewById(R.id.user_list_empty_state);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if ("followers".equals(listType)) {
            getSupportActionBar().setTitle("Followers");
        } else if ("following".equals(listType)) {
            getSupportActionBar().setTitle("Following");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(this); // تهيئة المحول
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(userAdapter);
    }

    private void fetchUsers() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user != null) {
                        List<String> userIdsToFetch = new ArrayList<>();
                        if ("followers".equals(listType)) {
                            userIdsToFetch.addAll(user.getFollowers().keySet());
                        } else if ("following".equals(listType)) {
                            userIdsToFetch.addAll(user.getFollowing().keySet());
                        }

                        if (userIdsToFetch.isEmpty()) {
                            userAdapter.submitList(new ArrayList<>());
                            tvEmptyState.setText("No " + listType + " found.");
                            tvEmptyState.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        // Firestore whereIn supports up to 10 values.
                        // For larger lists, you'd need to split into chunks and make multiple queries.
                        List<String> limitedUserIds = userIdsToFetch.stream().limit(10).collect(Collectors.toList());

                        db.collection("users").whereIn("userId", limitedUserIds).get()
                                .addOnSuccessListener(querySnapshot -> {
                                    List<UserModel> fetchedUsers = querySnapshot.toObjects(UserModel.class);
                                    userAdapter.submitList(fetchedUsers);
                                    if (fetchedUsers.isEmpty()) {
                                        tvEmptyState.setText("No " + listType + " found.");
                                        tvEmptyState.setVisibility(View.VISIBLE);
                                    } else {
                                        tvEmptyState.setVisibility(View.GONE);
                                    }
                                    progressBar.setVisibility(View.GONE);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FollowListActivity", "Error fetching users: " + e.getMessage());
                                    Toast.makeText(this, "Failed to load users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    tvEmptyState.setText("Failed to load " + listType + ".");
                                    tvEmptyState.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.GONE);
                                });
                    } else {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                        tvEmptyState.setText("User profile not found.");
                        tvEmptyState.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowListActivity", "Error fetching user profile: " + e.getMessage());
                    Toast.makeText(this, "Failed to load user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvEmptyState.setText("Failed to load user profile.");
                    tvEmptyState.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public void onUserClick(UserModel user) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra("userId", user.getUserId());
        startActivity(intent);
    }
}