package com.spidroid.starry.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.ChatMessage
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.ChatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {

    val repository = ChatRepository()
    private val auth = Firebase.auth

    // LiveData للمستخدم الحالي
    private val _currentUserModel = MutableLiveData<UserModel?>()
    val currentUserModel: LiveData<UserModel?> = _currentUserModel

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _chatPartner = MutableLiveData<UserModel?>()
    val chatPartner: LiveData<UserModel?> = _chatPartner

    private val _groupDetails = MutableLiveData<Chat?>()
    val groupDetails: LiveData<Chat?> = _groupDetails

    private val _uiState = MutableLiveData<UiState<Nothing>>()
    val uiState: LiveData<UiState<Nothing>> = _uiState

    private val _typingPartner = MutableLiveData<UserModel?>()
    val typingPartner: LiveData<UserModel?> = _typingPartner

    private var messagesListener: ListenerRegistration? = null
    private var chatDetailsListener: ListenerRegistration? = null
    private var partnerUserListener: ListenerRegistration? = null

    // دالة جديدة لجلب بيانات المستخدم الحالي
    fun loadCurrentUserData() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userDoc = repository.getUserData(userId).get().await()
                _currentUserModel.value = userDoc.toObject(UserModel::class.java)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load current user data", e)
                _currentUserModel.value = null
            }
        }
    }

    fun listenForMessages(chatId: String) {
        messagesListener?.remove()
        messagesListener = repository.getMessagesListener(chatId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("ChatViewModel", "Messages listen failed.", error)
                    _uiState.value = UiState.Error("Failed to load messages.")
                    return@addSnapshotListener
                }
                val messageList = snapshots?.documents?.mapNotNull {
                    it.toObject(ChatMessage::class.java)?.apply { messageId = it.id }
                } ?: emptyList()
                _messages.value = messageList
            }
    }

    fun loadChatDetails(chatId: String, isGroup: Boolean) {
        chatDetailsListener?.remove()
        chatDetailsListener = repository.getChatDetailsListener(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _uiState.value = UiState.Error("Failed to load chat details.")
                    return@addSnapshotListener
                }
                val chat = snapshot.toObject(Chat::class.java)
                if (isGroup) {
                    _groupDetails.value = chat
                } else {
                    val otherUserId = chat?.participants?.firstOrNull { it != auth.currentUser?.uid }
                    otherUserId?.let { partnerId ->
                        listenForPartnerData(partnerId)
                        updateTypingIndicator(chat.typingStatus, partnerId)
                    }
                }
            }
    }

    private fun listenForPartnerData(userId: String) {
        partnerUserListener?.remove()
        partnerUserListener = repository.getUserData(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _uiState.value = UiState.Error("Failed to load partner data.")
                    return@addSnapshotListener
                }
                _chatPartner.value = snapshot.toObject(UserModel::class.java)
            }
    }

    private fun updateTypingIndicator(typingStatus: Map<String, Long>, partnerId: String) {
        val partnerTypingTimestamp = typingStatus[partnerId]
        if (partnerTypingTimestamp != null && System.currentTimeMillis() - partnerTypingTimestamp < 5000) {
            _typingPartner.value = _chatPartner.value
        } else {
            _typingPartner.value = null
        }
    }

    fun updateUserTypingStatus(chatId: String, isTyping: Boolean) {
        auth.currentUser?.uid?.let { userId ->
            repository.updateUserTypingStatus(chatId, userId, isTyping)
                .addOnFailureListener { e ->
                    Log.w("ChatViewModel", "Failed to update typing status", e)
                }
        }
    }

    fun sendMessage(chatId: String, message: ChatMessage, mediaUri: Uri?) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                if (mediaUri != null) {
                    val mediaUrl = uploadMedia(chatId, mediaUri)
                    message.mediaUrl = mediaUrl
                    message.thumbnailUrl = mediaUrl
                    message.type = if (message.type == ChatMessage.TYPE_VIDEO) ChatMessage.TYPE_VIDEO else ChatMessage.TYPE_IMAGE
                }

                val docRef = repository.sendMessage(chatId, message).await()
                docRef.update("messageId", docRef.id).await()
                repository.updateLastMessage(chatId, message).await()

                _uiState.value = UiState.Success

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                _uiState.value = UiState.Error("Failed to send message: ${e.message}")
            }
        }
    }

    private suspend fun uploadMedia(chatId: String, uri: Uri): String {
        val ref = repository.uploadMedia(chatId, uri)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun editMessage(chatId: String, messageId: String, newContent: String) {
        viewModelScope.launch {
            try {
                repository.editMessage(chatId, messageId, newContent).await()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to edit message", e)
                _uiState.value = UiState.Error("Failed to edit message: ${e.message}")
            }
        }
    }

    fun castVote(chatId: String, messageId: String, optionIndex: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                repository.recordVote(chatId, messageId, optionIndex, userId).await()
                // الواجهة ستتحدث تلقائيًا بفضل المستمع الفوري
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to cast vote", e)
                _uiState.value = UiState.Error("Failed to vote: ${e.message}")
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(chatId, messageId).await()
                // The real-time listener will handle the UI update.
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to delete message", e)
                _uiState.value = UiState.Error("Failed to delete message: ${e.message}")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        chatDetailsListener?.remove()
        partnerUserListener?.remove()
    }
}