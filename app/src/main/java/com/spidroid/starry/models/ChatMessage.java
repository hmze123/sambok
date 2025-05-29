package com.spidroid.starry.models;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatMessage implements Parcelable {
  // Core Message Data
  private String messageId;
  private String senderId;
  private String senderName;
  private String senderAvatar;
  private String content;
  private String type;

  // Media Fields
  private String mediaUrl;
  private String thumbnailUrl;
  private long mediaSize;
  private long videoDuration;

  // File Fields
  private String fileName;
  private String fileUrl;
  private long fileSize;
  private String fileType;

  // Context Fields
  private String replyToId;
  private String replyPreview;
  private Map<String, String> reactions = new HashMap<>();

  // Status Tracking
  private Map<String, Boolean> readReceipts = new HashMap<>();
  private boolean deleted;
  private boolean edited;
  private boolean uploading;

  // Timestamps
  @ServerTimestamp private Date timestamp;
  @ServerTimestamp private Date lastUpdated;

  // Poll Data
  private Poll poll;

  // Constants
  public static final int MAX_CONTENT_LENGTH = 2000;
  public static final int MAX_MEDIA_SIZE_MB = 15;
  public static final int THUMBNAIL_SIZE = 256;

  // Message Types
  public static final String TYPE_TEXT = "text";
  public static final String TYPE_IMAGE = "image";
  public static final String TYPE_GIF = "gif";
  public static final String TYPE_VIDEO = "video";
  public static final String TYPE_POLL = "poll";
  public static final String TYPE_FILE = "file";

  // Message status
  public static final String STATUS_SENT = "sent";
  public static final String STATUS_DELIVERED = "delivered";
  public static final String STATUS_SEEN = "seen";
  private String deliveryStatus = STATUS_SENT;

  // Constructors
  public ChatMessage() {}

  public ChatMessage(@NonNull String senderId, @NonNull String content) {
    this.messageId = UUID.randomUUID().toString();
    this.senderId = senderId;
    setContent(content);
    this.type = TYPE_TEXT;
  }

  public ChatMessage(
      @NonNull String senderId,
      @NonNull String mediaUrl,
      @NonNull String type,
      String thumbnailUrl) {
    this.messageId = UUID.randomUUID().toString();
    this.senderId = senderId;
    this.mediaUrl = mediaUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.type = type;
  }

  public ChatMessage(@NonNull String senderId, @NonNull Poll poll) {
    this.messageId = UUID.randomUUID().toString();
    this.senderId = senderId;
    this.poll = poll;
    this.type = TYPE_POLL;
  }

  // Parcelable implementation
  protected ChatMessage(Parcel in) {
    messageId = in.readString();
    senderId = in.readString();
    senderName = in.readString();
    senderAvatar = in.readString();
    content = in.readString();
    type = in.readString();
    mediaUrl = in.readString();
    thumbnailUrl = in.readString();
    mediaSize = in.readLong();
    videoDuration = in.readLong();
    fileName = in.readString();
    fileUrl = in.readString();
    fileSize = in.readLong();
    fileType = in.readString();
    replyToId = in.readString();
    replyPreview = in.readString();
    deleted = in.readByte() != 0;
    edited = in.readByte() != 0;
    uploading = in.readByte() != 0;
    timestamp = new Date(in.readLong());
    lastUpdated = new Date(in.readLong());
    in.readMap(readReceipts, Boolean.class.getClassLoader());
    poll = in.readParcelable(Poll.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(messageId);
    dest.writeString(senderId);
    dest.writeString(senderName);
    dest.writeString(senderAvatar);
    dest.writeString(content);
    dest.writeString(type);
    dest.writeString(mediaUrl);
    dest.writeString(thumbnailUrl);
    dest.writeLong(mediaSize);
    dest.writeLong(videoDuration);
    dest.writeString(fileName);
    dest.writeString(fileUrl);
    dest.writeLong(fileSize);
    dest.writeString(fileType);
    dest.writeString(replyToId);
    dest.writeString(replyPreview);
    dest.writeByte((byte) (deleted ? 1 : 0));
    dest.writeByte((byte) (edited ? 1 : 0));
    dest.writeByte((byte) (uploading ? 1 : 0));
    dest.writeLong(timestamp != null ? timestamp.getTime() : -1);
    dest.writeLong(lastUpdated != null ? lastUpdated.getTime() : -1);
    dest.writeMap(readReceipts);
    dest.writeParcelable(poll, flags);
  }

  // Poll Option class
  public static class PollOption implements Parcelable {
    private String text;
    private int votes;

    public PollOption() {}

    public PollOption(String text) {
      this.text = text;
      this.votes = 0;
    }

    protected PollOption(Parcel in) {
      text = in.readString();
      votes = in.readInt();
    }

    public static final Creator<PollOption> CREATOR =
        new Creator<PollOption>() {
          @Override
          public PollOption createFromParcel(Parcel in) {
            return new PollOption(in);
          }

          @Override
          public PollOption[] newArray(int size) {
            return new PollOption[size];
          }
        };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(text);
      dest.writeInt(votes);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    // Getters & Setters
    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    public int getVotes() {
      return votes;
    }

    public void setVotes(int votes) {
      this.votes = votes;
    }
  }

  // Poll class
  public static class Poll implements Parcelable {
    private String question;
    private List<PollOption> options = new ArrayList<>();
    private boolean expired;
    private boolean voted;
    private int totalVotes;

    public Poll() {}

    protected Poll(Parcel in) {
      question = in.readString();
      options = in.createTypedArrayList(PollOption.CREATOR);
      expired = in.readByte() != 0;
      voted = in.readByte() != 0;
      totalVotes = in.readInt();
    }

    public static final Creator<Poll> CREATOR =
        new Creator<Poll>() {
          @Override
          public Poll createFromParcel(Parcel in) {
            return new Poll(in);
          }

          @Override
          public Poll[] newArray(int size) {
            return new Poll[size];
          }
        };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(question);
      dest.writeTypedList(options);
      dest.writeByte((byte) (expired ? 1 : 0));
      dest.writeByte((byte) (voted ? 1 : 0));
      dest.writeInt(totalVotes);
    }

    @Override
    public int describeContents() {
      return 0;
    }

    // Getters & Setters
    public String getQuestion() {
      return question;
    }

    public void setQuestion(String question) {
      this.question = question;
    }

    public List<PollOption> getOptions() {
      return options;
    }

    public void setOptions(List<PollOption> options) {
      this.options = options;
    }

    public boolean isExpired() {
      return expired;
    }

    public void setExpired(boolean expired) {
      this.expired = expired;
    }

    public boolean isVoted() {
      return voted;
    }

    public void setVoted(boolean voted) {
      this.voted = voted;
    }

    public int getTotalVotes() {
      return totalVotes;
    }

    public void setTotalVotes(int totalVotes) {
      this.totalVotes = totalVotes;
    }
  }

  // Main class getters/setters
  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getSenderId() {
    return senderId;
  }

  public void setSenderId(String senderId) {
    this.senderId = senderId;
  }

  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
  }

  public String getSenderAvatar() {
    return senderAvatar;
  }

  public void setSenderAvatar(String senderAvatar) {
    this.senderAvatar = senderAvatar;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    if (content != null && content.length() > MAX_CONTENT_LENGTH) {
      throw new IllegalArgumentException("Content exceeds maximum length");
    }
    this.content = content;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    if (!isValidType(type)) {
      throw new IllegalArgumentException("Invalid message type");
    }
    this.type = type;
  }

  public String getMediaUrl() {
    return mediaUrl;
  }

  public void setMediaUrl(String mediaUrl) {
    this.mediaUrl = mediaUrl;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public long getMediaSize() {
    return mediaSize;
  }

  public void setMediaSize(long mediaSize) {
    this.mediaSize = mediaSize;
  }

  public long getVideoDuration() {
    return videoDuration;
  }

  public void setVideoDuration(long videoDuration) {
    this.videoDuration = videoDuration;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public void setFileUrl(String fileUrl) {
    this.fileUrl = fileUrl;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getReplyToId() {
    return replyToId;
  }

  public void setReplyToId(String replyToId) {
    this.replyToId = replyToId;
  }

  public String getReplyPreview() {
    return replyPreview;
  }

  public void setReplyPreview(String replyPreview) {
    this.replyPreview = replyPreview;
  }

  public Map<String, String> getReactions() {
    return reactions;
  }

  public void setReactions(Map<String, String> reactions) {
    this.reactions = reactions != null ? reactions : new HashMap<>();
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public boolean isEdited() {
    return edited;
  }

  public void setEdited(boolean edited) {
    this.edited = edited;
  }

  public boolean isUploading() {
    return uploading;
  }

  public void setUploading(boolean uploading) {
    this.uploading = uploading;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Map<String, Boolean> getReadReceipts() {
    return readReceipts;
  }

  public void setReadReceipts(Map<String, Boolean> readReceipts) {
    this.readReceipts = readReceipts != null ? readReceipts : new HashMap<>();
  }

  public Poll getPoll() {
    return poll;
  }

  public void setPoll(Poll poll) {
    this.poll = poll;
  }

  // Poll helper methods
  public int getTotalVotes() {
    return poll != null ? poll.getTotalVotes() : 0;
  }

  public String getPollId() {
    return messageId;
  }

  // Validation methods
  @Exclude
  public boolean isValid() {
    if (type == null) return false;
    switch (type) {
      case TYPE_TEXT:
        return isValidContent(content) && mediaUrl == null;
      case TYPE_IMAGE:
      case TYPE_GIF:
      case TYPE_VIDEO:
        return isValidMediaUrl(mediaUrl) && content == null;
      case TYPE_POLL:
        return poll != null && poll.getQuestion() != null;
      case TYPE_FILE:
        return fileName != null && fileSize > 0;
      default:
        return false;
    }
  }

  @Exclude
  public static boolean isValidContent(String content) {
    return content != null && !content.trim().isEmpty() && content.length() <= MAX_CONTENT_LENGTH;
  }

  @Exclude
  public static boolean isValidMediaUrl(String url) {
    return url != null && (url.startsWith("http://") || url.startsWith("https://"));
  }

  // Status tracking
  public void markAsRead(String userId) {
    if (userId != null && !userId.isEmpty()) {
      readReceipts.put(userId, true);
    }
  }

  @Exclude
  public boolean isRead(String userId) {
    return readReceipts.containsKey(userId) && Boolean.TRUE.equals(readReceipts.get(userId));
  }

  // Parcelable creator
  public static final Creator<ChatMessage> CREATOR =
      new Creator<ChatMessage>() {
        @Override
        public ChatMessage createFromParcel(Parcel in) {
          return new ChatMessage(in);
        }

        @Override
        public ChatMessage[] newArray(int size) {
          return new ChatMessage[size];
        }
      };

  @Override
  public int describeContents() {
    return 0;
  }

  // Helper methods
  public static boolean isValidType(String type) {
    return type.equals(TYPE_TEXT)
        || type.equals(TYPE_IMAGE)
        || type.equals(TYPE_GIF)
        || type.equals(TYPE_VIDEO)
        || type.equals(TYPE_POLL)
        || type.equals(TYPE_FILE);
  }

  // Firestore serialization
  @Exclude
  public Map<String, Object> toFirestoreMap() {
    Map<String, Object> map = new HashMap<>();
    map.put("messageId", messageId);
    map.put("senderId", senderId);
    map.put("senderName", senderName);
    map.put("senderAvatar", senderAvatar);
    map.put("content", content);
    map.put("type", type);
    map.put("mediaUrl", mediaUrl);
    map.put("thumbnailUrl", thumbnailUrl);
    map.put("mediaSize", mediaSize);
    map.put("videoDuration", videoDuration);
    map.put("fileName", fileName);
    map.put("fileUrl", fileUrl);
    map.put("fileSize", fileSize);
    map.put("fileType", fileType);
    map.put("replyToId", replyToId);
    map.put("replyPreview", replyPreview);
    map.put("deleted", deleted);
    map.put("edited", edited);
    map.put("uploading", uploading);
    map.put("timestamp", timestamp);
    map.put("lastUpdated", lastUpdated);
    map.put("readReceipts", readReceipts);
    map.put("poll", poll);
    return map;
  }
}
