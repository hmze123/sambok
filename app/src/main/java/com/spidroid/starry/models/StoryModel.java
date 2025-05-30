package com.spidroid.starry.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StoryModel {

    // --- الحقول الأساسية ---
    private String storyId;
    private String userId;
    private String mediaUrl;
    private String thumbnailUrl;
    private String mediaType;
    private long duration;
    @ServerTimestamp
    private Date createdAt;
    private Date expiresAt;
    private Map<String, Boolean> viewers = new HashMap<>();

    // --- حقول بيانات المؤلف (لتحسين الأداء) ---
    @Exclude
    private String authorUsername;
    @Exclude
    private String authorDisplayName;
    @Exclude
    private String authorAvatarUrl;
    @Exclude
    private boolean authorVerified; // ** تم إضافة هذا الحقل **


    // --- الثوابت ---
    public static final String MEDIA_TYPE_IMAGE = "image";
    public static final String MEDIA_TYPE_VIDEO = "video";

    // --- المُنشئات ---
    public StoryModel() {
        // مطلوب لـ Firestore
    }

    public StoryModel(String userId, String mediaUrl, String mediaType, long duration, String thumbnailUrl) {
        this.storyId = UUID.randomUUID().toString();
        this.userId = userId;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.duration = duration;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = new Date();
        this.expiresAt = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }

    // --- Getters and Setters ---

    public String getStoryId() { return storyId; }
    public void setStoryId(String storyId) { this.storyId = storyId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public Map<String, Boolean> getViewers() { return viewers; }
    public void setViewers(Map<String, Boolean> viewers) { this.viewers = viewers; }

    @Exclude
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    @Exclude
    public String getAuthorDisplayName() { return authorDisplayName; }
    public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }

    @Exclude
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    // ** الدوال الجديدة التي تم إضافتها **
    @Exclude
    public boolean isAuthorVerified() { return authorVerified; }
    public void setAuthorVerified(boolean authorVerified) { this.authorVerified = authorVerified; }

    // --- دوال مساعدة ---
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