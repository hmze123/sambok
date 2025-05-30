package com.spidroid.starry.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import com.bumptech.glide.Glide;
// تم تحديث سطر الاستيراد هذا
import androidx.media3.exoplayer.ExoPlayer; // <--- التعديل هنا
//import com.google.android.exoplayer2.MediaItem;
import androidx.media3.ui.PlayerView;
import com.spidroid.starry.R;

// Fragment لعرض قصة واحدة (صورة أو فيديو)
public class StoryContentFragment extends Fragment { // تم جعل الكلاس public وفي ملفه الخاص
    private static final String ARG_MEDIA_URL = "media_url";
    private static final String ARG_MEDIA_TYPE = "media_type";
    private static final String ARG_CAPTION = "caption";

    private String mediaUrl;
    private String mediaType;
    private String caption;

    private ImageView ivStoryImage;
    private PlayerView pvStoryVideo;
    private ExoPlayer player;
    private TextView tvCaption;

    public static StoryContentFragment newInstance(String mediaUrl, String mediaType, String caption) {
        StoryContentFragment fragment = new StoryContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDIA_URL, mediaUrl);
        args.putString(ARG_MEDIA_TYPE, mediaType);
        args.putString(ARG_CAPTION, caption);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mediaUrl = getArguments().getString(ARG_MEDIA_URL);
            mediaType = getArguments().getString(ARG_MEDIA_TYPE);
            caption = getArguments().getString(ARG_CAPTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // يمكنك استخدام تخطيط مختلف هنا إذا كنت تريد تخطيطًا مختلفًا للصور والفيديوهات
        // أو دمج الاثنين في تخطيط واحد مع التحكم في الرؤية.
        // للتوضيح، سأفترض أن media_item_video يحتوي على PhotoView أو ImageView ليعرض الصور.
        View view = inflater.inflate(R.layout.media_item_video, container, false); // استخدام نفس التخطيط لـ PlayerView

        ivStoryImage = view.findViewById(R.id.photoView); // يجب أن يكون موجودًا في media_item_video أو layout آخر
        pvStoryVideo = view.findViewById(R.id.playerView);
        // ستحتاج لإضافة TextView للـ caption في الـ layout المستخدم (مثلاً media_item_video) أو ملف جديد
        tvCaption = view.findViewById(R.id.tv_story_caption_overlay); // تأكد من إضافة هذا الـ ID في media_item_video.xml

        // إخفاء/إظهار بناءً على نوع الميديا
        if ("image".equals(mediaType)) {
            if (pvStoryVideo != null) pvStoryVideo.setVisibility(View.GONE);
            if (ivStoryImage != null) {
                ivStoryImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(mediaUrl).into(ivStoryImage);
            }
        } else if ("video".equals(mediaType)) {
            if (ivStoryImage != null) ivStoryImage.setVisibility(View.GONE);
            if (pvStoryVideo != null) {
                pvStoryVideo.setVisibility(View.VISIBLE);
                setupVideoPlayer();
            }
        }

        if (tvCaption != null) {
            if (caption != null && !caption.isEmpty()) {
                tvCaption.setText(caption);
                tvCaption.setVisibility(View.VISIBLE);
            } else {
                tvCaption.setVisibility(View.GONE);
            }
        } else {
            // يمكن إضافة Toast أو Log إذا لم يتم العثور على tvCaption
        }


        return view;
    }

    private void setupVideoPlayer() {
        if (player == null && getContext() != null) {
            player = new ExoPlayer.Builder(requireContext()).build();
            pvStoryVideo.setPlayer(player);
            player.setMediaItem(MediaItem.fromUri(mediaUrl));
            player.prepare();
            player.setPlayWhenReady(true);
            player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE); // تكرار الفيديو
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
            // يمكنك أيضاً استئناف التقدم الزمني للقصة الرئيسية هنا إذا لزم الأمر
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}