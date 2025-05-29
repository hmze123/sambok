package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayoutMediator;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ActivityProfileBinding;
import com.spidroid.starry.fragments.ProfilePostsFragment;
import com.spidroid.starry.fragments.ProfileRepliesFragment;
import com.spidroid.starry.fragments.ProfileMediaFragment;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.activities.MediaViewerActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
  private ActivityProfileBinding binding;
  private FirebaseFirestore db;
  private FirebaseAuth auth;
  private String userId;
  private boolean isCurrentUser;
  private boolean isFollowing;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityProfileBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    db = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    userId = getIntent().getStringExtra("userId");
    isCurrentUser = userId.equals(auth.getCurrentUser().getUid());

    setupUI();
    loadUserData();
    setupTabs();

    binding.appbarLayout.addOnOffsetChangedListener(
        new AppBarLayout.OnOffsetChangedListener() {
          float startAvatarSize = 1f;
          float minAvatarScale = 0.01f; // Minimum 60% of original size
          int totalScrollRange = -1;

          @Override
          public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            if (totalScrollRange == -1) {
              totalScrollRange = appBarLayout.getTotalScrollRange();
            }

            float offsetFactor = (float) Math.abs(verticalOffset) / totalScrollRange;
            float scale = 1f - (offsetFactor * (1f - minAvatarScale));
            scale = Math.max(minAvatarScale, scale);

            binding.ivAvatar.setScaleX(scale);
            binding.ivAvatar.setScaleY(scale);
          }
        });
  }

  private void setupUI() {
    binding.btnBack.setOnClickListener(v -> finish());
    binding.btnSettings.setOnClickListener(v -> startSettings());

    if (isCurrentUser) {
      setupEditProfileButton();
    } else {
      setupFollowButton();
    }
  }

  private void setupEditProfileButton() {
    binding.btnAction.setIcon(null);
    binding.btnAction.setText(R.string.edit_profile);
    binding.btnAction.setStrokeColorResource(R.color.text_secondary);
    binding.btnAction.setOnClickListener(v -> startEditProfile());
  }

  private void setupFollowButton() {
    binding.btnAction.setIcon(null);
    binding.btnAction.setText(R.string.follow);
    binding.btnAction.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
    binding.btnAction.setStrokeColorResource(android.R.color.transparent);
    binding.btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.twitter_primary));
    binding.btnAction.setOnClickListener(v -> toggleFollow());
    checkFollowingStatus();
  }

  private void loadUserData() {
    db.collection("users")
        .document(userId)
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              UserModel user = documentSnapshot.toObject(UserModel.class);
              if (user != null) {
                updateUI(user);
              }
            });
  }

  private void updateUI(UserModel user) {
    Glide.with(this)
        .load(user.getProfileImageUrl())
        .placeholder(R.drawable.ic_default_avatar)
        .into(binding.ivAvatar);

    Glide.with(this)
        .load(user.getCoverImageUrl())
        .placeholder(R.drawable.ic_cover_placeholder)
        .into(binding.ivCover);

    // Profile image click
    binding.ivAvatar.setOnClickListener(
        v -> {
          if (isValidUrl(user.getProfileImageUrl())) {
            ArrayList<String> urls = new ArrayList<>();
            urls.add(user.getProfileImageUrl());
            MediaViewerActivity.launch(this, urls, 0, binding.ivAvatar);
          }
        });

    // Cover image click
    binding.ivCover.setOnClickListener(
        v -> {
          if (isValidUrl(user.getCoverImageUrl())) {
            ArrayList<String> urls = new ArrayList<>();
            urls.add(user.getCoverImageUrl());
            MediaViewerActivity.launch(this, urls, 0, binding.ivCover);
          }
        });

    binding.tvDisplayName.setText(user.getDisplayName());
    binding.tvUsername.setText("@" + user.getUsername());
    binding.tvBio.setText(user.getBio());
    binding.ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);

    binding.tvFollowersCount.setText(formatNumber(user.getFollowers().size()));
    binding.tvFollowingCount.setText(formatNumber(user.getFollowing().size()));

    setupSocialLinks(user.getSocialLinks());
  }

  private boolean isValidUrl(String url) {
    return url != null && !url.isEmpty() && (url.startsWith("http") || url.startsWith("content"));
  }

  private void setupSocialLinks(Map<String, String> socialLinks) {
    if (socialLinks == null || socialLinks.isEmpty()) {
      binding.layoutSocial.setVisibility(View.GONE);
      return;
    }

    binding.layoutSocial.removeAllViews();
    for (Map.Entry<String, String> entry : socialLinks.entrySet()) {
      ImageView icon = new ImageView(this);
      int iconRes = getSocialIcon(entry.getKey());
      if (iconRes != -1) {
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
        icon.setOnClickListener(v -> openUrl(entry.getValue()));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        params.setMarginEnd(dpToPx(8));
        binding.layoutSocial.addView(icon, params);
      }
    }
    binding.layoutSocial.setVisibility(View.VISIBLE);
  }

  private int getSocialIcon(String platform) {
    switch (platform.toLowerCase()) {
      case "twitter":
        return R.drawable.ic_twitter;
      case "instagram":
        return R.drawable.ic_instagram;
      case "facebook":
        return R.drawable.ic_facebook;
      case "linkedin":
        return R.drawable.ic_linkedin;
      default:
        return -1;
    }
  }

  private void setupTabs() {
    ProfilePagerAdapter pagerAdapter = new ProfilePagerAdapter(this, userId);
    binding.viewPager.setAdapter(pagerAdapter);

    new TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager,
            (tab, position) -> tab.setText(pagerAdapter.tabTitles[position]))
        .attach();
}

  private class ProfilePagerAdapter extends FragmentStateAdapter {
    private final String[] tabTitles = new String[] {"Posts", "Replies", "Media"};
    private final String userId;

    public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity, String userId) {
      super(fragmentActivity);
      this.userId = userId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      switch (position) {
        case 0:
          return new ProfilePostsFragment(userId);
        case 1:
          return new ProfileRepliesFragment();
        case 2:
          return new ProfileMediaFragment();
        default:
          throw new IllegalArgumentException();
      }
    }

    @Override
    public int getItemCount() {
      return tabTitles.length;
    }
  }

  private String formatNumber(int number) {
    if (number > 1000000) return String.format("%.1fM", number / 1000000f);
    if (number > 1000) return String.format("%.1fK", number / 1000f);
    return String.valueOf(number);
  }

  private int dpToPx(int dp) {
    return (int) (dp * getResources().getDisplayMetrics().density);
  }

  private void toggleFollow() {
    DocumentReference currentUserRef =
        db.collection("users").document(auth.getCurrentUser().getUid());
    DocumentReference targetUserRef = db.collection("users").document(userId);

    if (isFollowing) {
      // Unfollow
      currentUserRef.update("following." + userId, null);
      targetUserRef.update("followers." + auth.getCurrentUser().getUid(), null);
      binding.btnAction.setText(R.string.follow);
      isFollowing = false;
    } else {
      // Follow
      currentUserRef.update("following." + userId, true);
      targetUserRef.update("followers." + auth.getCurrentUser().getUid(), true);
      binding.btnAction.setText(R.string.following);
      isFollowing = true;
    }
  }

  private void checkFollowingStatus() {
    db.collection("users")
        .document(auth.getCurrentUser().getUid())
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              Object followingData = documentSnapshot.get("following");
              if (followingData instanceof Map) {
                Map<String, Object> following = (Map<String, Object>) followingData;
                if (following.containsKey(userId)) {
                  isFollowing = true;
                  binding.btnAction.setText(R.string.following);
                }
              } else if (followingData instanceof java.util.List) {
                // Optional: support if your app accidentally saved as a List
                java.util.List<?> followingList = (java.util.List<?>) followingData;
                if (followingList.contains(userId)) {
                  isFollowing = true;
                  binding.btnAction.setText(R.string.following);
                }
              }
            });
  }

  private void startEditProfile() {
    Intent intent = new Intent(this, EditProfileActivity.class);
    intent.putExtra("userId", userId);
    startActivity(intent);
  }

  private void startSettings() {
    Intent intent = new Intent(this, SettingsActivity.class);
    intent.putExtra("userId", userId);
    startActivity(intent);
  }

  private void openUrl(String url) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(intent);
  }
}
