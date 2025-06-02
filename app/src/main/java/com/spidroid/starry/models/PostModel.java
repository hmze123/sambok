package com.spidroid.starry.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
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
  private static final String TAG = "PostModel";

  public static final String TYPE_TEXT = "text";
  public static final String TYPE_IMAGE = "image";
  public static final String TYPE_VIDEO = "video"; // ★★★ تأكد من وجود هذا السطر ★★★
  public static final String TYPE_POLL = "poll";
  public static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mov", "avi", "mkv", "webm");
  public static final int MAX_CONTENT_LENGTH = 280;

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
  private long likeCount = 0;
  private long repostCount = 0;
  private long replyCount = 0;
  private long bookmarkCount = 0;
  @ServerTimestamp private Date createdAt;
  private Date updatedAt;
  private Map<String, Boolean> likes = new HashMap<>();
  private Map<String, Boolean> bookmarks = new HashMap<>();
  private Map<String, Boolean> reposts = new HashMap<>();
  private Map<String, String> reactions = new HashMap<>();
  private boolean isPinned = false;

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
  }

  // --- Getters and Setters (كما تم تعديلها سابقًا) ---
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
  public List<String> getMediaUrls() { return mediaUrls != null ? mediaUrls : new ArrayList<>(); }
  public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public long getVideoDuration() { return videoDuration; }
  public void setVideoDuration(long videoDuration) { this.videoDuration = videoDuration; }
  public List<LinkPreview> getLinkPreviews() { return linkPreviews != null ? linkPreviews : new ArrayList<>(); }
  public void setLinkPreviews(List<LinkPreview> linkPreviews) { this.linkPreviews = linkPreviews; }
  public long getLikeCount() { return likeCount; }
  public void setLikeCount(long likeCount) { this.likeCount = likeCount < 0 ? 0 : likeCount; }
  public long getRepostCount() { return repostCount; }
  public void setRepostCount(long repostCount) { this.repostCount = repostCount < 0 ? 0 : repostCount; }
  public long getReplyCount() { return replyCount; }
  public void setReplyCount(long replyCount) { this.replyCount = replyCount < 0 ? 0 : replyCount; }
  public long getBookmarkCount() { return bookmarkCount; }
  public void setBookmarkCount(long bookmarkCount) { this.bookmarkCount = bookmarkCount < 0 ? 0 : bookmarkCount; }
  public Date getCreatedAt() { return createdAt; }
  public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
  public Date getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
  public Map<String, Boolean> getLikes() { return likes != null ? likes : new HashMap<>(); }
  public void setLikes(Map<String, Boolean> likes) { this.likes = likes; }
  public Map<String, Boolean> getBookmarks() { return bookmarks != null ? bookmarks : new HashMap<>(); }
  public void setBookmarks(Map<String, Boolean> bookmarks) { this.bookmarks = bookmarks; }
  public Map<String, Boolean> getReposts() { return reposts != null ? reposts : new HashMap<>(); }
  public void setReposts(Map<String, Boolean> reposts) { this.reposts = reposts; }
  public Map<String, String> getReactions() { return reactions != null ? reactions : new HashMap<>(); }
  public void setReactions(Map<String, String> reactions) { this.reactions = reactions; }
  public boolean isPinned() { return isPinned; }
  public void setPinned(boolean pinned) { isPinned = pinned; }
  @Exclude public boolean isLiked() { return isLiked; }
  @Exclude public void setLiked(boolean liked) { isLiked = liked; }
  @Exclude public boolean isBookmarked() { return isBookmarked; }
  @Exclude public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
  @Exclude public boolean isReposted() { return isReposted; }
  @Exclude public void setReposted(boolean reposted) { isReposted = reposted; }
  public String getLanguage() { return language; }
  public void setLanguage(String language) { this.language = language; }

  // --- Helper Methods ---
  public void toggleLike() {
    Log.d(TAG, "Before toggleLike: postId=" + postId + ", isLiked=" + isLiked + ", likeCount=" + likeCount);
    isLiked = !isLiked;
    likeCount += (isLiked ? 1 : -1);
    if (likeCount < 0) likeCount = 0;
    Log.d(TAG, "After toggleLike: postId=" + postId + ", isLiked=" + isLiked + ", likeCount=" + likeCount);
  }
  public void toggleRepost() {
    Log.d(TAG, "Before toggleRepost: postId=" + postId + ", isReposted=" + isReposted + ", repostCount=" + repostCount);
    isReposted = !isReposted;
    repostCount += (isReposted ? 1 : -1);
    if (repostCount < 0) repostCount = 0;
    Log.d(TAG, "After toggleRepost: postId=" + postId + ", isReposted=" + isReposted + ", repostCount=" + repostCount);
  }
  public void toggleBookmark() {
    Log.d(TAG, "Before toggleBookmark: postId=" + postId + ", isBookmarked=" + isBookmarked + ", bookmarkCount=" + bookmarkCount);
    isBookmarked = !isBookmarked;
    bookmarkCount += (isBookmarked ? 1 : -1);
    if (bookmarkCount < 0) bookmarkCount = 0;
    Log.d(TAG, "After toggleBookmark: postId=" + postId + ", isBookmarked=" + isBookmarked + ", bookmarkCount=" + bookmarkCount);
  }
  @Exclude
  public void addReaction(String userId, String emoji) {
    if (userId == null || userId.isEmpty() || emoji == null || emoji.isEmpty()) return;
    if (this.reactions == null) this.reactions = new HashMap<>();
    this.reactions.put(userId, emoji);
  }
  @Exclude
  public void removeReaction(String userId) {
    if (userId == null || userId.isEmpty() || this.reactions == null) return;
    this.reactions.remove(userId);
  }
  @Exclude
  public String getUserReaction(String userId) {
    if (userId == null || this.reactions == null) return null;
    return this.reactions.get(userId);
  }
  @Exclude public boolean isVideoPost() { return TYPE_VIDEO.equals(contentType); }
  @Exclude public boolean isImagePost() { return TYPE_IMAGE.equals(contentType); }
  @Exclude
  public String getFirstMediaUrl() {
    return (mediaUrls != null && !mediaUrls.isEmpty()) ? mediaUrls.get(0) : null;
  }

  // --- Parcelable Implementation (كما هي مع إضافة isPinned) ---
  protected PostModel(Parcel in) {
    postId = in.readString();
    authorId = in.readString();
    authorUsername = in.readString();
    authorDisplayName = in.readString();
    authorAvatarUrl = in.readString();
    isAuthorVerified = in.readByte() != 0;
    content = in.readString();
    mediaUrls = in.createStringArrayList();
    contentType = in.readString();
    videoDuration = in.readLong();
    linkPreviews = in.createTypedArrayList(LinkPreview.CREATOR);
    likeCount = in.readLong();
    repostCount = in.readLong();
    replyCount = in.readLong();
    bookmarkCount = in.readLong();
    long tmpCreatedAt = in.readLong();
    createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    long tmpUpdatedAt = in.readLong();
    updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
    likes = new HashMap<>();
    in.readMap(likes, Boolean.class.getClassLoader());
    bookmarks = new HashMap<>();
    in.readMap(bookmarks, Boolean.class.getClassLoader());
    reposts = new HashMap<>();
    in.readMap(reposts, Boolean.class.getClassLoader());
    reactions = new HashMap<>();
    in.readMap(reactions, String.class.getClassLoader());
    isPinned = in.readByte() != 0;
    isLiked = in.readByte() != 0;
    isBookmarked = in.readByte() != 0;
    isReposted = in.readByte() != 0;
    language = in.readString();
  }

  public static final Creator<PostModel> CREATOR = new Creator<PostModel>() {
    @Override
    public PostModel createFromParcel(Parcel in) { return new PostModel(in); }
    @Override
    public PostModel[] newArray(int size) { return new PostModel[size]; }
  };

  @Override
  public int describeContents() { return 0; }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(postId);
    dest.writeString(authorId);
    dest.writeString(authorUsername);
    dest.writeString(authorDisplayName);
    dest.writeString(authorAvatarUrl);
    dest.writeByte((byte) (isAuthorVerified ? 1 : 0));
    dest.writeString(content);
    dest.writeStringList(mediaUrls);
    dest.writeString(contentType);
    dest.writeLong(videoDuration);
    dest.writeTypedList(linkPreviews);
    dest.writeLong(likeCount);
    dest.writeLong(repostCount);
    dest.writeLong(replyCount);
    dest.writeLong(bookmarkCount);
    dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
    dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
    dest.writeMap(likes);
    dest.writeMap(bookmarks);
    dest.writeMap(reposts);
    dest.writeMap(reactions);
    dest.writeByte((byte) (isPinned ? 1 : 0));
    dest.writeByte((byte) (isLiked ? 1 : 0));
    dest.writeByte((byte) (isBookmarked ? 1 : 0));
    dest.writeByte((byte) (isReposted ? 1 : 0));
    dest.writeString(language);
  }

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PostModel postModel = (PostModel) o;
    return Objects.equals(postId, postModel.postId) &&
            likeCount == postModel.likeCount &&
            repostCount == postModel.repostCount &&
            bookmarkCount == postModel.bookmarkCount &&
            isPinned == postModel.isPinned &&
            Objects.equals(content, postModel.content) &&
            Objects.equals(authorId, postModel.authorId) &&
            Objects.equals(reactions, postModel.reactions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(postId, content, authorId, likeCount, repostCount, bookmarkCount, reactions, isPinned);
  }
}