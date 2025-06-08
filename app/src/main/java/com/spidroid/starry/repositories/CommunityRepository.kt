// In: app/src/main/java/com/spidroid/starry/repositories/CommunityRepository.kt
package com.spidroid.starry.repositories

import CommunityModel
import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.models.PostModel // <-- إصلاح: إضافة import
import kotlinx.coroutines.tasks.await
import kotlin.Result

class CommunityRepository {
    private val communityCollection = Firebase.firestore.collection("communities")
    private val postCollection = Firebase.firestore.collection("posts")

    // دالة لإنشاء مجتمع جديد
    suspend fun createCommunity(name: String, description: String, isPublic: Boolean, ownerId: String): Result<String> {
        return try {
            val id = communityCollection.document().id
            val community = CommunityModel(
                id = id,
                name = name,
                description = description,
                isPublic = isPublic,
                ownerId = ownerId,
                admins = listOf(ownerId),
                members = listOf(ownerId)
            )
            communityCollection.document(id).set(community).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // دالة للانضمام لمجتمع
    suspend fun joinCommunity(communityId: String, userId: String): Result<Unit> {
        return try {
            communityCollection.document(communityId).update("members", FieldValue.arrayUnion(userId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // دالة لجلب منشورات المجتمع
    suspend fun getCommunityPosts(communityId: String): List<PostModel> {
        val querySnapshot = postCollection.whereEqualTo("communityId", communityId)
            .orderBy("timestamp", Query.Direction.DESCENDING) // <-- إصلاح: إضافة import لـ Query
            .get().await()
        return querySnapshot.toObjects(PostModel::class.java) // <-- إصلاح: استخدام .class.java
    }

    // يمكنك إضافة دوال أخرى مثل getCommunityDetails, leaveCommunity etc.
}