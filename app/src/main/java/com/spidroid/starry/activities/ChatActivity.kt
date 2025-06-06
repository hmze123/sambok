package com.spidroid.starry.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityChatBinding
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.ChatMessage
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.messages.MessageAdapter
import com.spidroid.starry.ui.messages.MessageClickListener
import java.util.*

class ChatActivity : AppCompatActivity(), MessageClickListener {

    private lateinit var binding: ActivityChatBinding
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val storage: FirebaseStorage by lazy { Firebase.storage }
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    private var currentUserId: String? = null
    private var currentUserModel: UserModel? = null
    private var chatId: String? = null
    private var isGroupChat = false

    private var messagesListener: ListenerRegistration? = null
    private var userDeletedMessagesListener: ListenerRegistration? = null
    private var chatDetailsListener: ListenerRegistration? = null

    private lateinit var messageAdapter: MessageAdapter
    private val userDeletedMessageIds = HashSet<String>()

    private var currentMediaUri: Uri? = null
    private val mediaPicker: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                currentMediaUri = it
                showMediaPreview(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            handleAuthError()
            return
        }

        chatId = intent.getStringExtra("chatId")
        isGroupChat = intent.getBooleanExtra("isGroup", false)
        if (chatId.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_chat_id_error), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupUI()
        loadCurrentUserData()
        listenForDeletedMessages()
    }

    private fun handleAuthError() {
        Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupInputBehavior()
        setupAppBar()
    }

    private fun loadCurrentUserData() {
        currentUserId?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUserModel = document.toObject(UserModel::class.java)
                        updateSendButtonState()
                    }
                }
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUserId!!, this, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = messageAdapter
        }
        listenForMessages()
    }

    private fun listenForMessages() {
        val safeChatId = chatId ?: return
        messagesListener?.remove()
        messagesListener = db.collection("chats").document(safeChatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshots?.documents?.mapNotNull { doc ->
                    if (doc.exists() && !userDeletedMessageIds.contains(doc.id)) {
                        doc.toObject(ChatMessage::class.java)?.apply { messageId = doc.id }
                    } else null
                } ?: emptyList()
                messageAdapter.submitList(messages) { scrollToBottom() }
            }
    }

    private fun listenForDeletedMessages() {
        val userId = currentUserId ?: return
        userDeletedMessagesListener = db.collection("users").document(userId).collection("deleted_messages")
            .addSnapshotListener { snapshots, _ ->
                val needsRefresh = snapshots?.documentChanges?.any { userDeletedMessageIds.add(it.document.id) } == true
                if (needsRefresh) listenForMessages()
            }
    }

    private fun setupInputBehavior() {
        with(binding.inputSection) {
            triggerButton.setOnClickListener { toggleMediaOptions() }
            btnSend.setOnClickListener { sendMessage() }
            btnAddPhoto.setOnClickListener { mediaPicker.launch("image/*") }
            btnAddVideo.setOnClickListener { mediaPicker.launch("video/*") }
            addGif.setOnClickListener { showGifPicker() }
            addPoll.setOnClickListener { createPoll() }

            postInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { updateSendButtonState() }
            })
        }
    }

    private fun updateSendButtonState() {
        val hasText = binding.inputSection.postInput.text.trim().isNotEmpty()
        val hasMedia = currentMediaUri != null
        val canSend = (hasText || hasMedia) && currentUserModel != null

        binding.inputSection.btnSend.isEnabled = canSend
        binding.inputSection.btnSend.isVisible = canSend
        binding.inputSection.btnRecord.isVisible = !canSend
    }

    private fun sendMessage() {
        val messageText = binding.inputSection.postInput.text.toString().trim()
        if (messageText.isEmpty() && currentMediaUri == null) return

        showProgress(true)
        currentMediaUri?.let {
            uploadMediaAndSendMessage(it, messageText)
        } ?: run {
            val message = createTextMessage(messageText)
            saveMessageToFirestore(message)
        }
    }

    private fun createTextMessage(content: String): ChatMessage {
        val user = currentUserModel!!
        val userId = currentUserId!!
        val replyToMessageId = binding.inputSection.replyHeader.getTag(R.id.tag_reply_to_message_id) as? String

        return ChatMessage(senderId = userId, content = content, type = ChatMessage.TYPE_TEXT).apply {
            senderName = user.displayName ?: user.username
            senderAvatar = user.profileImageUrl
            if (replyToMessageId != null) {
                this.replyToId = replyToMessageId
                this.replyPreview = binding.inputSection.replyHeader.text.toString()
            }
        }
    }

    private fun uploadMediaAndSendMessage(uri: Uri, text: String) {
        val safeChatId = chatId ?: return
        val fileName = "chat_media/$safeChatId/${UUID.randomUUID()}"
        val storageRef = storage.reference.child(fileName)

        storageRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val message = createTextMessage(text).apply {
                    type = if (contentResolver.getType(uri)?.startsWith("video/") == true) ChatMessage.TYPE_VIDEO else ChatMessage.TYPE_IMAGE
                    mediaUrl = task.result.toString()
                    thumbnailUrl = task.result.toString()
                }
                saveMessageToFirestore(message)
            } else {
                handleUploadFailure(task.exception)
            }
        }
    }

    private fun saveMessageToFirestore(message: ChatMessage) {
        val safeChatId = chatId ?: return
        db.collection("chats").document(safeChatId).collection("messages").add(message)
            .addOnSuccessListener { docRef ->
                docRef.update("messageId", docRef.id)
                clearInputFields()
                updateChatLastMessage(message)
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to send message", e) }
            .addOnCompleteListener { showProgress(false) }
    }

    private fun updateChatLastMessage(message: ChatMessage) {
        // ... (نفس الكود من الرد السابق)
    }

    private fun setupAppBar() {
        binding.toolbar.ivBack.setOnClickListener { finish() }
        loadChatDetails()
    }

    private fun loadChatDetails() {
        val safeChatId = chatId ?: return
        chatDetailsListener?.remove()
        chatDetailsListener = db.collection("chats").document(safeChatId)
            .addSnapshotListener { document, _ ->
                if (!isActivityValid() || document == null || !document.exists()) return@addSnapshotListener

                val chat = document.toObject(Chat::class.java) ?: return@addSnapshotListener
                if (chat.isGroup) bindGroupHeader(chat)
                else {
                    val otherUserId = chat.participants.firstOrNull { it != currentUserId }
                    otherUserId?.let { bindUserHeader(it) }
                }
            }
    }

    private fun bindGroupHeader(chat: Chat) {
        binding.toolbar.tvAppName.text = chat.groupName ?: "Group"
        Glide.with(this).load(chat.groupImage).placeholder(R.drawable.ic_default_group).into(binding.toolbar.ivAvatar)
        binding.toolbar.ivVerified.visibility = View.GONE
    }

    private fun bindUserHeader(userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (isActivityValid() && doc.exists()) {
                val user = doc.toObject(UserModel::class.java)
                binding.toolbar.tvAppName.text = user?.displayName ?: user?.username
                Glide.with(this).load(user?.profileImageUrl).placeholder(R.drawable.ic_default_avatar).into(binding.toolbar.ivAvatar)
                binding.toolbar.ivVerified.isVisible = user?.isVerified == true
            }
        }
    }

    private fun clearInputFields() {
        binding.inputSection.postInput.text.clear()
        clearMediaPreview()
        clearReplyUI()
    }

    private fun showMediaPreview(uri: Uri) {
        val mediaPreviewLayout = binding.inputSection.mediaPreview
        mediaPreviewLayout.visibility = View.VISIBLE
        val ivMedia = mediaPreviewLayout.findViewById<ImageView>(R.id.ivMedia)
        Glide.with(this).load(uri).into(ivMedia)
        updateSendButtonState()
    }

    private fun clearMediaPreview() {
        binding.inputSection.mediaPreview.visibility = View.GONE
        currentMediaUri = null
        updateSendButtonState()
    }

    private fun clearReplyUI() {
        binding.inputSection.replyHeader.visibility = View.GONE
        binding.inputSection.replyHeader.tag = null
    }

    private fun toggleMediaOptions() {
        binding.inputSection.bottomToolbar.isVisible = !binding.inputSection.bottomToolbar.isVisible
    }

    private fun showGifPicker() = Toast.makeText(this, "GIFs coming soon!", Toast.LENGTH_SHORT).show()
    private fun createPoll() = Toast.makeText(this, "Polls coming soon!", Toast.LENGTH_SHORT).show()

    private fun scrollToBottom() {
        if (messageAdapter.itemCount > 0) {
            binding.recyclerView.post { binding.recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1) }
        }
    }

    private fun showProgress(show: Boolean) {
        binding.inputSection.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.inputSection.btnSend.isEnabled = !show
    }

    private fun isActivityValid(): Boolean = !isFinishing && !isDestroyed
    private fun handleUploadFailure(e: Exception?) { /* ... */ }

    // --- MessageClickListener Implementation ---
    override fun onMessageLongClick(message: ChatMessage, position: Int) { /* ... */ }
    override fun onMediaClick(mediaUrl: String, position: Int) { /* ... */ }

    @SuppressLint("SetTextI18n")
    override fun onReplyClick(messageId: String) {
        val safeChatId = chatId ?: return
        db.collection("chats").document(safeChatId).collection("messages").document(messageId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val msg = doc.toObject(ChatMessage::class.java)
                    binding.inputSection.replyHeader.text = "Replying to ${msg?.senderName ?: "User"}"
                    binding.inputSection.replyHeader.visibility = View.VISIBLE
                    binding.inputSection.replyHeader.tag = messageId
                }
            }
    }

    override fun onPollVote(pollId: String, optionIndex: Int) { /* ... */ }
    override fun onFileClick(fileUrl: String) { /* ... */ }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
        userDeletedMessagesListener?.remove()
        chatDetailsListener?.remove()
    }

    companion object { private const val TAG = "ChatActivity" }
}
