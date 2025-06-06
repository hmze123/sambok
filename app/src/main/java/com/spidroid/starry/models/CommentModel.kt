// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/models/CommentModel.kt
package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Represents a comment on a post or another comment.
 *
 * This data class is Parcelable for easy passing between Android components.
 * It includes properties for comment content, author details, and interactions.
 */
@Parcelize
data class CommentModel(
    @get:Exclude var commentId: String? = null,
    var authorId: String? = null,

    @get:PropertyName("author_display_name")
    var authorDisplayName: String? = null,

    @get:PropertyName("author_username")
    var authorUsername: String? = null,

    @get:PropertyName("author_avatar_url")
    var authorAvatarUrl: String? = null,

    @get:PropertyName("author_verified")
    var authorVerified: Boolean = false,

    var content: String? = null,

    @ServerTimestamp
    var timestamp: Timestamp? = null,

    @get:PropertyName("like_count")
    var likeCount: Int = 0,

    @get:PropertyName("media_urls")
    var mediaUrls: MutableList<String> = mutableListOf(),

    @get:PropertyName("parent_post_id")
    var parentPostId: String? = null,

    @get:PropertyName("parent_comment_id")
    var parentCommentId: String? = null,

    @get:PropertyName("replies_count")
    var repliesCount: Int = 0,

    var likes: MutableMap<String, Boolean> = mutableMapOf(),
    var parentAuthorId: String? = null,
    var parentAuthorUsername: String? = null,

    // --- Local/UI-only properties ---
    @get:Exclude var isLiked: Boolean = false,
    @get:Exclude var depth: Int = 0

) : Parcelable {

    // ✨ تم إزالة المُنشئ الثانوي لتجنب مشكلة "cycle in delegation"
    // يجب أن يتم بناء كائن CommentModel باستخدام المُنشئ الأساسي مباشرة
    // (مثال: CommentModel(content = "...", parentPostId = "...", parentCommentId = "..."))

    // --- Computed Properties ---
    @get:Exclude
    val isReply: Boolean
        get() = depth > 0

    @get:Exclude
    val isTopLevel: Boolean
        get() = parentCommentId.isNullOrEmpty()

    @get:Exclude
    val javaDate: Date?
        get() = timestamp?.toDate()

    // --- Helper Methods ---
    @Exclude
    fun isReplyToAuthor(postAuthorId: String?): Boolean {
        return parentAuthorId != null && parentAuthorId == postAuthorId
    }

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "authorId" to authorId,
            "author_display_name" to authorDisplayName,
            "author_username" to authorUsername,
            "author_avatar_url" to authorAvatarUrl,
            "author_verified" to authorVerified,
            "content" to content,
            "like_count" to likeCount,
            "media_urls" to mediaUrls,
            "parent_post_id" to parentPostId,
            "parent_comment_id" to parentCommentId,
            "replies_count" to repliesCount,
            "likes" to likes,
            "timestamp" to timestamp, // Can be set to FieldValue.serverTimestamp() before saving
            "parentAuthorId" to parentAuthorId,
            "parentAuthorUsername" to parentAuthorUsername
        )
    }

    // --- Companion Object for Constants ---
    companion object {
        const val MAX_CONTENT_LENGTH = 500
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_PARENT_COMMENT_ID = "parentCommentId"
        const val FIELD_PARENT_POST_ID = "parentPostId"
    }
}