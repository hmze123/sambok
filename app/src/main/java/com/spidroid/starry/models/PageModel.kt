package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@IgnoreExtraProperties
@Parcelize
data class PageModel(
    var pageId: String = "",
    var pageName: String = "",
    var pageUsername: String = "", // for @mentioning
    var pageAvatarUrl: String? = null,
    var pageCoverUrl: String? = null,
    var category: String = "", // e.g., "Business", "Creator", "Community"
    var description: String? = null,

    // Key: userId, Value: true (for easier queries)
    var admins: MutableMap<String, Boolean> = mutableMapOf(),
    var ownerId: String = "", // The original creator

    var followerCount: Long = 0,

    @ServerTimestamp
    var createdAt: Date? = null
) : Parcelable
