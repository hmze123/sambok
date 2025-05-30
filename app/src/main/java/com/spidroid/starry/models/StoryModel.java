package com.spidroid.starry.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID; // لإنشاء معرّف فريد للقصة

public class StoryModel {

    private String storyId;
    private String userId; // معرف المستخدم الذي أنشأ القصة
    private String mediaUrl; // رابط الصورة/الفيديو للقصة
    private String thumbnailUrl; // رابط الصورة المصغرة للفيديو (إذا كانت القصة فيديو)
    private String mediaType; // "image" أو "video"
    private long duration; // مدة القصة بالمللي ثانية (خاصة بالفيديو)
    @ServerTimestamp
    private Date createdAt; // وقت إنشاء القصة
    private Date expiresAt; // وقت انتهاء صلاحية القصة (بعد 24 ساعة من الإنشاء)
    private Map<String, Boolean> viewers = new HashMap<>(); // معرفات المستخدمين الذين شاهدوا القصة

    // أنواع الوسائط
    public static final String MEDIA_TYPE_IMAGE = "image";
    public static final String MEDIA_TYPE_VIDEO = "video";

    public StoryModel() {
        // مطلوب لدوال Firebase
    }

    public StoryModel(String userId, String mediaUrl, String mediaType, long duration, String thumbnailUrl) {
        this.storyId = UUID.randomUUID().toString(); // إنشاء معرّف فريد للقصة
        this.userId = userId;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.duration = duration;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = new Date();
        this.expiresAt = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // تنتهي بعد 24 ساعة
    }

    // Getters
    public String getStoryId() {
        return storyId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public long getDuration() {
        return duration;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public Map<String, Boolean> getViewers() {
        return viewers;
    }

    // Setters
    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setViewers(Map<String, Boolean> viewers) {
        this.viewers = viewers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoryModel that = (StoryModel) o;
        return Objects.equals(storyId, that.storyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storyId);
    }
}