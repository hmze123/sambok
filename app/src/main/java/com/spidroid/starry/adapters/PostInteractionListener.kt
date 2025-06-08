package com.spidroid.starry.adapters

import android.view.View
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel

interface PostInteractionListener {
    fun onLikeClicked(post: PostModel?)
    fun onCommentClicked(post: PostModel?)
    fun onRepostClicked(post: PostModel?)
    fun onQuoteRepostClicked(post: PostModel?)
    fun onBookmarkClicked(post: PostModel?)
    fun onMenuClicked(post: PostModel?, anchorView: View?)
    fun onTogglePinPostClicked(post: PostModel?)
    fun onEditPost(post: PostModel?)
    fun onDeletePost(post: PostModel?)
    fun onCopyLink(post: PostModel?)
    fun onSharePost(post: PostModel?)
    fun onEditPostPrivacy(post: PostModel?)
    fun onReportPost(post: PostModel?)
    fun onLikeButtonLongClicked(post: PostModel?, anchorView: View?)
    fun onReactionSelected(post: PostModel?, emojiUnicode: String)
    fun onEmojiSummaryClicked(post: PostModel?)
    fun onHashtagClicked(hashtag: String?)
    fun onPostLongClicked(post: PostModel?)
    fun onMediaClicked(mediaUrls: MutableList<String?>?, position: Int, sharedView: View)
    fun onVideoPlayClicked(videoUrl: String?)
    fun onLayoutClicked(post: PostModel?)
    fun onSeeMoreClicked(post: PostModel?)
    fun onTranslateClicked(post: PostModel?)
    fun onShowOriginalClicked(post: PostModel?)
    fun onModeratePost(post: PostModel?)
    fun onUserClicked(user: UserModel?)
    fun onFollowClicked(user: UserModel?)
}