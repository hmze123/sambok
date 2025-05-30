package com.spidroid.starry.adapters;

import android.view.View;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import java.util.List;

// تم نقل الواجهة إلى ملفها الخاص
public interface PostInteractionListener {
    void onHashtagClicked(String hashtag);
    void onLikeClicked(PostModel post);
    void onCommentClicked(PostModel post);
    void onRepostClicked(PostModel post);
    void onBookmarkClicked(PostModel post);
    void onMenuClicked(PostModel post, View anchor);
    void onDeletePost(PostModel post);
    void onEditPost(PostModel post);
    void onModeratePost(PostModel post);
    void onPostLongClicked(PostModel post);
    void onMediaClicked(List<String> mediaUrls, int position);
    void onVideoPlayClicked(String videoUrl);
    void onSharePost(PostModel post);
    void onCopyLink(PostModel post);
    void onReportPost(PostModel post);
    void onToggleBookmark(PostModel post);
    void onLayoutClicked(PostModel post);
    void onSeeMoreClicked(PostModel post);
    void onTranslateClicked(PostModel post);
    void onShowOriginalClicked(PostModel post);
    void onFollowClicked(UserModel user);
    void onUserClicked(UserModel user);
}