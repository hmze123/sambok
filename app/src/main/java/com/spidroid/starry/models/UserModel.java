package com.spidroid.starry.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@IgnoreExtraProperties
public class UserModel implements Parcelable {
  // Social platform constants
  public static final String SOCIAL_TWITTER = "twitter";
  public static final String SOCIAL_INSTAGRAM = "instagram";
  public static final String SOCIAL_FACEBOOK = "facebook";
  public static final String SOCIAL_LINKEDIN = "linkedin";
  private static final Pattern URL_PATTERN =
      Pattern.compile("^(https?://)?([\\w-]+\\.)+[\\w-]+(/\\S*)?$");

  // Core Fields
  private String userId;
  private String username;
  private String displayName;
  private String email;
  private String phoneNumber;
  private String profileImageUrl;
  private String coverImageUrl;
  private String bio;

  // Timestamps
  @ServerTimestamp private Date createdAt;
  @ServerTimestamp private Date lastLogin;
  private Date lastPostDate;

  // Security & Authentication
  private String lastLoginIp;
  private boolean isVerified;
  private boolean is2FAEnabled;
  private List<String> backupCodes = new ArrayList<>();
  private List<LoginDevice> trustedDevices = new ArrayList<>();
  private Date passwordChangedAt;

  // Social features
  private Map<String, Boolean> followers = new HashMap<>();
  private Map<String, Boolean> following = new HashMap<>();
  private Map<String, String> socialLinks = new HashMap<>();
  private int postsCount;

  // App specific
  private String fcmToken;
  private AccountStatus accountStatus = AccountStatus.ACTIVE;
  private PrivacySettings privacySettings = new PrivacySettings();
  private Map<String, Boolean> notificationPreferences = new HashMap<>();

