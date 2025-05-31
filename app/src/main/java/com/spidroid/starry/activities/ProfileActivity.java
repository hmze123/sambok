package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;

import com.google.android.material.tabs.TabLayoutMediator;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ActivityProfileBinding;
import com.spidroid.starry.fragments.ProfileMediaFragment;
import com.spidroid.starry.fragments.ProfilePostsFragment;
import com.spidroid.starry.fragments.ProfileRepliesFragment;
import com.spidroid.starry.models.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {
  private ActivityProfileBinding binding;
  private FirebaseFirestore db;
  private FirebaseAuth auth;
  private String userId;
  private String currentAuthUserId;
  private boolean isCurrentUserProfile;
  private boolean isFollowing;
  private UserModel displayedUserProfile;
  private ListenerRegistration userProfileListener;

  private static final String TAG = "ProfileActivity"; // لإضافة تسجيل

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityProfileBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    db = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    userId = getIntent().getStringExtra("userId");
    if (userId == null || userId.isEmpty()) {
      Toast.makeText(this, getString(R.string.user_id_not_provided), Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    if (auth.getCurrentUser() != null) {
      currentAuthUserId = auth.getCurrentUser().getUid();
      isCurrentUserProfile = userId.equals(currentAuthUserId);
    } else {
      currentAuthUserId = null;
      isCurrentUserProfile = false;
    }

    setupUI();
    setupTabs();
  }

  @Override
  protected void onStart() {
    super.onStart();
    loadUserDataWithListener();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (userProfileListener != null) {
      userProfileListener.remove();
    }
  }

  private void setupUI() {
    setSupportActionBar(binding.toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    }
    binding.btnBack.setOnClickListener(v -> finish());

    if (isCurrentUserProfile) {
      binding.btnAction.setText(R.string.edit_profile);
      binding.btnAction.setIconResource(R.drawable.ic_edit);
      binding.btnAction.setOnClickListener(v ->
              startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class))
      );
      binding.btnSettings.setVisibility(View.VISIBLE);
      binding.btnSettings.setOnClickListener(v ->
              startActivity(new Intent(ProfileActivity.this, SettingsActivity.class))
      );
    } else {
      binding.btnSettings.setVisibility(View.GONE);
      binding.btnAction.setVisibility(View.VISIBLE);
      binding.btnAction.setOnClickListener(v -> toggleFollow());
    }

    final CollapsingToolbarLayout collapsingToolbarLayout = binding.collapsingToolbar;
    AppBarLayout appBarLayout = binding.appbarLayout;
    appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
      boolean isShow = true;
      int scrollRange = -1;

      @Override
      public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (scrollRange == -1) {
          scrollRange = appBarLayout.getTotalScrollRange();
        }
        if (scrollRange + verticalOffset == 0) {
          if (displayedUserProfile != null && displayedUserProfile.getDisplayName() != null && !displayedUserProfile.getDisplayName().isEmpty()) {
            collapsingToolbarLayout.setTitle(displayedUserProfile.getDisplayName());
          } else if (displayedUserProfile != null && displayedUserProfile.getUsername() != null) {
            collapsingToolbarLayout.setTitle(displayedUserProfile.getUsername());
          }
          isShow = true;
        } else if (isShow) {
          collapsingToolbarLayout.setTitle(" ");
          isShow = false;
        }
      }
    });

    binding.layoutFollowingInfo.setOnClickListener(v -> {
      if (displayedUserProfile != null && !userId.isEmpty()) {
        Intent intent = new Intent(ProfileActivity.this, FollowingListActivity.class);
        intent.putExtra(FollowersListActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
      } else {
        Toast.makeText(ProfileActivity.this, getString(R.string.user_data_not_loaded_yet), Toast.LENGTH_SHORT).show();
      }
    });

    binding.layoutFollowersInfo.setOnClickListener(v -> {
      if (displayedUserProfile != null && !userId.isEmpty()) {
        Intent intent = new Intent(ProfileActivity.this, FollowersListActivity.class);
        intent.putExtra(FollowersListActivity.EXTRA_USER_ID, userId);
        // ⭐ بداية التعديل: إضافة هذا السطر لتمرير نوع القائمة ⭐
        intent.putExtra(FollowersListActivity.EXTRA_LIST_TYPE, "followers");
        // ⭐ نهاية التعديل ⭐
        startActivity(intent);
      } else {
        Toast.makeText(ProfileActivity.this, getString(R.string.user_data_not_loaded_yet), Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void loadUserDataWithListener() {
    if (userProfileListener != null) {
      userProfileListener.remove();
    }
    binding.btnAction.setEnabled(false);
    binding.layoutFollowingInfo.setEnabled(false);
    binding.layoutFollowersInfo.setEnabled(false);

    DocumentReference userRef = db.collection("users").document(userId);
    userProfileListener = userRef.addSnapshotListener((snapshot, e) -> {
      if (e != null) {
        Log.w(TAG, "Listen failed for user " + userId, e);
        Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_load_profile, e.getMessage()), Toast.LENGTH_SHORT).show();
        binding.btnAction.setEnabled(true);
        binding.layoutFollowingInfo.setEnabled(true);
        binding.layoutFollowersInfo.setEnabled(true);
        return;
      }

      if (snapshot != null && snapshot.exists()) {
        displayedUserProfile = snapshot.toObject(UserModel.class);
        if (displayedUserProfile != null) {
          displayedUserProfile.setUserId(snapshot.getId());
          populateProfileData(displayedUserProfile);
          if (!isCurrentUserProfile && currentAuthUserId != null) {
            checkFollowingStatus(); // سيمكن الزر بعد التحقق
          } else if (isCurrentUserProfile) {
            binding.btnAction.setEnabled(true);
          }
        } else {
          Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_parse_user_data), Toast.LENGTH_SHORT).show();
          binding.btnAction.setEnabled(true);
        }
      } else {
        Log.d(TAG, "User document does not exist for userId: " + userId);
        Toast.makeText(ProfileActivity.this, getString(R.string.user_profile_not_found), Toast.LENGTH_SHORT).show();
        binding.btnAction.setEnabled(true); // تمكين حتى لو لم يتم العثور على الملف الشخصي للسماح بالرجوع
      }
      binding.layoutFollowingInfo.setEnabled(true);
      binding.layoutFollowersInfo.setEnabled(true);
    });
  }

  private void checkFollowingStatus() {
    if (currentAuthUserId == null) {
      updateFollowButtonState(false);
      binding.btnAction.setEnabled(true);
      return;
    }
    if (displayedUserProfile == null || displayedUserProfile.getFollowers() == null) {
      Log.w(TAG, "Cannot check following status: displayedUserProfile or its followers map is null");
      updateFollowButtonState(false);
      binding.btnAction.setEnabled(true);
      return;
    }
    isFollowing = displayedUserProfile.getFollowers().containsKey(currentAuthUserId);
    updateFollowButtonState(isFollowing);
    binding.btnAction.setEnabled(true);
  }


  private void updateFollowButtonState(boolean following) {
    if (isCurrentUserProfile) return;

    isFollowing = following;
    if (isFollowing) {
      binding.btnAction.setText(R.string.following);
      binding.btnAction.setIcon(null);
      // يمكنك تخصيص الألوان والخلفيات هنا إذا أردت
      // binding.btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.m3_surface_container_highest));
      // binding.btnAction.setTextColor(ContextCompat.getColor(this, R.color.m3_onSurface));
    } else {
      binding.btnAction.setText(R.string.follow);
      binding.btnAction.setIconResource(R.drawable.ic_add);
      // binding.btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.m3_primary));
      // binding.btnAction.setTextColor(ContextCompat.getColor(this, R.color.m3_onPrimary));
    }
  }

  private void toggleFollow() {
    if (currentAuthUserId == null) {
      Toast.makeText(this, getString(R.string.login_to_follow), Toast.LENGTH_SHORT).show();
      return;
    }
    if (displayedUserProfile == null || userId == null) {
      Toast.makeText(this, getString(R.string.cannot_follow_no_user_data), Toast.LENGTH_SHORT).show();
      return;
    }

    binding.btnAction.setEnabled(false);

    DocumentReference currentUserDocRef = db.collection("users").document(currentAuthUserId);
    DocumentReference targetUserDocRef = db.collection("users").document(userId);

    db.runTransaction(transaction -> {
      DocumentSnapshot currentUserSnap = transaction.get(currentUserDocRef);
      DocumentSnapshot targetUserSnap = transaction.get(targetUserDocRef);

      if (!currentUserSnap.exists() || !targetUserSnap.exists()) {
        throw new FirebaseFirestoreException("User document not found for transaction.", FirebaseFirestoreException.Code.NOT_FOUND);
      }

      Map<String, Boolean> currentUserFollowing = currentUserSnap.get("following") instanceof Map ? new HashMap<>((Map<String, Boolean>) currentUserSnap.get("following")) : new HashMap<>();
      Map<String, Boolean> targetUserFollowers = targetUserSnap.get("followers") instanceof Map ? new HashMap<>((Map<String, Boolean>) targetUserSnap.get("followers")) : new HashMap<>();

      if (isFollowing) {
        currentUserFollowing.remove(userId);
        targetUserFollowers.remove(currentAuthUserId);
      } else {
        currentUserFollowing.put(userId, true);
        targetUserFollowers.put(currentAuthUserId, true);
      }

      transaction.set(currentUserDocRef, new HashMap<String, Object>() {{ put("following", currentUserFollowing); }}, SetOptions.merge());
      transaction.set(targetUserDocRef, new HashMap<String, Object>() {{ put("followers", targetUserFollowers); }}, SetOptions.merge());

      return null;
    }).addOnSuccessListener(aVoid -> {
      binding.btnAction.setEnabled(true);
      Log.d(TAG, "Follow status toggled successfully for user: " + userId);
      // SnapshotListener سيتولى تحديث واجهة المستخدم لـ isFollowing وعدد المتابعين
    }).addOnFailureListener(e -> {
      Log.e(TAG, "Failed to toggle follow for user: " + userId, e);
      Toast.makeText(ProfileActivity.this, getString(R.string.failed_to_update_follow_status, e.getMessage()), Toast.LENGTH_SHORT).show();
      checkFollowingStatus(); // إعادة الزر إلى حالته الصحيحة
      binding.btnAction.setEnabled(true);
    });
  }

  private void populateProfileData(UserModel user) {
    if (user == null) {
      Log.e(TAG, "User model is null in populateProfileData");
      return;
    }

    String displayName = (TextUtils.isEmpty(user.getDisplayName()) && user.getUsername() != null) ? user.getUsername() : user.getDisplayName();
    if (TextUtils.isEmpty(displayName)) displayName = "User"; // قيمة افتراضية نهائية

    String username = user.getUsername() != null ? "@" + user.getUsername() : "@unknown";

    binding.tvDisplayName.setText(displayName);
    binding.tvUsername.setText(username);

    if (user.getBio() != null && !user.getBio().isEmpty()) {
      binding.tvBio.setVisibility(View.VISIBLE);
      binding.tvBio.setText(user.getBio());
    } else {
      binding.tvBio.setVisibility(View.GONE);
    }

    binding.ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);

    Glide.with(this)
            .load(user.getProfileImageUrl())
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .into(binding.ivAvatar);

    Glide.with(this)
            .load(user.getCoverImageUrl())
            .placeholder(R.color.m3_surfaceContainerLow)
            .error(R.color.m3_surfaceContainerLow)
            .into(binding.ivCover);

    binding.tvFollowersCount.setText(String.format(Locale.getDefault(), "%d", user.getFollowers() != null ? user.getFollowers().size() : 0));
    binding.tvFollowingCount.setText(String.format(Locale.getDefault(), "%d", user.getFollowing() != null ? user.getFollowing().size() : 0));

    binding.layoutSocialLinksContainer.removeAllViews();
    Map<String, String> socialLinks = user.getSocialLinks();

    if (socialLinks != null && !socialLinks.isEmpty()) {
      binding.tvAboutTitle.setVisibility(View.VISIBLE);
      LayoutInflater inflater = LayoutInflater.from(this);

      for (Map.Entry<String, String> entry : socialLinks.entrySet()) {
        final String platform = entry.getKey();
        final String url = entry.getValue();

        if (url == null || url.trim().isEmpty()) continue;

        View socialLinkView = inflater.inflate(R.layout.item_profile_social_link, binding.layoutSocialLinksContainer, false);
        ImageView ivPlatformIcon = socialLinkView.findViewById(R.id.ivPlatformIcon);
        TextView tvPlatformUrl = socialLinkView.findViewById(R.id.tvPlatformUrl);

        switch (platform.toLowerCase()) {
          case UserModel.SOCIAL_TWITTER:
            ivPlatformIcon.setImageResource(R.drawable.ic_share); // استبدل بأيقونة تويتر المناسبة
            break;
          case UserModel.SOCIAL_INSTAGRAM:
            ivPlatformIcon.setImageResource(R.drawable.ic_add_photo); // استبدل بأيقونة انستغرام المناسبة
            break;
          default:
            ivPlatformIcon.setImageResource(R.drawable.ic_link);
            break;
        }
        tvPlatformUrl.setText(url);

        socialLinkView.setOnClickListener(v -> {
          try {
            String formattedUrl = url;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
              formattedUrl = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl));
            startActivity(intent);
          } catch (Exception ex) {
            Log.e(TAG, "Could not open social link: " + url, ex);
            Toast.makeText(ProfileActivity.this, "Could not open link: " + url, Toast.LENGTH_SHORT).show();
          }
        });
        binding.layoutSocialLinksContainer.addView(socialLinkView);
      }
      if (binding.layoutSocialLinksContainer.getChildCount() == 0) {
        binding.tvAboutTitle.setVisibility(View.GONE);
      }
    } else {
      binding.tvAboutTitle.setVisibility(View.GONE);
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

  private static class ProfilePagerAdapter extends FragmentStateAdapter {
    private final String[] tabTitles = new String[] {"Posts", "Replies", "Media"};
    private final String userIdForFragments;

    public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity, String userId) {
      super(fragmentActivity);
      this.userIdForFragments = userId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      switch (position) {
        case 0:
          return ProfilePostsFragment.newInstance(userIdForFragments);
        case 1:
          return ProfileRepliesFragment.newInstance(userIdForFragments);
        case 2:
          return ProfileMediaFragment.newInstance(userIdForFragments);
        default:
          // Should not happen
          return ProfilePostsFragment.newInstance(userIdForFragments);
      }
    }

    @Override
    public int getItemCount() {
      return tabTitles.length;
    }
  }
}