package com.spidroid.starry.repositories

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.spidroid.starry.models.ChatMessage
import java.util.*

class ChatRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Function to get a real-time listener for messages in a chat
    fun getMessagesListener(chatId: String): Query {
        return db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    // Function to get a real-time listener for chat details
    fun getChatDetailsListener(chatId: String): DocumentReference {
        return db.collection("chats").document(chatId)
    }

    // Function to send a message (save to Firestore)
    fun sendMessage(chatId: String, message: ChatMessage): Task<DocumentReference> {
        return db.collection("chats").document(chatId).collection("messages").add(message)
    }

    // Function to update the last message in the chat document
    fun updateLastMessage(chatId: String, message: ChatMessage): Task<Void> {
        val lastMessageData = mapOf(
            "lastMessage" to (message.content ?: message.type.replaceFirstChar { it.titlecase() }),
            "lastMessageType" to message.type,
            "lastMessageTime" to FieldValue.serverTimestamp(),
            "lastMessageSender" to message.senderId
        )
        return db.collection("chats").document(chatId).update(lastMessageData)
    }

    // Function to upload media to Firebase Storage
    fun uploadMedia(chatId: String, fileUri: Uri): StorageReference {
        val fileName = "chat_media/$chatId/${UUID.randomUUID()}"
        return storage.reference.child(fileName)
    }

    // Function to get user data
    fun getUserData(userId: String): DocumentReference {
        return db.collection("users").document(userId)
    }

    // Function to update the typing status of a user
    fun updateUserTypingStatus(chatId: String, userId: String, isTyping: Boolean): Task<Void> {
        val typingUpdate = if (isTyping) {
            // Store current server timestamp when user starts typing
            mapOf("typingStatus.$userId" to System.currentTimeMillis())
        } else {
            // Remove user from typing status map when they stop
            mapOf("typingStatus.$userId" to FieldValue.delete())
        }
        return db.collection("chats").document(chatId).update(typingUpdate)
    }

    // Function to edit a message
    fun editMessage(chatId: String, messageId: String, newContent: String): Task<Void> {
        val messageRef = db.collection("chats").document(chatId)
            .collection("messages").document(messageId)

        val updates = mapOf(
            "content" to newContent,
            "edited" to true,
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        return messageRef.update(updates)
    }

    // Function to "soft delete" a message
    fun deleteMessage(chatId: String, messageId: String): Task<Void> {
        val messageRef = db.collection("chats").document(chatId)
            .collection("messages").document(messageId)

        val updates = mapOf(
            "content" to "This message was deleted.",
            "deleted" to true,
            "mediaUrl" to FieldValue.delete(), // حذف رابط الوسائط
            "thumbnailUrl" to FieldValue.delete(),
            "type" to ChatMessage.TYPE_TEXT // تغيير النوع إلى نص
        )
        return messageRef.update(updates)
    }
}