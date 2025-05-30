package com.spidroid.starry.activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.spidroid.starry.R;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.media3.common.PlaybackException; // تأكد من وجود هذا الاستيراد الصريح

// for Progress bar for stories
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.view.ViewGroup;
import android.widget.LinearLayout;


public class StoryViewerActivity extends AppCompatActivity {

    private static final String TAG = "StoryViewerActivity";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration storyListener;

    private ImageView ivStoryMedia;
    private PlayerView pvStoryVideo;
    private ProgressBar pbLoading;
    private TextView tvStoryAuthorName;
    private ImageView ivStoryAuthorAvatar;
    private ImageView ivClose;

    private List<StoryModel> stories = new ArrayList<>();
    private int currentStoryIndex = 0;
    private String viewedUserId; // المستخدم الذي نشاهد قصصه

    private ExoPlayer player;

    private LinearLayout progressBarsContainer;
    private List<LinearProgressIndicator> progressIndicators = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer); // ستحتاج لإنشاء هذا التخطيط
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN); // وضع ملء الشاشة

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        viewedUserId = getIntent().getStringExtra("userId");
        if (viewedUserId == null || viewedUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadStoriesForUser();
        loadAuthorInfo();
    }

    private void initializeViews() {
        ivStoryMedia = findViewById(R.id.ivStoryMedia);
        pvStoryVideo = findViewById(R.id.pvStoryVideo);
        pbLoading = findViewById(R.id.pbLoading);
        tvStoryAuthorName = findViewById(R.id.tvStoryAuthorName);
        ivStoryAuthorAvatar = findViewById(R.id.ivStoryAuthorAvatar);
        ivClose = findViewById(R.id.ivClose);
        progressBarsContainer = findViewById(R.id.progressBarsContainer); // حاوية أشرطة التقدم
    }

    private void setupListeners() {
        findViewById(R.id.viewNext).setOnClickListener(v -> showNextStory());
        findViewById(R.id.viewPrevious).setOnClickListener(v -> showPreviousStory());
        ivClose.setOnClickListener(v -> finish());
    }

    private void loadAuthorInfo() {
        db.collection("users").document(viewedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user != null) {
                        tvStoryAuthorName.setText(user.getDisplayName());
                        Glide.with(this).load(user.getProfileImageUrl()).into(ivStoryAuthorAvatar);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading author info", e));
    }

    private void loadStoriesForUser() {
        if (storyListener != null) storyListener.remove();

        storyListener = db.collection("stories")
                .whereEqualTo("userId", viewedUserId)
                .whereGreaterThan("expiresAt", new Date()) // جلب القصص النشطة فقط
                .orderBy("expiresAt")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e);
                        Toast.makeText(this, "Error loading stories.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    if (querySnapshot != null) {
                        List<StoryModel> activeStories = new ArrayList<>();
                        for (StoryModel story : querySnapshot.toObjects(StoryModel.class)) {
                            // لا تقم بإضافة قصة المستخدم الحالي هنا، سيتم التعامل معها بشكل منفصل
                            if (auth.getCurrentUser() != null && !story.getUserId().equals(auth.getCurrentUser().getUid())) {
                                activeStories.add(story);
                            }
                        }
                        stories.clear();
                        stories.addAll(activeStories);
                        if (stories.isEmpty()) {
                            Toast.makeText(this, "No active stories for this user.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        setupProgressBars(); // تهيئة أشرطة التقدم
                        displayStory(currentStoryIndex);
                    }
                });
    }

    private void setupProgressBars() {
        progressBarsContainer.removeAllViews();
        progressIndicators.clear();

        for (int i = 0; i < stories.size(); i++) {
            LinearProgressIndicator progressBar = new LinearProgressIndicator(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, // العرض سيتغير ديناميكياً
                    (int) getResources().getDimension(R.dimen.story_progress_height), // ارتفاع شريط التقدم
                    1.0f // توزيع المساحة بالتساوي
            );
            params.setMargins(4, 0, 4, 0); // المسافة بين أشرطة التقدم
            progressBar.setLayoutParams(params);
            progressBar.setMax(100); // 100% تقدم
            progressBar.setProgress(0);
            progressBar.setIndicatorColor(getResources().getColor(R.color.white));
            progressBar.setTrackColor(getResources().getColor(R.color.text_secondary)); // لون خلفية الشريط
            progressBarsContainer.addView(progressBar);
            progressIndicators.add(progressBar);
        }
    }


    private void displayStory(int index) {
        if (index < 0 || index >= stories.size()) {
            finish(); // لا توجد قصص أخرى
            return;
        }

        currentStoryIndex = index;
        StoryModel story = stories.get(currentStoryIndex);

        // قم بتحديث شريط التقدم الحالي إلى 0% وإعادة تعيين السابق
        for (int i = 0; i < progressIndicators.size(); i++) {
            progressIndicators.get(i).setProgress(i < currentStoryIndex ? 100 : 0);
        }

        // إخفاء وعرض المشاهدات
        ivStoryMedia.setVisibility(View.GONE);
        pvStoryVideo.setVisibility(View.GONE);
        pbLoading.setVisibility(View.VISIBLE);
        releasePlayer(); // تحرير المشغل قبل عرض قصة جديدة

        if (Objects.equals(story.getMediaType(), StoryModel.MEDIA_TYPE_IMAGE)) {
            ivStoryMedia.setVisibility(View.VISIBLE);
            Glide.with(this).load(story.getMediaUrl()).into(ivStoryMedia);
            pbLoading.setVisibility(View.GONE);
            startProgressBarAnimation(story.getDuration() > 0 ? story.getDuration() : 5000); // صور 5 ثواني
        } else if (Objects.equals(story.getMediaType(), StoryModel.MEDIA_TYPE_VIDEO)) {
            pvStoryVideo.setVisibility(View.VISIBLE);
            initializePlayer(story.getMediaUrl());
            pbLoading.setVisibility(View.GONE); // سيبدأ ExoPlayer في التحميل
            startProgressBarAnimation(story.getDuration()); // فيديو بمدة محددة
        }

        markStoryAsViewed(story.getStoryId());
    }

    private void startProgressBarAnimation(long duration) {
        // سيتم إضافة منطق الرسوم المتحركة هنا في تحديثات لاحقة
        // هذا مجرد مكان مبدئي لبدء تحديث شريط التقدم
        LinearProgressIndicator currentProgressBar = progressIndicators.get(currentStoryIndex);
        currentProgressBar.setProgress(0); // ابدأ من 0
        // يمكنك استخدام ValueAnimator لتحديث التقدم بشكل سلس بمرور الوقت
    }


    private void initializePlayer(String videoUrl) {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            pvStoryVideo.setPlayer(player);
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        showNextStory();
                    }
                }
                @Override
                public void onPlayerError(@NonNull PlaybackException error) { // تم التعديل هنا: إزالة com.google.media3.common
                    Log.e(TAG, "ExoPlayer error: " + error.getMessage());
                    Toast.makeText(StoryViewerActivity.this, "Error playing video", Toast.LENGTH_SHORT).show();
                    showNextStory(); // انتقل إلى القصة التالية عند حدوث خطأ
                }
            });
        }
        player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)));
        player.prepare();
        player.play();
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    private void showNextStory() {
        if (currentStoryIndex < stories.size() - 1) {
            displayStory(currentStoryIndex + 1);
        } else {
            finish(); // لا توجد قصص أخرى، أغلق النشاط
        }
    }

    private void showPreviousStory() {
        if (currentStoryIndex > 0) {
            displayStory(currentStoryIndex - 1);
        } else {
            // يمكن إعادة تشغيل القصة الحالية أو إغلاقها حسب UX المطلوب
            displayStory(0); // إعادة تشغيل الأولى
        }
    }

    private void markStoryAsViewed(String storyId) {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        db.collection("stories").document(storyId)
                .update("viewers." + currentUserId, true)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark story as viewed", e));
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storyListener != null) {
            storyListener.remove();
        }
        releasePlayer();
    }
}