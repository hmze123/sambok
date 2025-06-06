package com.spidroid.starry.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityStoryViewerBinding
import com.spidroid.starry.models.StoryModel
import com.spidroid.starry.models.UserModel
import java.util.*

class StoryViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoryViewerBinding
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    private var storyListener: ListenerRegistration? = null
    private var authorInfoListener: ListenerRegistration? = null

    private var player: ExoPlayer? = null
    private var currentProgressAnimator: ValueAnimator? = null

    private val storiesForUser = mutableListOf<StoryModel>()
    private var viewedUserId: String? = null
    private var currentStoryIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        viewedUserId = intent.getStringExtra(EXTRA_USER_ID)
        if (viewedUserId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
        loadAuthorInfoAndStories()
    }

    private fun setupListeners() {
        binding.viewNext.setOnClickListener { showNextStorySegment() }
        binding.viewPrevious.setOnClickListener { showPreviousStorySegment() }
        binding.ivClose.setOnClickListener { finish() }
    }

    private fun loadAuthorInfoAndStories() {
        showLoading(true)
        authorInfoListener?.remove()
        authorInfoListener = db.collection("users").document(viewedUserId!!)
            .addSnapshotListener { snapshot, e ->
                if (isFinishing || isDestroyed) return@addSnapshotListener
                if (e != null || snapshot == null || !snapshot.exists()) {
                    Log.e(TAG, "Error loading author info or author does not exist.", e)
                    Toast.makeText(this, "Could not load user data.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addSnapshotListener
                }
                val storyAuthor = snapshot.toObject<UserModel>()
                storyAuthor?.let {
                    bindAuthorInfo(it)
                    loadStoriesForUser()
                }
            }
    }

    private fun bindAuthorInfo(author: UserModel) {
        binding.tvStoryAuthorName.text = author.displayName ?: author.username
        Glide.with(this)
            .load(author.profileImageUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .into(binding.ivStoryAuthorAvatar)
    }

    private fun loadStoriesForUser() {
        storyListener?.remove()
        storyListener = db.collection("stories")
            .whereEqualTo("userId", viewedUserId)
            .whereGreaterThan("expiresAt", Date())
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { querySnapshot, e ->
                if (isFinishing || isDestroyed) return@addSnapshotListener
                showLoading(false)
                if (e != null || querySnapshot == null) {
                    Log.e(TAG, "Error fetching stories.", e)
                    if (storiesForUser.isEmpty()) finish()
                    return@addSnapshotListener
                }

                storiesForUser.clear()
                storiesForUser.addAll(querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject<StoryModel>()?.apply { storyId = doc.id }
                })

                if (storiesForUser.isEmpty()) {
                    Toast.makeText(this, "No active stories for this user.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    currentStoryIndex = 0
                    setupProgressBarsUI()
                    displayCurrentStory()
                }
            }
    }

    private fun setupProgressBarsUI() {
        binding.progressBarsContainer.removeAllViews()
        storiesForUser.forEach { _ ->
            val progressBar = LinearProgressIndicator(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, resources.getDimensionPixelSize(R.dimen.story_progress_height), 1f).also {
                    val margin = (2 * resources.displayMetrics.density).toInt()
                    it.setMargins(margin, 0, margin, 0)
                }
                max = 100
                progress = 0
                trackColor = ContextCompat.getColor(this@StoryViewerActivity, R.color.story_progress_inactive_track)
                setIndicatorColor(ContextCompat.getColor(this@StoryViewerActivity, R.color.story_progress_active))
            }
            binding.progressBarsContainer.addView(progressBar)
        }
    }

    private fun displayCurrentStory() {
        if (currentStoryIndex !in storiesForUser.indices) {
            finish()
            return
        }

        releasePlayer()
        currentProgressAnimator?.cancel()

        val story = storiesForUser[currentStoryIndex]
        updateProgressBars(currentStoryIndex)

        binding.tvStoryTimestamp.text = DateUtils.getRelativeTimeSpanString(
            story.createdAt?.time ?: System.currentTimeMillis(),
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS
        )

        binding.pbStoryItemLoading.visibility = View.VISIBLE

        if (story.mediaType == StoryModel.MEDIA_TYPE_VIDEO) {
            binding.pvStoryVideo.visibility = View.VISIBLE
            binding.ivStoryMedia.visibility = View.GONE
            initializePlayer(story)
        } else {
            binding.pvStoryVideo.visibility = View.GONE
            binding.ivStoryMedia.visibility = View.VISIBLE
            Glide.with(this)
                .load(story.mediaUrl)
                .into(binding.ivStoryMedia)
            binding.pbStoryItemLoading.visibility = View.GONE
            startStoryTimer(story.duration)
        }

        markStoryAsViewed(story.storyId)
    }

    private fun updateProgressBars(activeIndex: Int) {
        for (i in 0 until binding.progressBarsContainer.childCount) {
            val bar = binding.progressBarsContainer.getChildAt(i) as? LinearProgressIndicator
            bar?.progress = if (i < activeIndex) 100 else 0
        }
    }

    private fun startStoryTimer(durationMillis: Long) {
        val progressBar = binding.progressBarsContainer.getChildAt(currentStoryIndex) as? LinearProgressIndicator ?: return
        currentProgressAnimator = ValueAnimator.ofInt(progressBar.progress, 100).apply {
            duration = durationMillis * (100 - progressBar.progress) / 100
            interpolator = LinearInterpolator()
            addUpdateListener {
                progressBar.progress = it.animatedValue as Int
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    showNextStorySegment()
                }
            })
            start()
        }
    }

    private fun initializePlayer(story: StoryModel) {
        player = ExoPlayer.Builder(this).build().also {
            binding.pvStoryVideo.player = it
            it.setMediaItem(MediaItem.fromUri(story.mediaUrl))
            it.playWhenReady = true
            it.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        binding.pbStoryItemLoading.visibility = View.GONE
                        startStoryTimer(it.duration.takeIf { d -> d > 0 } ?: StoryModel.DEFAULT_IMAGE_DURATION_MS)
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(this@StoryViewerActivity, "Error playing video", Toast.LENGTH_SHORT).show()
                    showNextStorySegment()
                }
            })
            it.prepare()
        }
    }

    private fun showNextStorySegment() {
        if (currentStoryIndex < storiesForUser.size - 1) {
            currentStoryIndex++
            displayCurrentStory()
        } else {
            finish()
        }
    }

    private fun showPreviousStorySegment() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            displayCurrentStory()
        }
    }

    private fun markStoryAsViewed(storyId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("viewed_stories").document(storyId).set(mapOf("viewedAt" to Date()))
            .addOnFailureListener { e -> Log.e(TAG, "Failed to mark story as viewed", e) }
    }

    private fun releasePlayer() {
        player?.stop()
        player?.release()
        player = null
    }

    private fun showLoading(isLoading: Boolean) {
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onPause() {
        super.onPause()
        currentProgressAnimator?.pause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        currentProgressAnimator?.resume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        storyListener?.remove()
        authorInfoListener?.remove()
        currentProgressAnimator?.cancel()
        releasePlayer()
    }

    companion object {
        private const val TAG = "StoryViewerActivity"
        const val EXTRA_USER_ID = "userId"
    }
}