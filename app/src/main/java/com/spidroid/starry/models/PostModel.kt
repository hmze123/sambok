package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@IgnoreExtraProperties
@Parcelize
data class PostModel(
    // ... الحقول الحالية
    val communityId: String? = null,
    val communityName: String? = null,
    var postId: String? = null,
    var authorId: String? = null,
    var authorUsername: String? = null,
    var authorDisplayName: String? = null,
    var authorAvatarUrl: String? = null,
    var isAuthorVerified: Boolean = false,
    var content: String? = null,
    var mediaUrls: MutableList<String> = mutableListOf(),
    var contentType: String? = TYPE_TEXT,
    var videoDuration: Long = 0,
    var linkPreviews: MutableList<LinkPreview> = mutableListOf(),

    // --- الحقل الجديد للمنشور المقتبس ---
    var quotedPost: QuotedPost? = null,

    var likeCount: Long = 0,
    var repostCount: Long = 0,
    var replyCount: Long = 0,
    var bookmarkCount: Long = 0,

    @ServerTimestamp
    var createdAt: Date? = null,
    var updatedAt: Date? = null,

    var likes: MutableMap<String, Boolean> = mutableMapOf(),
    var bookmarks: MutableMap<String, Boolean> = mutableMapOf(),
    var reposts: MutableMap<String, Boolean> = mutableMapOf(),

    @get:PropertyName("emojiReactions")
    @set:PropertyName("emojiReactions")
    var reactions: MutableMap<String, String> = mutableMapOf(),

    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,

    var language: String? = null,
    var hashtags: List<String> = listOf(),
    var mentions: MutableList<String> = mutableListOf(),

    // --- Local/UI-only properties ---
    @get:Exclude @set:Exclude var isLiked: Boolean = false,
    @get:Exclude @set:Exclude var isBookmarked: Boolean = false,
    @get:Exclude @set:Exclude var isReposted: Boolean = false,
    @get:Exclude @set:Exclude var translatedContent: String? = null,
    @get:Exclude @set:Exclude var isTranslated: Boolean = false

) : Parcelable {

    // --- الكلاس المتداخل الجديد للمنشور المقتبس ---
    @IgnoreExtraProperties
    @Parcelize
    data class QuotedPost(
        var postId: String? = null,
        var authorId: String? = null,
        var authorUsername: String? = null,
        var authorDisplayName: String? = null,
        var authorAvatarUrl: String? = null,
        var isAuthorVerified: Boolean = false,
        var content: String? = null,
        @ServerTimestamp var createdAt: Date? = null
    ) : Parcelable

    // ... بقية الكود في PostModel يبقى كما هو ...
    @IgnoreExtraProperties
    @Parcelize
    data class LinkPreview(
        var url: String? = null,
        var title: String? = null,
        var description: String? = null,
        var imageUrl: String? = null,
        var siteName: String? = null
    ) : Parcelable

    companion object {
        const val TYPE_TEXT: String = "text"
        const val TYPE_IMAGE: String = "image"
        const val TYPE_VIDEO: String = "video"
        const val TYPE_POLL: String = "poll"
        val VIDEO_EXTENSIONS: List<String> = listOf("mp4", "mov", "avi", "mkv", "webm")
        const val MAX_CONTENT_LENGTH: Int = 280
    }
}