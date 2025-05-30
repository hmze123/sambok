package com.spidroid.starry.ui.home;

import static android.content.ContentValues.TAG;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ComposePostActivity;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.activities.CreateStoryActivity; // استيراد نشاط إنشاء القصة
import com.spidroid.starry.activities.StoryViewerActivity; // استيراد نشاط عارض القصص
import com.spidroid.starry.databinding.FragmentHomeBinding;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.models.StoryModel; // استيراد نموذج القصة
import com.spidroid.starry.adapters.StoryAdapter; // استيراد محول القصص

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment implements StoryAdapter.OnStoryClickListener { // تنفيذ واجهة OnStoryClickListener

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private ListenerRegistration storiesListener; // المستمع للقصص
    private StoryAdapter storyAdapter;
    private UserModel currentUserModel; // لربط صورة المستخدم الحالي بالقصص

    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // تهيئة Firestore
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFloatingActionButton();
        setupViewPager();
        setupStoriesRecyclerView(); // تهيئة RecyclerView للقصص
        loadStories(); // تحميل القصص
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
            userListener = db.collection("users") // استخدام db هنا
                    .document(currentUser.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null || documentSnapshot == null || !documentSnapshot.exists())
                            return;

                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (user != null) {
                            currentUserModel = user; // حفظ نموذج المستخدم الحالي
                            storyAdapter.setCurrentUserStory(currentUserModel); // تحديث محول القصص بصورة المستخدم الحالي
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                Glide.with(requireContext())
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.ic_default_avatar)
                                        .error(R.drawable.ic_default_avatar)
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .into(ivAvatar);
                            }
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

    private void setupStoriesRecyclerView() {
        storyAdapter = new StoryAdapter(requireContext(), this); // تمرير this كـ OnStoryClickListener
        binding.rvStories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvStories.setAdapter(storyAdapter);
    }

    private void loadStories() {
        if (storiesListener != null) storiesListener.remove();

        // تأكد من أن المستخدم الحالي غير null قبل محاولة جلب القصص
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // ربما ترغب في عرض رسالة توست أو حالة فارغة هنا
            //Toast.makeText(requireContext(), "User not logged in, cannot load stories.", Toast.LENGTH_SHORT).show();
            return;
        }

        storiesListener = db.collection("stories")
                .whereGreaterThan("expiresAt", new Date())
                .orderBy("expiresAt", Query.Direction.ASCENDING) // *** تم التعديل هنا: تحديد الاتجاه صراحةً ***
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error); // استخدم TAG للـ Log
                        Toast.makeText(requireContext(), "Error loading stories: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        // لا تغلق النشاط هنا، فقط اعرض الخطأ
                        return;
                    }

                    if (querySnapshot != null) {
                        List<StoryModel> activeStories = new ArrayList<>();
                        for (StoryModel story : querySnapshot.toObjects(StoryModel.class)) {
                            if (currentUser != null && !story.getUserId().equals(currentUser.getUid())) { // استخدام currentUser
                                activeStories.add(story);
                            }
                        }
                        storyAdapter.setStories(activeStories);
                    }
                });
    }

    @Override
    public void onAddStoryClicked() {
        // عندما ينقر المستخدم على "إضافة قصتي"
        Intent intent = new Intent(getActivity(), CreateStoryActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStoryPreviewClicked(StoryModel story) {
        // عندما ينقر المستخدم على قصة شخص آخر
        Intent intent = new Intent(getActivity(), StoryViewerActivity.class);
        intent.putExtra("userId", story.getUserId()); // يمكنك تمرير معرف المستخدم لعرض جميع قصصه
        startActivity(intent);
    }

    @Override
    public void onMyStoryPreviewClicked(UserModel userModel) {
        // عندما ينقر المستخدم على قصته الخاصة (دائرته)
        // يمكنك التحقق مما إذا كان لديه قصص نشطة بالفعل
        db.collection("stories")
                .whereEqualTo("userId", userModel.getUserId())
                .whereGreaterThan("expiresAt", new Date())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // ليس لديه قصص نشطة، افتح نشاط إنشاء قصة جديدة
                        onAddStoryClicked();
                    } else {
                        // لديه قصص نشطة، افتح عارض القصص
                        onStoryPreviewClicked(querySnapshot.toObjects(StoryModel.class).get(0)); // يمكن تمرير القصة الأولى أو قائمة القصص
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to check your stories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        if (storiesListener != null) {
            storiesListener.remove(); // Clean up stories listener
            storiesListener = null;
        }
        binding = null;
    }
}