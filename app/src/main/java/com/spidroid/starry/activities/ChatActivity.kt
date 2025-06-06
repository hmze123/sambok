// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/ChatActivity.kt
package com.spidroid.starry.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.ImageView
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
import java.util.Date
import java.util.UUID
import com.spidroid.starry.databinding.ItemMediaPreviewBinding // ✨ تم إضافة هذا الاستيراد

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
    private var chatDetailsListener: ListenerRegistration? = null

    private lateinit var messageAdapter: MessageAdapter
    private var currentMediaUri: Uri? = null

    // لانتقاء الوسائط من الجهاز
    private val mediaPickerLauncher: ActivityResultLauncher<String> =
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
        listenForChatDetails()
        listenForMessages()
    }

    private fun handleAuthError() {
        Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupInputLayout()
        setupAppBar()
    }

    private fun loadCurrentUserData() {
        currentUserId?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    // Corrected access: check if activity is valid before posting value
                    if (isFinishing || isDestroyed) return@addOnSuccessListener
                    currentUserModel = document.toObject(UserModel::class.java)
                    updateSendButtonState() // تحديث حالة زر الإرسال بعد تحميل بيانات المستخدم
                }
                .addOnFailureListener {
                    // Corrected access: check if activity is valid before posting value
                    if (isFinishing || isDestroyed) return@addOnFailureListener
                    Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUserId!!, this, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
            // إضافة مسافة في الأسفل لتجنب تداخل لوحة المفاتيح
            setPadding(0,0,0, 16)
            clipToPadding = false
        }
    }

    private fun listenForMessages() {
        messagesListener = db.collection("chats").document(chatId!!).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                // Corrected access: check if activity is valid before processing snapshot
                if (isFinishing || isDestroyed || error != null) {
                    Log.w(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }
                val messages = snapshots?.documents?.mapNotNull {
                    it.toObject(ChatMessage::class.java)?.apply { messageId = it.id }
                } ?: emptyList()
                messageAdapter.submitList(messages) {
                    scrollToBottom()
                }
            }
    }

    private fun setupInputLayout() {
        with(binding.inputSection) {
            triggerButton.setOnClickListener { toggleMediaOptions() }
            btnSend.setOnClickListener { sendMessage() }
            addPhoto.setOnClickListener { mediaPickerLauncher.launch("image/*") }
            addVideo.setOnClickListener { mediaPickerLauncher.launch("video/*") }
            addGif.setOnClickListener { Toast.makeText(this@ChatActivity, "GIFs coming soon!", Toast.LENGTH_SHORT).show() }
            addPoll.setOnClickListener { Toast.makeText(this@ChatActivity, "Polls coming soon!", Toast.LENGTH_SHORT).show() }

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
        val mediaUri = currentMediaUri

        if (messageText.isEmpty() && mediaUri == null) return

        showProgress(true)

        if (mediaUri != null) {
            uploadMediaAndSendMessage(mediaUri, messageText)
        } else {
            val message = createMessageObject(content = messageText, type = ChatMessage.TYPE_TEXT)
            saveMessageToFirestore(message)
        }
    }

    private fun createMessageObject(content: String, type: String, mediaUrl: String? = null, thumbnailUrl: String? = null): ChatMessage {
        val user = currentUserModel!!
        return ChatMessage(senderId = user.userId, type = type).apply {
            this.content = if (content.isNotBlank()) content else null
            this.senderName = user.displayName ?: user.username
            this.senderAvatar = user.profileImageUrl
            this.mediaUrl = mediaUrl
            this.thumbnailUrl = thumbnailUrl
            // Handle reply logic here if needed
        }
    }

    private fun uploadMediaAndSendMessage(uri: Uri, text: String) {
        val fileName = "chat_media/${chatId}/${UUID.randomUUID()}"
        val storageRef = storage.reference.child(fileName)

        storageRef.putFile(uri)
            .addOnProgressListener { showProgress(true) }
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }.addOnCompleteListener { task ->
                // Corrected access: check if activity is valid before proceeding
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (task.isSuccessful) {
                    val mediaType = if (contentResolver.getType(uri)?.startsWith("video/") == true) ChatMessage.TYPE_VIDEO else ChatMessage.TYPE_IMAGE
                    val message = createMessageObject(content = text, type = mediaType, mediaUrl = task.result.toString(), thumbnailUrl = task.result.toString())
                    saveMessageToFirestore(message)
                } else {
                    handleUploadFailure(task.exception)
                }
            }
    }

    private fun saveMessageToFirestore(message: ChatMessage) {
        db.collection("chats").document(chatId!!).collection("messages").add(message)
            .addOnSuccessListener { docRef ->
                // Corrected access: check if activity is valid before posting value
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                docRef.update("messageId", docRef.id) // Save the auto-generated ID
                clearInput()
                updateChatLastMessage(message)
            }
            .addOnFailureListener { e ->
                // Corrected access: check if activity is valid before logging
                if (isFinishing || isDestroyed) return@addOnFailureListener
                Log.e(TAG, "Failed to send message", e)
            }
            .addOnCompleteListener {
                // Corrected access: check if activity is valid before proceeding
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                showProgress(false)
            }
    }

    private fun updateChatLastMessage(message: ChatMessage) {
        val lastMessageData = mapOf(
            "lastMessage" to (message.content ?: message.type.replaceFirstChar { it.titlecase() }),
            "lastMessageType" to message.type,
            "lastMessageTime" to FieldValue.serverTimestamp(),
            "lastMessageSender" to message.senderId
        )
        db.collection("chats").document(chatId!!).update(lastMessageData)
    }

    private fun setupAppBar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun listenForChatDetails() {
        chatDetailsListener = db.collection("chats").document(chatId!!)
            .addSnapshotListener { snapshot, error ->
                // Corrected access: check if activity is valid before processing snapshot
                if (isFinishing || isDestroyed || error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }
                val chat = snapshot.toObject(Chat::class.java) ?: return@addSnapshotListener
                if (chat.isGroup) {
                    bindGroupHeader(chat)
                } else {
                    val otherUserId = chat.participants.firstOrNull { it != currentUserId }
                    otherUserId?.let { bindUserHeader(it) }
                }
            }
    }

    private fun bindGroupHeader(chat: Chat) {
        binding.tvAppName.text = chat.groupName ?: "Group"
        Glide.with(this).load(chat.groupImage).placeholder(R.drawable.ic_default_group).into(binding.ivAvatar)
        binding.ivVerified.visibility = View.GONE
    }

    private fun bindUserHeader(userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            // Corrected access: check if activity is valid before processing
            if (isFinishing || isDestroyed || !doc.exists()) return@addOnSuccessListener
            val user = doc.toObject(UserModel::class.java)
            binding.tvAppName.text = user?.displayName ?: user?.username
            Glide.with(this).load(user?.profileImageUrl).placeholder(R.drawable.ic_default_avatar).into(binding.ivAvatar)
            binding.ivVerified.isVisible = user?.isVerified == true
        }
    }

    private fun clearInput() {
        binding.inputSection.postInput.text.clear()
        clearMediaPreview()
    }

    private fun showMediaPreview(uri: Uri) {
        // ✨ تم الحصول على كائن الربط لـ item_media_preview
        val mediaPreviewBinding = binding.inputSection.mediaPreview
        mediaPreviewBinding.root.visibility = View.VISIBLE // ✨ استخدام root للوصول إلى visibility
        // ✨ الوصول إلى ImageView من خلال كائن الربط
        Glide.with(this).load(uri).into(mediaPreviewBinding.ivMedia)
        // ✨ الوصول إلى ImageButton من خلال كائن الربط
        mediaPreviewBinding.btnRemove.setOnClickListener {
            clearMediaPreview()
        }
        updateSendButtonState()
    }

    private fun clearMediaPreview() {
        // ✨ تم الحصول على كائن الربط لـ item_media_preview
        val mediaPreviewBinding = binding.inputSection.mediaPreview
        mediaPreviewBinding.root.visibility = View.GONE // ✨ استخدام root للوصول إلى visibility
        currentMediaUri = null
        updateSendButtonState()
    }

    private fun toggleMediaOptions() {
        binding.inputSection.bottomToolbar.isVisible = !binding.inputSection.bottomToolbar.isVisible
    }

    private fun showProgress(show: Boolean) {
        binding.inputSection.progressBar.isVisible = show
        binding.inputSection.btnSend.isEnabled = !show
    }

    private fun handleUploadFailure(exception: Exception?) {
        showProgress(false)
        Toast.makeText(this, "Upload failed: ${exception?.message}", Toast.LENGTH_LONG).show()
    }

    private fun scrollToBottom() {
        if (messageAdapter.itemCount > 0) {
            binding.recyclerView.post {
                binding.recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
            }
        }
    }

    private fun isActivityInvalid(): Boolean = isFinishing || isDestroyed

    // --- MessageClickListener Implementation ---
    override fun onMessageLongClick(message: ChatMessage, position: Int) {
        // Implement context menu logic here
        Toast.makeText(this, "Long clicked: ${message.content}", Toast.LENGTH_SHORT).show()
    }
    override fun onMediaClick(mediaUrl: String, position: Int) {
        // Implement full-screen media viewer
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putStringArrayListExtra("media_urls", arrayListOf(mediaUrl))
            putExtra("position", 0)
        }
        startActivity(intent)
    }
    override fun onReplyClick(messageId: String) {}
    override fun onPollVote(pollId: String, optionIndex: Int) {}
    override fun onFileClick(fileUrl: String) {}

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
        chatDetailsListener?.remove()
    }

    companion object {
        private const val TAG = "ChatActivity"
    }
}