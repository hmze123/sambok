package com.spidroid.starry.activities;

import android.animation.Animator; // ★ إضافة هذا
import android.animation.AnimatorListenerAdapter; // ★ إضافة هذا
import android.animation.ValueAnimator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler; // ★ إضافة هذا
import android.os.Looper;  // ★ إضافة هذا
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout; // ★ إضافة هذا
import android.widget.ProgressBar; // ★ تغيير هذا إلى ProgressBar العادي إذا لم يكن LinearProgressIndicator
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.LinearProgressIndicator; // ★ استخدام هذا بدلاً من ProgressBar العادي
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query; // ★ إضافة هذا
import com.google.firebase.firestore.QueryDocumentSnapshot; // ★ إضافة هذا
import com.spidroid.starry.R;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel; // ستحتاجه لجلب بيانات المؤلف إذا لم تكن مدمجة

import java.util.ArrayList;
import java.util.Collections; // ★ إضافة هذا
import java.util.Comparator;  // ★ إضافة هذا
import java.util.Date;
import java.util.List;
import java.util.Objects;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import android.view.animation.LinearInterpolator;

import de.hdodenhof.circleimageview.CircleImageView; // ★ إضافة هذا

public class StoryViewerActivity extends AppCompatActivity {

    private static final String TAG = "StoryViewerActivity";
    public static final String EXTRA_USER_ID = "userId"; // ★ تعريف الثابت

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration storyListener;
    private ListenerRegistration authorInfoListener; // ★ مستمع لبيانات المؤلف

    private ImageView ivStoryMedia;
    private PlayerView pvStoryVideo;
    private ProgressBar pbActivityLoading; // ★ لتوضيح أنه للتحميل العام للـ Activity
    private TextView tvStoryAuthorName;
    private CircleImageView ivStoryAuthorAvatar; // ★ تغيير النوع إلى CircleImageView
    private ImageView ivClose;
    private LinearLayout progressBarsContainer; // ★ حاوية لأشرطة التقدم

    // ★ قائمة بقصص المستخدم الحالي الذي يتم عرضه
    private List<StoryModel> currentUserStories = new ArrayList<>();
    private int currentStoryIndexInList = 0; // مؤشر للقصة الحالية ضمن قائمة قصص المستخدم
    private String viewedUserId;
    private UserModel storyAuthor; // ★ لتخزين بيانات صاحب القصة
    private TextView tvStoryTimestamp;
    private ExoPlayer player;
    private List<ValueAnimator> progressAnimators = new ArrayList<>(); // ★ لتتبع متحركات أشرطة التقدم
    private Handler storyAdvanceHandler = new Handler(Looper.getMainLooper()); // ★ للتحكم في مؤقت الصورة

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        viewedUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (viewedUserId == null || viewedUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadAuthorInfoAndStories(); // ★ دالة واحدة لجلب كل شيء
    }

    private void initializeViews() {
        ivStoryMedia = findViewById(R.id.ivStoryMedia);
        pvStoryVideo = findViewById(R.id.pvStoryVideo);
        pbActivityLoading = findViewById(R.id.pbLoading); // ★ استخدام ID الصحيح
        tvStoryAuthorName = findViewById(R.id.tvStoryAuthorName);
        ivStoryAuthorAvatar = findViewById(R.id.ivStoryAuthorAvatar); // ★ استخدام ID الصحيح
        ivClose = findViewById(R.id.ivClose);
        progressBarsContainer = findViewById(R.id.progressBarsContainer);
        tvStoryTimestamp = findViewById(R.id.tvStoryTimestamp);
    }

    private void setupListeners() {
        findViewById(R.id.viewNext).setOnClickListener(v -> showNextStorySegment());
        findViewById(R.id.viewPrevious).setOnClickListener(v -> showPreviousStorySegment());
        ivClose.setOnClickListener(v -> finish());
    }

