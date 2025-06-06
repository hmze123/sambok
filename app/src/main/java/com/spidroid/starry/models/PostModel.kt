package com.spidroid.starry.models

// ★ تأكد من هذا الاستيراد
// ★ تأكد من هذا الاستيراد
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.Log
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.Date
import java.util.Objects

@IgnoreExtraProperties
abstract // ★ سيتم تجاهل "stability" و "emojiCounts" إذا لم تكن معرّفة هنا
class PostModel : Parcelable {
    // --- Getters and Setters ---
    var postId: String? = null
    var authorId: String? = null
    var authorUsername: String? = null
    var authorDisplayName: String? = null
    var authorAvatarUrl: String? = null
    var isAuthorVerified: Boolean = false
    var content: String? = null
    private var mediaUrls: MutableList<String?>? = ArrayList<String?>()
    var contentType: String? = null
    var videoDuration: Long = 0
    private var linkPreviews: MutableList<LinkPreview?>? = ArrayList<LinkPreview?>()
    private var likeCount: Long = 0
    private var repostCount: Long = 0
    private var replyCount: Long = 0
    private var bookmarkCount: Long = 0

    @ServerTimestamp
    var createdAt: Date? = null
    var updatedAt: Date? = null
    private var likes: MutableMap<String?, Boolean?>? = HashMap<String?, Boolean?>()
    private var bookmarks: MutableMap<String?, Boolean?>? = HashMap<String?, Boolean?>()
    private var reposts: MutableMap<String?, Boolean?>? = HashMap<String?, Boolean?>()

    // إذا كان اسم الحقل في Firestore هو emojiReactions
    @PropertyName("emojiReactions")
    private var reactions: MutableMap<String?, String?>? = HashMap<String?, String?>()

    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    @PropertyName("isPinned")
    var isPinned: Boolean = false

    @get:Exclude
    @set:Exclude
    @Exclude
    var isLiked: Boolean = false

    @get:Exclude
    @set:Exclude
    @Exclude
    var isBookmarked: Boolean = false

    @get:Exclude
    @set:Exclude
    @Exclude
    var isReposted: Boolean = false
    var language: String? = null

    constructor()

    constructor(authorId: String, content: String) {
        this.authorId = authorId
        this.content = content
        this.createdAt = Date()
        this.contentType = TYPE_TEXT
    }

    fun getMediaUrls(): MutableList<String?> {
        return (if (mediaUrls != null) mediaUrls else java.util.ArrayList<kotlin.String?>())!!
    }

    fun setMediaUrls(mediaUrls: MutableList<String?>?) {
        this.mediaUrls = mediaUrls
    }

    fun getLinkPreviews(): MutableList<LinkPreview?> {
        return (if (linkPreviews != null) linkPreviews else java.util.ArrayList<LinkPreview?>())!!
    }

    fun setLinkPreviews(linkPreviews: MutableList<LinkPreview?>?) {
        this.linkPreviews = linkPreviews
    }

    fun getLikeCount(): Long {
        return likeCount
    }

    fun setLikeCount(likeCount: Long) {
        this.likeCount = if (likeCount < 0) 0 else likeCount
    }

    fun getRepostCount(): Long {
        return repostCount
    }

    fun setRepostCount(repostCount: Long) {
        this.repostCount = if (repostCount < 0) 0 else repostCount
    }

    fun getReplyCount(): Long {
        return replyCount
    }

    fun setReplyCount(replyCount: Long) {
        this.replyCount = if (replyCount < 0) 0 else replyCount
    }

    fun getBookmarkCount(): Long {
        return bookmarkCount
    }

    fun setBookmarkCount(bookmarkCount: Long) {
        this.bookmarkCount = if (bookmarkCount < 0) 0 else bookmarkCount
    }

    fun getLikes(): MutableMap<String?, Boolean?> {
        return (if (likes != null) likes else java.util.HashMap<kotlin.String?, kotlin.Boolean?>())!!
    }

    fun setLikes(likes: MutableMap<String?, Boolean?>?) {
        this.likes = likes
    }

    fun getBookmarks(): MutableMap<String?, Boolean?> {
        return (if (bookmarks != null) bookmarks else java.util.HashMap<kotlin.String?, kotlin.Boolean?>())!!
    }

    fun setBookmarks(bookmarks: MutableMap<String?, Boolean?>?) {
        this.bookmarks = bookmarks
    }

