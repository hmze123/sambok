// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/models/StoryModel.kt
package com.spidroid.starry.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.Objects
import java.util.UUID

class StoryModel {
    // --- Getters and Setters ---
    // --- الحقول الأساسية ---
    var storyId: String? = null // ✨ تم تغيير هذا ليصبح قابلاً للـ null
    var userId: String? = null
    var mediaUrl: String? = null
    var thumbnailUrl: String? = null
    var mediaType: String? = null
    var duration: Long = 0

    @ServerTimestamp
    var createdAt: Date? = null
    var expiresAt: Date? = null
    var viewers: MutableMap<String, Boolean> = mutableMapOf() // ✨ تم تغيير التهيئة إلى mutableMapOf() وإزالة Nullability في القيم


    // --- حقول بيانات المؤلف (لتحسين الأداء) ---
    @Exclude // ✨ تم إزالة @get:Exclude المكرر
    var authorUsername: String? = null

    @Exclude // ✨ تم إزالة @get:Exclude المكرر
    var authorDisplayName: String? = null

    @Exclude // ✨ تم إزالة @get:Exclude المكرر
    var authorAvatarUrl: String? = null

    // ** الدوال الجديدة التي تم إضافتها **
    @Exclude // ✨ تم إزالة @get:Exclude المكرر
    var isAuthorVerified: Boolean = false // ** تم إضافة هذا الحقل **


    // --- المُنشئات ---
    constructor()

    constructor(
        userId: String?,
        mediaUrl: String?,
        mediaType: String?,
        duration: Long,
        thumbnailUrl: String?
    ) {
        // ✨ لا يتم تعيين storyId هنا، بل يتم تركه لـ Firestore ليقوم بتعيينه
        this.userId = userId
        this.mediaUrl = mediaUrl
        this.mediaType = mediaType
        this.duration = duration
        this.thumbnailUrl = thumbnailUrl
        this.createdAt = Date()
        this.expiresAt = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
    }

    // --- دوال مساعدة ---
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as StoryModel
        return storyId == that.storyId
    }

    override fun hashCode(): Int {
        return Objects.hash(storyId)
    }

    companion object {
        // --- الثوابت ---
        const val MEDIA_TYPE_IMAGE: String = "image"
        const val MEDIA_TYPE_VIDEO: String = "video"
    }
}