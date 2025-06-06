package com.spidroid.starry.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R

// تم تحديث سطر الاستيراد هذا
// <--- التعديل هنا
//import com.google.android.exoplayer2.MediaItem;
// Fragment لعرض قصة واحدة (صورة أو فيديو)
class StoryContentFragment : Fragment() {
    private var mediaUrl: String? = null
    private var mediaType: String? = null
    private var caption: String? = null

    private var ivStoryImage: ImageView? = null
    private var pvStoryVideo: PlayerView? = null
    private var player: ExoPlayer? = null
    private var tvCaption: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getArguments() != null) {
            mediaUrl = getArguments()!!.getString(StoryContentFragment.Companion.ARG_MEDIA_URL)
            mediaType = getArguments()!!.getString(StoryContentFragment.Companion.ARG_MEDIA_TYPE)
            caption = getArguments()!!.getString(StoryContentFragment.Companion.ARG_CAPTION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // يمكنك استخدام تخطيط مختلف هنا إذا كنت تريد تخطيطًا مختلفًا للصور والفيديوهات
        // أو دمج الاثنين في تخطيط واحد مع التحكم في الرؤية.
        // للتوضيح، سأفترض أن media_item_video يحتوي على PhotoView أو ImageView ليعرض الصور.
        val view = inflater.inflate(
            R.layout.media_item_video,
            container,
            false
        ) // استخدام نفس التخطيط لـ PlayerView

        ivStoryImage =
            view.findViewById<ImageView?>(R.id.photoView) // يجب أن يكون موجودًا في media_item_video أو layout آخر
        pvStoryVideo = view.findViewById<PlayerView?>(R.id.playerView)
        // ستحتاج لإضافة TextView للـ caption في الـ layout المستخدم (مثلاً media_item_video) أو ملف جديد
        tvCaption =
            view.findViewById<TextView?>(R.id.tv_story_caption_overlay) // تأكد من إضافة هذا الـ ID في media_item_video.xml

        // إخفاء/إظهار بناءً على نوع الميديا
        if ("image" == mediaType) {
            if (pvStoryVideo != null) pvStoryVideo!!.setVisibility(View.GONE)
            if (ivStoryImage != null) {
                ivStoryImage!!.setVisibility(View.VISIBLE)
                Glide.with(this).load(mediaUrl).into(ivStoryImage!!)
            }
        } else if ("video" == mediaType) {
            if (ivStoryImage != null) ivStoryImage!!.setVisibility(View.GONE)
            if (pvStoryVideo != null) {
                pvStoryVideo!!.setVisibility(View.VISIBLE)
                setupVideoPlayer()
            }
        }

        if (tvCaption != null) {
            if (caption != null && !caption!!.isEmpty()) {
                tvCaption!!.setText(caption)
                tvCaption!!.setVisibility(View.VISIBLE)
            } else {
                tvCaption!!.setVisibility(View.GONE)
            }
        } else {
            // يمكن إضافة Toast أو Log إذا لم يتم العثور على tvCaption
        }


        return view
    }

    private fun setupVideoPlayer() {
        if (player == null && getContext() != null) {
            player = ExoPlayer.Builder(requireContext()).build()
            pvStoryVideo!!.setPlayer(player)
            player!!.setMediaItem(MediaItem.fromUri(mediaUrl!!))
            player!!.prepare()
            player!!.setPlayWhenReady(true)
            player!!.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE) // تكرار الفيديو
        }
    }

    override fun onResume() {
        super.onResume()
        if (player != null) {
            player!!.setPlayWhenReady(true)
            // يمكنك أيضاً استئناف التقدم الزمني للقصة الرئيسية هنا إذا لزم الأمر
        }
    }

    override fun onPause() {
        super.onPause()
        if (player != null) {
            player!!.setPlayWhenReady(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (player != null) {
            player!!.release()
            player = null
        }
    }

    companion object {
        // تم جعل الكلاس public وفي ملفه الخاص
        private const val ARG_MEDIA_URL = "media_url"
        private const val ARG_MEDIA_TYPE = "media_type"
        private const val ARG_CAPTION = "caption"

        fun newInstance(
            mediaUrl: String?,
            mediaType: String?,
            caption: String?
        ): StoryContentFragment {
            val fragment = StoryContentFragment()
            val args = Bundle()
            args.putString(StoryContentFragment.Companion.ARG_MEDIA_URL, mediaUrl)
            args.putString(StoryContentFragment.Companion.ARG_MEDIA_TYPE, mediaType)
            args.putString(StoryContentFragment.Companion.ARG_CAPTION, caption)
            fragment.setArguments(args)
            return fragment
        }
    }
}