    private void loadAuthorInfoAndStories() {
        pbActivityLoading.setVisibility(View.VISIBLE);
        if (authorInfoListener != null) authorInfoListener.remove();
        authorInfoListener = db.collection("users").document(viewedUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed for author info.", e);
                        Toast.makeText(this, "Error loading user data.", Toast.LENGTH_SHORT).show();
                        pbActivityLoading.setVisibility(View.GONE);
                        finish();
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        storyAuthor = snapshot.toObject(UserModel.class);
                        if (storyAuthor != null) {
                            storyAuthor.setUserId(snapshot.getId()); // تأكد من تعيين ID المستخدم
                            tvStoryAuthorName.setText(storyAuthor.getDisplayName() != null ? storyAuthor.getDisplayName() : storyAuthor.getUsername());
                            if (ivStoryAuthorAvatar.getContext() != null && storyAuthor.getProfileImageUrl() != null && !storyAuthor.getProfileImageUrl().isEmpty()) {
                                Glide.with(ivStoryAuthorAvatar.getContext()).load(storyAuthor.getProfileImageUrl()).placeholder(R.drawable.ic_default_avatar).into(ivStoryAuthorAvatar);
                            } else if (ivStoryAuthorAvatar.getContext() != null) {
                                ivStoryAuthorAvatar.setImageResource(R.drawable.ic_default_avatar);
                            }
                            loadStoriesForCurrentUser(); // جلب القصص بعد جلب بيانات المؤلف
                        } else {
                            pbActivityLoading.setVisibility(View.GONE);
                            Toast.makeText(this, "Failed to parse user data.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        pbActivityLoading.setVisibility(View.GONE);
                        Log.d(TAG, "Author document does not exist for userId: " + viewedUserId);
                        Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }


    private void loadStoriesForCurrentUser() {
        if (viewedUserId == null) return;
        if (storyListener != null) storyListener.remove();

        storyListener = db.collection("stories")
                .whereEqualTo("userId", viewedUserId)
                .whereGreaterThan("expiresAt", new Date()) // جلب القصص النشطة فقط
                .orderBy("createdAt", Query.Direction.ASCENDING) // ترتيب حسب وقت الإنشاء
                .addSnapshotListener((querySnapshot, e) -> {
                    pbActivityLoading.setVisibility(View.GONE); // إخفاء التحميل العام عند وصول البيانات
                    if (e != null) {
                        Log.e(TAG, "Listen failed for stories.", e);
                        Toast.makeText(this, "Error loading stories.", Toast.LENGTH_SHORT).show();
                        if (currentUserStories.isEmpty()) finish(); // أغلق إذا لم يكن هناك قصص سابقة
                        return;
                    }

                    if (querySnapshot != null) {
                        currentUserStories.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            StoryModel story = doc.toObject(StoryModel.class);
                            story.setStoryId(doc.getId()); // تعيين ID القصة
                            // ملء بيانات المؤلف المباشرة إذا لم تكن موجودة (كحل احتياطي)
                            if (storyAuthor != null) {
                                story.setAuthorDisplayName(storyAuthor.getDisplayName());
                                story.setAuthorUsername(storyAuthor.getUsername());
                                story.setAuthorAvatarUrl(storyAuthor.getProfileImageUrl());
                                story.setAuthorVerified(storyAuthor.isVerified());
                            }
                            currentUserStories.add(story);
                        }
                        Log.d(TAG, "Loaded " + currentUserStories.size() + " stories for user: " + viewedUserId);

                        if (currentUserStories.isEmpty()) {
                            Toast.makeText(this, "No active stories for this user.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            setupProgressBarsUI();
                            currentStoryIndexInList = 0; // ابدأ من القصة الأولى
                            displayStorySegment();
                        }
                    } else if (currentUserStories.isEmpty()) { // إذا كان querySnapshot هو null ولم يكن هناك قصص سابقة
                        Toast.makeText(this, "No stories found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void setupProgressBarsUI() {
        progressBarsContainer.removeAllViews();
        progressAnimators.clear(); // مسح أي متحركات قديمة

        for (int i = 0; i < currentUserStories.size(); i++) {
            LinearProgressIndicator progressBar = new LinearProgressIndicator(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    (int) getResources().getDimension(R.dimen.story_progress_height), // تأكد من وجود هذا البعد
                    1.0f
            );
            int margin = (int) (2 * getResources().getDisplayMetrics().density); // هامش صغير بين الأشرطة
            params.setMargins(margin, 0, margin, 0);
            progressBar.setLayoutParams(params);
            progressBar.setMax(100); // القيمة القصوى للتقدم
            progressBar.setProgress(0); // ابدأ من الصفر
            if (i == currentStoryIndexInList) { // القصة الحالية
                progressBar.setIndicatorColor(ContextCompat.getColor(this, R.color.white)); // لون شريط التقدم النشط
            } else if (i < currentStoryIndexInList) { // القصص السابقة
                progressBar.setProgress(100);
                progressBar.setIndicatorColor(ContextCompat.getColor(this, R.color.white));
            } else { // القصص اللاحقة
                progressBar.setIndicatorColor(ContextCompat.getColor(this, R.color.grey)); // لون مختلف قليلاً للقصص غير النشطة بعد
            }
            progressBar.setTrackColor(ContextCompat.getColor(this, R.color.m3_outline)); // لون خلفية شريط التقDEMO
            progressBarsContainer.addView(progressBar);

            if (i == currentStoryIndexInList) {
                progressBar.setIndicatorColor(ContextCompat.getColor(this, R.color.story_progress_active));
            } else if (i < currentStoryIndexInList) {
                progressBar.setProgress(100);
                progressBar.setIndicatorColor(ContextCompat.getColor(this, R.color.story_progress_active));
            } else {
                progressBar.setIndicatorColor(ContextCompat.getColor(this, R.color.story_progress_inactive_segment));
            }
            progressBar.setTrackColor(ContextCompat.getColor(this, R.color.story_progress_inactive_track));

            // إنشاء ValueAnimator لكل شريط تقدم ولكن لا تبدأه الآن
            ValueAnimator animator = ValueAnimator.ofInt(0, 100);
            animator.setInterpolator(new LinearInterpolator());
            final int currentIndex = i;
            animator.addUpdateListener(animation -> {
                if (progressBarsContainer.getChildAt(currentIndex) instanceof LinearProgressIndicator) {
                    ((LinearProgressIndicator) progressBarsContainer.getChildAt(currentIndex)).setProgress((Integer) animation.getAnimatedValue());
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (currentIndex == currentStoryIndexInList) { // تأكد أن هذا هو المتحرك للقصة الحالية
                        showNextStorySegment();
                    }
                }
            });
            progressAnimators.add(animator);
        }
    }


    private void displayStorySegment() {
        if (currentStoryIndexInList < 0 || currentStoryIndexInList >= currentUserStories.size()) {
            Log.d(TAG, "Story index out of bounds or no stories. Finishing.");
            finish();
            return;
        }

        // ★★ هنا يتم تعريف وتهيئة المتغير story ★★
        StoryModel story = currentUserStories.get(currentStoryIndexInList);
        Log.d(TAG, "Displaying story segment " + (currentStoryIndexInList + 1) + "/" + currentUserStories.size() + " for user: " + viewedUserId + ", URL: " + story.getMediaUrl());
        Log.d(TAG, "displayStorySegment: Story ID = " + (story != null ? story.getStoryId() : "story is null"));
        if (story != null) {
            Log.d(TAG, "displayStorySegment: Story CreatedAt = " + story.getCreatedAt());
        }
        Log.d(TAG, "displayStorySegment: tvStoryTimestamp is null? " + (tvStoryTimestamp == null));

        if (story != null && story.getCreatedAt() != null && tvStoryTimestamp != null) {
            tvStoryTimestamp.setText(DateUtils.getRelativeTimeSpanString(
                    story.getCreatedAt().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
            Log.d(TAG, "displayStorySegment: Timestamp set to: " + tvStoryTimestamp.getText());
        } else if (tvStoryTimestamp != null) {
            tvStoryTimestamp.setText("");
            Log.d(TAG, "displayStorySegment: Timestamp cleared (story or createdAt was null)");
        }
// ...
        // ★★ هذا هو السطر الذي يظهر فيه الخطأ، ويجب أن يكون story معرّفًا قبله ★★
        if (story.getCreatedAt() != null && tvStoryTimestamp != null) { // tvStoryTimestamp هو TextView لعرض وقت القصة
            tvStoryTimestamp.setText(DateUtils.getRelativeTimeSpanString(
                    story.getCreatedAt().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE));
        } else if (tvStoryTimestamp != null) {
            tvStoryTimestamp.setText("");
        }

        // إيقاف جميع المتحركات السابقة
        stopAllProgressAnimations();
        storyAdvanceHandler.removeCallbacksAndMessages(null); // إزالة أي مؤقتات صور سابقة

      //  StoryModel story = currentUserStories.get(currentStoryIndexInList);
        Log.d(TAG, "Displaying story segment " + (currentStoryIndexInList + 1) + "/" + currentUserStories.size() + " for user: " + viewedUserId + ", URL: " + story.getMediaUrl());


        // تحديث ألوان أشرطة التقدم
        for (int i = 0; i < progressBarsContainer.getChildCount(); i++) {
            if (progressBarsContainer.getChildAt(i) instanceof LinearProgressIndicator) {
                LinearProgressIndicator bar = (LinearProgressIndicator) progressBarsContainer.getChildAt(i);
                if (i < currentStoryIndexInList) {
                    bar.setProgress(100);
                    bar.setIndicatorColor(ContextCompat.getColor(this, R.color.white));
                } else if (i == currentStoryIndexInList) {
                    bar.setProgress(0); // إعادة تعيين تقدم الشريط الحالي
                    bar.setIndicatorColor(ContextCompat.getColor(this, R.color.white));
                } else {
                    bar.setProgress(0);
                    bar.setIndicatorColor(ContextCompat.getColor(this, R.color.grey));
                }
            }
        }


        ivStoryMedia.setVisibility(View.GONE);
        pvStoryVideo.setVisibility(View.GONE);
        pbActivityLoading.setVisibility(View.VISIBLE); // إظهار التحميل عند تبديل القصة
        releasePlayer(); // تحرير المشغل القديم

        if (Objects.equals(story.getMediaType(), StoryModel.MEDIA_TYPE_IMAGE)) {
            ivStoryMedia.setVisibility(View.VISIBLE);
            if (ivStoryMedia.getContext() != null && story.getMediaUrl() != null && !story.getMediaUrl().isEmpty()) {
                Glide.with(ivStoryMedia.getContext()).load(story.getMediaUrl()).placeholder(R.color.m3_outline).into(ivStoryMedia);
            }
            pbActivityLoading.setVisibility(View.GONE);
            startStoryTimer(story.getDuration() > 0 ? story.getDuration() : 5000); // مؤقت للصور
        } else if (Objects.equals(story.getMediaType(), StoryModel.MEDIA_TYPE_VIDEO)) {
            pvStoryVideo.setVisibility(View.VISIBLE);
            initializePlayer(story.getMediaUrl(), story.getDuration()); // تمرير مدة الفيديو
            pbActivityLoading.setVisibility(View.GONE); // سيتم إخفاؤه بواسطة مستمع ExoPlayer
        } else {
            Log.w(TAG, "Unknown media type for story: " + story.getMediaType());
            pbActivityLoading.setVisibility(View.GONE);
            Toast.makeText(this, "Unsupported story format.", Toast.LENGTH_SHORT).show();
            // يمكنك الانتقال للقصة التالية أو إنهاء النشاط
            storyAdvanceHandler.postDelayed(this::showNextStorySegment, 100); // محاولة الانتقال بعد تأخير بسيط
        }

        markStoryAsViewed(story.getStoryId());
    }


    private void startStoryTimer(long durationMillis) {
        // إيقاف أي مؤقت سابق
        storyAdvanceHandler.removeCallbacksAndMessages(null);
        // بدء متحرك شريط التقدم
        startCurrentProgressBarAnimation(durationMillis);

        // لا حاجة لمؤقت منفصل إذا كان المتحرك سيتولى الانتقال في onAnimationEnd
    }

    private void initializePlayer(String videoUrl, long videoDuration) {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            pvStoryVideo.setPlayer(player);
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (pbActivityLoading != null) { // تحقق من null
                        pbActivityLoading.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                    }
                    if (state == Player.STATE_READY) {
                        // بدء متحرك شريط التقدم عندما يكون الفيديو جاهزًا
                        long actualDuration = player.getDuration(); // المدة الفعلية للفيديو
                        if (actualDuration > 0) {
                            startCurrentProgressBarAnimation(actualDuration);
                        } else if (videoDuration > 0) { // استخدام المدة المخزنة إذا لم تكن مدة ExoPlayer متاحة فوراً
                            startCurrentProgressBarAnimation(videoDuration);
                        } else {
                            startCurrentProgressBarAnimation(5000); // قيمة افتراضية إذا لم تتوفر مدة
                        }
                    } else if (state == Player.STATE_ENDED) {
                        // لا حاجة لـ showNextStorySegment هنا، لأن onAnimationEnd للمتحرك سيتولى ذلك
                    }
                }
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "ExoPlayer error: " + error.getMessage());
                    Toast.makeText(StoryViewerActivity.this, "Error playing video", Toast.LENGTH_SHORT).show();
                    pbActivityLoading.setVisibility(View.GONE);
                    showNextStorySegment(); // انتقل للقصة التالية عند حدوث خطأ
                }
            });
        }
        player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)));
        player.prepare();
        player.play();
    }

