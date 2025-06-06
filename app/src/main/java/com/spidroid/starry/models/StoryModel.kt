package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class StoryModel(
    // --- الحقول الأساسية ---
    @get:Exclude // Firestore should generate this ID, we set it manually after fetching
    var storyId: String = "",
    var userId: String = "",
    var mediaUrl: String = "",
    var thumbnailUrl: String? = null,
    var mediaType: String = MEDIA_TYPE_IMAGE,
    var duration: Long = 5000L, // Default duration 5 seconds for images

    @ServerTimestamp
    var createdAt: Date? = null,
    var expiresAt: Date? = null,
    var viewers: MutableMap<String, Boolean> = mutableMapOf(),

    // --- حقول بيانات المؤلف (لتحسين الأداء) ---
    var authorUsername: String? = null,
    var authorDisplayName: String? = null,
    var authorAvatarUrl: String? = null,
    var isAuthorVerified: Boolean = false

) : Parcelable {

    // مُنشئ فارغ مطلوب لـ Firestore
    constructor() : this("", "", "", null, MEDIA_TYPE_IMAGE, 5000L, null, null, mutableMapOf())

    // دالة مساعدة لتحديد ما إذا كانت القصة قد انتهت صلاحيتها
    @get:Exclude
    val isExpired: Boolean
        get() = expiresAt?.before(Date()) ?: true

    companion object {
        const val MEDIA_TYPE_IMAGE: String = "image"
        const val MEDIA_TYPE_VIDEO: String = "video"
        const val DEFAULT_IMAGE_DURATION_MS: Long = 5000
    }
}