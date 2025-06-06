// app/src/main/java/com/spidroid/starry/viewmodels/ProfilePostState.kt
package com.spidroid.starry.viewmodels

import com.spidroid.starry.models.PostModel

// Sealed class لتمثيل حالات تحميل منشورات الملف الشخصي
sealed class ProfilePostState {
    object Loading : ProfilePostState()
    data class Success(val posts: List<PostModel>) : ProfilePostState()
    data class Error(val message: String) : ProfilePostState()
    object Empty : ProfilePostState() // حالة عدم وجود منشورات
}