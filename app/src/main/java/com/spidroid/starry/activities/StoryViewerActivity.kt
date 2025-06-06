package com.spidroid.starry.activities

// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
// ★ تغيير هذا إلى ProgressBar العادي إذا لم يكن LinearProgressIndicator
// ★ استخدام هذا بدلاً من ProgressBar العادي
// ★ إضافة هذا
// ★ إضافة هذا
// ستحتاجه لجلب بيانات المؤلف إذا لم تكن مدمجة
// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
import com.google.firebase.auth.FirebaseAuth

class StoryViewerActivity : AppCompatActivity() {
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var storyListener: ListenerRegistration? = null
    private var authorInfoListener: ListenerRegistration? = null // ★ مستمع لبيانات المؤلف

    private var ivStoryMedia: android.widget.ImageView? = null
    private var pvStoryVideo: PlayerView? = null
    private var pbActivityLoading: ProgressBar? = null // ★ لتوضيح أنه للتحميل العام للـ Activity
    private var tvStoryAuthorName: TextView? = null
    private var ivStoryAuthorAvatar: de.hdodenhof.circleimageview.CircleImageView? =
        null // ★ تغيير النوع إلى CircleImageView
    private var ivClose: android.widget.ImageView? = null
    private var progressBarsContainer: LinearLayout? = null // ★ حاوية لأشرطة التقدم

