package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

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

    @get:PropertyName("isGroup")
    var isGroup: Boolean = false,

    // --- الحقل الجديد لمؤشر الكتابة ---
    // سيحتوي على userId كـ key و timestamp كـ value
    var typingStatus: MutableMap<String, Long> = mutableMapOf(),

    // --- Group-specific fields ---
    var groupName: String? = null,
    var groupImage: String? = null,
    var creatorId: String? = null,
    var admins: MutableList<String> = mutableListOf(),
    var groupDescription: String? = null
) : Parcelable {

    val lastMessageTimestamp: Long
        get() = lastMessageTime?.time ?: 0L
}