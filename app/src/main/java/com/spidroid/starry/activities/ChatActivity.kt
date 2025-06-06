package com.spidroid.starry.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.spidroid.starry.R
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.ChatMessage
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.messages.ContextMenuDialog
import com.spidroid.starry.ui.messages.MessageAdapter
import com.spidroid.starry.ui.messages.MessageContextMenuListener
import com.spidroid.starry.ui.messages.PollDialog
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.UUID

class ChatActivity : AppCompatActivity(), MessageAdapter.MessageClickListener {

    private companion object {
        private const val MAX_MESSAGE_LENGTH = 2000
        private const val TAG = "ChatActivity"
    }

    private val userDeletedMessageIds = HashSet<String>()
    private var userDeletedMessagesListener: ListenerRegistration? = null
    private var chatDetailsListener: ListenerRegistration? = null

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String? = null
    private var currentUserModel: UserModel? = null
    private var messagesListener: ListenerRegistration? = null

    private lateinit var btnSend: ImageButton
    private lateinit var btnAddMedia: ImageButton
    private lateinit var btnAddVideo: ImageButton // يمكن أن تكون nullable إذا كان تهيئتها مشروطًا جدًا
    private lateinit var btnAddGif: ImageButton
    private lateinit var btnAddPoll: ImageButton
    private lateinit var btnMenu: ImageButton
    private lateinit var triggerButton: ImageButton
    private lateinit var postInput: EditText
    private lateinit var mediaPreviewContainer: FrameLayout
    private var mediaPreviewImage: ImageView? = null // Nullable لأن تهيئتها داخل شرط
    private lateinit var progressBar: ProgressBar
    private lateinit var messagesRecycler: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var bottomToolbar: LinearLayout
    private lateinit var replyHeader: TextView
    private lateinit var tvDisplayName: TextView
    private lateinit var ivAvatar: CircleImageView
    private lateinit var ivVerified: ImageView

    private var chatId: String? = null
    private var isGroupChat = false
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
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        initializeFirebase()

        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null after initializeFirebase. Finishing activity.")
            Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initializeViews() // يجب استدعاؤها بعد initializeFirebase وقبل استخدام الـ views