    private void startCurrentProgressBarAnimation(long durationMillis) {
        if (currentStoryIndexInList < 0 || currentStoryIndexInList >= progressAnimators.size()) {
            Log.e(TAG, "Attempted to start animation for an invalid progress bar index: " + currentStoryIndexInList);
            return;
        }
        // إيقاف أي متحرك سابق يعمل لنفس الشريط (احتياطي)
        ValueAnimator existingAnimator = progressAnimators.get(currentStoryIndexInList);
        if (existingAnimator.isRunning()) {
            existingAnimator.cancel();
        }

        // إعادة تعيين تقدم الشريط المرئي
        if (progressBarsContainer.getChildAt(currentStoryIndexInList) instanceof LinearProgressIndicator) {
            ((LinearProgressIndicator) progressBarsContainer.getChildAt(currentStoryIndexInList)).setProgress(0);
        }

        // تعيين مدة المتحرك وبدءه
        existingAnimator.setDuration(durationMillis > 0 ? durationMillis : 5000); // مدة افتراضية إذا كانت 0
        existingAnimator.start();
        Log.d(TAG, "Started progress animation for story " + currentStoryIndexInList + " with duration: " + durationMillis);
    }

    private void stopCurrentProgressAnimation() {
        if (currentStoryIndexInList >= 0 && currentStoryIndexInList < progressAnimators.size()) {
            ValueAnimator animator = progressAnimators.get(currentStoryIndexInList);
            if (animator != null && animator.isRunning()) {
                animator.cancel();
                Log.d(TAG, "Stopped progress animation for story " + currentStoryIndexInList);
            }
        }
    }
    private void stopAllProgressAnimations() {
        for (ValueAnimator animator : progressAnimators) {
            if (animator.isRunning()) {
                animator.cancel();
            }
        }
        Log.d(TAG, "Stopped all progress animations.");
    }


    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    private void showNextStorySegment() {
        stopCurrentProgressAnimation(); // أوقف المتحرك الحالي قبل الانتقال
        if (currentStoryIndexInList < currentUserStories.size() - 1) {
            currentStoryIndexInList++;
            displayStorySegment();
        } else {
            Log.d(TAG, "Finished all stories for this user.");
            finish(); // انتهت جميع قصص المستخدم الحالي
        }
    }

