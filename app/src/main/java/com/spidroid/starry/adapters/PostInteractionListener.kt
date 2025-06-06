package com.spidroid.starry.adapters

import android.view.View
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel

interface PostInteractionListener {
    // التفاعلات الأساسية
    fun onLikeClicked(post: PostModel?)
    fun onCommentClicked(post: PostModel?)
    fun onRepostClicked(post: PostModel?)
    fun onBookmarkClicked(post: PostModel?)

    // قائمة الخيارات
    fun onMenuClicked(post: PostModel?, anchorView: View?)

    // خيارات من القائمة
    fun onTogglePinPostClicked(post: PostModel?)
    fun onEditPost(post: PostModel?)
    fun onDeletePost(post: PostModel?)
    fun onCopyLink(post: PostModel?)
    fun onSharePost(post: PostModel?)
    fun onEditPostPrivacy(post: PostModel?)
    fun onReportPost(post: PostModel?)

    // تفاعلات الريأكشنات
    fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?)
    // تم تعديل هذه الدالة لتكون أكثر عمومية
    fun onReactionSelected(post: PostModel?, emojiUnicode: String)
    fun onEmojiSummaryClicked(post: PostModel?)

    // تفاعلات أخرى
    fun onHashtagClicked(hashtag: String?)
    fun onPostLongClicked(post: PostModel?)
    fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int)
    fun onVideoPlayClicked(videoUrl: String?)
    fun onLayoutClicked(post: PostModel?)
    fun onSeeMoreClicked(post: PostModel?)
    fun onTranslateClicked(post: PostModel?)
    fun onShowOriginalClicked(post: PostModel?)

    // تفاعلات المستخدم
    fun onUserClicked(user: UserModel?)
    fun onFollowClicked(user: UserModel?)
    fun onModeratePost(post: PostModel?)
}