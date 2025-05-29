package com.spidroid.starry.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentModel implements Parcelable {
  public static final String FIELD_TIMESTAMP = "timestamp";
  public static final String FIELD_PARENT_COMMENT_ID = "parentCommentId";
  public static final String FIELD_PARENT_POST_ID = "parentPostId";

  private String commentId;
  private String authorId;
  private String authorDisplayName;
  private String authorUsername;
  private String authorAvatarUrl;
  private String content;

  @ServerTimestamp private Timestamp timestamp;

  private int likeCount;
  private List<String> mediaUrls = new ArrayList<>();
  private String parentPostId;
  private String parentCommentId;
  private int repliesCount;
  private Map<String, Boolean> likes = new HashMap<>();
  private boolean isLiked;
  private int depth;
  private String parentAuthorId;
  private String parentAuthorUsername;
  private boolean authorVerified;

  // Required empty constructor for Firestore
  public CommentModel() {}

  // Constructor for new comments
  public CommentModel(String content, String parentPostId, String parentCommentId) {
    this.content = content;
    this.parentPostId = parentPostId;
    this.parentCommentId = parentCommentId;
  }

  protected CommentModel(Parcel in) {
    commentId = in.readString();
    authorId = in.readString();
    authorDisplayName = in.readString();
    authorUsername = in.readString();
    authorAvatarUrl = in.readString();
    content = in.readString();
    likeCount = in.readInt();
    mediaUrls = in.createStringArrayList();
    parentPostId = in.readString();
    parentCommentId = in.readString();
    repliesCount = in.readInt();
    isLiked = in.readByte() != 0;
    timestamp = in.readParcelable(Timestamp.class.getClassLoader());
    depth = in.readInt();
    parentAuthorId = in.readString();
    parentAuthorUsername = in.readString();
    authorVerified = in.readByte() != 0;
  }

  public static final Creator<CommentModel> CREATOR =
      new Creator<CommentModel>() {
        @Override
        public CommentModel createFromParcel(Parcel in) {
          return new CommentModel(in);
        }

        @Override
        public CommentModel[] newArray(int size) {
          return new CommentModel[size];
        }
      };

  @Exclude
  public String getCommentId() {
    return commentId;
  }

  public void setCommentId(String commentId) {
    this.commentId = commentId;
  }

  public String getAuthorId() {
    return authorId;
  }

  public void setAuthorId(String authorId) {
    this.authorId = authorId;
  }

  @Exclude
  public int getDepth() {
    return depth;
  }

  @Exclude
  public void setDepth(int depth) {
    this.depth = depth;
  }

  @Exclude
  public boolean isReply() {
    return depth > 0;
  }

  @PropertyName("author_display_name")
  public String getAuthorDisplayName() {
    return authorDisplayName;
  }

  @PropertyName("author_display_name")
  public void setAuthorDisplayName(String authorDisplayName) {
    this.authorDisplayName = authorDisplayName;
  }

  @PropertyName("author_username")
  public String getAuthorUsername() {
    return authorUsername;
  }

  @PropertyName("author_username")
  public void setAuthorUsername(String authorUsername) {
    this.authorUsername = authorUsername;
  }

  @PropertyName("author_avatar_url")
  public String getAuthorAvatarUrl() {
    return authorAvatarUrl;
  }

  @PropertyName("author_avatar_url")
  public void setAuthorAvatarUrl(String authorAvatarUrl) {
    this.authorAvatarUrl = authorAvatarUrl;
  }

  @PropertyName("author_verified")
  public boolean isAuthorVerified() {
    return authorVerified;
  }

  @PropertyName("author_verified")
  public void setAuthorVerified(boolean verified) {
    this.authorVerified = verified;
  }

  public String getParentAuthorId() {
    return parentAuthorId;
  }

  public void setParentAuthorId(String parentAuthorId) {
    this.parentAuthorId = parentAuthorId;
  }

  public String getParentAuthorUsername() {
    return parentAuthorUsername;
  }

  public void setParentAuthorUsername(String parentAuthorUsername) {
    this.parentAuthorUsername = parentAuthorUsername;
  }

  @Exclude
  public boolean isReplyToAuthor(String postAuthorId) {
    return parentAuthorId != null && parentAuthorId.equals(postAuthorId);
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @PropertyName("like_count")
  public int getLikeCount() {
    return likeCount;
  }

  @PropertyName("like_count")
  public void setLikeCount(int likeCount) {
    this.likeCount = likeCount;
  }

  @PropertyName("media_urls")
  public List<String> getMediaUrls() {
    return mediaUrls;
  }

  @PropertyName("media_urls")
  public void setMediaUrls(List<String> mediaUrls) {
    this.mediaUrls = mediaUrls;
  }

  @PropertyName("parent_post_id")
  public String getParentPostId() {
    return parentPostId;
  }

  @PropertyName("parent_post_id")
  public void setParentPostId(String parentPostId) {
    this.parentPostId = parentPostId;
  }

  @PropertyName("parent_comment_id")
  public String getParentCommentId() {
    return parentCommentId;
  }

  @PropertyName("parent_comment_id")
  public void setParentCommentId(String parentCommentId) {
    this.parentCommentId = parentCommentId;
  }

  @PropertyName("replies_count")
  public int getRepliesCount() {
    return repliesCount;
  }

  @PropertyName("replies_count")
  public void setRepliesCount(int repliesCount) {
    this.repliesCount = repliesCount;
  }

  public Map<String, Boolean> getLikes() {
    return likes;
  }

  public void setLikes(Map<String, Boolean> likes) {
    this.likes = likes;
  }

  @Exclude
  public boolean isLiked() {
    return isLiked;
  }

  @Exclude
  public void setLiked(boolean liked) {
    isLiked = liked;
  }

  @PropertyName("timestamp")
  public Timestamp getTimestamp() {
    return timestamp;
  }

  @PropertyName("timestamp")
  public void setTimestamp(Timestamp timestamp) {
    this.timestamp = timestamp;
  }

  @Exclude
  public Date getJavaDate() {
    return timestamp != null ? timestamp.toDate() : null;
  }

  @Exclude
  public void setJavaDate(Date date) {
    this.timestamp = date != null ? new Timestamp(date) : null;
  }

  @Exclude
  public boolean isTopLevel() {
    return parentCommentId == null || parentCommentId.isEmpty();
  }

  @Exclude
  public Map<String, Object> toMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("author_id", authorId);
    map.put("author_display_name", authorDisplayName);
    map.put("author_username", authorUsername);
    map.put("author_avatar_url", authorAvatarUrl);
    map.put("content", content);
    map.put("like_count", likeCount);
    map.put("media_urls", mediaUrls);
    map.put("parent_post_id", parentPostId);
    map.put("parent_comment_id", parentCommentId);
    map.put("replies_count", repliesCount);
    map.put("likes", likes);
    map.put("timestamp", timestamp);
    return map;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(commentId);
    dest.writeString(authorId);
    dest.writeString(authorDisplayName);
    dest.writeString(authorUsername);
    dest.writeString(authorAvatarUrl);
    dest.writeString(content);
    dest.writeInt(likeCount);
    dest.writeStringList(mediaUrls);
    dest.writeString(parentPostId);
    dest.writeString(parentCommentId);
    dest.writeInt(repliesCount);
    dest.writeByte((byte) (isLiked ? 1 : 0));
    dest.writeParcelable(timestamp, flags);
    dest.writeInt(depth);
    dest.writeString(parentAuthorId);
    dest.writeString(parentAuthorUsername);
    dest.writeByte((byte) (authorVerified ? 1 : 0));
  }
}