    fun getReposts(): MutableMap<String?, Boolean?> {
        return (if (reposts != null) reposts else java.util.HashMap<kotlin.String?, kotlin.Boolean?>())!!
    }

    fun setReposts(reposts: MutableMap<String?, Boolean?>?) {
        this.reposts = reposts
    }

    @PropertyName("emojiReactions")
    fun getReactions(): MutableMap<String?, String?> {
        return (if (reactions != null) reactions else java.util.HashMap<kotlin.String?, kotlin.String?>())!!
    }

    @PropertyName("emojiReactions")
    fun setReactions(reactions: MutableMap<String?, String?>?) {
        this.reactions = reactions
    }

    // ... (باقي دوال toggleLike, toggleRepost, toggleBookmark, addReaction, etc.) ...
    // ... (Parcelable implementation كما هي) ...
    fun toggleLike() {
        Log.d(
            PostModel.Companion.TAG,
            "Before toggleLike: postId=" + postId + ", isLiked=" + isLiked + ", likeCount=" + likeCount
        )
        isLiked = !isLiked
        likeCount += (if (isLiked) 1 else -1).toLong()
        if (likeCount < 0) likeCount = 0
        Log.d(
            PostModel.Companion.TAG,
            "After toggleLike: postId=" + postId + ", isLiked=" + isLiked + ", likeCount=" + likeCount
        )
    }

    fun toggleRepost() {
        Log.d(
            PostModel.Companion.TAG,
            "Before toggleRepost: postId=" + postId + ", isReposted=" + isReposted + ", repostCount=" + repostCount
        )
        isReposted = !isReposted
        repostCount += (if (isReposted) 1 else -1).toLong()
        if (repostCount < 0) repostCount = 0
        Log.d(
            PostModel.Companion.TAG,
            "After toggleRepost: postId=" + postId + ", isReposted=" + isReposted + ", repostCount=" + repostCount
        )
    }

    fun toggleBookmark() {
        Log.d(
            PostModel.Companion.TAG,
            "Before toggleBookmark: postId=" + postId + ", isBookmarked=" + isBookmarked + ", bookmarkCount=" + bookmarkCount
        )
        isBookmarked = !isBookmarked
        bookmarkCount += (if (isBookmarked) 1 else -1).toLong()
        if (bookmarkCount < 0) bookmarkCount = 0
        Log.d(
            PostModel.Companion.TAG,
            "After toggleBookmark: postId=" + postId + ", isBookmarked=" + isBookmarked + ", bookmarkCount=" + bookmarkCount
        )
    }

    @Exclude
    fun addReaction(userId: String?, emoji: String?) {
        if (userId == null || userId.isEmpty() || emoji == null || emoji.isEmpty()) return
        if (this.reactions == null) this.reactions = HashMap<String?, String?>()
        this.reactions!!.put(userId, emoji)
    }

    @Exclude
    fun removeReaction(userId: String?) {
        if (userId == null || userId.isEmpty() || this.reactions == null) return
        this.reactions!!.remove(userId)
    }

    @Exclude
    fun getUserReaction(userId: String?): String? {
        if (userId == null || this.reactions == null) return null
        return this.reactions!!.get(userId)
    }

    @get:Exclude
    val isVideoPost: Boolean
        get() = TYPE_VIDEO == contentType

    @get:Exclude
    val isImagePost: Boolean
        get() = TYPE_IMAGE == contentType

    @get:Exclude
    val firstMediaUrl: String?
        get() = if (mediaUrls != null && !mediaUrls!!.isEmpty()) mediaUrls!!.get(0) else null