    private void showPreviousStorySegment() {
        stopCurrentProgressAnimation(); // أوقف المتحرك الحالي قبل الانتقال
        if (currentStoryIndexInList > 0) {
            currentStoryIndexInList--;
            displayStorySegment();
        } else {
            // إذا كنا في القصة الأولى، أعد تشغيلها أو انتقل للمستخدم السابق
            currentStoryIndexInList = 0; // أعد تشغيل القصة الأولى
            displayStorySegment();
        }
    }


    private void markStoryAsViewed(String storyId) {
        String currentAuthUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
        if (currentAuthUserId == null || storyId == null || storyId.isEmpty()) return;

        db.collection("stories").document(storyId)
                .update("viewers." + currentAuthUserId, true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Story " + storyId + " marked as viewed by " + currentAuthUserId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark story as viewed", e));
    }


    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
        stopAllProgressAnimations(); // إيقاف جميع المتحركات عند الإيقاف المؤقت
        storyAdvanceHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // إذا كان هناك قصة حالية، استأنف المؤقت أو الفيديو
        if (!currentUserStories.isEmpty() && currentStoryIndexInList < currentUserStories.size()) {
            StoryModel currentStory = currentUserStories.get(currentStoryIndexInList);
            if (Objects.equals(currentStory.getMediaType(), StoryModel.MEDIA_TYPE_IMAGE)) {
                // لا تستدعي startStoryTimer مباشرة هنا، لأن displayStorySegment ستقوم بذلك
                // إذا كان المتحرك قد توقف، displayStorySegment سيعيد تشغيله
                // فقط تأكد من أن displayStorySegment يتم استدعاؤها إذا لزم الأمر (مثلاً، إذا كانت هذه أول مرة)
            } else if (Objects.equals(currentStory.getMediaType(), StoryModel.MEDIA_TYPE_VIDEO) && player != null) {
                player.play(); // استئناف الفيديو
                // المتحرك سيبدأ عندما يكون الفيديو جاهزًا
            }
            // قد تحتاج إلى إعادة عرض القصة الحالية إذا تم إيقاف المتحرك بالكامل
            if (currentStoryIndexInList >= 0 && currentStoryIndexInList < progressAnimators.size()) {
                if (!progressAnimators.get(currentStoryIndexInList).isRunning()) {
                    // displayStorySegment(); // كن حذرًا من الاستدعاءات المتكررة
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storyListener != null) {
            storyListener.remove();
        }
        if (authorInfoListener != null) {
            authorInfoListener.remove();
        }
        releasePlayer();
        stopAllProgressAnimations();
        storyAdvanceHandler.removeCallbacksAndMessages(null);
    }
}