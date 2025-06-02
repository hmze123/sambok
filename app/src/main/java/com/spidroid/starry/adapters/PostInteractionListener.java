package com.spidroid.starry.adapters;

import android.view.View;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import java.util.List;

public interface PostInteractionListener {
    // التفاعلات الأساسية
    void onLikeClicked(PostModel post);
    void onCommentClicked(PostModel post);
    void onRepostClicked(PostModel post);
    void onBookmarkClicked(PostModel post);

    // قائمة الخيارات
    void onMenuClicked(PostModel post, View anchorView);

    // خيارات من القائمة
    void onTogglePinPostClicked(PostModel post);
    void onEditPost(PostModel post);
    void onDeletePost(PostModel post);
    void onCopyLink(PostModel post);
    void onSharePost(PostModel post);
    void onEditPostPrivacy(PostModel post);
    void onReportPost(PostModel post);

    // تفاعلات الريأكشنات
    void onLikeButtonLongClicked(PostModel post, View anchorView);
    void onEmojiSelected(PostModel post, String emojiUnicode);
    void onEmojiSummaryClicked(PostModel post);

    // تفاعلات أخرى
    void onHashtagClicked(String hashtag);
    void onPostLongClicked(PostModel post);
    void onMediaClicked(List<String> mediaUrls, int position);
    void onVideoPlayClicked(String videoUrl);
    void onLayoutClicked(PostModel post);
    void onSeeMoreClicked(PostModel post);
    void onTranslateClicked(PostModel post);
    void onShowOriginalClicked(PostModel post);

    // تفاعلات المستخدم
    void onUserClicked(UserModel user);
    void onFollowClicked(UserModel user);
    void onModeratePost(PostModel post);
}