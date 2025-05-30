package com.spidroid.starry.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ComposePostActivity;
import com.spidroid.starry.activities.CreateStoryActivity;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.activities.StoryViewerActivity;
import com.spidroid.starry.adapters.StoryAdapter;
import com.spidroid.starry.databinding.FragmentHomeBinding;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.viewmodels.StoryViewModel;

import java.util.HashSet;

public class HomeFragment extends Fragment implements StoryAdapter.OnStoryClickListener {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private StoryViewModel storyViewModel;
    private StoryAdapter storyAdapter;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        storyViewModel = new ViewModelProvider(this).get(StoryViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupObservers();
    }

    @Override
    public void onStart() {
        super.onStart();
        // جلب البيانات عند بدء الـ fragment
        storyViewModel.fetchCurrentUser();
        storyViewModel.fetchStoriesForCurrentUserAndFollowing();
    }

    private void setupUI() {
        binding.fabCompose.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ComposePostActivity.class)));

        setupViewPager();
        setupStoriesRecyclerView();
        setupToolbar();
    }

    private void setupStoriesRecyclerView() {
        String currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : "";
        storyAdapter = new StoryAdapter(requireContext(), currentUserId, this);
        binding.rvStories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvStories.setAdapter(storyAdapter);
    }

    private void setupObservers() {
        // مراقبة بيانات المستخدم الحالي لتحديث صورة الأفاتار في الشريط العلوي
        storyViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                storyAdapter.setCurrentUser(user);
                updateToolbarAvatar(user.getProfileImageUrl());
            }
        });

        // هذا هو المراقب الرئيسي الذي يستقبل كل بيانات القصص الجاهزة
        storyViewModel.getStoryFeedState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                // تمرير البيانات المجهزة إلى الـ Adapter
                storyAdapter.setStories(state.stories, state.hasMyActiveStory);
                storyAdapter.setViewedStories(state.viewedStoryIds);
            }
        });
    }

    private void setupToolbar() {
        binding.ivUserAvatar.setOnClickListener(v -> {
            if (auth.getCurrentUser() != null) {
                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra("userId", auth.getCurrentUser().getUid());
                startActivity(intent);
            }
        });
    }

    private void updateToolbarAvatar(String imageUrl) {
        if (isAdded() && imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivUserAvatar);
        }
    }

    // --- تنفيذ واجهات OnStoryClickListener ---
    @Override
    public void onAddStoryClicked() {
        startActivity(new Intent(getActivity(), CreateStoryActivity.class));
    }

    @Override
    public void onViewMyStoryClicked() {
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(getActivity(), StoryViewerActivity.class);
            intent.putExtra("userId", auth.getCurrentUser().getUid());
            startActivity(intent);
        }
    }

    @Override
    public void onStoryPreviewClicked(StoryModel story) {
        Intent intent = new Intent(getActivity(), StoryViewerActivity.class);
        intent.putExtra("userId", story.getUserId());
        startActivity(intent);
    }

    // --- ViewPager and other Fragment methods ---
    private void setupViewPager() {
        binding.viewPager.setAdapter(new ViewPagerAdapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(position == 0 ? "For You" : "Following"))
                .attach();
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        ViewPagerAdapter(@NonNull Fragment fragment) { super(fragment); }
        @NonNull @Override public Fragment createFragment(int position) {
            return position == 0 ? new ForYouFragment() : new FollowingFragment();
        }
        @Override public int getItemCount() { return 2; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}