  // Account Status Enum
  public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    DELETED,
    RESTRICTED
  }

  // Constructors
  public UserModel() {}

  public UserModel(@NonNull String userId, @NonNull String username, @NonNull String email) {
    if (userId.isEmpty() || username.isEmpty() || email.isEmpty()) {
      throw new IllegalArgumentException("Required fields cannot be empty");
    }
    this.userId = userId;
    this.username = username;
    this.email = email;
    this.createdAt = null;
    this.lastLogin = null;
    this.followers = new HashMap<>();
    this.following = new HashMap<>();
    this.socialLinks = new HashMap<>();
    this.accountStatus = AccountStatus.ACTIVE;
    this.privacySettings = new PrivacySettings();
    this.notificationPreferences = new HashMap<>();
  }

  // Parcelable implementation
  protected UserModel(Parcel in) {
    userId = in.readString();
    username = in.readString();
    displayName = in.readString();
    email = in.readString();
    phoneNumber = in.readString();
    profileImageUrl = in.readString();
    coverImageUrl = in.readString();
    bio = in.readString();
    createdAt = readNullableDate(in);
    lastLogin = readNullableDate(in);
    lastPostDate = readNullableDate(in);
    lastLoginIp = in.readString();
    isVerified = in.readByte() != 0;
    is2FAEnabled = in.readByte() != 0;
    backupCodes = in.createStringArrayList();
    trustedDevices = in.createTypedArrayList(LoginDevice.CREATOR);
    followers = (Map<String, Boolean>) in.readHashMap(Boolean.class.getClassLoader());
    following = (Map<String, Boolean>) in.readHashMap(Boolean.class.getClassLoader());
    postsCount = in.readInt();
    socialLinks = in.readHashMap(String.class.getClassLoader());
    fcmToken = in.readString();
    accountStatus = AccountStatus.valueOf(in.readString());
    privacySettings = in.readParcelable(PrivacySettings.class.getClassLoader());
    passwordChangedAt = readNullableDate(in);
    notificationPreferences = in.readHashMap(Boolean.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(userId);
    dest.writeString(username);
    dest.writeString(displayName);
    dest.writeString(email);
    dest.writeString(phoneNumber);
    dest.writeString(profileImageUrl);
    dest.writeString(coverImageUrl);
    dest.writeString(bio);
    writeNullableDate(dest, createdAt);
    writeNullableDate(dest, lastLogin);
    writeNullableDate(dest, lastPostDate);
    dest.writeString(lastLoginIp);
    dest.writeByte((byte) (isVerified ? 1 : 0));
    dest.writeByte((byte) (is2FAEnabled ? 1 : 0));
    dest.writeStringList(backupCodes);
    dest.writeTypedList(trustedDevices);
    dest.writeMap(followers);
    dest.writeMap(following);
    dest.writeInt(postsCount);
    dest.writeMap(socialLinks);
    dest.writeString(fcmToken);
    dest.writeString(accountStatus.name());
    dest.writeParcelable(privacySettings, flags);
    writeNullableDate(dest, passwordChangedAt);
    dest.writeMap(notificationPreferences);
  }

  // Date handling helpers
  private Date readNullableDate(Parcel in) {
    long timestamp = in.readLong();
    return timestamp != -1 ? new Date(timestamp) : null;
  }

  private void writeNullableDate(Parcel dest, Date date) {
    dest.writeLong(date != null ? date.getTime() : -1);
  }

  // Getters and setters with validation
  public String getUserId() {
    return userId;
  }

  public void setUserId(@NonNull String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("User ID cannot be null or empty");
    }
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(@NonNull String username) {
    if (username == null || username.trim().isEmpty()) {
      throw new IllegalArgumentException("Username cannot be null or empty");
    }
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(@NonNull String email) {
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("Email cannot be null or empty");
    }
    this.email = email;
  }

  public Map<String, Boolean> getFollowers() {
    return Collections.unmodifiableMap(followers);
  }

  public void setFollowers(@Nullable Map<String, Boolean> followers) {
    this.followers = followers != null ? new HashMap<>(followers) : new HashMap<>();
  }

  public Map<String, String> getSocialLinks() {
    return Collections.unmodifiableMap(socialLinks);
  }

  public void setSocialLinks(@Nullable Map<String, String> socialLinks) {
    this.socialLinks = socialLinks != null ? new HashMap<>(socialLinks) : new HashMap<>();
  }

  // Helper methods
  public void addFollower(String userId) {
    if (userId != null && !followers.containsKey(userId)) {
      followers.put(userId, true);
    }
  }

  public void addSocialLink(String platform, String url) {
    if (isValidPlatform(platform) && isValidUrl(url)) {
      socialLinks.put(platform.toLowerCase(), url);
    }
  }

  private boolean isValidPlatform(String platform) {
    return platform.equalsIgnoreCase(SOCIAL_TWITTER)
        || platform.equalsIgnoreCase(SOCIAL_INSTAGRAM)
        || platform.equalsIgnoreCase(SOCIAL_FACEBOOK)
        || platform.equalsIgnoreCase(SOCIAL_LINKEDIN);
  }

  private boolean isValidUrl(String url) {
    return url != null && URL_PATTERN.matcher(url).matches();
  }

  public void updatePassword() {
    this.passwordChangedAt = new Date();
  }

  public boolean isPasswordExpired(long maxAgeDays) {
    if (passwordChangedAt == null) return true;
    long diff = System.currentTimeMillis() - passwordChangedAt.getTime();
    return (diff / (1000 * 60 * 60 * 24)) > maxAgeDays;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getProfileImageUrl() {
    return profileImageUrl;
  }

  public String getCoverImageUrl() {
    return coverImageUrl;
  }

  public void setProfileImageUrl(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }

  public void setCoverImageUrl(String coverImageUrl) {
    this.coverImageUrl = coverImageUrl;
  }

  public String getBio() {
    return bio;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public Date getLastLogin() {
    return lastLogin;
  }

  public Date getLastPostDate() {
    return lastPostDate;
  }

  public String getLastLoginIp() {
    return lastLoginIp;
  }

  public boolean isVerified() {
    return isVerified;
  }

  public boolean isIs2FAEnabled() {
    return is2FAEnabled;
  } // Firebase expects "is" prefix for boolean

  public List<String> getBackupCodes() {
    return backupCodes;
  }

  public List<LoginDevice> getTrustedDevices() {
    return trustedDevices;
  }

  public Date getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public Map<String, Boolean> getFollowing() {
    return Collections.unmodifiableMap(following);
  }

  public void setFollowing(@Nullable Map<String, Boolean> following) {
    this.following = following != null ? new HashMap<>(following) : new HashMap<>();
  }

  public int getPostsCount() {
    return postsCount;
  }

  public String getFcmToken() {
    return fcmToken;
  }

  public AccountStatus getAccountStatus() {
    return accountStatus;
  }

  public PrivacySettings getPrivacySettings() {
    return privacySettings;
  }

  public Map<String, Boolean> getNotificationPreferences() {
    return notificationPreferences;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public void setPrivacySettings(PrivacySettings privacySettings) {
    this.privacySettings = privacySettings;
  }

  // For 2FA
  public void set2FAEnabled(boolean is2FAEnabled) {
    this.is2FAEnabled = is2FAEnabled;
  }

  // For notifications
  public void setNotificationPreferences(Map<String, Boolean> notificationPreferences) {
    this.notificationPreferences = notificationPreferences;
  }

  // For trusted devices
  public void setTrustedDevices(List<LoginDevice> trustedDevices) {
    this.trustedDevices = trustedDevices;
  }

  // PrivacySettings inner class
  @IgnoreExtraProperties
  public static class PrivacySettings implements Parcelable {
    private boolean privateAccount = false;
    private boolean showActivityStatus = true;
    private boolean allowDMsFromEveryone = true;
    private boolean showLastSeen = true;
    private boolean allowTagging = true;

    public PrivacySettings() {}

    // Parcelable implementation
    protected PrivacySettings(Parcel in) {
      privateAccount = in.readByte() != 0;
      showActivityStatus = in.readByte() != 0;
      allowDMsFromEveryone = in.readByte() != 0;
      showLastSeen = in.readByte() != 0;
      allowTagging = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeByte((byte) (privateAccount ? 1 : 0));
      dest.writeByte((byte) (showActivityStatus ? 1 : 0));
      dest.writeByte((byte) (allowDMsFromEveryone ? 1 : 0));
      dest.writeByte((byte) (showLastSeen ? 1 : 0));
      dest.writeByte((byte) (allowTagging ? 1 : 0));
    }

    // Getters and setters
    public boolean isPrivateAccount() {
      return privateAccount;
    }

    public void setPrivateAccount(boolean privateAccount) {
      this.privateAccount = privateAccount;
    }

    public void setShowActivityStatus(boolean showActivityStatus) {
      this.showActivityStatus = showActivityStatus;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    public boolean isShowActivityStatus() {
      return showActivityStatus;
    }

    public boolean isAllowDMsFromEveryone() {
      return allowDMsFromEveryone;
    }

    public boolean isShowLastSeen() {
      return showLastSeen;
    }

    public boolean isAllowTagging() {
      return allowTagging;
    }

    public static final Creator<PrivacySettings> CREATOR =
        new Creator<PrivacySettings>() {
          @Override
          public PrivacySettings createFromParcel(Parcel in) {
            return new PrivacySettings(in);
          }

          @Override
          public PrivacySettings[] newArray(int size) {
            return new PrivacySettings[size];
          }
        };
  }

  // LoginDevice inner class with enhanced security
  @IgnoreExtraProperties
  public static class LoginDevice implements Parcelable {
    private String deviceId;
    private String deviceName;
    private String deviceModel;
    private String osVersion;
    private long lastUsed;
    private String location;
    private boolean isCurrentDevice;

    public LoginDevice() {}

    // Add getters and setters
    public String getDeviceId() {
      return deviceId;
    }

    public String getDeviceName() {
      return deviceName;
    }

    public String getDeviceModel() {
      return deviceModel;
    }

    public String getOsVersion() {
      return osVersion;
    }

    public long getLastUsed() {
      return lastUsed;
    }

    public String getLocation() {
      return location;
    }

    public boolean isCurrentDevice() {
      return isCurrentDevice;
    }

    // Parcelable implementation
    protected LoginDevice(Parcel in) {
      deviceId = in.readString();
      deviceName = in.readString();
      deviceModel = in.readString();
      osVersion = in.readString();
      lastUsed = in.readLong();
      location = in.readString();
      isCurrentDevice = in.readByte() != 0;
    }

    public static final Creator<LoginDevice> CREATOR =
        new Creator<LoginDevice>() {
          @Override
          public LoginDevice createFromParcel(Parcel in) {
            return new LoginDevice(in);
          }

          @Override
          public LoginDevice[] newArray(int size) {
            return new LoginDevice[size];
          }
        };

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(deviceId);
      dest.writeString(deviceName);
      dest.writeString(deviceModel);
      dest.writeString(osVersion);
      dest.writeLong(lastUsed);
      dest.writeString(location);
      dest.writeByte((byte) (isCurrentDevice ? 1 : 0));
    }
  }

  // Parcelable creator
  public static final Creator<UserModel> CREATOR =
      new Creator<UserModel>() {
        @Override
        public UserModel createFromParcel(Parcel in) {
          return new UserModel(in);
        }

        @Override
        public UserModel[] newArray(int size) {
          return new UserModel[size];
        }
      };

  @Override
  public int describeContents() {
    return 0;
  }
}
