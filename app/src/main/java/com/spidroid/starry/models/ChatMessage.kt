package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class ChatMessage(
    var messageId: String? = null,
    var senderId: String = "",
    var senderName: String? = null,
    var senderAvatar: String? = null,
    var content: String? = null,
    var type: String = TYPE_TEXT,
    var mediaUrl: String? = null,
    var thumbnailUrl: String? = null,
    var mediaSize: Long = 0,
    var videoDuration: Long = 0,
    var fileName: String? = null,
    var fileUrl: String? = null,
    var fileSize: Long = 0,
    var fileType: String? = null,
    var replyToId: String? = null,
    var replyPreview: String? = null,
    var reactions: MutableMap<String, String> = mutableMapOf(),
    var readReceipts: MutableMap<String, Boolean> = mutableMapOf(),
    var deleted: Boolean = false,
    var edited: Boolean = false,
    @get:Exclude var uploading: Boolean = false,
    @ServerTimestamp var timestamp: Date? = null,
    @ServerTimestamp var lastUpdated: Date? = null,

    // --- تم تعديل هذا الجزء ---
    var poll: Poll? = null,

    var deliveryStatus: String = STATUS_SENT

) : Parcelable {

    @Parcelize
    data class Poll(
        var question: String = "",
        var options: MutableList<PollOption> = mutableListOf(),
        var expired: Boolean = false,
        // خريطة لتخزين المستخدمين الذين قاموا بالتصويت
        var voters: MutableMap<String, Int> = mutableMapOf() // Key: userId, Value: optionIndex
    ) : Parcelable {
        @get:Exclude
        val totalVotes: Int
            get() = voters.size
    }

    @Parcelize
    data class PollOption(
        var text: String = "",
        var votes: Int = 0
    ) : Parcelable

    // --- نهاية الجزء المعدل ---

    companion object {
        const val MAX_CONTENT_LENGTH = 2000
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_GIF = "gif"
        const val TYPE_VIDEO = "video"
        const val TYPE_POLL = "poll"
        const val TYPE_FILE = "file"
        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_SEEN = "seen"
    }

    @get:Exclude
    val isValid: Boolean
        get() = when (type) {
            TYPE_TEXT -> content != null && content!!.length <= MAX_CONTENT_LENGTH
            TYPE_IMAGE, TYPE_GIF, TYPE_VIDEO -> mediaUrl != null
            TYPE_POLL -> poll?.question?.isNotBlank() == true
            TYPE_FILE -> fileName != null && fileSize > 0
            else -> false
        }

    fun markAsRead(userId: String) {
        readReceipts[userId] = true
    }

    @Exclude
    fun isReadBy(userId: String): Boolean {
        return readReceipts[userId] == true
    }
}