    protected constructor(`in`: Parcel) {
        postId = `in`.readString()
        authorId = `in`.readString()
        authorUsername = `in`.readString()
        authorDisplayName = `in`.readString()
        authorAvatarUrl = `in`.readString()
        isAuthorVerified = `in`.readByte().toInt() != 0
        content = `in`.readString()
        mediaUrls = `in`.createStringArrayList()
        contentType = `in`.readString()
        videoDuration = `in`.readLong()
        linkPreviews = `in`.createTypedArrayList<LinkPreview?>(LinkPreview.Companion.CREATOR)
        likeCount = `in`.readLong()
        repostCount = `in`.readLong()
        replyCount = `in`.readLong()
        bookmarkCount = `in`.readLong()
        val tmpCreatedAt: Long = `in`.readLong()
        createdAt = if (tmpCreatedAt == -1L) null else Date(tmpCreatedAt)
        val tmpUpdatedAt: Long = `in`.readLong()
        updatedAt = if (tmpUpdatedAt == -1L) null else Date(tmpUpdatedAt)
        likes = HashMap<String?, Boolean?>()
        `in`.readMap(likes, Boolean::class.java.getClassLoader())
        bookmarks = HashMap<String?, Boolean?>()
        `in`.readMap(bookmarks, Boolean::class.java.getClassLoader())
        reposts = HashMap<String?, Boolean?>()
        `in`.readMap(reposts, Boolean::class.java.getClassLoader())
        reactions = HashMap<String?, String?>()
        `in`.readMap(reactions, String::class.java.getClassLoader())
        isPinned = `in`.readByte().toInt() != 0
        isLiked = `in`.readByte().toInt() != 0
        isBookmarked = `in`.readByte().toInt() != 0
        isReposted = `in`.readByte().toInt() != 0
        language = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(postId)
        dest.writeString(authorId)
        dest.writeString(authorUsername)
        dest.writeString(authorDisplayName)
        dest.writeString(authorAvatarUrl)
        dest.writeByte((if (isAuthorVerified) 1 else 0).toByte())
        dest.writeString(content)
        dest.writeStringList(mediaUrls)
        dest.writeString(contentType)
        dest.writeLong(videoDuration)
        dest.writeTypedList<LinkPreview?>(linkPreviews)
        dest.writeLong(likeCount)
        dest.writeLong(repostCount)
        dest.writeLong(replyCount)
        dest.writeLong(bookmarkCount)
        dest.writeLong(if (createdAt != null) createdAt!!.getTime() else -1)
        dest.writeLong(if (updatedAt != null) updatedAt!!.getTime() else -1)
        dest.writeMap(likes)
        dest.writeMap(bookmarks)
        dest.writeMap(reposts)
        dest.writeMap(reactions)
        dest.writeByte((if (isPinned) 1 else 0).toByte())
        dest.writeByte((if (isLiked) 1 else 0).toByte())
        dest.writeByte((if (isBookmarked) 1 else 0).toByte())
        dest.writeByte((if (isReposted) 1 else 0).toByte())
        dest.writeString(language)
    }

    class LinkPreview : Parcelable {
        var url: String? = null
        var title: String? = null
        var description: String? = null
        var imageUrl: String? = null
        var siteName: String? = null

        constructor()
        protected constructor(`in`: Parcel) {
            url = `in`.readString()
            title = `in`.readString()
            description = `in`.readString()
            imageUrl = `in`.readString()
            siteName = `in`.readString()
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(url)
            dest.writeString(title)
            dest.writeString(description)
            dest.writeString(imageUrl)
            dest.writeString(siteName)
        }

        companion object {
            val CREATOR: Creator<LinkPreview?> = object : Creator<LinkPreview?> {
                override fun createFromParcel(`in`: Parcel): LinkPreview {
                    return LinkPreview(`in`)
                }

                override fun newArray(size: Int): Array<LinkPreview?> {
                    return arrayOfNulls<LinkPreview>(size)
                }
            }
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val postModel = o as PostModel
        return postId == postModel.postId && likeCount == postModel.likeCount && repostCount == postModel.repostCount && bookmarkCount == postModel.bookmarkCount && isPinned == postModel.isPinned &&
                content == postModel.content &&
                authorId == postModel.authorId &&
                reactions == postModel.reactions
    }

    override fun hashCode(): Int {
        return Objects.hash(
            postId,
            content,
            authorId,
            likeCount,
            repostCount,
            bookmarkCount,
            reactions,
            isPinned
        )
    }

    companion object {
        private const val TAG = "PostModel"

        const val TYPE_TEXT: String = "text"
        const val TYPE_IMAGE: String = "image"
        const val TYPE_VIDEO: String = "video"
        const val TYPE_POLL: String = "poll"
        val VIDEO_EXTENSIONS: MutableList<String?> =
            mutableListOf<String?>("mp4", "mov", "avi", "mkv", "webm")
        const val MAX_CONTENT_LENGTH: Int = 280

        val CREATOR: Creator<PostModel?> = object : Creator<PostModel?> {
            override fun createFromParcel(`in`: Parcel): PostModel {
                return PostModel(`in`)
            }

            override fun newArray(size: Int): Array<PostModel?> {
                return arrayOfNulls<PostModel>(size)
            }
        }
    }
}