        if (chatId.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_chat_id_error), Toast.LENGTH_LONG).show()
            Log.e(TAG, "Chat ID is null or empty in onCreate. Finishing activity.")
            finish()
            return
        }

        loadCurrentUserData()
        setupRecyclerView()
        setupInputBehavior()
        setupAppBar()
        loadChatDetails()
    }

    private fun initializeFirebase() {
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        val user: FirebaseUser? = auth.currentUser
        if (user != null) {
            currentUserId = user.uid
        } else {
            if (!isFinishing) { // التحقق من أن النشاط لم ينتهِ بعد
                Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Current user is null in initializeFirebase (called from onCreate).")
                finishActivityCleanup()
            }
            return // الخروج من الدالة إذا كان المستخدم null
        }

        // التأكد أن currentUserId ليس null قبل استخدامه في الاستعلام
        currentUserId?.let { userId ->
            userDeletedMessagesListener = db.collection("users")
                .document(userId)
                .collection("deleted_messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to deleted messages", error)
                        return@addSnapshotListener
                    }
                    snapshots?.let {
                        userDeletedMessageIds.clear()
                        for (doc in it) {
                            userDeletedMessageIds.add(doc.id)
                        }
                    }
                }
        }
    }

    private fun finishActivityCleanup() {
        finish()
    }

    private fun loadCurrentUserData() {
        currentUserId ?: return // الخروج إذا كان currentUserId هو null
        db.collection("users").document(currentUserId!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    currentUserModel = documentSnapshot.toObject(UserModel::class.java)
                    if (currentUserModel == null) {
                        Log.e(TAG, "currentUserModel is null after deserialization from Firestore.")
                        Toast.makeText(this, "Failed to load your profile data.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "Current user data loaded: ${currentUserModel?.username ?: "N/A"}")
                        updateSendButtonState()
                    }
                } else {
                    Log.e(TAG, "Current user document does not exist in Firestore.")
                    Toast.makeText(this, "Your profile data not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load current user data.", e)
                Toast.makeText(this, "Error loading your profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initializeViews() {
        tvDisplayName = findViewById(R.id.tv_app_name)
        ivAvatar = findViewById(R.id.ivAvatar)
        ivVerified = findViewById(R.id.iv_verified)
        btnMenu = findViewById(R.id.btnMenu)
        btnSend = findViewById(R.id.btnSend)
        postInput = findViewById(R.id.postInput)
        mediaPreviewContainer = findViewById(R.id.mediaPreview)
        mediaPreviewContainer.let { container ->
            mediaPreviewImage = container.findViewById(R.id.ivMedia)
            val btnRemoveMedia: ImageButton? = container.findViewById(R.id.btnRemove)
            btnRemoveMedia?.setOnClickListener { clearMediaPreview() }
        }
        progressBar = findViewById(R.id.progressBar)
        bottomToolbar = findViewById(R.id.bottomToolbar)
        replyHeader = findViewById(R.id.replyHeader)
        triggerButton = findViewById(R.id.triggerButton)
        btnAddMedia = findViewById(R.id.addPhoto)
        btnAddVideo = findViewById(R.id.addVideo)
        btnAddGif = findViewById(R.id.addGif)
        btnAddPoll = findViewById(R.id.addPoll)

        btnAddVideo.setOnClickListener { mediaPicker.launch("video/*") }

        val intent = intent // لا حاجة لتكرار intent
        isGroupChat = intent.getBooleanExtra("isGroup", false)
        chatId = intent.getStringExtra("chatId")

        btnAddPoll.visibility = if (isGroupChat) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        messagesRecycler = findViewById(R.id.recyclerView)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messagesRecycler.layoutManager = layoutManager
        messageAdapter = MessageAdapter(currentUserId ?: "", this, this) // توفير قيمة افتراضية أو معالجة null
        messagesRecycler.adapter = messageAdapter

        val currentChatId = chatId
        if (currentChatId.isNullOrEmpty()) {
            Log.e(TAG, "Cannot setup messages listener, chatId is null or empty for RecyclerView.")
            return
        }
        messagesListener?.remove() // إزالة المستمع القديم إذا كان موجودًا

        messagesListener = db.collection("chats")
            .document(currentChatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Messages listen failed for chatId: $currentChatId", error)
                    return@addSnapshotListener
                }
                val messages = ArrayList<ChatMessage>()
                snapshots?.let {
                    for (doc in it) {
                        val isDeleted = doc.getBoolean("deleted") == true
                        val docId = doc.id
                        val isDeletedForMe = userDeletedMessageIds.contains(docId)

                        if (!isDeleted && !isDeletedForMe) {
                            val message = doc.toObject(ChatMessage::class.java).apply {
                                messageId = docId
                            }
                            Log.d(TAG, "Fetched message: ID=${message.messageId}, Content=[${message.content}], SenderName=[${message.senderName}]")
                            messages.add(message)
                        }
                    }
                }
                runOnUiThread {
                    messageAdapter.submitList(messages)
                    scrollToBottom()
                }
            }
    }

    private fun setupInputBehavior() {
        triggerButton.setOnClickListener { toggleMediaOptions() }

        postInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSendButtonState()
                toggleSendButtonVisibility()
            }
        })

        btnSend.setOnClickListener { sendMessage() }
        btnAddMedia.setOnClickListener { mediaPicker.launch("image/*") }
        // btnAddVideo listener تم تعيينه في initializeViews
        btnAddGif.setOnClickListener { showGifPicker() }
        btnAddPoll.setOnClickListener { createPoll() }

        postInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER &&
                postInput.text.toString().trim().isNotEmpty()
            ) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun toggleSendButtonVisibility() {
        val btnRecordView: View? = findViewById(R.id.btnRecord) // قد يكون null
        if (btnRecordView == null) return

        val hasText = postInput.text.toString().trim().isNotEmpty()
        val hasMedia = mediaPreviewContainer.visibility == View.VISIBLE
        val showSend = hasText || hasMedia

        btnSend.visibility = if (showSend) View.VISIBLE else View.GONE
        btnRecordView.visibility = if (showSend) View.GONE else View.VISIBLE
    }

    private fun updateSendButtonState() {
        val hasText = postInput.text.toString().trim().isNotEmpty()
        val hasMedia = mediaPreviewContainer.visibility == View.VISIBLE
        btnSend.isEnabled = (hasText || hasMedia) && currentUserModel != null
    }

    private fun sendMessage() {
        val messageText = postInput.text.toString().trim()
        val replyToTag = postInput.getTag(R.id.tag_reply_to_message_id)
        val replyToMessageId = replyToTag as? String // safe cast

        if (messageText.length > MAX_MESSAGE_LENGTH) {
            Toast.makeText(this, "Message exceeds character limit ($MAX_MESSAGE_LENGTH)", Toast.LENGTH_SHORT).show()
            return
        }
        if (messageText.isEmpty() && currentMediaUri == null) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUserId == null) {
            Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_SHORT).show()
            return
        }
        val localCurrentUserModel = currentUserModel
        if (localCurrentUserModel == null) {
            Toast.makeText(this, "User data not loaded yet. Please wait.", Toast.LENGTH_SHORT).show()
            loadCurrentUserData() // محاولة تحميل بيانات المستخدم مرة أخرى
            return
        }

        showProgress(true)

        if (currentMediaUri != null) {
            uploadMediaAndSendMessage(messageText, replyToMessageId)
        } else {
            val message = ChatMessage(currentUserId!!, messageText).apply {
                senderName = if (!localCurrentUserModel.displayName.isNullOrEmpty()) localCurrentUserModel.displayName else localCurrentUserModel.username
                senderAvatar = localCurrentUserModel.profileImageUrl
                isEdited = false
                if (replyToMessageId != null && replyHeader.text != null) {
                    this.replyToId = replyToMessageId
                    this.replyPreview = replyHeader.text.toString()
                }
            }
            saveMessageToFirestore(message)
        }
    }

    private fun uploadMediaAndSendMessage(messageText: String, replyToMessageId: String?) {
        val fileUri = currentMediaUri ?: run {
            Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show()
            showProgress(false)
            return
        }
        val localCurrentUserId = currentUserId ?: run {
            Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_SHORT).show()
            showProgress(false)
            return
        }
        val localCurrentUserModel = currentUserModel ?: run {
            Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_SHORT).show()
            showProgress(false)
            return
        }


        val storageRef = storage.reference
        val fileName = "chat_media/$chatId/${UUID.randomUUID()}"
        val fileRef = storageRef.child(fileName)

        val mimeType = contentResolver.getType(fileUri)
        val messageType: String = when {
            mimeType?.startsWith("video/") == true -> ChatMessage.TYPE_VIDEO
            mimeType == "image/gif" -> ChatMessage.TYPE_GIF
            else -> ChatMessage.TYPE_IMAGE // افتراضي للصورة
        }

        fileRef.putFile(fileUri)
            .addOnSuccessListener { taskSnapshot ->
                fileRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val message = ChatMessage(localCurrentUserId, messageText).apply {
                            senderName = if (!localCurrentUserModel.displayName.isNullOrEmpty()) localCurrentUserModel.displayName else localCurrentUserModel.username
                            senderAvatar = localCurrentUserModel.profileImageUrl
                            type = messageType
                            mediaUrl = uri.toString()
                            thumbnailUrl = uri.toString() // يمكن تحسين هذا للحصول على thumbnail حقيقي
                            if (replyToMessageId != null && replyHeader.text != null) {
                                this.replyToId = replyToMessageId
                                this.replyPreview = replyHeader.text.toString()
                            }
                        }
                        saveMessageToFirestore(message)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get download URL", e)
                        Toast.makeText(this, "Failed to get media URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        showProgress(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload failed", e)
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                showProgress(false)
            }
    }

    private fun saveMessageToFirestore(message: ChatMessage) {
        val currentChatId = chatId
        if (currentChatId.isNullOrEmpty()) {
            Log.e(TAG, "Cannot save message, chatId is null or empty.")
            showProgress(false)
            Toast.makeText(this, "Error sending message: Invalid chat.", Toast.LENGTH_SHORT).show()
            return
        }

        val messageMap = message.toFirestoreMap().apply {
            put("timestamp", FieldValue.serverTimestamp())
            currentUserModel?.let { user ->
                if (!containsKey("senderName")) {
                    put("senderName", if (!user.displayName.isNullOrEmpty()) user.displayName else user.username)
                }
                if (!containsKey("senderAvatar")) {
                    put("senderAvatar", user.profileImageUrl)
                }
            }
        }


        db.collection("chats")
            .document(currentChatId)
            .collection("messages")
            .add(messageMap)
            .addOnCompleteListener { task ->
                showProgress(false)
                if (task.isSuccessful) {
                    val docRef = task.result
                    if (docRef != null) {
                        Log.d(TAG, "Message sent with ID: ${docRef.id}")
                        docRef.update("messageId", docRef.id) // تحديث الرسالة بـ ID الخاص بها
                        clearInputFields()
                        var lastMsgContent = message.content
                        when {
                            message.replyToId != null && message.replyPreview != null -> {
                                lastMsgContent = message.replyPreview // أو نص مختصر للرد
                            }
                            ChatMessage.TYPE_IMAGE == message.type || ChatMessage.TYPE_GIF == message.type -> {
                                lastMsgContent = "[Image]"
                            }
                            ChatMessage.TYPE_VIDEO == message.type -> {
                                lastMsgContent = "[Video]"
                            }
                            ChatMessage.TYPE_FILE == message.type -> {
                                lastMsgContent = "[File: ${message.fileName}]"
                            }
                            ChatMessage.TYPE_POLL == message.type && message.poll != null -> {
                                lastMsgContent = "[Poll: ${message.poll!!.question}]"
                            }
                        }
                        updateChatLastMessage(lastMsgContent, message.senderName, message.type)
                        scrollToBottom()
                    } else {
                        Log.e(TAG, "Failed to send message, DocumentReference is null.")
                        Toast.makeText(this@ChatActivity, "Failed to send message.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to send message: $errorMessage", task.exception)
                    Toast.makeText(this@ChatActivity, "Failed to send message: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateChatLastMessage(messageContent: String?, senderName: String?, messageType: String?) {
        val currentChatId = chatId
        if (currentChatId.isNullOrEmpty()) {
            Log.e(TAG, "Cannot update last message, chatId is null or empty.")
            return
        }
        val updates = hashMapOf<String, Any>(
            "lastMessage" to (messageContent ?: ""),
            "lastMessageSender" to (senderName ?: currentUserModel?.displayName ?: "User"),
            "lastMessageType" to (messageType ?: ChatMessage.TYPE_TEXT),
            "lastMessageTime" to FieldValue.serverTimestamp()
        )

        db.collection("chats")
            .document(currentChatId)
            .update(updates)
            .addOnFailureListener { e -> Log.e(TAG, "Error updating last message for chat: $currentChatId", e) }
    }

    private fun clearInputFields() {
        postInput.setText("")
        mediaPreviewContainer.visibility = View.GONE
        currentMediaUri = null
        mediaPreviewImage?.let {
            if (!isDestroyed && !isFinishing) {
                Glide.with(this).clear(it)
            }
        }
        clearReplyUI()
        updateSendButtonState()
        toggleSendButtonVisibility()
    }

    private fun showEditMessageDialog(message: ChatMessage) {
        if (currentUserId == null || message.senderId != currentUserId) {
            Toast.makeText(this, "You can only edit your own messages.", Toast.LENGTH_SHORT).show()
            return
        }
        if (ChatMessage.TYPE_TEXT != message.type) {
            Toast.makeText(this, "Only text messages can be edited.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_message, null)
        val input = dialogView.findViewById<EditText>(R.id.et_edit_message_input)
        input.setText(message.content)
        input.setSelection(message.content?.length ?: 0)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Message")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newText = input.text.toString().trim()
                if (validateEdit(message, newText)) {
                    updateMessageInFirestore(message, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateEdit(original: ChatMessage, newText: String): Boolean {
        if (newText.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (newText == original.content) {
            return false // لا يوجد تغيير
        }
        if (newText.length > MAX_MESSAGE_LENGTH) {
            Toast.makeText(this, "Message exceeds character limit ($MAX_MESSAGE_LENGTH)", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun updateMessageInFirestore(message: ChatMessage, newContent: String) {
        val currentChatId = chatId
        val currentMessageId = message.messageId
        if (currentChatId == null || currentMessageId == null) {
            Log.e(TAG, "Cannot update message, chatId or messageId is null.")
            return
        }
        val updates = mapOf(
            "content" to newContent,
            "edited" to true,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("chats")
            .document(currentChatId)
            .collection("messages")
            .document(currentMessageId)
            .update(updates)
            .addOnSuccessListener { Toast.makeText(this, "Message updated", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating message", e)
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSend.isEnabled = !show
        postInput.isEnabled = !show
        btnAddMedia.isEnabled = !show
        btnAddVideo.isEnabled = !show
        btnAddGif.isEnabled = !show
        btnAddPoll.isEnabled = !show
        triggerButton.isEnabled = !show
    }

    private fun showMediaPreview(uri: Uri) {
        mediaPreviewContainer.visibility = View.VISIBLE
        mediaPreviewImage?.let {
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_cover_placeholder) // تأكد من وجود هذا الرسم
                .into(it)
        }
        updateSendButtonState()
        toggleSendButtonVisibility()
    }

    private fun toggleMediaOptions() {
        val show = bottomToolbar.visibility != View.VISIBLE
        bottomToolbar.visibility = if (show) View.VISIBLE else View.GONE
        triggerButton.setImageResource(if (show) R.drawable.ic_close else R.drawable.ic_add) // تأكد من وجود هذه الرسوم
        if (show) {
            Utils.hideKeyboard(this)
        }
    }

    private fun showGifPicker() {
        Toast.makeText(this, "GIF picker coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun createPoll() {
        if (!isGroupChat) {
            Toast.makeText(this, "Polls are only available in group chats.", Toast.LENGTH_SHORT).show()
            return
        }
        val localCurrentUserId = currentUserId
        val localCurrentUserModel = currentUserModel
        if (localCurrentUserId == null || localCurrentUserModel == null) {
            Toast.makeText(this, "Cannot create poll: User data not loaded.", Toast.LENGTH_SHORT).show()
            return
        }

        PollDialog().apply {
            setOnPollCreatedListener { question, optionsList ->
                val poll = ChatMessage.Poll().apply {
                    this.question = question
                    this.options = optionsList.map { ChatMessage.PollOption(it) }
                }
                val message = ChatMessage(localCurrentUserId, poll).apply {
                    senderName = if (!localCurrentUserModel.displayName.isNullOrEmpty()) localCurrentUserModel.displayName else localCurrentUserModel.username
                    senderAvatar = localCurrentUserModel.profileImageUrl
                    content = question // أو نص يمثل الاستطلاع
                }
                saveMessageToFirestore(message)
            }
        }.show(supportFragmentManager, "PollDialog")
    }

    private fun scrollToBottom() {
        messagesRecycler.post {
            if (messageAdapter.itemCount > 0) {
                messagesRecycler.smoothScrollToPosition(messageAdapter.itemCount - 1)
            }
        }
    }

    private fun setupAppBar() {
        findViewById<ImageView>(R.id.iv_back)?.setOnClickListener { finish() }
        tvDisplayName.text = "Loading..."
        ivAvatar.setImageResource(R.drawable.ic_default_avatar) // تأكد من وجود هذا الرسم
        ivVerified.visibility = View.GONE
    }

    @SuppressLint("StringFormatMatches") // لقمع تحذير محتمل لـ getString إذا كان التنسيق معقدًا
    private fun loadChatDetails() {
        val currentChatId = chatId
        if (currentChatId.isNullOrEmpty()) {
            Log.e(TAG, "Cannot load chat details, chatId is null or empty for AppBar.")
            Toast.makeText(this, getString(R.string.invalid_chat_id_error), Toast.LENGTH_SHORT).show()
            finishActivityCleanup()
            return
        }
        chatDetailsListener?.remove()
        chatDetailsListener = db.collection("chats")
            .document(currentChatId)
            .addSnapshotListener { document, error ->
                if (!isActivityValid()) return@addSnapshotListener

                if (error != null) {
                    Log.e(TAG, "Chat details listen failed for chatId: $currentChatId", error)
                    tvDisplayName.text = "Error"
                    return@addSnapshotListener
                }
                if (document == null || !document.exists()) {
                    Log.w(TAG, "Chat document does not exist for chatId: $currentChatId")
                    tvDisplayName.text = "Chat not found"
                    ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                    ivVerified.visibility = View.GONE
                    return@addSnapshotListener
                }

                val chat = document.toObject(Chat::class.java)?.apply { id = document.id }
                if (chat != null) {
                    Log.d(TAG, "Chat details loaded/updated for AppBar: $currentChatId, Group: ${chat.isGroup}, Name: ${chat.groupName}")
                    if (isGroupChat) { // أو chat.isGroup إذا كنت تثق في البيانات من Firestore
                        tvDisplayName.text = chat.groupName ?: "Group"
                        if (!chat.groupImage.isNullOrEmpty()) {
                            Glide.with(this).load(chat.groupImage)
                                .placeholder(R.drawable.ic_default_group) // تأكد من وجود هذا الرسم
                                .error(R.drawable.ic_default_group)
                                .into(ivAvatar)
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_default_group)
                        }
                        ivVerified.visibility = View.GONE
                    } else {
                        val otherUserId = getOtherUserId(chat.participants)
                        if (!otherUserId.isNullOrEmpty()) {
                            loadOtherUserDetails(otherUserId)
                        } else {
                            Log.w(TAG, "Could not find other user ID for direct chat (AppBar): $currentChatId")
                            tvDisplayName.text = getString(R.string.unknown_user_display_name)
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                            ivVerified.visibility = View.GONE
                        }
                    }
                } else {
                    Log.e(TAG, "Chat object is null after deserialization (AppBar) for chatId: $currentChatId")
                    tvDisplayName.text = "Error Loading Chat"
                }
            }
    }

    private fun isActivityValid(): Boolean = !isFinishing && !isDestroyed

    private fun getOtherUserId(participants: List<String>?): String? {
        participants ?: return null
        val localCurrentUserId = currentUserId ?: return null
        return participants.firstOrNull { it != localCurrentUserId }
    }

    @SuppressLint("StringFormatMatches")
    private fun loadOtherUserDetails(userId: String) {
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot load other user details for AppBar, userId is empty.")
            tvDisplayName.text = getString(R.string.unknown_user_display_name)
            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            ivVerified.visibility = View.GONE
            return
        }
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!isActivityValid()) return@addOnSuccessListener

                if (document.exists()) {
                    val user = document.toObject(UserModel::class.java)
                    if (user != null) {
                        tvDisplayName.text = if (!user.displayName.isNullOrEmpty()) user.displayName else user.username
                        if (!user.profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@ChatActivity).load(user.profileImageUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .into(ivAvatar)
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                        ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE
                    } else {
                        Log.e(TAG, "UserModel is null after deserialization (AppBar) for other user: $userId")
                        tvDisplayName.text = getString(R.string.unknown_user_display_name)
                    }
                } else {
                    Log.w(TAG, "User document not found (AppBar) for other user ID: $userId")
                    tvDisplayName.text = getString(R.string.unknown_user_display_name)
                }
            }
            .addOnFailureListener { e ->
                if (!isActivityValid()) return@addOnFailureListener
                Log.e(TAG, "Failed to load other user details for AppBar, userId: $userId", e)
                tvDisplayName.text = getString(R.string.unknown_user_display_name)
            }
    }

    override fun onMessageLongClick(message: ChatMessage, position: Int) {
        val viewHolder = messagesRecycler.findViewHolderForAdapterPosition(position)
        viewHolder?.itemView?.let {
            showMessageContextMenu(message, it)
        } ?: Log.w(TAG, "ViewHolder not found for position: $position during long click.")
    }

    override fun onMessageLongClick(
        message: ChatMessage?,
        position: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun onMediaClick(mediaUrl: String?, position: Int) {
        if (mediaUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Media URL is invalid.", Toast.LENGTH_SHORT).show()
            return
        }
        val mediaList = arrayListOf(mediaUrl) // استخدام arrayListOf
        var mediaView: View? = null
        messagesRecycler.findViewHolderForAdapterPosition(position)?.itemView?.let { itemView ->
            mediaView = itemView.findViewById(R.id.imageContent)
            if (mediaView == null) { // إذا لم يكن imageContent، تحقق من videoThumbnail
                val itemViewType = messageAdapter.getItemViewType(position)
                if (itemViewType == MessageAdapter.VIEW_TYPE_VIDEO_SENT || itemViewType == MessageAdapter.VIEW_TYPE_VIDEO_RECEIVED) {
                    mediaView = itemView.findViewById(R.id.videoThumbnail)
                }
            }
        }
        MediaViewerActivity.launch(this, mediaList, 0, mediaView)
    }

    override fun onReplyClick(messageId: String?) {
        val currentChatId = chatId
        if (currentChatId == null || messageId == null) return

        db.collection("chats").document(currentChatId).collection("messages").document(messageId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val repliedMessage = documentSnapshot.toObject(ChatMessage::class.java)
                    repliedMessage?.let {
                        val senderName = it.senderName ?: "User"
                        val contentPreview: String = when {
                            ChatMessage.TYPE_TEXT == it.type && it.content != null ->
                                if (it.content!!.length > 30) it.content!!.substring(0, 30) + "..." else it.content!!
                            ChatMessage.TYPE_IMAGE == it.type || ChatMessage.TYPE_GIF == it.type -> "[Image]"
                            ChatMessage.TYPE_VIDEO == it.type -> "[Video]"
                            ChatMessage.TYPE_FILE == it.type -> "[File: ${it.fileName ?: ""}]"
                            ChatMessage.TYPE_POLL == it.type && it.poll != null -> "[Poll: ${it.poll!!.question ?: ""}]"
                            else -> "[Message]"
                        }
                        replyHeader.text = getString(R.string.replying_to_user, senderName) + ": " + contentPreview
                        replyHeader.visibility = View.VISIBLE
                        postInput.setTag(R.id.tag_reply_to_message_id, it.messageId)
                    }
                }
            }
    }

    override fun onPollVote(pollId: String?, optionIndex: Int) {
        val currentChatId = chatId
        val localCurrentUserId = currentUserId
        if (currentChatId == null || pollId == null || localCurrentUserId == null) return

        val messageRef = db.collection("chats").document(currentChatId)
            .collection("messages").document(pollId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(messageRef)
            val message = snapshot.toObject(ChatMessage::class.java)
            val poll = message?.poll
            if (message == null || poll == null || poll.options.isNullOrEmpty() || poll.isVoted || poll.isExpired) {
                return@runTransaction null // لا تقم بأي شيء
            }
            if (optionIndex < 0 || optionIndex >= poll.options!!.size) {
                Log.e(TAG, "Invalid poll option index: $optionIndex")
                return@runTransaction null
            }

            val options = poll.options!!
            val selectedOption = options[optionIndex]
            selectedOption.votes += 1 // زيادة عدد الأصوات
            poll.totalVotes += 1
            poll.isVoted = true // تم التصويت (يمكن تخصيص هذا لكل مستخدم إذا لزم الأمر)

            transaction.update(messageRef, "poll", poll)
            null // يجب أن تعيد Transaction.Function<Void> قيمة Void (أي null في Kotlin)
        }.addOnSuccessListener { Log.d(TAG, "Poll vote successful for option $optionIndex") }
            .addOnFailureListener { e -> Log.e(TAG, "Poll vote failed", e) }
    }

    override fun onFileClick(fileUrl: String?) {
        if (!fileUrl.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
            try {
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "No application can handle this file type.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "No activity found to handle file URL: $fileUrl", e)
            }
        } else {
            Toast.makeText(this, "File URL is invalid.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessageContextMenu(message: ChatMessage, anchorView: View) {
        ContextMenuDialog(this, anchorView, message, MessageMenuListener(message)).show()
    }

    private inner class MessageMenuListener(private val message: ChatMessage) : MessageContextMenuListener {
        override fun onReplySelected() { onReplyClick(message.messageId) }
        override fun onEditSelected() { showEditMessageDialog(message) }
        override fun onDeleteSelected() { showDeleteOptionsDialog(message) }
        override fun onReactionSelected() { showReactionPicker(message) }
        override fun onReportSelected(msg: ChatMessage) {
            val intent = Intent(this@ChatActivity, ReportActivity::class.java).apply {
                putExtra(ReportActivity.EXTRA_REPORTED_ITEM_ID, msg.messageId)
                putExtra(ReportActivity.EXTRA_REPORT_TYPE, "message")
                putExtra(ReportActivity.EXTRA_REPORTED_AUTHOR_ID, msg.senderId)
            }
            startActivity(intent)
        }
    }

    private fun showDeleteOptionsDialog(message: ChatMessage) {
        val localCurrentUserId = currentUserId ?: return
        val isCurrentUserMessage = message.senderId == localCurrentUserId
        val options = mutableListOf<String>()

        if (isCurrentUserMessage) {
            options.add("Delete for everyone")
        }
        options.add("Delete for me")

        MaterialAlertDialogBuilder(this)
            .setTitle("Delete message")
            .setItems(options.toTypedArray()) { _, which ->
                val deleteForAll = isCurrentUserMessage && which == 0
                deleteMessage(message, deleteForAll)
            }
            .show()
    }

    private fun showReactionPicker(message: ChatMessage) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.reaction_picker, null)
        dialog.setContentView(view)
        val grid: GridLayout? = view.findViewById(R.id.reaction_grid) // تأكد من وجود هذا الـ ID
        val reactions = arrayOf("❤️", "😂", "😮", "😢", "👍", "👎")

        grid?.let {
            for (reaction in reactions) {
                val emojiView = LayoutInflater.from(this)
                    .inflate(R.layout.item_emoji_reaction, grid, false) as TextView
                emojiView.text = reaction
                emojiView.setOnClickListener {
                    addReaction(message, reaction)
                    dialog.dismiss()
                }
                grid.addView(emojiView)
            }
        }
        dialog.show()
    }

    private fun addReaction(message: ChatMessage, reaction: String) {
        val currentChatId = chatId
        val msgId = message.messageId
        val localCurrentUserId = currentUserId
        if (currentChatId == null || msgId == null || localCurrentUserId == null) {
            Log.e(TAG, "Cannot add reaction, critical ID is null.")
            return
        }
        db.collection("chats").document(currentChatId).collection("messages")
            .document(msgId)
            .update("reactions.$localCurrentUserId", reaction) // استخدام dot notation لتحديث حقل في map
    }

    private fun deleteMessage(message: ChatMessage, forEveryone: Boolean) {
        val messageId = message.messageId
        val currentChatId = chatId
        val localCurrentUserId = currentUserId
        if (messageId == null || currentChatId == null || localCurrentUserId == null) {
            Log.e(TAG, "Cannot delete message, critical ID is null.")
            return
        }

        if (forEveryone) {
            db.collection("chats").document(currentChatId).collection("messages")
                .document(messageId)
                .update("deleted", true)
                .addOnSuccessListener { showToast("Message deleted for everyone.") }
                .addOnFailureListener { e -> Log.e(TAG, "Delete for everyone failed for message: $messageId", e) }
        } else {
            db.collection("users").document(localCurrentUserId).collection("deleted_messages")
                .document(messageId)
                .set(Collections.singletonMap("chatId", currentChatId)) // استخدام mapOf() في Kotlin
                // .set(mapOf("chatId" to currentChatId)) // الطريقة المفضلة في Kotlin
                .addOnSuccessListener { showToast("Message deleted for you.") }
                .addOnFailureListener { e -> Log.e(TAG, "Delete for me failed for message: $messageId", e) }
        }
    }

    private fun clearReplyUI() {
        replyHeader.visibility = View.GONE
        replyHeader.text = ""
        postInput.setTag(R.id.tag_reply_to_message_id, null)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
        userDeletedMessagesListener?.remove()
        chatDetailsListener?.remove()
    }

    private fun clearMediaPreview() {
        mediaPreviewContainer.visibility = View.GONE
        currentMediaUri = null
        mediaPreviewImage?.let {
            if (!isDestroyed && !isFinishing) {
                Glide.with(this).clear(it)
            }
        }
        updateSendButtonState()
        toggleSendButtonVisibility()
    }

    // يمكن تحويل Utils إلى object أو وضعه في ملف منفصل كـ top-level functions
    object Utils {
        fun hideKeyboard(activity: AppCompatActivity) {
            val view = activity.currentFocus
            view?.let {
                val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }
    }
}

