package com.spidroid.starry.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ComposePostActivity;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.databinding.FragmentHomeBinding;
import com.spidroid.starry.models.UserModel;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private ListenerRegistration userListener;

    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFloatingActionButton();
        setupViewPager();
    }

    @Override
    public void onStart() {
        super.onStart();
        setupToolbar();
    }

    private void setupFloatingActionButton() {
        FloatingActionButton fabCompose = binding.fabCompose;
        fabCompose.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ComposePostActivity.class);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        CircleImageView ivAvatar = binding.ivUserAvatar;
        FirebaseUser currentUser = auth.getCurrentUser();

        // Set default image first
        ivAvatar.setImageResource(R.drawable.ic_default_avatar);

        if (currentUser != null) {
            // Remove any existing listener to prevent duplicates
            if (userListener != null) userListener.remove();

            // Real-time listener to Firestore user document
            userListener = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null || documentSnapshot == null || !documentSnapshot.exists())
                            return;

                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (user != null &&
                                user.getProfileImageUrl() != null &&
                                !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(requireContext())
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(ivAvatar);
                        }
                    });
        }

        ivAvatar.setOnClickListener(v -> {
            if (currentUser != null) {
                Intent profileIntent = new Intent(getActivity(), ProfileActivity.class);
                profileIntent.putExtra("userId", currentUser.getUid());
                startActivity(profileIntent);
            } else {
                Toast.makeText(getContext(), "Please login to view profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupViewPager() {
        binding.viewPager.setAdapter(new ViewPagerAdapter(this));

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(position == 0 ? "For You" : "Following"))
                .attach();
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? new ForYouFragment() : new FollowingFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove(); // Clean up listener
            userListener = null;
        }
        binding = null;
    }
}