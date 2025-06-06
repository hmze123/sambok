package com.spidroid.starry.models

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.regex.Pattern

@IgnoreExtraProperties
@Parcelize
data class UserModel(
    // Core Fields
    var userId: String = "",
    var username: String = "",
    var displayName: String? = null,
    var email: String = "",
    var phoneNumber: String? = null,
    var profileImageUrl: String? = null,
    var coverImageUrl: String? = null,
    var bio: String? = null,

    // Timestamps
    @ServerTimestamp var createdAt: Date? = null,
    @ServerTimestamp var lastLogin: Date? = null,
    var lastPostDate: Date? = null,

    // Security & Authentication
    var lastLoginIp: String? = null,
    var isVerified: Boolean = false,
    var is2FAEnabled: Boolean = false,
    var backupCodes: MutableList<String> = mutableListOf(),
    var trustedDevices: MutableList<LoginDevice> = mutableListOf(),
    var passwordChangedAt: Date? = null,

    // Social features
    var followers: MutableMap<String, Boolean> = mutableMapOf(),
    var following: MutableMap<String, Boolean> = mutableMapOf(),
    var socialLinks: MutableMap<String, String> = mutableMapOf(),
    var postsCount: Int = 0,

    // App specific
    var fcmToken: String? = null,
    var accountStatus: AccountStatus = AccountStatus.ACTIVE,
    var privacySettings: PrivacySettings = PrivacySettings(),
    var notificationPreferences: MutableMap<String, Boolean> = mutableMapOf()
) : Parcelable {

    // ✨ تم إزالة المُنشئ الثانوي لتجنب مشكلة "cycle in delegation"
    // يجب أن يتم بناء كائن UserModel باستخدام المُنشئ الأساسي مباشرة
    // (مثال: UserModel(userId = "...", username = "...", email = "..."))


    // Enum for AccountStatus
    enum class AccountStatus {
        ACTIVE,
        SUSPENDED,
        DELETED,
        RESTRICTED
    }

    @IgnoreExtraProperties
    @Parcelize
    data class PrivacySettings(
        var privateAccount: Boolean = false,
        var showActivityStatus: Boolean = true,
        var allowDMsFromEveryone: Boolean = true,
        var showLastSeen: Boolean = true,
        var allowTagging: Boolean = true
    ) : Parcelable

    @IgnoreExtraProperties
    @Parcelize
    data class LoginDevice(
        var deviceId: String? = null,
        var deviceName: String? = null,
        var deviceModel: String? = null,
        var osVersion: String? = null,
        var lastUsed: Long = 0L, // Using Long for timestamp
        var location: String? = null,
        var isCurrentDevice: Boolean = false
    ) : Parcelable

    companion object {
        // Social platform constants
        const val SOCIAL_TWITTER = "twitter"
        const val SOCIAL_INSTAGRAM = "instagram"
        const val SOCIAL_FACEBOOK = "facebook"
        const val SOCIAL_LINKEDIN = "linkedin"

        // URL_PATTERN can be a top-level constant or in companion object
        private val URL_PATTERN: Pattern =
            Pattern.compile("^(https?://)?([\\w-]+\\.)+[\\w-]+(/\\S*)?$")
    }


    // Custom setters with validation (if needed, otherwise direct property access is fine)
    fun updateUserId(newUserId: String) {
        if (newUserId.trim().isEmpty()) {
            throw IllegalArgumentException("User ID cannot be null or empty")
        }
        this.userId = newUserId
    }

    fun updateUsername(newUsername: String) {
        if (newUsername.trim().isEmpty()) {
            throw IllegalArgumentException("Username cannot be null or empty")
        }
        this.username = newUsername
    }

    fun updateEmail(newEmail: String) {
        if (newEmail.trim().isEmpty()) {
            throw IllegalArgumentException("Email cannot be null or empty")
        }
        this.email = newEmail
    }

    // Methods
    fun addFollower(userId: String) {
        if (!followers.containsKey(userId)) {
            followers[userId] = true
        }
    }

    fun addSocialLink(platform: String, url: String) {
        if (isValidPlatform(platform) && isValidUrl(url)) {
            socialLinks[platform.lowercase()] = url
        }
    }

    private fun isValidPlatform(platform: String?): Boolean {
        return platform != null && listOf(
            SOCIAL_TWITTER,
            SOCIAL_INSTAGRAM,
            SOCIAL_FACEBOOK,
            SOCIAL_LINKEDIN
        ).any { it.equals(platform, ignoreCase = true) }
    }

    private fun isValidUrl(url: String?): Boolean {
        return url != null && URL_PATTERN.matcher(url).matches()
    }

    fun updatePasswordTimestamp() {
        this.passwordChangedAt = Date()
    }

    fun isPasswordExpired(maxAgeDays: Long): Boolean {
        val changedAt = passwordChangedAt ?: return true // If never changed, consider expired
        val diff = System.currentTimeMillis() - changedAt.time
        return (diff / (1000 * 60 * 60 * 24)) > maxAgeDays
    }

    // Getters for unmodifiable maps (if strict immutability is needed for external access)
    // Firestore typically works fine with mutable maps for direct updates.
    // If you need to expose immutable versions:
    fun getImmutableFollowers(): Map<String, Boolean> = followers.toMap()
    fun getImmutableFollowing(): Map<String, Boolean> = following.toMap()
    fun getImmutableSocialLinks(): Map<String, String> = socialLinks.toMap()

// Note: Getters and setters for properties like 'userId', 'username', 'email', etc.,
// are automatically generated by Kotlin for 'var' properties.
// If you need custom logic in a getter or setter, you can define it like:
// var someProperty: String = ""
//    get() = field // 'field' is the backing field
//    set(value) {
//        // custom logic
//        field = value
//
}