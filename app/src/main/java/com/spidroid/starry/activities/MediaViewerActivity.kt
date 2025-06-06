// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/MediaViewerActivity.kt
package com.spidroid.starry.activities

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityMediaViewerBinding
import com.spidroid.starry.databinding.MediaItemImageBinding
import com.spidroid.starry.databinding.MediaItemVideoBinding
import java.io.File
import kotlin.math.max
import kotlin.math.min

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewerBinding
    private var mediaUrls: ArrayList<String> = arrayListOf()
    private var currentPosition = 0

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        initializePermissionLauncher()

        // تأكد من تصفية القيم null إذا كان EXTRA_MEDIA_URLS قد يحتوي عليها
        intent.getStringArrayListExtra(EXTRA_MEDIA_URLS)?.let {
            mediaUrls.addAll(it.filterNotNull())
        }
        currentPosition = intent.getIntExtra(EXTRA_POSITION, 0)

        if (mediaUrls.isEmpty()) {
            Toast.makeText(this, "No media to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViewPager()
        setupButtonListeners()
    }

    private fun initializePermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                downloadCurrentMedia()
            } else {
                Toast.makeText(this, "Storage permission is required to download media.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupViewPager() {
        val adapter = MediaPagerAdapter()
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentPosition, false)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                // عند تغيير الصفحة، تأكد من تحرير اللاعب السابق إذا كان فيديو
                val currentHolder = (binding.viewPager.adapter as? MediaPagerAdapter)?.getCurrentViewHolder(position)
                if (currentHolder is MediaPagerAdapter.VideoViewHolder) {
                    currentHolder.player?.play()
                }
                // وإيقاف اللاعب في الصفحة السابقة
                for (i in 0 until (binding.viewPager.adapter?.itemCount ?: 0)) {
                    if (i != position) {
                        val holder = (binding.viewPager.adapter as? MediaPagerAdapter)?.getCurrentViewHolder(i)
                        if (holder is MediaPagerAdapter.VideoViewHolder) {
                            holder.player?.pause()
                            holder.player?.seekTo(0) // إعادة تعيين الفيديو إلى البداية
                        }
                    }
                }
            }
        })
    }

    private fun setupButtonListeners() {
        binding.btnClose.setOnClickListener { supportFinishAfterTransition() }
        binding.btnShare.setOnClickListener { shareCurrentMedia() }
        binding.btnMenu.setOnClickListener { showMenu(it) }
    }

    private fun showMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_media_download, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_download -> {
                        checkStoragePermissionAndDownload()
                        true
                    }
                    else -> false
                }
            }
        }.show()
    }

    private fun checkStoragePermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed for scoped storage on Android 10+
            downloadCurrentMedia()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    downloadCurrentMedia()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun downloadCurrentMedia() {
        val url = mediaUrls.getOrNull(currentPosition) ?: return
        try {
            val fileName = "Starry_${System.currentTimeMillis()}${getFileExtension(url)}"
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            val dm = getSystemService(DOWNLOAD_SERVICE) as? DownloadManager
            dm?.enqueue(request)
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start download.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCurrentMedia() {
        val url = mediaUrls.getOrNull(currentPosition) ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(url) ?: "image/*" // Default to image if type is unknown
            putExtra(Intent.EXTRA_STREAM, Uri.parse(url))
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun getFileExtension(url: String?): String {
        return MimeTypeMap.getFileExtensionFromUrl(url)?.let { ".$it" } ?: ".jpg"
    }

    private fun getMimeType(url: String?): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private inner class MediaPagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val viewHolders = mutableMapOf<Int, RecyclerView.ViewHolder>() // لتتبع الـ ViewHolders النشطة

        private val TYPE_IMAGE = 1 // ✨ تم تغيير 'const val' إلى 'val'
        private val TYPE_VIDEO = 2 // ✨ تم تغيير 'const val' إلى 'val'

        override fun getItemViewType(position: Int): Int {
            return if (mediaUrls[position].contains(".mp4", ignoreCase = true) || mediaUrls[position].contains(".mov", ignoreCase = true)) TYPE_VIDEO else TYPE_IMAGE
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_VIDEO) {
                VideoViewHolder(MediaItemVideoBinding.inflate(inflater, parent, false))
            } else {
                ImageViewHolder(MediaItemImageBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val url = mediaUrls[position]
            viewHolders[position] = holder // حفظ الـ ViewHolder النشط
            when (holder) {
                is VideoViewHolder -> holder.bind(url)
                is ImageViewHolder -> holder.bind(url)
            }
        }

        override fun getItemCount(): Int = mediaUrls.size

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            if (holder is VideoViewHolder) {
                holder.releasePlayer()
            }
            viewHolders.values.remove(holder) // إزالة الـ ViewHolder المعاد تدويره
        }

        // دالة مساعدة للحصول على الـ ViewHolder الحالي
        fun getCurrentViewHolder(position: Int): RecyclerView.ViewHolder? {
            return viewHolders[position]
        }


        inner class ImageViewHolder(private val binding: MediaItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                binding.photoView.setOnClickListener { toggleControls() }
            }
            fun bind(url: String) {
                Glide.with(itemView)
                    .load(url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_cover_placeholder)
                    .into(binding.photoView)
            }
        }

        inner class VideoViewHolder(private val binding: MediaItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
            var player: ExoPlayer? = null

            init {
                binding.playerView.setOnClickListener { toggleControls() }
            }

            fun bind(url: String) {
                initializePlayer()
                player?.setMediaItem(MediaItem.fromUri(url))
                player?.prepare()
                //player?.play()
            }

            private fun initializePlayer() {
                if (player == null) {
                    player = ExoPlayer.Builder(itemView.context).build().also {
                        binding.playerView.player = it
                        it.repeatMode = Player.REPEAT_MODE_ONE
                        it.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                binding.loadingProgressBar.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                            }
                            override fun onPlayerError(error: PlaybackException) {
                                Toast.makeText(itemView.context, "Error playing video", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }

            fun releasePlayer() {
                player?.stop()
                player?.release()
                player = null
                binding.playerView.player = null
            }
        }
    }

    private fun toggleControls() {
        binding.topControls.visibility = if (binding.topControls.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        val adapter = binding.viewPager.adapter as? MediaPagerAdapter
        for (i in 0 until (adapter?.itemCount ?: 0)) {
            val holder = adapter?.getCurrentViewHolder(i)
            if (holder is MediaPagerAdapter.VideoViewHolder) {
                holder.player?.pause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val adapter = binding.viewPager.adapter as? MediaPagerAdapter
        val currentHolder = adapter?.getCurrentViewHolder(binding.viewPager.currentItem)
        if (currentHolder is MediaPagerAdapter.VideoViewHolder) {
            currentHolder.player?.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val adapter = binding.viewPager.adapter as? MediaPagerAdapter
        for (i in 0 until (adapter?.itemCount ?: 0)) {
            val holder = adapter?.getCurrentViewHolder(i)
            if (holder is MediaPagerAdapter.VideoViewHolder) {
                holder.releasePlayer()
            }
        }
    }

    companion object {
        private const val EXTRA_MEDIA_URLS = "media_urls"
        private const val EXTRA_POSITION = "position"

        @JvmStatic
        fun launch(activity: Activity, urls: ArrayList<String?>, pos: Int, sharedView: View?) {
            val intent = Intent(activity, MediaViewerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_MEDIA_URLS, urls)
                putExtra(EXTRA_POSITION, max(0, min(pos, urls.size - 1)))
            }

            if (sharedView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedView, "media_transition")
                ActivityCompat.startActivity(activity, intent, options.toBundle())
            } else {
                activity.startActivity(intent)
                activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }
    }
}