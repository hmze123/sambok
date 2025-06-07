package com.spidroid.starry.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityChatBinding
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.ChatMessage
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.messages.*
import com.spidroid.starry.viewmodels.ChatViewModel
import com.spidroid.starry.viewmodels.UiState

// --- السطر الذي تم تعديله ---
class ChatActivity : AppCompatActivity(), MessageClickListener, MessageContextMenuListener, PollDialog.OnPollCreatedListener {

    private lateinit var binding: ActivityChatBinding
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val viewModel: ChatViewModel by viewModels()

    private var currentUserId: String? = null
    private var chatId: String? = null
    private var isGroupChat = false

    private lateinit var messageAdapter: MessageAdapter
    private var currentUserModel: UserModel? = null
    private var currentMediaUri: Uri? = null
    private var messageToReply: ChatMessage? = null

    private val typingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var typingRunnable: Runnable? = null

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
        observeViewModel()
        viewModel.loadCurrentUserData()
        viewModel.loadChatDetails(chatId!!, isGroupChat)
        viewModel.listenForMessages(chatId!!)
    }

    private fun observeViewModel() {
        viewModel.currentUserModel.observe(this) { user ->
            if (user == null) { Toast.makeText(this, "Failed to load your user data.", Toast.LENGTH_LONG).show() }
            this.currentUserModel = user
            updateSendButtonState()
        }
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages) { scrollToBottom() }
        }
        viewModel.chatPartner.observe(this) { user ->
            user?.let { bindUserHeader(it) }
        }
        viewModel.groupDetails.observe(this) { chat ->
            chat?.let { bindGroupHeader(it) }
        }
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> showProgress(true)
                is UiState.Error -> {
                    showProgress(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is UiState.Success -> {
                    showProgress(false)
                    clearInput()
                }
                else -> { /* No-op */ }
            }
        }
        viewModel.typingPartner.observe(this) { user ->
            binding.tvTypingIndicator.visibility = if (user != null) View.VISIBLE else View.GONE
        }
    }

    private fun setupUI() {
        setupRecyclerView()
        setupInputLayout()
        setupAppBar()
    }

    private fun handleAuthError() {
        Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUserId!!, this, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = messageAdapter
            setPadding(0, 0, 0, 16)
            clipToPadding = false
        }
    }

    private fun setupInputLayout() {
        with(binding.inputSection) {
            triggerButton.setOnClickListener { toggleMediaOptions() }
            btnSend.setOnClickListener { sendMessage() }
            addPhoto.setOnClickListener { mediaPickerLauncher.launch("image/*") }
            addVideo.setOnClickListener { mediaPickerLauncher.launch("video/*") }
            addGif.setOnClickListener { Toast.makeText(this@ChatActivity, "GIFs coming soon!", Toast.LENGTH_SHORT).show() }
            addPoll.setOnClickListener { showPollCreationDialog() }
            replyPreviewContainer.findViewById<View>(R.id.btn_cancel_reply).setOnClickListener { cancelReply() }

            postInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    typingRunnable?.let { typingHandler.removeCallbacks(it) }
                    if (s.toString().isNotEmpty()) {
                        viewModel.updateUserTypingStatus(chatId!!, true)
                    }
                }
                override fun afterTextChanged(s: Editable?) {
                    updateSendButtonState()
                    typingRunnable = Runnable { viewModel.updateUserTypingStatus(chatId!!, false) }
                    typingHandler.postDelayed(typingRunnable!!, 1500)
                }
            })
        }
    }

    private fun sendMessage() {
        val messageText = binding.inputSection.postInput.text.toString().trim()
        val mediaUri = currentMediaUri

        if (messageText.isEmpty() && mediaUri == null) return

        val user = currentUserModel
        if (user == null) {
            Toast.makeText(this, "User data not loaded, please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = ChatMessage(
            senderId = user.userId,
            senderName = user.displayName ?: user.username,
            senderAvatar = user.profileImageUrl,
            content = if (messageText.isNotBlank()) messageText else null,
            type = if (mediaUri != null) {
                if (contentResolver.getType(mediaUri)?.startsWith("video/") == true) ChatMessage.TYPE_VIDEO else ChatMessage.TYPE_IMAGE
            } else {
                ChatMessage.TYPE_TEXT
            }
        ).apply {
            messageToReply?.let {
                this.replyToId = it.messageId
                this.replyPreview = it.content ?: "Attachment"
            }
        }
        viewModel.sendMessage(chatId!!, message, mediaUri)
    }

    private fun showPollCreationDialog() {
        val pollDialog = PollDialog()
        pollDialog.setOnPollCreatedListener(this)
        pollDialog.show(supportFragmentManager, "PollDialog")
    }

    // --- تطبيق دوال الواجهات ---

    override fun onPollCreated(poll: ChatMessage.Poll) {
        val user = currentUserModel ?: return
        val message = ChatMessage(
            senderId = user.userId,
            senderName = user.displayName ?: user.username,
            senderAvatar = user.profileImageUrl,
            type = ChatMessage.TYPE_POLL,
            poll = poll
        )
        viewModel.sendMessage(chatId!!, message, null)
    }

    override fun onPollVote(messageId: String, optionIndex: Int) {
        viewModel.castVote(chatId!!, messageId, optionIndex)
    }

    override fun onMessageLongClick(message: ChatMessage, position: Int) {
        if (message.uploading) return
        val dialog = ContextMenuDialog.newInstance(message)
        dialog.setListener(this)
        dialog.show(supportFragmentManager, "MessageContextMenu")
    }

    override fun onDeleteMessage(message: ChatMessage) {
        if (message.senderId != currentUserId) {
            Toast.makeText(this, "You can only delete your own messages.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message? This cannot be undone.")
            .setPositiveButton("Delete for Everyone") { _, _ ->
                if (chatId.isNullOrEmpty() || message.messageId.isNullOrEmpty()) { return@setPositiveButton }
                viewModel.deleteMessage(chatId!!, message.messageId!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onEditMessage(message: ChatMessage) { showEditMessageDialog(message) }
    override fun onReportMessage(message: ChatMessage) { /* ... */ }
    override fun onReplyToMessage(message: ChatMessage) {
        messageToReply = message
        binding.inputSection.replyPreviewContainer.visibility = View.VISIBLE
        binding.inputSection.replyPreviewContainer.findViewById<TextView>(R.id.tv_reply_to_name).text = "Replying to ${message.senderName ?: "Someone"}"
        binding.inputSection.replyPreviewContainer.findViewById<TextView>(R.id.tv_reply_preview_text).text = message.content ?: "an attachment"
        binding.inputSection.postInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(binding.inputSection.postInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    override fun onTranslateMessage(message: ChatMessage) { /* ... */ }
    override fun onMediaClick(mediaUrl: String, position: Int) { /* ... */ }
    override fun onReplyClick(messageId: String) { /* ... */ }
    override fun onFileClick(fileUrl: String) { /* ... */ }

    // ... (بقية الدوال المساعدة تبقى كما هي)
    private fun showEditMessageDialog(message: ChatMessage) { /* ... */ }
    private fun setupAppBar() { /* ... */ }
    private fun bindGroupHeader(chat: Chat) { /* ... */ }
    private fun bindUserHeader(user: UserModel) { /* ... */ }
    private fun updateSendButtonState() { /* ... */ }
    private fun clearInput() { /* ... */ }
    private fun cancelReply() {
        messageToReply = null
        binding.inputSection.replyPreviewContainer.visibility = View.GONE
    }
    private fun showMediaPreview(uri: Uri) { /* ... */ }
    private fun clearMediaPreview() { /* ... */ }
    private fun toggleMediaOptions() { /* ... */ }
    private fun showProgress(show: Boolean) { /* ... */ }
    private fun scrollToBottom() { /* ... */ }

    override fun onDestroy() {
        super.onDestroy()
        typingRunnable?.let { typingHandler.removeCallbacks(it) }
        chatId?.let { safeChatId ->
            if (auth.currentUser != null) {
                viewModel.updateUserTypingStatus(safeChatId, false)
            }
        }
    }

    companion object {
        private const val TAG = "ChatActivity"
    }
}