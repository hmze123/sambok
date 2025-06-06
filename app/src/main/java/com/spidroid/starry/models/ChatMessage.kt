package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

/**
 * Represents a single message within a chat.
 *
 * This data class handles different message types like text, media, and polls.
 * It's Parcelable for easy passing between Android components.
 */
@Parcelize
data class ChatMessage(
    // Core Message Data
    var messageId: String = UUID.randomUUID().toString(),
    var senderId: String = "",
    var senderName: String? = null,
    var senderAvatar: String? = null,
    var content: String? = null,
    var type: String = TYPE_TEXT,

    // Media Fields
    var mediaUrl: String? = null,
    var thumbnailUrl: String? = null,
    var mediaSize: Long = 0,
    var videoDuration: Long = 0,

    // File Fields
    var fileName: String? = null,
    var fileUrl: String? = null,
    var fileSize: Long = 0,
    var fileType: String? = null,

    // Context Fields
    var replyToId: String? = null,
    var replyPreview: String? = null,
    var reactions: MutableMap<String, String> = mutableMapOf(),

    // Status Tracking
    var readReceipts: MutableMap<String, Boolean> = mutableMapOf(),
    var deleted: Boolean = false,
    var edited: Boolean = false,
    @get:Exclude var uploading: Boolean = false, // Exclude from Firestore

    // Timestamps
    @ServerTimestamp var timestamp: Date? = null,
    @ServerTimestamp var lastUpdated: Date? = null,

    // Poll Data
    var poll: Poll? = null,

    // Delivery Status
    var deliveryStatus: String = STATUS_SENT

) : Parcelable {

    // Secondary constructors to match the Java version for convenience.
    constructor(senderId: String, content: String) : this(
        senderId = senderId,
        content = content,
        type = TYPE_TEXT
    )

    constructor(senderId: String, mediaUrl: String, type: String, thumbnailUrl: String?) : this(
        senderId = senderId,
        mediaUrl = mediaUrl,
        type = type,
        thumbnailUrl = thumbnailUrl
    )

    constructor(senderId: String, poll: Poll) : this(
        senderId = senderId,
        poll = poll,
        type = TYPE_POLL
    )

    /**
     * Nested data class for Polls. Also Parcelable.
     */
    @Parcelize
    data class Poll(
        var question: String? = null,
        var options: MutableList<PollOption> = mutableListOf(),
        var expired: Boolean = false,
        var voted: Boolean = false, // This might be better managed per-user.
        var totalVotes: Int = 0
    ) : Parcelable

    /**
     * Nested data class for Poll Options. Also Parcelable.
     */
    @Parcelize
    data class PollOption(
        var text: String? = null,
        var votes: Int = 0
    ) : Parcelable

    // Companion object holds constants and static-like helper functions.
    companion object {
        const val MAX_CONTENT_LENGTH = 2000
        const val MAX_MEDIA_SIZE_MB = 15
        const val THUMBNAIL_SIZE = 256

        // Message Types
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_GIF = "gif"
        const val TYPE_VIDEO = "video"
        const val TYPE_POLL = "poll"
        const val TYPE_FILE = "file"

        // Message Status
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_SEEN = "seen"

        @JvmStatic
        fun isValidType(type: String?): Boolean {
            return type in listOf(TYPE_TEXT, TYPE_IMAGE, TYPE_GIF, TYPE_VIDEO, TYPE_POLL, TYPE_FILE)
        }

        @JvmStatic
        fun isValidContent(content: String?): Boolean {
            return !content.isNullOrBlank() && content.length <= MAX_CONTENT_LENGTH
        }

        @JvmStatic
        fun isValidMediaUrl(url: String?): Boolean {
            return url?.startsWith("http", ignoreCase = true) == true
        }
    }

    // --- Business Logic Methods ---

    /**
     * Checks if the message data is valid based on its type.
     * @return True if the message is valid, false otherwise.
     */
    @get:Exclude
    val isValid: Boolean
        get() = when (type) {
            TYPE_TEXT -> isValidContent(content) && mediaUrl == null
            TYPE_IMAGE, TYPE_GIF, TYPE_VIDEO -> isValidMediaUrl(mediaUrl) && content == null
            TYPE_POLL -> poll?.question != null
            TYPE_FILE -> fileName != null && fileSize > 0
            else -> false
        }

    /**
     * Marks a message as read by a specific user.
     * @param userId The ID of the user who has read the message.
     */
    fun markAsRead(userId: String) {
        readReceipts[userId] = true
    }

    /**
     * Checks if a specific user has read the message.
     * @param userId The ID of the user to check.
     * @return True if the user has read the message, false otherwise.
     */
    @Exclude
    fun isReadBy(userId: String): Boolean {
        return readReceipts[userId] == true
    }
}
