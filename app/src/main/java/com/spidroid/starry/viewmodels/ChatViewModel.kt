package com.spidroid.starry.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.ChatMessage
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.repositories.ChatRepository
import com.spidroid.starry.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _chatPartner = MutableLiveData<UserModel?>()
    val chatPartner: LiveData<UserModel?> = _chatPartner

    private val _groupDetails = MutableLiveData<Chat?>()
    val groupDetails: LiveData<Chat?> = _groupDetails

    private val _currentUserModel = MutableLiveData<UserModel?>()
    val currentUserModel: LiveData<UserModel?> = _currentUserModel

    private val _uiState = MutableLiveData<UiState<Nothing>>()
    val uiState: LiveData<UiState<Nothing>> = _uiState

    private val _typingPartner = MutableLiveData<UserModel?>()
    val typingPartner: LiveData<UserModel?> = _typingPartner

    private var messagesListener: ListenerRegistration? = null
    private var chatDetailsListener: ListenerRegistration? = null
    private var partnerUserListener: ListenerRegistration? = null

    fun loadCurrentUserData() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // استخدام UserRepository لجلب المستخدم
                _currentUserModel.value = userRepository.getUser(userId)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load current user data", e)
                _currentUserModel.value = null
            }
        }
    }

    fun listenForMessages(chatId: String) {
        messagesListener?.remove()
        messagesListener = chatRepository.getMessagesListener(chatId).addSnapshotListener { snapshots, error ->
            if (error != null) {
                _uiState.value = UiState.Error("Failed to load messages.")
                return@addSnapshotListener
            }
            val messageList = snapshots?.documents?.mapNotNull { doc ->
                doc.toObject<ChatMessage>()?.apply { messageId = doc.id }
            } ?: emptyList()
            _messages.value = messageList
        }
    }

    fun loadChatDetails(chatId: String, isGroup: Boolean) {
        chatDetailsListener?.remove()
        chatDetailsListener = chatRepository.getChatDetailsListener(chatId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                _uiState.value = UiState.Error("Failed to load chat details.")
                return@addSnapshotListener
            }
            val chat = snapshot.toObject<Chat>()
            if (isGroup) {
                _groupDetails.value = chat
            } else {
                val otherUserId = chat?.participants?.firstOrNull { it != auth.currentUser?.uid }
                otherUserId?.let { partnerId ->
                    listenForPartnerData(partnerId)
                    chat.typingStatus?.let { updateTypingIndicator(it, partnerId) }
                }
            }
        }
    }

    private fun listenForPartnerData(userId: String) {
        partnerUserListener?.remove()
        // ملاحظة: ChatRepository لا يحتوي على getUserData، استخدمنا UserRepository بدلاً من ذلك
        // هذا الجزء قد يحتاج لمراجعة إذا كان الهدف مختلف
        partnerUserListener = chatRepository.getUserData(userId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                _uiState.value = UiState.Error("Failed to load partner data.")
                return@addSnapshotListener
            }
            _chatPartner.value = snapshot.toObject<UserModel>()
        }
    }

    fun sendMessage(chatId: String, message: ChatMessage, mediaUri: Uri?) {
        _uiState.value = UiState.Loading
        val senderId = auth.currentUser?.uid ?: return
        val messageToSend = message.copy(senderId = senderId)

        viewModelScope.launch {
            try {
                if (mediaUri != null) {
                    val mediaUrl = uploadMedia(chatId, mediaUri)
                    messageToSend.mediaUrl = mediaUrl
                }
                val docRef = chatRepository.sendMessage(chatId, messageToSend)
                docRef.update("messageId", docRef.id).await()
                chatRepository.updateLastMessage(chatId, messageToSend).await()
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to send message: ${e.message}")
            }
        }
    }

    private suspend fun uploadMedia(chatId: String, uri: Uri): String {
        val ref = chatRepository.uploadMedia(chatId, uri)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun editMessage(chatId: String, messageId: String, newContent: String) {
        viewModelScope.launch {
            try {
                chatRepository.editMessage(chatId, messageId, newContent).await()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to edit message: ${e.message}")
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(chatId, messageId).await()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete message: ${e.message}")
            }
        }
    }

    fun castVote(chatId: String, messageId: String, optionIndex: Int) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                chatRepository.recordVote(chatId, messageId, optionIndex, userId).await()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to vote: ${e.message}")
            }
        }
    }

    fun updateUserTypingStatus(chatId: String, isTyping: Boolean) {
        auth.currentUser?.uid?.let { userId ->
            chatRepository.updateUserTypingStatus(chatId, userId, isTyping).addOnFailureListener {
                Log.w("ChatViewModel", "Failed to update typing status", it)
            }
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

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
        chatDetailsListener?.remove()
        partnerUserListener?.remove()
    }
}