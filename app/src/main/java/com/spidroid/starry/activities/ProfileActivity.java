package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout; // تأكد من هذا الاستيراد
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
import com.google.firebase.firestore.ListenerRegistration;
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

public class ProfileActivity extends AppCompatActivity {
  private ActivityProfileBinding binding;
  private FirebaseFirestore db;
  private FirebaseAuth auth;
  private String userId;
  private boolean isCurrentUser;
  private boolean isFollowing;
  private UserModel currentUserProfile;
  private ListenerRegistration userProfileListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityProfileBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    db = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    userId = getIntent().getStringExtra("userId");
    if (userId == null || userId.isEmpty()) {
      Toast.makeText(this, "User ID not provided.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    isCurrentUser = (auth.getCurrentUser() != null) && userId.equals(auth.getCurrentUser().getUid());

    setupUI(); // استدعاء setupUI هنا
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

    if (isCurrentUser) {
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
      // سيتم تحديث زر المتابعة في loadUserDataWithListener
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
          if (currentUserProfile != null) {
            collapsingToolbarLayout.setTitle(currentUserProfile.getDisplayName());
          }
          isShow = true;
        } else if (isShow) {
          collapsingToolbarLayout.setTitle(" ");
          isShow = false;
        }
      }
    });

    // --- ★★ بداية إضافة مستمعي النقر لقوائم المتابعين والمتابَعين ★★ ---
    binding.layoutFollowingInfo.setOnClickListener(v -> {
      if (currentUserProfile != null && !userId.isEmpty()) { // تحقق من userId أيضًا
        Intent intent = new Intent(ProfileActivity.this, FollowingListActivity.class);
        intent.putExtra(FollowersListActivity.EXTRA_USER_ID, userId); // استخدام userId من Intent
        startActivity(intent);
      } else {
        Toast.makeText(ProfileActivity.this, "User data not loaded yet.", Toast.LENGTH_SHORT).show();
      }
    });

    binding.layoutFollowersInfo.setOnClickListener(v -> {
      if (currentUserProfile != null && !userId.isEmpty()) { // تحقق من userId أيضًا
        Intent intent = new Intent(ProfileActivity.this, FollowersListActivity.class);
        intent.putExtra(FollowersListActivity.EXTRA_USER_ID, userId); // استخدام userId من Intent
        startActivity(intent);
      } else {
        Toast.makeText(ProfileActivity.this, "User data not loaded yet.", Toast.LENGTH_SHORT).show();
      }
    });
    // --- ★★ نهاية إضافة مستمعي النقر ★★ ---
  }

  private void loadUserDataWithListener() {
    if (userProfileListener != null) {
      userProfileListener.remove();
    }
    binding.btnAction.setEnabled(false);
    binding.layoutFollowingInfo.setEnabled(false); // تعطيل مبدئي
    binding.layoutFollowersInfo.setEnabled(false); // تعطيل مبدئي


    DocumentReference userRef = db.collection("users").document(userId);
    userProfileListener = userRef.addSnapshotListener((snapshot, e) -> {
      if (e != null) {
        Log.w("ProfileActivity", "Listen failed.", e);
        Toast.makeText(ProfileActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        binding.btnAction.setEnabled(true);
        binding.layoutFollowingInfo.setEnabled(true);
        binding.layoutFollowersInfo.setEnabled(true);
        return;
      }

      if (snapshot != null && snapshot.exists()) {
        currentUserProfile = snapshot.toObject(UserModel.class);
        if (currentUserProfile != null) {
          currentUserProfile.setUserId(snapshot.getId());
          populateProfileData(currentUserProfile);
          if (!isCurrentUser) {
            checkFollowingStatus();
          } else {
            binding.btnAction.setEnabled(true);
          }
        } else {
          Toast.makeText(ProfileActivity.this, "Failed to parse user data.", Toast.LENGTH_SHORT).show();
          binding.btnAction.setEnabled(true);
        }
      } else {
        Log.d("ProfileActivity", "Current data: null for userId: " + userId);
        Toast.makeText(ProfileActivity.this, "User profile not found.", Toast.LENGTH_SHORT).show();
        binding.btnAction.setEnabled(true);
      }
      // تمكين الأزرار بعد اكتمال تحميل البيانات (سواء نجح أو فشل جزئيًا)
      binding.layoutFollowingInfo.setEnabled(true);
      binding.layoutFollowersInfo.setEnabled(true);
    });
  }

  private void checkFollowingStatus() {
    if (auth.getCurrentUser() == null) {
      binding.btnAction.setEnabled(true); // تمكين الزر إذا لم يكن هناك مستخدم مسجل
      return;
    }
    String currentAuthUserId = auth.getCurrentUser().getUid();

    db.collection("users").document(currentAuthUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
              UserModel loggedInUser = documentSnapshot.toObject(UserModel.class);
              if (loggedInUser != null && loggedInUser.getFollowing() != null) {
                isFollowing = loggedInUser.getFollowing().containsKey(userId);
              } else {
                isFollowing = false;
              }
              updateFollowButton();
              binding.btnAction.setEnabled(true);
            })
            .addOnFailureListener(e -> {
              Log.e("ProfileActivity", "Error checking following status", e);
              binding.btnAction.setEnabled(true);
            });
  }

  private void updateFollowButton() {
    if (isCurrentUser) return;

    if (isFollowing) {
      binding.btnAction.setText(R.string.following);
      binding.btnAction.setIcon(null);
    } else {
      binding.btnAction.setText(R.string.follow);
      binding.btnAction.setIconResource(R.drawable.ic_add);
    }
    binding.btnAction.setOnClickListener(v -> toggleFollow());
  }

  private void toggleFollow() {
    if (auth.getCurrentUser() == null) {
      Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
      return;
    }
    binding.btnAction.setEnabled(false);

    String currentAuthUserId = auth.getCurrentUser().getUid();
    DocumentReference currentUserDocRef = db.collection("users").document(currentAuthUserId);
    DocumentReference targetUserDocRef = db.collection("users").document(userId);

    db.runTransaction(transaction -> {
      DocumentSnapshot currentUserSnap = transaction.get(currentUserDocRef);
      DocumentSnapshot targetUserSnap = transaction.get(targetUserDocRef);

      Map<String, Object> currentUserFollowing = currentUserSnap.get("following") != null ?
              new HashMap<>((Map<String, Object>) currentUserSnap.get("following")) : new HashMap<>();
      Map<String, Object> targetUserFollowers = targetUserSnap.get("followers") != null ?
              new HashMap<>((Map<String, Object>) targetUserSnap.get("followers")) : new HashMap<>();

      if (isFollowing) {
        currentUserFollowing.remove(userId);
        targetUserFollowers.remove(currentAuthUserId);
      } else {
        currentUserFollowing.put(userId, true);
        targetUserFollowers.put(currentAuthUserId, true);
      }

      transaction.update(currentUserDocRef, "following", currentUserFollowing);
      transaction.update(targetUserDocRef, "followers", targetUserFollowers);
      return null;
    }).addOnSuccessListener(aVoid -> {
      // تم نقل تحديث isFollowing و updateFollowButton إلى داخل addSnapshotListener
      // لضمان أن الواجهة تعكس دائمًا البيانات الفعلية من Firestore.
      // لا حاجة لإعادة تحميل البيانات يدويًا هنا إذا كان الـ listener يعمل.
      binding.btnAction.setEnabled(true);
    }).addOnFailureListener(e -> {
      Log.e("ProfileActivity", "Failed to toggle follow", e);
      Toast.makeText(ProfileActivity.this, "Failed to update follow status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      binding.btnAction.setEnabled(true);
    });
  }

  private void populateProfileData(UserModel user) {
    if (user == null) {
      Log.e("ProfileActivity", "User model is null in populateProfileData");
      return;
    }

    binding.tvDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
    binding.tvUsername.setText("@" + user.getUsername());

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
            .placeholder(R.color.m3_surfaceContainer)
            .error(R.color.m3_surfaceContainer)
            .into(binding.ivCover);

    binding.tvFollowersCount.setText(String.format(Locale.getDefault(), "%d", user.getFollowers() != null ? user.getFollowers().size() : 0));
    binding.tvFollowingCount.setText(String.format(Locale.getDefault(), "%d", user.getFollowing() != null ? user.getFollowing().size() : 0));

    // --- كود الروابط الاجتماعية (من الرد السابق) ---
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
            ivPlatformIcon.setImageResource(R.drawable.ic_share); // استبدل بأيقونة تويتر
            tvPlatformUrl.setText(platform.substring(0, 1).toUpperCase() + platform.substring(1));
            break;
          case UserModel.SOCIAL_INSTAGRAM:
            ivPlatformIcon.setImageResource(R.drawable.ic_add_photo); // استبدل بأيقونة انستغرام
            tvPlatformUrl.setText(platform.substring(0, 1).toUpperCase() + platform.substring(1));
            break;
          case UserModel.SOCIAL_FACEBOOK:
            ivPlatformIcon.setImageResource(R.drawable.ic_group_add); // استبدل بأيقونة فيسبوك
            tvPlatformUrl.setText(platform.substring(0, 1).toUpperCase() + platform.substring(1));
            break;
          case UserModel.SOCIAL_LINKEDIN:
            ivPlatformIcon.setImageResource(R.drawable.ic_social_connections); // استبدل بأيقونة لينكدإن
            tvPlatformUrl.setText(platform.substring(0, 1).toUpperCase() + platform.substring(1));
            break;
          default:
            ivPlatformIcon.setImageResource(R.drawable.ic_link);
            tvPlatformUrl.setText(url);
            break;
        }

        socialLinkView.setOnClickListener(v -> {
          try {
            String formattedUrl = url;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
              formattedUrl = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl));
            startActivity(intent);
          } catch (Exception ex) {
            Log.e("ProfileActivity", "Could not open social link: " + url, ex);
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
          return ProfilePostsFragment.newInstance(userId);
        case 1:
          return new ProfileRepliesFragment();
        case 2:
          return new ProfileMediaFragment();
        default:
          return ProfilePostsFragment.newInstance(userId);
      }
    }

    @Override
    public int getItemCount() {
      return tabTitles.length;
    }
  }
}