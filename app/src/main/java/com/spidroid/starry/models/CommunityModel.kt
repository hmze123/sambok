// In: app/src/main/java/com/spidroid/starry/models/CommunityModel.kt
data class CommunityModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "", // UID لمنشئ المجتمع
    val members: List<String> = emptyList(), // قائمة بـ UIDs للأعضاء
    val admins: List<String> = emptyList(), // قائمة بـ UIDs للمشرفين
    val profilePictureUrl: String? = null,
    val coverPhotoUrl: String? = null,
    val isPublic: Boolean = true, // true = مجتمع عام، false = مجتمع خاص
    val createdAt: Long = System.currentTimeMillis()
)