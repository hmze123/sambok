package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ActivityProfileBinding;
import com.spidroid.starry.fragments.ProfilePostsFragment;
import com.spidroid.starry.fragments.ProfileRepliesFragment;
import com.spidroid.starry.fragments.ProfileMediaFragment;
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityProfileBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    db = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    userId = getIntent().getStringExtra("userId");
    isCurrentUser = (auth.getCurrentUser() != null) && userId.equals(auth.getCurrentUser().getUid());

    setupUI();
    loadUserData();
    setupTabs();
  }

  private void setupUI() {
    setSupportActionBar(binding.toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    }
    binding.btnBack.setOnClickListener(v -> finish());
    // ... (باقي إعدادات الواجهة)
  }

  private void loadUserData() {
    // ... (منطق تحميل بيانات المستخدم يبقى كما هو)
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

  // الكلاس الداخلي الذي يحتوي على التعديل
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
          // *** تم التعديل هنا: استخدام دالة newInstance ***
          return ProfilePostsFragment.newInstance(userId);
        case 1:
          return new ProfileRepliesFragment(); // هذه لا تتطلب بيانات، لذا تبقى كما هي
        case 2:
          return new ProfileMediaFragment(); // وهذه أيضاً
        default:
          // استخدام newInstance حتى لو لم يكن هناك وسائط هي ممارسة جيدة
          return ProfilePostsFragment.newInstance(userId);
      }
    }

    @Override
    public int getItemCount() {
      return tabTitles.length;
    }
  }

  // ... (باقي دوال ProfileActivity)
}