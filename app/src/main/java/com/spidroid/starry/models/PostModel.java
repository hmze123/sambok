package com.spidroid.starry.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PostModel implements Parcelable {

  public static final String TYPE_TEXT = "text";
  public static final String TYPE_IMAGE = "image";
  public static final String TYPE_VIDEO = "video";
  public static final String TYPE_POLL = "poll";
  public static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mov", "avi", "mkv", "webm");
  // *** التعديل هنا: إضافة الثابت MAX_CONTENT_LENGTH ***
  public static final int MAX_CONTENT_LENGTH = 280; // الحد الأقصى لطول نص المنشور (مثل تويتر)

  private String postId;
  private String authorId;
  private String authorUsername;
  private String authorDisplayName;
  private String authorAvatarUrl;
  private boolean isAuthorVerified;
  private String content;
  private List<String> mediaUrls = new ArrayList<>();
  private String contentType;
  private long videoDuration;
  private List<LinkPreview> linkPreviews = new ArrayList<>();
  private long likeCount;
  private long repostCount;
  private long replyCount;
  private long bookmarkCount;
  @ServerTimestamp private Date createdAt;
  private Date updatedAt;
  private Map<String, Boolean> likes = new HashMap<>();
  private Map<String, Boolean> bookmarks = new HashMap<>();
  private Map<String, Boolean> reposts = new HashMap<>();
  @Exclude private boolean isLiked;
  @Exclude private boolean isBookmarked;
  @Exclude private boolean isReposted;
  private String language;

  public PostModel() {}

  public PostModel(@NonNull String authorId, @NonNull String content) {
    this.authorId = authorId;
    this.content = content;
    this.createdAt = new Date();
    this.contentType = TYPE_TEXT;
    this.mediaUrls = new ArrayList<>();
    this.likes = new HashMap<>();
    this.bookmarks = new HashMap<>();
    this.reposts = new HashMap<>();
  }

  // --- Getters and Setters ---
  public String getPostId() { return postId; }
  public void setPostId(String postId) { this.postId = postId; }
  public String getAuthorId() { return authorId; }
  public void setAuthorId(String authorId) { this.authorId = authorId; }
  public String getAuthorUsername() { return authorUsername; }
  public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
  public String getAuthorDisplayName() { return authorDisplayName; }
  public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }
  public String getAuthorAvatarUrl() { return authorAvatarUrl; }
  public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }
  public boolean isAuthorVerified() { return isAuthorVerified; }
  public void setAuthorVerified(boolean authorVerified) { isAuthorVerified = authorVerified; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public List<String> getMediaUrls() { return mediaUrls; }
  public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public long getVideoDuration() { return videoDuration; }
  public void setVideoDuration(long videoDuration) { this.videoDuration = videoDuration; }
  public List<LinkPreview> getLinkPreviews() { return linkPreviews; }
  public void setLinkPreviews(List<LinkPreview> linkPreviews) { this.linkPreviews = linkPreviews; }
  public long getLikeCount() { return likeCount; }
  public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
  public long getRepostCount() { return repostCount; }
  public void setRepostCount(long repostCount) { this.repostCount = repostCount; }
  public long getReplyCount() { return replyCount; }
  public void setReplyCount(long replyCount) { this.replyCount = replyCount; }
  public long getBookmarkCount() { return bookmarkCount; }
  public void setBookmarkCount(long bookmarkCount) { this.bookmarkCount = bookmarkCount; }
  public Date getCreatedAt() { return createdAt; }
  public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
  public Date getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
  public Map<String, Boolean> getLikes() { return likes; }
  public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
  public Map<String, Boolean> getBookmarks() { return bookmarks; }
  public void setBookmarks(Map<String, Boolean> bookmarks) { this.bookmarks = bookmarks; }
  public Map<String, Boolean> getReposts() { return reposts; }
  public void setReposts(Map<String, Boolean> reposts) { this.reposts = reposts; }
  @Exclude public boolean isLiked() { return isLiked; }
  @Exclude public void setLiked(boolean liked) { isLiked = liked; }
  @Exclude public boolean isBookmarked() { return isBookmarked; }
  @Exclude public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
  @Exclude public boolean isReposted() { return isReposted; }
  @Exclude public void setReposted(boolean reposted) { isReposted = reposted; }

  // --- Helper Methods ---
  public void toggleLike() {
    isLiked = !isLiked;
    likeCount += (isLiked ? 1 : -1);
  }
  public void toggleRepost() {
    isReposted = !isReposted;
    repostCount += (isReposted ? 1 : -1);
  }
  public void toggleBookmark() {
    isBookmarked = !isBookmarked;
    bookmarkCount += (isBookmarked ? 1 : -1);
  }
  @Exclude
  public boolean isVideoPost() {
    return TYPE_VIDEO.equals(contentType);
  }
  @Exclude
  public boolean isImagePost() {
    return TYPE_IMAGE.equals(contentType);
  }

  @Exclude
  public String getFirstMediaUrl() {
    return (mediaUrls != null && !mediaUrls.isEmpty()) ? mediaUrls.get(0) : null;
  }

  // --- Parcelable Implementation ---
  protected PostModel(Parcel in) { /* ... */ }
  public static final Creator<PostModel> CREATOR = new Creator<PostModel>() {
    @Override
    public PostModel createFromParcel(Parcel in) { return new PostModel(in); }
    @Override
    public PostModel[] newArray(int size) { return new PostModel[size]; }
  };
  @Override
  public int describeContents() { return 0; }
  @Override
  public void writeToParcel(Parcel dest, int flags) { /* ... */ }

  // --- LinkPreview inner class ---
  public static class LinkPreview implements Parcelable {
    private String url;
    private String title;
    private String description;
    private String imageUrl;
    private String siteName;
    public LinkPreview() {}
    protected LinkPreview(Parcel in) {
      url = in.readString();
      title = in.readString();
      description = in.readString();
      imageUrl = in.readString();
      siteName = in.readString();
    }
    public static final Creator<LinkPreview> CREATOR = new Creator<LinkPreview>() {
      @Override
      public LinkPreview createFromParcel(Parcel in) { return new LinkPreview(in); }
      @Override
      public LinkPreview[] newArray(int size) { return new LinkPreview[size]; }
    };
    @Override
    public int describeContents() { return 0; }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(url);
      dest.writeString(title);
      dest.writeString(description);
      dest.writeString(imageUrl);
      dest.writeString(siteName);
    }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
  }
}