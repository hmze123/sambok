// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/StoryContentFragment.kt
package com.spidroid.starry.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException // ✨ تم التأكد من الاستيراد
import androidx.media3.common.Player // ✨ تم التأكد من الاستيراد
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.spidroid.starry.R // ✨ تم التأكد من الاستيراد
import com.spidroid.starry.databinding.MediaItemVideoBinding
import com.spidroid.starry.models.StoryModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoryContentFragment : Fragment() {

    private var _binding: MediaItemVideoBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null
    private var mediaUrl: String? = null
    private var mediaType: String? = null
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private var loadingProgressBar: ProgressBar? = null

    // ✨ تم دمج كلا كتلي companion object هنا
    companion object {
        private const val TAG = "StoryContentFragment"
        private const val ARG_MEDIA_URL = "media_url"
        private const val ARG_MEDIA_TYPE = "media_type"

        @JvmStatic
        fun newInstance(mediaUrl: String, mediaType: String) =
            StoryContentFragment().apply {
                arguments = bundleOf(
                    ARG_MEDIA_URL to mediaUrl,
                    ARG_MEDIA_TYPE to mediaType
                )
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mediaUrl = it.getString(ARG_MEDIA_URL)
            mediaType = it.getString(ARG_MEDIA_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MediaItemVideoBinding.inflate(inflater, container, false)
        loadingProgressBar = binding.root.findViewById(R.id.loadingProgressBar)
        return binding.root
    }

    private fun initializePlayer() {
        val safeContext = context ?: return
        val url = mediaUrl ?: return

        if (mediaType == StoryModel.MEDIA_TYPE_VIDEO) {
            player = ExoPlayer.Builder(safeContext)
                .build()
                .also { exoPlayer ->
                    binding.playerView.player = exoPlayer
                    val mediaItem = MediaItem.fromUri(url)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.playWhenReady = playWhenReady
                    exoPlayer.seekTo(currentItem, playbackPosition)
                    exoPlayer.prepare()

                    exoPlayer.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            loadingProgressBar?.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                        }
                        // ✨ تم إضافة دالة onPlayerError المفقودة لتتوافق مع Player.Listener
                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "ExoPlayer error: ${error.message}", error)
                            // يمكنك إضافة Toast أو رسالة للمستخدم هنا
                        }
                    })
                }
            binding.playerView.visibility = View.VISIBLE
            binding.photoView.visibility = View.GONE
        } else {
            binding.playerView.visibility = View.GONE
            binding.photoView.visibility = View.VISIBLE
            Glide.with(this).load(url).into(binding.photoView)
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.stop()
            exoPlayer.release()
        }
        player = null
    }

    override fun onStart() {
        super.onStart()
        if (androidx.media3.common.util.Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if ((androidx.media3.common.util.Util.SDK_INT <= 23 || player == null)) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (androidx.media3.common.util.Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (androidx.media3.common.util.Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        loadingProgressBar = null
    }
}