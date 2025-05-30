package com.spidroid.starry.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class PostModel implements Parcelable {
  // Content type constants
  public static final String TYPE_TEXT = "text";
  public static final String TYPE_IMAGE = "image";
  public static final String TYPE_VIDEO = "video";
  public static final String TYPE_POLL = "poll";

  // Moderation status constants
  public static final String MODERATION_PENDING = "pending";
  public static final String MODERATION_APPROVED = "approved";
  public static final String MODERATION_FLAGGED = "flagged";

  // Video extensions
  public static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mov", "avi", "mkv", "webm");
  public static final List<String> VALID_CONTENT_TYPES =
          List.of(TYPE_TEXT, TYPE_IMAGE, TYPE_VIDEO, TYPE_POLL);

  // Core Fields
  private String postId;
  private String authorId;
  private String authorUsername;
  private String authorDisplayName;
  private String authorAvatarUrl;
  private boolean isAuthorVerified;
  private String content;
  private List<String> mediaUrls;
  private String language;
  private String contentType;
  private long videoDuration; // إضافة حقل مدة الفيديو

  private boolean isFollowing;

  // URL Preview Support
  private List<LinkPreview> linkPreviews = new ArrayList<>();
  private boolean hasLinks;
  private String primaryLinkUrl;

  // Engagement Metrics
  // In PostModel.java (change all count fields to long)
  private long likeCount;
  private long repostCount;
  private long replyCount;
  private long viewCount;
  private long bookmarkCount;

  // Timestamps
  @ServerTimestamp private Date createdAt;
  private Date updatedAt;
  private Date editedAt;

  // Location
  private GeoPoint location;

  // User Interaction States
  private boolean isLiked;
  private boolean isReposted;
  private boolean isBookmarked;
  private boolean isPinned;

  // Reference Fields
  private String parentPostId;
  private String rootPostId;
  private List<String> mentionIds;

  // Content Moderation
  private boolean isSensitive;
  private boolean isDeleted; // هذا هو المتغير الصحيح
  private String deletedReason;
  private String moderationStatus;
  private List<String> moderatorNotes;
  private boolean isPreviewSafe;

  // Additional Metadata
  private Map<String, String> externalLinks;
  private List<String> hashtags;
  private String sourceApp;

  private Map<String, Boolean> likes = new HashMap<>();
  private Map<String, Boolean> bookmarks = new HashMap<>();
  private Map<String, Boolean> reposts = new HashMap<>();

  private boolean isExpanded = false;
  private boolean isTranslated = false;
  private String translatedContent = "";

  public boolean isExpanded() {
    return isExpanded;
  }

  public void setExpanded(boolean expanded) {
    isExpanded = expanded;
    cachedSpannable = null; // Invalidate cached spannable
  }

  public boolean isTranslated() {
    return isTranslated;
  }

  public void setTranslated(boolean translated) {
    isTranslated = translated;
    cachedSpannable = null;
  }

  public String getTranslatedContent() {
    return translatedContent;
  }

  public void setTranslatedContent(String content) {
    translatedContent = content;
  }

  public Map<String, Boolean> getLikes() {
    return likes;
  }

  public Map<String, Boolean> getBookmarks() {
    return bookmarks;
  }

  public Map<String, Boolean> getReposts() {
    return reposts;
  }

  // setters
  public void setLikes(Map<String, Boolean> likes) {
    this.likes = likes != null ? likes : new HashMap<>();
  }

  public void setBookmarks(Map<String, Boolean> bookmarks) {
    this.bookmarks = bookmarks != null ? bookmarks : new HashMap<>();
  }

  public void setReposts(Map<String, Boolean> reposts) {
    this.reposts = reposts != null ? reposts : new HashMap<>();
  }

  // Empty constructor for Firestore
  public PostModel() {}

  // Minimal constructor for new posts
  public PostModel(@NonNull String authorId, @NonNull String content) {
    this.authorId = authorId;
    this.content = content;
    this.contentType = TYPE_TEXT;
    this.createdAt = new Date();
    this.mediaUrls = new ArrayList<>();
    this.mentionIds = new ArrayList<>();
    this.hashtags = new ArrayList<>();
    this.externalLinks = new HashMap<>();
    this.moderatorNotes = new ArrayList<>();
    this.linkPreviews = new ArrayList<>();
    this.language = "en";
    this.sourceApp = "Horizon for Android";
    this.moderationStatus = MODERATION_PENDING;
  }

  // Parcelable implementation
  protected PostModel(Parcel in) {
    // Core Fields
    postId = in.readString();
    authorId = in.readString();
    authorUsername = in.readString();
    authorDisplayName = in.readString();
    authorAvatarUrl = in.readString();
    isAuthorVerified = in.readByte() != 0;
    isFollowing = in.readByte() != 0;
    content = in.readString();
    mediaUrls = in.createStringArrayList();
    language = in.readString();
    contentType = in.readString();
    videoDuration = in.readLong(); // قراءة مدة الفيديو

    // Link Previews
    linkPreviews = in.createTypedArrayList(LinkPreview.CREATOR);

    // Link Metadata
    hasLinks = in.readByte() != 0;
    primaryLinkUrl = in.readString();

    // Engagement Metrics
    viewCount = in.readLong();
    likeCount = in.readLong();
    repostCount = in.readLong();
    replyCount = in.readLong();
    bookmarkCount = in.readLong();

    // Interaction Maps
    likes = in.readHashMap(String.class.getClassLoader());
    bookmarks = in.readHashMap(String.class.getClassLoader());
    reposts = in.readHashMap(String.class.getClassLoader());

    // Timestamps
    createdAt = readNullableDate(in);
    updatedAt = readNullableDate(in);
    editedAt = readNullableDate(in);

    // Location
    Double lat = in.readDouble();
    Double lng = in.readDouble();
    location = (lat != -1 && lng != -1) ? new GeoPoint(lat, lng) : null;

    // User States
    isLiked = in.readByte() != 0;
    isReposted = in.readByte() != 0;
    isBookmarked = in.readByte() != 0;
    isPinned = in.readByte() != 0;

    // Post Relationships
    parentPostId = in.readString();
    rootPostId = in.readString();
    mentionIds = in.createStringArrayList();

    // Moderation
    isSensitive = in.readByte() != 0;
    isDeleted = in.readByte() != 0; // قراءة المتغير الصحيح
    deletedReason = in.readString();
    moderationStatus = in.readString();
    moderatorNotes = in.createStringArrayList();
    isPreviewSafe = in.readByte() != 0;

    // Metadata
    externalLinks = in.readHashMap(String.class.getClassLoader());
    hashtags = in.createStringArrayList();
    sourceApp = in.readString();
  }

  public static final Creator<PostModel> CREATOR =
          new Creator<PostModel>() {
            @Override
            public PostModel createFromParcel(Parcel in) {
              return new PostModel(in);
            }

            @Override
            public PostModel[] newArray(int size) {
              return new PostModel[size];
            }
          };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    // Core Fields
    dest.writeString(postId);
    dest.writeString(authorId);
    dest.writeString(authorUsername);
    dest.writeString(authorDisplayName);
    dest.writeString(authorAvatarUrl);
    dest.writeByte((byte) (isAuthorVerified ? 1 : 0));
    dest.writeByte((byte) (isFollowing ? 1 : 0));
    dest.writeString(content);
    dest.writeStringList(mediaUrls);
    dest.writeString(language);
    dest.writeString(contentType);
    dest.writeLong(videoDuration); // كتابة مدة الفيديو

    // Link Previews (moved up)
    dest.writeTypedList(linkPreviews);

    // Link Metadata
    dest.writeByte((byte) (hasLinks ? 1 : 0));
    dest.writeString(primaryLinkUrl);

    // Engagement Metrics
    dest.writeLong(likeCount);
    dest.writeLong(repostCount);
    dest.writeLong(replyCount);
    dest.writeLong(bookmarkCount);
    dest.writeLong(viewCount);

    // Interaction Maps
    dest.writeMap(likes);
    dest.writeMap(bookmarks);
    dest.writeMap(reposts);

    // Timestamps
    writeNullableDate(dest, createdAt);
    writeNullableDate(dest, updatedAt);
    writeNullableDate(dest, editedAt);

    // Location
    dest.writeDouble(location != null ? location.getLatitude() : -1);
    dest.writeDouble(location != null ? location.getLongitude() : -1);

    // User States
    dest.writeByte((byte) (isLiked ? 1 : 0));
    dest.writeByte((byte) (isReposted ? 1 : 0));
    dest.writeByte((byte) (isBookmarked ? 1 : 0));
    dest.writeByte((byte) (isPinned ? 1 : 0));

    // Post Relationships
    dest.writeString(parentPostId);
    dest.writeString(rootPostId);
    dest.writeStringList(mentionIds);

    // Moderation
    dest.writeByte((byte) (isSensitive ? 1 : 0));
    dest.writeByte((byte) (isDeleted ? 1 : 0)); // كتابة المتغير الصحيح
    dest.writeString(deletedReason);
    dest.writeString(moderationStatus);
    dest.writeStringList(moderatorNotes);
    dest.writeByte((byte) (isPreviewSafe ? 1 : 0));

    // Metadata
    dest.writeMap(externalLinks);
    dest.writeStringList(hashtags);
    dest.writeString(sourceApp);
  }

  // transient fields
  @Exclude private transient SpannableStringBuilder cachedSpannable;

  @Exclude
  public SpannableStringBuilder getCachedSpannable() {
    return cachedSpannable;
  }

  @Exclude
  public void setCachedSpannable(SpannableStringBuilder spannable) {
    this.cachedSpannable = spannable;
  }

  // Helper methods
  private Date readNullableDate(Parcel in) {
    long timestamp = in.readLong();
    return timestamp != -1 ? new Date(timestamp) : null;
  }

  private void writeNullableDate(Parcel dest, Date date) {
    dest.writeLong(date != null ? date.getTime() : -1);
  }

  private boolean isValidUrl(String url) {
    return Pattern.compile("^(https?://)?([\\w-]+\\.)+[\\w-]+(/\\S*)?$").matcher(url).matches();
  }

  private boolean isValidMediaUrl(String url) {
    return url != null && (url.startsWith("http://") || url.startsWith("https://"));
  }

  public static boolean isValidContentType(String type) {
    return VALID_CONTENT_TYPES.contains(type);
  }

  private boolean isValidModerationStatus(String status) {
    return status.equals(MODERATION_PENDING)
            || status.equals(MODERATION_APPROVED)
            || status.equals(MODERATION_FLAGGED);
  }

  public static String getFileExtension(String url) {
    int dotIndex = url.lastIndexOf('.');
    if (dotIndex == -1 || dotIndex == url.length() - 1) {
      return ""; // No extension or extension is empty
    }
    String extension = url.substring(dotIndex + 1);
    // Remove any query parameters
    int queryIndex = extension.indexOf('?');
    if (queryIndex != -1) {
      extension = extension.substring(0, queryIndex);
    }
    return extension.toLowerCase();
  }

  // URL Preview Management
  public void addLinkPreview(LinkPreview preview) {
    if (linkPreviews == null) linkPreviews = new ArrayList<>();
    if (isValidUrl(preview.getUrl())) {
      linkPreviews.add(preview);
      updateLinkMetadata();
    }
  }

  public void removeLinkPreview(String url) {
    if (linkPreviews != null) {
      linkPreviews.removeIf(preview -> preview.getUrl().equals(url));
      updateLinkMetadata();
    }
  }

  private void updateLinkMetadata() {
    hasLinks = !linkPreviews.isEmpty();
    primaryLinkUrl = hasLinks ? linkPreviews.get(0).getUrl() : null;
  }

  // Media Management
  public void addMediaUrl(String url) {
    if (isValidMediaUrl(url)) {
      mediaUrls.add(url);
      updateContentType();
    }
  }

  private void updateContentType() {
    if (!mediaUrls.isEmpty()) {
      String ext = getFileExtension(mediaUrls.get(0));
      if (VIDEO_EXTENSIONS.contains(ext)) {
        contentType = TYPE_VIDEO;
      } else {
        contentType = TYPE_IMAGE;
      }
    } else {
      contentType = TYPE_TEXT; // إذا لم تكن هناك وسائط، فافترض أنها نصية
    }
  }

  public boolean hasVideo() {
    return mediaUrls.stream().anyMatch(url -> VIDEO_EXTENSIONS.contains(getFileExtension(url)));
  }

  // Toggle methods
  public void toggleLike() {
    setLiked(!isLiked);
    likeCount = isLiked ? likeCount + 1 : likeCount - 1;
  }

  public void toggleRepost() {
    setReposted(!isReposted);
    // تم التعديل هنا: استخدام `isReposted` و منطق الزيادة/النقصان الصحيح
    repostCount += isReposted ? 1 : -1;
  }

  public void toggleBookmark() {
    setBookmarked(!isBookmarked);
    // تم التعديل هنا: استخدام `isBookmarked` و منطق الزيادة/النقصان الصحيح
    bookmarkCount += isBookmarked ? 1 : -1;
  }

  // Getters and Setters
  public String getPostId() {
    return postId;
  }

  public void setPostId(String postId) {
    this.postId = postId;
  }

  public String getAuthorId() {
    return authorId;
  }

  public void setAuthorId(@NonNull String authorId) {
    if (authorId == null) throw new IllegalArgumentException("Author ID cannot be null");
    this.authorId = authorId;
  }

  public String getAuthorUsername() {
    return authorUsername;
  }

  public void setAuthorUsername(String authorUsername) {
    this.authorUsername = authorUsername;
  }

  public String getAuthorDisplayName() {
    return authorDisplayName;
  }

  public void setAuthorDisplayName(String authorDisplayName) {
    this.authorDisplayName = authorDisplayName;
  }

  public String getAuthorAvatarUrl() {
    return authorAvatarUrl;
  }

  public void setAuthorAvatarUrl(String authorAvatarUrl) {
    this.authorAvatarUrl = authorAvatarUrl;
  }

  public boolean isAuthorVerified() {
    return isAuthorVerified;
  }

  public void setAuthorVerified(boolean authorVerified) {
    isAuthorVerified = authorVerified;
  }

  private void followUser(String userId) {
    // Firestore operation to follow
  }

  private void unfollowUser(String userId) {
    // Firestore operation to unfollow
  }

  public boolean isFollowing() {
    return isFollowing;
  }

  public void setFollowing(boolean following) {
    isFollowing = following;
  }

  public String getContent() {
    return content;
  }

  public void setContent(@NonNull String content) {
    if (content == null || content.length() > 600) {
      throw new IllegalArgumentException("Content must be between 1-600 characters");
    }
    this.content = content;
  }

  public List<String> getMediaUrls() {
    return mediaUrls;
  }

  public void setMediaUrls(@Nullable List<String> mediaUrls) {
    this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
    updateContentType(); // التأكد من تحديث contentType عند تعيين mediaUrls
  }

  public long getVideoDuration() { // getter لـ videoDuration
    return videoDuration;
  }

  public void setVideoDuration(long videoDuration) { // setter لـ videoDuration
    this.videoDuration = videoDuration;
  }

  public boolean isVideoPost() {
    return contentType.equals(TYPE_VIDEO);
  }

  public boolean isImagePost() {
    return contentType.equals(TYPE_IMAGE);
  }

  public String getFirstMediaUrl() {
    return !mediaUrls.isEmpty() ? mediaUrls.get(0) : null;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(@NonNull String contentType) {
    if (!isValidContentType(contentType)) {
      throw new IllegalArgumentException("Invalid content type");
    }
    this.contentType = contentType;
  }

  public List<LinkPreview> getLinkPreviews() {
    return Collections.unmodifiableList(linkPreviews);
  }

  public void setLinkPreviews(@Nullable List<LinkPreview> linkPreviews) {
    this.linkPreviews = linkPreviews != null ? new ArrayList<>(linkPreviews) : new ArrayList<>();
    updateLinkMetadata();
  }

  public boolean hasLinks() {
    return hasLinks;
  }

  public String getPrimaryLinkUrl() {
    return primaryLinkUrl;
  }

  public boolean isPreviewSafe() {
    return isPreviewSafe;
  }

  public void setPreviewSafe(boolean previewSafe) {
    isPreviewSafe = previewSafe;
  }

  public long getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(long likeCount) {
    this.likeCount = likeCount;
  }

  public long getRepostCount() {
    return repostCount;
  }

  public void setRepostCount(long repostCount) {
    this.repostCount = repostCount;
  }

  public long getReplyCount() {
    return replyCount;
  }

  public void setReplyCount(long replyCount) {
    this.replyCount = replyCount;
  }

  public long getViewCount() {
    return viewCount;
  }

  public void setViewCount(int viewCount) {
    this.viewCount = viewCount;
  }

  public long getBookmarkCount() {
    return bookmarkCount;
  }

  public void setBookmarkCount(long bookmarkCount) {
    this.bookmarkCount = bookmarkCount;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Date getEditedAt() {
    return editedAt;
  }

  public void setEditedAt(Date editedAt) {
    this.editedAt = editedAt;
  }

  public GeoPoint getLocation() {
    return location;
  }

  public void setLocation(GeoPoint location) {
    this.location = location;
  }

  public boolean isLiked() {
    return isLiked;
  }

  public void setLiked(boolean liked) {
    isLiked = liked;
  }

  public boolean isReposted() {
    return isReposted;
  }

  public void setReposted(boolean reposted) {
    isReposted = reposted;
  }

  public boolean isBookmarked() {
    return isBookmarked;
  }

  public void setBookmarked(boolean bookmarked) {
    isBookmarked = bookmarked;
  }

  public boolean isPinned() {
    return isPinned;
  }

  public void setPinned(boolean pinned) {
    isPinned = pinned;
  }

  public String getParentPostId() {
    return parentPostId;
  }

  public void setParentPostId(String parentPostId) {
    this.parentPostId = parentPostId;
  }

  public String getRootPostId() {
    return rootPostId;
  }

  public void setRootPostId(String rootPostId) {
    this.rootPostId = rootPostId;
  }

  public List<String> getMentionIds() {
    return mentionIds;
  }

  public void setMentionIds(@Nullable List<String> mentionIds) {
    this.mentionIds = mentionIds != null ? mentionIds : new ArrayList<>();
  }

  public boolean isSensitive() {
    return isSensitive;
  }

  public void setSensitive(boolean sensitive) {
    this.isSensitive = sensitive;
  }

  public boolean isDeleted() { // Getter للمتغير الصحيح `isDeleted`
    return isDeleted;
  }

  public void setDeleted(boolean deleted) { // Setter للمتغير الصحيح `isDeleted`
    this.isDeleted = deleted; // استخدام `this.isDeleted`
  }

  public String getDeletedReason() {
    return deletedReason;
  }

  public void setDeletedReason(String deletedReason) {
    this.deletedReason = deletedReason;
  }

  public String getModerationStatus() {
    return moderationStatus;
  }

  public void setModerationStatus(@NonNull String moderationStatus) {
    if (!isValidModerationStatus(moderationStatus)) {
      throw new IllegalArgumentException("Invalid moderation status");
    }
    this.moderationStatus = moderationStatus;
  }

  public List<String> getModeratorNotes() {
    return moderatorNotes;
  }

  public void setModeratorNotes(@Nullable List<String> moderatorNotes) {
    this.moderatorNotes = moderatorNotes != null ? moderatorNotes : new ArrayList<>();
  }

  public Map<String, String> getExternalLinks() {
    return externalLinks;
  }

  public void setExternalLinks(@Nullable Map<String, String> externalLinks) {
    this.externalLinks = externalLinks != null ? externalLinks : new HashMap<>();
  }

  public List<String> getHashtags() {
    return hashtags;
  }

  public void setHashtags(@Nullable List<String> hashtags) {
    this.hashtags = hashtags != null ? hashtags : new ArrayList<>();
  }

  public String getSourceApp() {
    return sourceApp;
  }

  public void setSourceApp(String sourceApp) {
    this.sourceApp = sourceApp;
  }

  // Equals and HashCode
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PostModel post = (PostModel) o;
    return likeCount == post.likeCount
            && repostCount == post.repostCount
            && replyCount == post.replyCount
            && bookmarkCount == post.bookmarkCount
            && isLiked == post.isLiked
            && isReposted == post.isReposted
            && isBookmarked == post.isBookmarked
            && Objects.equals(postId, post.postId)
            && Objects.equals(content, post.content)
            && Objects.equals(mediaUrls, post.mediaUrls)
            && Objects.equals(linkPreviews, post.linkPreviews)
            && Objects.equals(createdAt, post.createdAt)
            && Objects.equals(authorId, post.authorId)
            && Objects.equals(authorAvatarUrl, post.authorAvatarUrl)
            && Objects.equals(hashtags, post.hashtags)
            && Objects.equals(translatedContent, post.translatedContent)
            && isExpanded == post.isExpanded
            && isTranslated == post.isTranslated
            && videoDuration == post.videoDuration; // مقارنة مدة الفيديو
  }

  @Override
  public int hashCode() {
    return Objects.hash(
            postId,
            content,
            likeCount,
            repostCount,
            replyCount,
            bookmarkCount,
            isLiked,
            isReposted,
            isBookmarked,
            mediaUrls,
            linkPreviews,
            createdAt,
            authorId,
            authorAvatarUrl,
            hashtags,
            translatedContent,
            isExpanded,
            isTranslated,
            videoDuration); // تضمين مدة الفيديو في الـ hash code
  }

  // LinkPreview inner class
  public static class LinkPreview implements Parcelable {
    private String url;
    private String title;
    private String description;
    private String imageUrl;
    private String siteName;
    private String faviconUrl;
    private String mediaType;
    private int imageWidth;
    private int imageHeight;
    private Date cacheExpiry;

    public LinkPreview() {}

    protected LinkPreview(Parcel in) {
      url = in.readString();
      title = in.readString();
      description = in.readString();
      imageUrl = in.readString();
      siteName = in.readString();
      faviconUrl = in.readString();
      mediaType = in.readString();
      imageWidth = in.readInt();
      imageHeight = in.readInt();
      cacheExpiry = in.readLong() != -1 ? new Date(in.readLong()) : null;
      long tmpCacheExpiry = in.readLong();
      cacheExpiry = tmpCacheExpiry == -1 ? null : new Date(tmpCacheExpiry);
    }

    public static final Creator<LinkPreview> CREATOR =
            new Creator<LinkPreview>() {
              @Override
              public LinkPreview createFromParcel(Parcel in) {
                return new LinkPreview(in);
              }

              @Override
              public LinkPreview[] newArray(int size) {
                return new LinkPreview[size];
              }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(url);
      dest.writeString(title);
      dest.writeString(description);
      dest.writeString(imageUrl);
      dest.writeString(siteName);
      dest.writeString(faviconUrl);
      dest.writeString(mediaType);
      dest.writeInt(imageWidth);
      dest.writeInt(imageHeight);
      dest.writeLong(cacheExpiry != null ? cacheExpiry.getTime() : -1);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    // Getters and Setters with validation
    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      if (!isValidUrl(url)) {
        throw new IllegalArgumentException("Invalid URL format");
      }
      this.url = url;
    }

    private boolean isValidUrl(String url) {
      return Pattern.compile("^(https?://)?([\\w-]+\\.)+[\\w-]+(/\\S*)?$").matcher(url).matches();
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getImageUrl() {
      return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
      if (imageUrl != null && !isValidUrl(imageUrl)) {
        throw new IllegalArgumentException("Invalid image URL");
      }
      this.imageUrl = imageUrl;
    }

    public String getSiteName() {
      return siteName;
    }

    public void setSiteName(String siteName) {
      this.siteName = siteName;
    }

    public String getFaviconUrl() {
      return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
      if (faviconUrl != null && !isValidUrl(faviconUrl)) {
        throw new IllegalArgumentException("Invalid favicon URL");
      }
      this.faviconUrl = faviconUrl;
    }

    public String getMediaType() {
      return mediaType;
    }

    public void setMediaType(String mediaType) {
      this.mediaType = mediaType;
    }

    public int getImageWidth() {
      return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
      if (imageWidth < 0) throw new IllegalArgumentException("Width cannot be negative");
      this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
      return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
      if (imageHeight < 0) throw new IllegalArgumentException("Height cannot be negative");
      this.imageHeight = imageHeight;
    }

    public Date getCacheExpiry() {
      return cacheExpiry;
    }

    public void setCacheExpiry(Date cacheExpiry) {
      this.cacheExpiry = cacheExpiry;
    }
  }
}