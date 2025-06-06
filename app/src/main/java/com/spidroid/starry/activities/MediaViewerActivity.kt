package com.spidroid.starry.activities

import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.ControllerVisibilityListener
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.chrisbanes.photoview.PhotoView
import com.spidroid.starry.R
import com.spidroid.starry.activities.MediaViewerActivity.MediaPagerAdapter.VideoViewHolder
import java.io.File
import java.net.URLConnection
import kotlin.math.max
import kotlin.math.min

class MediaViewerActivity : AppCompatActivity() {
    private var viewPager: ViewPager2? = null
    private var mediaUrls: ArrayList<String?>? = null
    private var currentPosition = 0
    private var btnMenu: ImageButton? = null
    private val loadingProgressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_viewer)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Initialize data
        val intent = getIntent()
        mediaUrls = intent.getStringArrayListExtra("media_urls")
        currentPosition = intent.getIntExtra("position", 0)

        if (mediaUrls == null || mediaUrls!!.isEmpty()) {
            Toast.makeText(this, "No media to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupViewPager()
        setupButtonListeners()
    }

    private fun initializeViews() {
        viewPager = findViewById<ViewPager2?>(R.id.viewPager)
        btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        findViewById<View?>(R.id.btnClose).setOnClickListener(View.OnClickListener { v: View? -> supportFinishAfterTransition() })
        findViewById<View?>(R.id.btnShare).setOnClickListener(View.OnClickListener { v: View? -> shareCurrentMedia() })
    }

    private fun setupViewPager() {
        val adapter = MediaPagerAdapter()
        viewPager!!.setAdapter(adapter)
        viewPager!!.setCurrentItem(currentPosition, false)
        viewPager!!.setOffscreenPageLimit(1)

        viewPager!!.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPosition = position
                }
            })
    }

    private fun setupButtonListeners() {
        btnMenu!!.setOnClickListener(View.OnClickListener { v: View? -> showMenu() })
    }

    private inner class MediaPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
        override fun getItemViewType(position: Int): Int {
            return if (isVideo(mediaUrls!!.get(position)!!)) MediaPagerAdapter.Companion.TYPE_VIDEO else MediaPagerAdapter.Companion.TYPE_IMAGE
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == MediaPagerAdapter.Companion.TYPE_VIDEO) {
                return VideoViewHolder(
                    LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.media_item_video, parent, false)
                )
            }
            return ImageViewHolder(
                LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.media_item_image, parent, false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val url = mediaUrls!!.get(position)
            if (holder is VideoViewHolder) {
                holder.bind(url)
            } else if (holder is ImageViewHolder) {
                holder.bind(url)
            }
        }

        override fun getItemCount(): Int {
            return mediaUrls!!.size
        }

        fun isVideo(url: String): Boolean {
            try {
                val uri = Uri.parse(url)
                val mimeType = URLConnection.guessContentTypeFromName(uri.getLastPathSegment())
                return mimeType != null && mimeType.startsWith("video/")
            } catch (e: Exception) {
                return url.matches(".*\\.(mp4|mov|mkv|webm|3gp|avi|m3u8)(\\?.*)?$".toRegex())
            }
        }

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val photoView: PhotoView

            init {
                photoView = itemView.findViewById<PhotoView>(R.id.photoView)
                photoView.setOnClickListener(View.OnClickListener { v: View? -> toggleControls() })
            }

            fun bind(url: String?) {
                Glide.with(itemView)
                    .load(url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_cover_placeholder)
                    .into(photoView)
            }
        }

        inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val playerView: PlayerView
            private var player: ExoPlayer? = null
            private val loadingProgressBar: ProgressBar

            init {
                playerView = itemView.findViewById<PlayerView>(R.id.playerView)
                loadingProgressBar = itemView.findViewById<ProgressBar>(R.id.loadingProgressBar)

                playerView.setControllerVisibilityListener(
                    ControllerVisibilityListener { visibility: Int ->
                        if (visibility == View.VISIBLE) showControls()
                        else hideControls()
                    })
            }

            fun bind(url: String?) {
                initializePlayer()
                loadingProgressBar.setVisibility(View.VISIBLE)

                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                player!!.setMediaItem(mediaItem)
                player!!.prepare()

                player!!.addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                                loadingProgressBar.setVisibility(View.GONE)
                            } else if (state == Player.STATE_BUFFERING) {
                                loadingProgressBar.setVisibility(View.VISIBLE)
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            loadingProgressBar.setVisibility(View.GONE)
                            Toast.makeText(
                                itemView.getContext(),
                                "Error playing video",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    })

                player!!.play()
            }

            private fun initializePlayer() {
                if (player == null) {
                    player = ExoPlayer.Builder(itemView.getContext()).build()
                    player!!.setRepeatMode(Player.REPEAT_MODE_ONE)
                    playerView.setPlayer(player)
                }
            }

            fun releasePlayer() {
                if (player != null) {
                    player!!.stop()
                    player!!.release()
                    player = null
                }
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            if (holder is VideoViewHolder) {
                holder.releasePlayer()
            }
        }

        companion object {
            private const val TYPE_IMAGE = 1
            private const val TYPE_VIDEO = 2
        }
    }

    private fun toggleControls() {
        val controls = findViewById<View?>(R.id.topControls)
        if (controls != null) {
            controls.setVisibility(if (controls.getVisibility() == View.VISIBLE) View.GONE else View.VISIBLE)
        }
    }

    private fun showControls() {
        val controls = findViewById<View?>(R.id.topControls)
        if (controls != null) {
            controls.setVisibility(View.VISIBLE)
        }
    }

    private fun hideControls() {
        val controls = findViewById<View?>(R.id.topControls)
        if (controls != null) {
            controls.setVisibility(View.GONE)
        }
    }

    private fun shareCurrentMedia() {
        if (currentPosition < 0 || currentPosition >= mediaUrls!!.size) return

        val url = mediaUrls!!.get(currentPosition)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.setType(getMimeType(url))
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url))
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun downloadMedia(url: String?) {
        val request = DownloadManager.Request(Uri.parse(url))
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "Starry_" + System.currentTimeMillis() + getFileExtension(url)

        request.setDestinationUri(Uri.fromFile(File(dir, fileName)))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager?
        if (dm != null) dm.enqueue(request)
    }

    private fun getFileExtension(url: String?): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)
        return if (ext != null && !ext.isEmpty()) "." + ext else ".mp4"
    }

    private fun getMimeType(url: String?): String {
        val type =
            MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
        return if (type != null) type else "image/*"
    }

    private fun showMenu() {
        val popup = PopupMenu(this, btnMenu)
        popup.getMenuInflater().inflate(R.menu.menu_media_download, popup.getMenu())
        popup.setOnMenuItemClickListener(
            PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
                if (item!!.getItemId() == R.id.action_download) {
                    downloadMedia(mediaUrls!!.get(currentPosition))
                    return@setOnMenuItemClickListener true
                }
                false
            })
        popup.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewPager != null) {
            val recyclerView = viewPager!!.getChildAt(0) as RecyclerView
            for (i in 0..<recyclerView.getChildCount()) {
                val holder =
                    recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
                if (holder is VideoViewHolder) {
                    holder.releasePlayer()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    companion object {
        fun launch(activity: Activity, urls: ArrayList<String?>?, pos: Int, sharedView: View?) {
            val intent = Intent(activity, MediaViewerActivity::class.java)
            intent.putStringArrayListExtra("media_urls", urls)
            intent.putExtra("position", pos)

            if (sharedView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity, sharedView, "media_transition"
                    )
                ActivityCompat.startActivity(activity, intent, options.toBundle())
            } else {
                activity.startActivity(intent)
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }

        fun launchWithoutTransition(
            activity: Activity, mediaUrls: ArrayList<String?>, position: Int
        ) {
            val intent = Intent(activity, MediaViewerActivity::class.java)
            intent.putStringArrayListExtra("media_urls", mediaUrls)
            intent.putExtra(
                "position",
                max(0.0, min(position.toDouble(), (mediaUrls.size - 1).toDouble())).toInt()
            )
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}