    // ★ قائمة بقصص المستخدم الحالي الذي يتم عرضه
    private val currentUserStories: kotlin.collections.MutableList<StoryModel> =
        java.util.ArrayList<StoryModel>()
    private var currentStoryIndexInList = 0 // مؤشر للقصة الحالية ضمن قائمة قصص المستخدم
    private var viewedUserId: kotlin.String? = null
    private var storyAuthor: UserModel? = null // ★ لتخزين بيانات صاحب القصة
    private var tvStoryTimestamp: TextView? = null
    private var player: ExoPlayer? = null
    private val progressAnimators: kotlin.collections.MutableList<ValueAnimator> =
        java.util.ArrayList<ValueAnimator>() // ★ لتتبع متحركات أشرطة التقدم
    private val storyAdvanceHandler: android.os.Handler =
        android.os.Handler(Looper.getMainLooper()) // ★ للتحكم في مؤقت الصورة

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_viewer)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        viewedUserId = getIntent().getStringExtra(StoryViewerActivity.Companion.EXTRA_USER_ID)
        if (viewedUserId == null || viewedUserId!!.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupListeners()
        loadAuthorInfoAndStories() // ★ دالة واحدة لجلب كل شيء
    }

    private fun initializeViews() {
        ivStoryMedia = findViewById<android.widget.ImageView>(R.id.ivStoryMedia)
        pvStoryVideo = findViewById<PlayerView>(R.id.pvStoryVideo)
        pbActivityLoading = findViewById<ProgressBar?>(R.id.pbLoading) // ★ استخدام ID الصحيح
        tvStoryAuthorName = findViewById<TextView>(R.id.tvStoryAuthorName)
        ivStoryAuthorAvatar =
            findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.ivStoryAuthorAvatar) // ★ استخدام ID الصحيح
        ivClose = findViewById<android.widget.ImageView>(R.id.ivClose)
        progressBarsContainer = findViewById<LinearLayout>(R.id.progressBarsContainer)
        tvStoryTimestamp = findViewById<TextView?>(R.id.tvStoryTimestamp)
    }

    private fun setupListeners() {
        findViewById<android.view.View?>(R.id.viewNext).setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> showNextStorySegment() })
        findViewById<android.view.View?>(R.id.viewPrevious).setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> showPreviousStorySegment() })
        ivClose!!.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> finish() })
    }

    private fun loadAuthorInfoAndStories() {
        pbActivityLoading.setVisibility(android.view.View.VISIBLE)
        if (authorInfoListener != null) authorInfoListener.remove()
        authorInfoListener = db.collection("users").document(viewedUserId)
            .addSnapshotListener({ snapshot, e ->
                if (e != null) {
                    android.util.Log.e(
                        StoryViewerActivity.Companion.TAG,
                        "Listen failed for author info.",
                        e
                    )
                    Toast.makeText(this, "Error loading user data.", Toast.LENGTH_SHORT).show()
                    pbActivityLoading.setVisibility(android.view.View.GONE)
                    finish()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    storyAuthor = snapshot.toObject(UserModel::class.java)
                    if (storyAuthor != null) {
                        storyAuthor.setUserId(snapshot.getId()) // تأكد من تعيين ID المستخدم
                        tvStoryAuthorName.setText(if (storyAuthor.getDisplayName() != null) storyAuthor.getDisplayName() else storyAuthor.getUsername())
                        if (ivStoryAuthorAvatar!!.getContext() != null && storyAuthor.getProfileImageUrl() != null && !storyAuthor.getProfileImageUrl()
                                .isEmpty()
                        ) {
                            Glide.with(ivStoryAuthorAvatar!!.getContext())
                                .load(storyAuthor.getProfileImageUrl())
                                .placeholder(R.drawable.ic_default_avatar).into(ivStoryAuthorAvatar)
                        } else if (ivStoryAuthorAvatar!!.getContext() != null) {
                            ivStoryAuthorAvatar!!.setImageResource(R.drawable.ic_default_avatar)
                        }
                        loadStoriesForCurrentUser() // جلب القصص بعد جلب بيانات المؤلف
                    } else {
                        pbActivityLoading.setVisibility(android.view.View.GONE)
                        Toast.makeText(this, "Failed to parse user data.", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                } else {
                    pbActivityLoading.setVisibility(android.view.View.GONE)
                    android.util.Log.d(
                        StoryViewerActivity.Companion.TAG,
                        "Author document does not exist for userId: " + viewedUserId
                    )
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }


    private fun loadStoriesForCurrentUser() {
        if (viewedUserId == null) return
        if (storyListener != null) storyListener.remove()

        storyListener = db.collection("stories")
            .whereEqualTo("userId", viewedUserId)
            .whereGreaterThan("expiresAt", java.util.Date()) // جلب القصص النشطة فقط
            .orderBy("createdAt", Query.Direction.ASCENDING) // ترتيب حسب وقت الإنشاء
            .addSnapshotListener({ querySnapshot, e ->
                pbActivityLoading.setVisibility(android.view.View.GONE) // إخفاء التحميل العام عند وصول البيانات
                if (e != null) {
                    android.util.Log.e(
                        StoryViewerActivity.Companion.TAG,
                        "Listen failed for stories.",
                        e
                    )
                    Toast.makeText(this, "Error loading stories.", Toast.LENGTH_SHORT).show()
                    if (currentUserStories.isEmpty()) finish() // أغلق إذا لم يكن هناك قصص سابقة

                    return@addSnapshotListener
                }
                if (querySnapshot != null) {
                    currentUserStories.clear()
                    for (doc in querySnapshot) {
                        val story: StoryModel = doc.toObject(StoryModel::class.java)
                        story.setStoryId(doc.getId()) // تعيين ID القصة
                        // ملء بيانات المؤلف المباشرة إذا لم تكن موجودة (كحل احتياطي)
                        if (storyAuthor != null) {
                            story.setAuthorDisplayName(storyAuthor.getDisplayName())
                            story.setAuthorUsername(storyAuthor.getUsername())
                            story.setAuthorAvatarUrl(storyAuthor.getProfileImageUrl())
                            story.setAuthorVerified(storyAuthor.isVerified())
                        }
                        currentUserStories.add(story)
                    }
                    android.util.Log.d(
                        StoryViewerActivity.Companion.TAG,
                        "Loaded " + currentUserStories.size + " stories for user: " + viewedUserId
                    )

                    if (currentUserStories.isEmpty()) {
                        Toast.makeText(this, "No active stories for this user.", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    } else {
                        setupProgressBarsUI()
                        currentStoryIndexInList = 0 // ابدأ من القصة الأولى
                        displayStorySegment()
                    }
                } else if (currentUserStories.isEmpty()) { // إذا كان querySnapshot هو null ولم يكن هناك قصص سابقة
                    Toast.makeText(this, "No stories found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun setupProgressBarsUI() {
        progressBarsContainer.removeAllViews()
        progressAnimators.clear() // مسح أي متحركات قديمة

        for (i in currentUserStories.indices) {
            val progressBar: LinearProgressIndicator = LinearProgressIndicator(this)
            val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                0,
                getResources().getDimension(R.dimen.story_progress_height)
                    .toInt(),  // تأكد من وجود هذا البعد
                1.0f
            )
            val margin =
                (2 * getResources().getDisplayMetrics().density).toInt() // هامش صغير بين الأشرطة
            params.setMargins(margin, 0, margin, 0)
            progressBar.setLayoutParams(params)
            progressBar.setMax(100) // القيمة القصوى للتقدم
            progressBar.setProgress(0) // ابدأ من الصفر
            if (i == currentStoryIndexInList) { // القصة الحالية
                progressBar.setIndicatorColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                ) // لون شريط التقدم النشط
            } else if (i < currentStoryIndexInList) { // القصص السابقة
                progressBar.setProgress(100)
                progressBar.setIndicatorColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )
            } else { // القصص اللاحقة
                progressBar.setIndicatorColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.grey
                    )
                ) // لون مختلف قليلاً للقصص غير النشطة بعد
            }
            progressBar.setTrackColor(
                androidx.core.content.ContextCompat.getColor(
                    this,
                    R.color.m3_outline
                )
            ) // لون خلفية شريط التقDEMO
            progressBarsContainer.addView(progressBar)

            if (i == currentStoryIndexInList) {
                progressBar.setIndicatorColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.story_progress_active
                    )
                )
            } else if (i < currentStoryIndexInList) {
                progressBar.setProgress(100)
                progressBar.setIndicatorColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.story_progress_active
                    )
                )
            } else {
                progressBar.setIndicatorColor(
                    androidx.core.content.ContextCompat.getColor(
                        this,
                        R.color.story_progress_inactive_segment
                    )
                )
            }
            progressBar.setTrackColor(
                androidx.core.content.ContextCompat.getColor(
                    this,
                    R.color.story_progress_inactive_track
                )
            )

            // إنشاء ValueAnimator لكل شريط تقدم ولكن لا تبدأه الآن
            val animator: ValueAnimator = ValueAnimator.ofInt(0, 100)
            animator.setInterpolator(LinearInterpolator())
            val currentIndex = i
            animator.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator? ->
                if (progressBarsContainer.getChildAt(currentIndex) is LinearProgressIndicator) {
                    (progressBarsContainer.getChildAt(currentIndex) as LinearProgressIndicator).setProgress(
                        animation.getAnimatedValue() as Int?
                    )
                }
            })
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator?) {
                    if (currentIndex == currentStoryIndexInList) { // تأكد أن هذا هو المتحرك للقصة الحالية
                        showNextStorySegment()
                    }
                }
            })
            progressAnimators.add(animator)
        }
    }


    private fun displayStorySegment() {
        if (currentStoryIndexInList < 0 || currentStoryIndexInList >= currentUserStories.size) {
            android.util.Log.d(
                StoryViewerActivity.Companion.TAG,
                "Story index out of bounds or no stories. Finishing."
            )
            finish()
            return
        }

        // ★★ هنا يتم تعريف وتهيئة المتغير story ★★
        val story: StoryModel = currentUserStories.get(currentStoryIndexInList)
        android.util.Log.d(
            StoryViewerActivity.Companion.TAG,
            "Displaying story segment " + (currentStoryIndexInList + 1) + "/" + currentUserStories.size + " for user: " + viewedUserId + ", URL: " + story.getMediaUrl()
        )
        android.util.Log.d(
            StoryViewerActivity.Companion.TAG,
            "displayStorySegment: Story ID = " + (if (story != null) story.getStoryId() else "story is null")
        )
        if (story != null) {
            android.util.Log.d(
                StoryViewerActivity.Companion.TAG,
                "displayStorySegment: Story CreatedAt = " + story.getCreatedAt()
            )
        }
        android.util.Log.d(
            StoryViewerActivity.Companion.TAG,
            "displayStorySegment: tvStoryTimestamp is null? " + (tvStoryTimestamp == null)
        )

        if (story != null && story.getCreatedAt() != null && tvStoryTimestamp != null) {
            tvStoryTimestamp.setText(
                DateUtils.getRelativeTimeSpanString(
                    story.getCreatedAt().getTime(),
                    java.lang.System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
            )
            android.util.Log.d(
                StoryViewerActivity.Companion.TAG,
                "displayStorySegment: Timestamp set to: " + tvStoryTimestamp.getText()
            )
        } else if (tvStoryTimestamp != null) {
            tvStoryTimestamp.setText("")
            android.util.Log.d(
                StoryViewerActivity.Companion.TAG,
                "displayStorySegment: Timestamp cleared (story or createdAt was null)"
            )
        }
        // ...
        // ★★ هذا هو السطر الذي يظهر فيه الخطأ، ويجب أن يكون story معرّفًا قبله ★★
        if (story.getCreatedAt() != null && tvStoryTimestamp != null) { // tvStoryTimestamp هو TextView لعرض وقت القصة
            tvStoryTimestamp.setText(
                DateUtils.getRelativeTimeSpanString(
                    story.getCreatedAt().getTime(),
                    java.lang.System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
            )
        } else if (tvStoryTimestamp != null) {
            tvStoryTimestamp.setText("")
        }

        // إيقاف جميع المتحركات السابقة
        stopAllProgressAnimations()
        storyAdvanceHandler.removeCallbacksAndMessages(null) // إزالة أي مؤقتات صور سابقة

        //  StoryModel story = currentUserStories.get(currentStoryIndexInList);
        android.util.Log.d(
            StoryViewerActivity.Companion.TAG,
            "Displaying story segment " + (currentStoryIndexInList + 1) + "/" + currentUserStories.size + " for user: " + viewedUserId + ", URL: " + story.getMediaUrl()
        )


        // تحديث ألوان أشرطة التقدم
        for (i in 0..<progressBarsContainer.getChildCount()) {
            if (progressBarsContainer.getChildAt(i) is LinearProgressIndicator) {
                val bar: LinearProgressIndicator =
                    progressBarsContainer.getChildAt(i) as LinearProgressIndicator
                if (i < currentStoryIndexInList) {
                    bar.setProgress(100)
                    bar.setIndicatorColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.white
                        )
                    )
                } else if (i == currentStoryIndexInList) {
                    bar.setProgress(0) // إعادة تعيين تقدم الشريط الحالي
                    bar.setIndicatorColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.white
                        )
                    )
                } else {
                    bar.setProgress(0)
                    bar.setIndicatorColor(
                        androidx.core.content.ContextCompat.getColor(
                            this,
                            R.color.grey
                        )
                    )
                }
            }
        }


        ivStoryMedia!!.setVisibility(android.view.View.GONE)
        pvStoryVideo.setVisibility(android.view.View.GONE)
        pbActivityLoading.setVisibility(android.view.View.VISIBLE) // إظهار التحميل عند تبديل القصة
        releasePlayer() // تحرير المشغل القديم

        if (story.getMediaType() == StoryModel.Companion.MEDIA_TYPE_IMAGE) {
            ivStoryMedia!!.setVisibility(android.view.View.VISIBLE)
            if (ivStoryMedia!!.getContext() != null && story.getMediaUrl() != null && !story.getMediaUrl()
                    .isEmpty()
            ) {
                Glide.with(ivStoryMedia!!.getContext()).load(story.getMediaUrl())
                    .placeholder(R.color.m3_outline).into(ivStoryMedia)
            }
            pbActivityLoading.setVisibility(android.view.View.GONE)
            startStoryTimer(if (story.getDuration() > 0) story.getDuration() else 5000) // مؤقت للصور
        } else if (story.getMediaType() == StoryModel.Companion.MEDIA_TYPE_VIDEO) {
            pvStoryVideo.setVisibility(android.view.View.VISIBLE)
            initializePlayer(story.getMediaUrl(), story.getDuration()) // تمرير مدة الفيديو
            pbActivityLoading.setVisibility(android.view.View.GONE) // سيتم إخفاؤه بواسطة مستمع ExoPlayer
        } else {
            android.util.Log.w(
                StoryViewerActivity.Companion.TAG,
                "Unknown media type for story: " + story.getMediaType()
            )
            pbActivityLoading.setVisibility(android.view.View.GONE)
            Toast.makeText(this, "Unsupported story format.", Toast.LENGTH_SHORT).show()
            // يمكنك الانتقال للقصة التالية أو إنهاء النشاط
            storyAdvanceHandler.postDelayed(
                java.lang.Runnable { this.showNextStorySegment() },
                100
            ) // محاولة الانتقال بعد تأخير بسيط
        }

        markStoryAsViewed(story.getStoryId())
    }


    private fun startStoryTimer(durationMillis: kotlin.Long) {
        // إيقاف أي مؤقت سابق
        storyAdvanceHandler.removeCallbacksAndMessages(null)
        // بدء متحرك شريط التقدم
        startCurrentProgressBarAnimation(durationMillis)

        // لا حاجة لمؤقت منفصل إذا كان المتحرك سيتولى الانتقال في onAnimationEnd
    }

    private fun initializePlayer(videoUrl: kotlin.String?, videoDuration: kotlin.Long) {
        if (player == null) {
            player = ExoPlayer.Builder(this).build()
            pvStoryVideo.setPlayer(player)
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (pbActivityLoading != null) { // تحقق من null
                        pbActivityLoading.setVisibility(if (state == Player.STATE_BUFFERING) android.view.View.VISIBLE else android.view.View.GONE)
                    }
                    if (state == Player.STATE_READY) {
                        // بدء متحرك شريط التقدم عندما يكون الفيديو جاهزًا
                        val actualDuration = player.getDuration() // المدة الفعلية للفيديو
                        if (actualDuration > 0) {
                            startCurrentProgressBarAnimation(actualDuration)
                        } else if (videoDuration > 0) { // استخدام المدة المخزنة إذا لم تكن مدة ExoPlayer متاحة فوراً
                            startCurrentProgressBarAnimation(videoDuration)
                        } else {
                            startCurrentProgressBarAnimation(5000) // قيمة افتراضية إذا لم تتوفر مدة
                        }
                    } else if (state == Player.STATE_ENDED) {
                        // لا حاجة لـ showNextStorySegment هنا، لأن onAnimationEnd للمتحرك سيتولى ذلك
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    android.util.Log.e(
                        StoryViewerActivity.Companion.TAG,
                        "ExoPlayer error: " + error.message
                    )
                    Toast.makeText(
                        this@StoryViewerActivity,
                        "Error playing video",
                        Toast.LENGTH_SHORT
                    ).show()
                    pbActivityLoading.setVisibility(android.view.View.GONE)
                    showNextStorySegment() // انتقل للقصة التالية عند حدوث خطأ
                }
            })
        }
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(videoUrl)))
        player.prepare()
        player.play()
    }

    private fun startCurrentProgressBarAnimation(durationMillis: kotlin.Long) {
        if (currentStoryIndexInList < 0 || currentStoryIndexInList >= progressAnimators.size) {
            android.util.Log.e(
                StoryViewerActivity.Companion.TAG,
                "Attempted to start animation for an invalid progress bar index: " + currentStoryIndexInList
            )
            return
        }
        // إيقاف أي متحرك سابق يعمل لنفس الشريط (احتياطي)
        val existingAnimator: ValueAnimator = progressAnimators.get(currentStoryIndexInList)
        if (existingAnimator.isRunning()) {
            existingAnimator.cancel()
        }

        // إعادة تعيين تقدم الشريط المرئي
        if (progressBarsContainer.getChildAt(currentStoryIndexInList) is LinearProgressIndicator) {
            (progressBarsContainer.getChildAt(currentStoryIndexInList) as LinearProgressIndicator).setProgress(
                0
            )
        }

        // تعيين مدة المتحرك وبدءه
        existingAnimator.setDuration(if (durationMillis > 0) durationMillis else 5000) // مدة افتراضية إذا كانت 0
        existingAnimator.start()
        android.util.Log.d(
            StoryViewerActivity.Companion.TAG,
            "Started progress animation for story " + currentStoryIndexInList + " with duration: " + durationMillis
        )
    }

    private fun stopCurrentProgressAnimation() {
        if (currentStoryIndexInList >= 0 && currentStoryIndexInList < progressAnimators.size) {
            val animator: ValueAnimator? = progressAnimators.get(currentStoryIndexInList)
            if (animator != null && animator.isRunning()) {
                animator.cancel()
                android.util.Log.d(
                    StoryViewerActivity.Companion.TAG,
                    "Stopped progress animation for story " + currentStoryIndexInList
                )
            }
        }
    }

    private fun stopAllProgressAnimations() {
        for (animator in progressAnimators) {
            if (animator.isRunning()) {
                animator.cancel()
            }
        }
        android.util.Log.d(StoryViewerActivity.Companion.TAG, "Stopped all progress animations.")
    }


    private fun releasePlayer() {
        if (player != null) {
            player.stop()
            player.release()
            player = null
        }
    }

    private fun showNextStorySegment() {
        stopCurrentProgressAnimation() // أوقف المتحرك الحالي قبل الانتقال
        if (currentStoryIndexInList < currentUserStories.size - 1) {
            currentStoryIndexInList++
            displayStorySegment()
        } else {
            android.util.Log.d(
                StoryViewerActivity.Companion.TAG,
                "Finished all stories for this user."
            )
            finish() // انتهت جميع قصص المستخدم الحالي
        }
    }

    private fun showPreviousStorySegment() {
        stopCurrentProgressAnimation() // أوقف المتحرك الحالي قبل الانتقال
        if (currentStoryIndexInList > 0) {
            currentStoryIndexInList--
            displayStorySegment()
        } else {
            // إذا كنا في القصة الأولى، أعد تشغيلها أو انتقل للمستخدم السابق
            currentStoryIndexInList = 0 // أعد تشغيل القصة الأولى
            displayStorySegment()
        }
    }


    private fun markStoryAsViewed(storyId: kotlin.String?) {
        val currentAuthUserId: kotlin.String? =
            if (auth.getCurrentUser() != null) auth.getCurrentUser().getUid() else null
        if (currentAuthUserId == null || storyId == null || storyId.isEmpty()) return

        db.collection("stories").document(storyId)
            .update("viewers." + currentAuthUserId, true)
            .addOnSuccessListener({ aVoid ->
                android.util.Log.d(
                    StoryViewerActivity.Companion.TAG,
                    "Story " + storyId + " marked as viewed by " + currentAuthUserId
                )
            })
            .addOnFailureListener({ e ->
                android.util.Log.e(
                    StoryViewerActivity.Companion.TAG,
                    "Failed to mark story as viewed",
                    e
                )
            })
    }


    override fun onPause() {
        super.onPause()
        releasePlayer()
        stopAllProgressAnimations() // إيقاف جميع المتحركات عند الإيقاف المؤقت
        storyAdvanceHandler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        // إذا كان هناك قصة حالية، استأنف المؤقت أو الفيديو
        if (!currentUserStories.isEmpty() && currentStoryIndexInList < currentUserStories.size) {
            val currentStory: StoryModel = currentUserStories.get(currentStoryIndexInList)
            if (currentStory.getMediaType() == StoryModel.Companion.MEDIA_TYPE_IMAGE) {
                // لا تستدعي startStoryTimer مباشرة هنا، لأن displayStorySegment ستقوم بذلك
                // إذا كان المتحرك قد توقف، displayStorySegment سيعيد تشغيله
                // فقط تأكد من أن displayStorySegment يتم استدعاؤها إذا لزم الأمر (مثلاً، إذا كانت هذه أول مرة)
            } else if (currentStory.getMediaType() == StoryModel.Companion.MEDIA_TYPE_VIDEO && player != null) {
                player.play() // استئناف الفيديو
                // المتحرك سيبدأ عندما يكون الفيديو جاهزًا
            }
            // قد تحتاج إلى إعادة عرض القصة الحالية إذا تم إيقاف المتحرك بالكامل
            if (currentStoryIndexInList >= 0 && currentStoryIndexInList < progressAnimators.size) {
                if (!progressAnimators.get(currentStoryIndexInList).isRunning()) {
                    // displayStorySegment(); // كن حذرًا من الاستدعاءات المتكررة
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (storyListener != null) {
            storyListener.remove()
        }
        if (authorInfoListener != null) {
            authorInfoListener.remove()
        }
        releasePlayer()
        stopAllProgressAnimations()
        storyAdvanceHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "StoryViewerActivity"
        const val EXTRA_USER_ID: kotlin.String = "userId" // ★ تعريف الثابت
    }
}