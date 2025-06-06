// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/models/Chat.kt
package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Represents a chat, which can be a one-on-one or a group chat.
 *
 * This is a data class, which automatically provides component functions,
 * equals(), hashCode(), toString(), and copy().
 *
 * @Parcelize annotation automatically implements the Parcelable interface.
 */
@IgnoreExtraProperties
@Parcelize
data class Chat(
    var id: String? = null,
    var participants: MutableList<String> = mutableListOf(),
    var lastMessage: String? = null,
    var lastMessageType: String? = null,
    var lastMessageSender: String? = null,

    @ServerTimestamp
    var lastMessageTime: Date? = null,

    @ServerTimestamp
    var createdAt: Date? = null,

    var unreadCounts: MutableMap<String, Int> = mutableMapOf(),

    // Use @get:PropertyName to ensure Firestore uses "isGroup" for boolean properties
    // during serialization, which is a common convention.
    @get:PropertyName("isGroup")
    var isGroup: Boolean = false,

    // --- Group-specific fields ---
    var groupName: String? = null,
    var groupImage: String? = null,
    var creatorId: String? = null,
    var admins: MutableList<String> = mutableListOf(),
    var groupDescription: String? = null
) : Parcelable {

    /**
     * A computed property to get the timestamp of the last message as a Long.
     * Returns 0 if the lastMessageTime is null.
     */
    val lastMessageTimestamp: Long
        get() = lastMessageTime?.time ?: 0L
}