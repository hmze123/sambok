package com.spidroid.starry.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FieldValue; // استخدم FieldValue للطوابع الزمنية من الخادم
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class NotificationModel {

    // أنواع الإشعارات ك ثوابت (Constants)
    public static final String TYPE_LIKE = "like";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_FOLLOW = "follow";
    public static final String TYPE_MENTION_POST = "mention_post";
    public static final String TYPE_MENTION_COMMENT = "mention_comment";
    // يمكنك إضافة أنواع أخرى هنا مثل إشعار الرد على تعليق، إلخ.

    private String notificationId; // معرّف الإشعار الفريد (سيتم إنشاؤه بواسطة Firestore أو يمكنك تعيينه)
    private String type; // نوع الإشعار (مثل "like", "comment")
    private String fromUserId; // معرّف المستخدم الذي تسبب في الإشعار
    private String fromUsername;
    private String fromUserAvatarUrl;
    private String toUserId; // معرّف المستخدم الذي سيتلقى الإشعار (صاحب الحساب)

    private String postId; // معرّف المنشور (إذا كان الإشعار متعلقًا بمنشور)
    private String postContentPreview; // معاينة لمحتوى المنشور
    private String commentId; // معرّف التعليق (إذا كان الإشعار متعلقًا بتعليق)
    private String commentContentPreview; // معاينة لمحتوى التعليق

    @ServerTimestamp // هذا سيجعل Firestore يضع الطابع الزمني للخادم تلقائيًا عند الإنشاء
    private Date timestamp;
    private boolean read; // هل تمت قراءة الإشعار أم لا

    // مُنشئ فارغ مطلوب لـ Firestore
    public NotificationModel() {}

    // مُنشئ عام (يمكنك إضافة المزيد حسب الحاجة)
    public NotificationModel(String type, String fromUserId, String fromUsername, String fromUserAvatarUrl, String toUserId) {
        this.type = type;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.fromUserAvatarUrl = fromUserAvatarUrl;
        this.toUserId = toUserId;
        this.read = false; // مبدئيًا لم تتم قراءته
        // الـ timestamp سيتم تعيينه بواسطة Firestore باستخدام @ServerTimestamp
    }

    // Getters
    @Exclude // لمنع Firestore من محاولة حفظ هذا الحقل مرة أخرى إذا قمت بتعيينه يدويًا
    public String getNotificationId() {
        return notificationId;
    }

    public String getType() {
        return type;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public String getFromUserAvatarUrl() {
        return fromUserAvatarUrl;
    }

    public String getToUserId() {
        return toUserId;
    }

    public String getPostId() {
        return postId;
    }

    public String getPostContentPreview() {
        return postContentPreview;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getCommentContentPreview() {
        return commentContentPreview;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return read;
    }

    // Setters
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public void setFromUsername(String fromUsername) {
        this.fromUsername = fromUsername;
    }

    public void setFromUserAvatarUrl(String fromUserAvatarUrl) {
        this.fromUserAvatarUrl = fromUserAvatarUrl;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public void setPostContentPreview(String postContentPreview) {
        this.postContentPreview = postContentPreview;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public void setCommentContentPreview(String commentContentPreview) {
        this.commentContentPreview = commentContentPreview;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}