package com.spidroid.starry.repositories

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.spidroid.starry.models.ChatMessage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val chatsCollection = firestore.collection("chats")
    private val usersCollection = firestore.collection("users")

    fun getUserData(userId: String): DocumentReference {
        return usersCollection.document(userId)
    }

    fun getMessagesListener(chatId: String): Query {
        return chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    fun getChatDetailsListener(chatId: String): DocumentReference {
        return chatsCollection.document(chatId)
    }

    suspend fun sendMessage(chatId: String, message: ChatMessage): DocumentReference {
        // Ensure timestamp and senderId are set
        val finalMessage = message.copy(
            timestamp = Date()
        )
        return chatsCollection.document(chatId).collection("messages").add(finalMessage).await()
    }

    fun updateLastMessage(chatId: String, message: ChatMessage): Task<Void> {
        val updates = mapOf(
            "lastMessage" to message.content,
            "lastMessageTimestamp" to FieldValue.serverTimestamp()
        )
        return chatsCollection.document(chatId).update(updates)
    }

    fun uploadMedia(chatId: String, uri: Uri): StorageReference {
        val filename = UUID.randomUUID().toString()
        return storage.reference.child("chat_media/$chatId/$filename")
    }

    fun editMessage(chatId: String, messageId: String, newContent: String): Task<Void> {
        return chatsCollection.document(chatId).collection("messages").document(messageId)
            .update("content", newContent)
    }

    fun deleteMessage(chatId: String, messageId: String): Task<Void> {
        return chatsCollection.document(chatId).collection("messages").document(messageId)
            .delete()
    }

    fun recordVote(chatId: String, messageId: String, optionIndex: Int, userId: String): Task<Void> {
        val voteField = "pollOptions.$optionIndex.votes"
        return firestore.runTransaction { transaction ->
            val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
            transaction.update(messageRef, voteField, FieldValue.arrayUnion(userId))
            null
        }
    }

    fun updateUserTypingStatus(chatId: String, userId: String, isTyping: Boolean): Task<Void> {
        val typingStatusUpdate = if (isTyping) {
            mapOf("typingStatus.$userId" to System.currentTimeMillis())
        } else {
            mapOf("typingStatus.$userId" to FieldValue.delete())
        }
        return chatsCollection.document(chatId).update(typingStatusUpdate)
    }
}