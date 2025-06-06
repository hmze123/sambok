// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/models/NotificationModel.kt
package com.spidroid.starry.models

// استخدم FieldValue للطوابع الزمنية من الخادم
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

class NotificationModel {
    // Setters
    // Getters
    // لمنع Firestore من محاولة حفظ هذا الحقل مرة أخرى إذا قمت بتعيينه يدويًا
    // يمكنك إضافة أنواع أخرى هنا مثل إشعار الرد على تعليق، إلخ.
    @get:Exclude
    var notificationId: String? =
        null // معرّف الإشعار الفريد (سيتم إنشاؤه بواسطة Firestore أو يمكنك تعيينه)
    var type: String? = null // نوع الإشعار (مثل "like", "comment")
    var fromUserId: String? = null // معرّف المستخدم الذي تسبب في الإشعار
    var fromUsername: String? = null
    var fromUserAvatarUrl: String? = null
    var toUserId: String? = null // معرّف المستخدم الذي سيتلقى الإشعار (صاحب الحساب)

    var postId: String? = null // معرّف المنشور (إذا كان الإشعار متعلقًا بمنشور)
    var postContentPreview: String? = null // معاينة لمحتوى المنشور
    var commentId: String? = null // معرّف التعليق (إذا كان الإشعار متعلقًا بتعليق)
    var commentContentPreview: String? = null // معاينة لمحتوى التعليق

    @ServerTimestamp // هذا سيجعل Firestore يضع الطابع الزمني للخادم تلقائيًا عند الإنشاء
    var timestamp: Date? = null
    var isRead: Boolean = false // هل تمت قراءة الإشعار أم لا

    // مُنشئ فارغ مطلوب لـ Firestore
    constructor()

    // مُنشئ عام (يمكنك إضافة المزيد حسب الحاجة)
    constructor(
        type: String?,
        fromUserId: String?,
        fromUsername: String?,
        fromUserAvatarUrl: String?,
        toUserId: String?
    ) {
        this.type = type
        this.fromUserId = fromUserId
        this.fromUsername = fromUsername
        this.fromUserAvatarUrl = fromUserAvatarUrl
        this.toUserId = toUserId
        this.isRead = false // مبدئيًا لم تتم قراءته
        // الـ timestamp سيتم تعيينه بواسطة Firestore باستخدام @ServerTimestamp
    }

    companion object {
        // أنواع الإشعارات ك ثوابت (Constants)
        const val TYPE_LIKE: String = "like"
        const val TYPE_COMMENT: String = "comment"
        const val TYPE_FOLLOW: String = "follow"
        const val TYPE_MENTION_POST: String = "mention_post"
        const val TYPE_MENTION_COMMENT: String = "mention_comment"
    }
}