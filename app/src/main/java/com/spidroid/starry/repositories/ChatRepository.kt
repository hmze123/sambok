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

    fun getMessagesListener(chatId: String): Query {
        return db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    fun getChatDetailsListener(chatId: String): DocumentReference {
        return db.collection("chats").document(chatId)
    }

    fun sendMessage(chatId: String, message: ChatMessage): Task<DocumentReference> {
        return db.collection("chats").document(chatId).collection("messages").add(message)
    }

    fun updateLastMessage(chatId: String, message: ChatMessage): Task<Void> {
        val lastMessageData = mapOf(
            "lastMessage" to (message.content ?: message.type.replaceFirstChar { it.titlecase() }),
            "lastMessageType" to message.type,
            "lastMessageTime" to FieldValue.serverTimestamp(),
            "lastMessageSender" to message.senderId
        )
        return db.collection("chats").document(chatId).update(lastMessageData)
    }

    fun uploadMedia(chatId: String, fileUri: Uri): StorageReference {
        val fileName = "chat_media/$chatId/${UUID.randomUUID()}"
        return storage.reference.child(fileName)
    }

    fun getUserData(userId: String): DocumentReference {
        return db.collection("users").document(userId)
    }

    fun editMessage(chatId: String, messageId: String, newContent: String): Task<Void> {
        val messageRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        val updates = mapOf(
            "content" to newContent,
            "edited" to true,
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        return messageRef.update(updates)
    }

    fun deleteMessage(chatId: String, messageId: String): Task<Void> {
        val messageRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        val updates = mapOf(
            "content" to "This message was deleted.",
            "deleted" to true,
            "mediaUrl" to FieldValue.delete(),
            "thumbnailUrl" to FieldValue.delete(),
            "type" to ChatMessage.TYPE_TEXT
        )
        return messageRef.update(updates)
    }

    fun recordVote(chatId: String, messageId: String, optionIndex: Int, userId: String): Task<Void> {
        val messageRef = db.collection("chats").document(chatId).collection("messages").document(messageId)

        return db.runTransaction { transaction ->
            val snapshot = transaction.get(messageRef)
            val poll = snapshot.toObject(ChatMessage::class.java)?.poll ?: return@runTransaction null

            if (poll.voters.containsKey(userId)) {
                throw FirebaseFirestoreException("User has already voted.", FirebaseFirestoreException.Code.ALREADY_EXISTS)
            }

            if (optionIndex < 0 || optionIndex >= poll.options.size) {
                throw FirebaseFirestoreException("Invalid option index.", FirebaseFirestoreException.Code.INVALID_ARGUMENT)
            }

            val newVoteCount = poll.options[optionIndex].votes + 1
            transaction.update(messageRef, "poll.options.$optionIndex.votes", newVoteCount)
            transaction.update(messageRef, "poll.voters.$userId", optionIndex)

            null
        }
    }

    fun updateUserTypingStatus(chatId: String, userId: String, isTyping: Boolean): Task<Void> {
        val typingUpdate = if (isTyping) {
            mapOf("typingStatus.$userId" to System.currentTimeMillis())
        } else {
            mapOf("typingStatus.$userId" to FieldValue.delete())
        }
        return db.collection("chats").document(chatId).update(typingUpdate)
    }
}
