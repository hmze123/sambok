package com.spidroid.starry.activities;

import android.animation.ValueAnimator; // استيراد ValueAnimator
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
import androidx.media3.common.PlaybackException;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.widget.LinearLayout;
import android.view.animation.LinearInterpolator; // استيراد LinearInterpolator


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
    private String viewedUserId;

    private ExoPlayer player;
    private ValueAnimator currentStoryProgressBarAnimator; // متحرك شريط التقدم للقصة الحالية

    private LinearLayout progressBarsContainer;
    private List<LinearProgressIndicator> progressIndicators = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

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
        // تم إزالة loadAuthorInfo() هنا، سيتم جلب معلومات المؤلف من StoryModel مباشرة
    }

    private void initializeViews() {
        ivStoryMedia = findViewById(R.id.ivStoryMedia);
        pvStoryVideo = findViewById(R.id.pvStoryVideo);
        pbLoading = findViewById(R.id.pbLoading);
        tvStoryAuthorName = findViewById(R.id.tvStoryAuthorName);
        ivStoryAuthorAvatar = findViewById(R.id.ivStoryAuthorAvatar);
        ivClose = findViewById(R.id.ivClose);
        progressBarsContainer = findViewById(R.id.progressBarsContainer);
    }

    private void setupListeners() {
        findViewById(R.id.viewNext).setOnClickListener(v -> showNextStory());
        findViewById(R.id.viewPrevious).setOnClickListener(v -> showPreviousStory());
        ivClose.setOnClickListener(v -> finish());
    }

    // تم حذف دالة loadAuthorInfo() حيث سيتم جلب البيانات مباشرة من StoryModel
    // private void loadAuthorInfo() { /* ... */ }

    private void loadStoriesForUser() {
        if (storyListener != null) storyListener.remove();

        storyListener = db.collection("stories")
                .whereEqualTo("userId", viewedUserId)
                .whereGreaterThan("expiresAt", new Date())
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
                            // تم إزالة الشرط الزائد:
                            // if (auth.getCurrentUser() != null && !story.getUserId().equals(auth.getCurrentUser().getUid())) {
                            activeStories.add(story);
                            // }
                        }
                        stories.clear();
                        stories.addAll(activeStories);
                        if (stories.isEmpty()) {
                            Toast.makeText(this, "No active stories for this user.", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        setupProgressBars();
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
                    0,
                    (int) getResources().getDimension(R.dimen.story_progress_height),
                    1.0f
            );
            params.setMargins(4, 0, 4, 0);
            progressBar.setLayoutParams(params);
            progressBar.setMax(100);
            progressBar.setProgress(0);
            progressBar.setIndicatorColor(getResources().getColor(R.color.white));
            progressBar.setTrackColor(getResources().getColor(R.color.text_secondary));
            progressBarsContainer.addView(progressBar);
            progressIndicators.add(progressBar);
        }
    }


    private void displayStory(int index) {
        if (index < 0 || index >= stories.size()) {
            finish();
            return;
        }

        currentStoryIndex = index;
        StoryModel story = stories.get(currentStoryIndex);

        // إيقاف أي رسوم متحركة سابقة
        if (currentStoryProgressBarAnimator != null) {
            currentStoryProgressBarAnimator.cancel();
        }

        // تحديث حالة أشرطة التقدم: الأشرطة السابقة 100%، الحالية 0%
        for (int i = 0; i < progressIndicators.size(); i++) {
            progressIndicators.get(i).setProgress(i < currentStoryIndex ? 100 : 0);
        }

        // تعيين معلومات المؤلف من StoryModel مباشرة
        tvStoryAuthorName.setText(story.getAuthorDisplayName() != null ? story.getAuthorDisplayName() : story.getUserId());
        Glide.with(this).load(story.getAuthorAvatarUrl()).into(ivStoryAuthorAvatar);


        ivStoryMedia.setVisibility(View.GONE);
        pvStoryVideo.setVisibility(View.GONE);
        pbLoading.setVisibility(View.VISIBLE);
        releasePlayer();

        if (Objects.equals(story.getMediaType(), StoryModel.MEDIA_TYPE_IMAGE)) {
            ivStoryMedia.setVisibility(View.VISIBLE);
            Glide.with(this).load(story.getMediaUrl()).into(ivStoryMedia);
            pbLoading.setVisibility(View.GONE);
            startProgressBarAnimation(story.getDuration() > 0 ? story.getDuration() : 5000);
        } else if (Objects.equals(story.getMediaType(), StoryModel.MEDIA_TYPE_VIDEO)) {
            pvStoryVideo.setVisibility(View.VISIBLE);
            initializePlayer(story.getMediaUrl());
            pbLoading.setVisibility(View.GONE);
            startProgressBarAnimation(story.getDuration());
        }

        markStoryAsViewed(story.getStoryId());
    }

    private void startProgressBarAnimation(long duration) {
        if (currentStoryIndex < 0 || currentStoryIndex >= progressIndicators.size()) {
            return; // تأكد من أن المؤشر صالح
        }
        LinearProgressIndicator currentProgressBar = progressIndicators.get(currentStoryIndex);
        currentProgressBar.setProgress(0);

        currentStoryProgressBarAnimator = ValueAnimator.ofInt(0, 100);
        currentStoryProgressBarAnimator.setDuration(duration);
        currentStoryProgressBarAnimator.setInterpolator(new LinearInterpolator());
        currentStoryProgressBarAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            currentProgressBar.setProgress(progress);
        });
        currentStoryProgressBarAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (currentStoryIndex == progressIndicators.size() - 1) {
                    // آخر قصة، إغلاق النشاط
                    finish();
                } else {
                    // الانتقال للقصة التالية
                    showNextStory();
                }
            }
        });
        currentStoryProgressBarAnimator.start();
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
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "ExoPlayer error: " + error.getMessage());
                    Toast.makeText(StoryViewerActivity.this, "Error playing video", Toast.LENGTH_SHORT).show();
                    showNextStory();
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
        stopProgressBarAnimation(); // إيقاف الرسوم المتحركة الحالية
        if (currentStoryIndex < stories.size() - 1) {
            displayStory(currentStoryIndex + 1);
        } else {
            finish();
        }
    }

    private void showPreviousStory() {
        stopProgressBarAnimation(); // إيقاف الرسوم المتحركة الحالية
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

    private void stopProgressBarAnimation() {
        if (currentStoryProgressBarAnimator != null) {
            currentStoryProgressBarAnimator.cancel();
            currentStoryProgressBarAnimator = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
        stopProgressBarAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storyListener != null) {
            storyListener.remove();
        }
        releasePlayer();
        stopProgressBarAnimation();
    }
}