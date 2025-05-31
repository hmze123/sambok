package com.spidroid.starry.adapters;

import android.view.View;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import java.util.List;

public interface PostInteractionListener {
    // الدوال الموجودة لديك بالفعل
    void onHashtagClicked(String hashtag);
    void onLikeClicked(PostModel post);
    void onCommentClicked(PostModel post);
    void onRepostClicked(PostModel post);
    void onBookmarkClicked(PostModel post);

    void onMenuClicked(PostModel post, View anchorView);
    void onDeletePost(PostModel post);
    void onEditPost(PostModel post);
    void onReportPost(PostModel post);
    void onSharePost(PostModel post);
    void onCopyLink(PostModel post);
    void onToggleBookmark(PostModel post);

    void onLikeButtonLongClicked(PostModel post, View anchorView);
    void onEmojiSelected(PostModel post, String emoji);

    // ⭐ دالة جديدة للنقر على ملخص الإيموجي ⭐
    void onEmojiSummaryClicked(PostModel post);

    // دوال أخرى قد تكون موجودة لديك
    void onPostLongClicked(PostModel post);
    void onMediaClicked(List<String> mediaUrls, int position);
    void onVideoPlayClicked(String videoUrl);
    void onLayoutClicked(PostModel post);
    void onSeeMoreClicked(PostModel post);
    void onTranslateClicked(PostModel post);
    void onShowOriginalClicked(PostModel post);

    void onUserClicked(UserModel user);
    void onFollowClicked(UserModel user);
    void onModeratePost(PostModel post);
}