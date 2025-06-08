package com.spidroid.starry.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import java.util.*

class ChatActivity : AppCompatActivity(), MessageClickListener, MessageContextMenuListener, PollDialog.OnPollCreatedListener {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val auth: FirebaseAuth by lazy { Firebase.auth }

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

    private fun handleAuthError() {
        Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupInputLayout()
        setupAppBar()
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

    private fun setupAppBar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.currentUserModel.observe(this) { user ->
            if (user == null && auth.currentUser != null) {
                Toast.makeText(this, "Failed to load your user data.", Toast.LENGTH_LONG).show()
            }
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

        viewModel.typingPartner.observe(this) { user ->
            binding.tvTypingIndicator.visibility = if (user != null) View.VISIBLE else View.GONE
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
                is UiState.SuccessWithData -> {
                    showProgress(false)
                }
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

    private fun showEditMessageDialog(message: ChatMessage) {
        val container = android.widget.FrameLayout(this).apply {
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
        }
        val editText = EditText(this).apply {
            setText(message.content)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        container.addView(editText)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty() && newContent != message.content) {
                    if (message.messageId.isNullOrEmpty() || chatId.isNullOrEmpty()) {
                        Toast.makeText(this, "Cannot edit this message.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    viewModel.editMessage(chatId!!, message.messageId!!, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Implement all listener methods ---

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
                if (chatId.isNullOrEmpty() || message.messageId.isNullOrEmpty()) {
                    Toast.makeText(this, "Error deleting message.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.deleteMessage(chatId!!, message.messageId!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onEditMessage(message: ChatMessage) { showEditMessageDialog(message) }
    override fun onReportMessage(message: ChatMessage) { Toast.makeText(this, "Report feature coming soon!", Toast.LENGTH_SHORT).show() }
    override fun onReplyToMessage(message: ChatMessage) {
        messageToReply = message
        binding.inputSection.replyPreviewContainer.visibility = View.VISIBLE
        binding.inputSection.replyPreviewContainer.findViewById<TextView>(R.id.tv_reply_to_name).text = "Replying to ${message.senderName ?: "Someone"}"
        binding.inputSection.replyPreviewContainer.findViewById<TextView>(R.id.tv_reply_preview_text).text = message.content ?: "an attachment"
        binding.inputSection.postInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(binding.inputSection.postInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    override fun onTranslateMessage(message: ChatMessage) { Toast.makeText(this, "Translate feature coming soon!", Toast.LENGTH_SHORT).show() }
    override fun onMediaClick(mediaUrl: String, position: Int) {
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putStringArrayListExtra("media_urls", arrayListOf(mediaUrl))
            putExtra("position", 0)
        }
        startActivity(intent)
    }
    override fun onReplyClick(messageId: String) { /* Not implemented yet */ }
    override fun onPollVote(messageId: String, optionIndex: Int) {
        viewModel.castVote(chatId!!, messageId, optionIndex)
    }
    override fun onFileClick(fileUrl: String) { /* Not implemented yet */ }

    // --- Helper functions ---

    private fun bindGroupHeader(chat: Chat) {
        binding.tvAppName.text = chat.groupName ?: "Group"
        Glide.with(this).load(chat.groupImage).placeholder(R.drawable.ic_default_group).into(binding.ivAvatar)
        binding.ivVerified.visibility = View.GONE
    }

    private fun bindUserHeader(user: UserModel) {
        binding.tvAppName.text = user.displayName ?: user.username
        Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_default_avatar).into(binding.ivAvatar)
        binding.ivVerified.isVisible = user.isVerified
    }

    private fun updateSendButtonState() {
        val hasText = binding.inputSection.postInput.text.trim().isNotEmpty()
        val hasMedia = currentMediaUri != null
        val canSend = (hasText || hasMedia) && currentUserModel != null
        binding.inputSection.btnSend.isEnabled = canSend
        binding.inputSection.btnSend.isVisible = canSend
        binding.inputSection.btnRecord.isVisible = !canSend
    }

    private fun clearInput() {
        binding.inputSection.postInput.text.clear()
        clearMediaPreview()
        cancelReply()
    }

    private fun cancelReply() {
        messageToReply = null
        binding.inputSection.replyPreviewContainer.visibility = View.GONE
    }

    private fun showMediaPreview(uri: Uri) {
        val mediaPreviewBinding = binding.inputSection.mediaPreview
        mediaPreviewBinding.root.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(mediaPreviewBinding.ivMedia)
        mediaPreviewBinding.btnRemove.setOnClickListener { clearMediaPreview() }
        updateSendButtonState()
    }

    private fun clearMediaPreview() {
        binding.inputSection.mediaPreview.root.visibility = View.GONE
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

    private fun scrollToBottom() {
        if (messageAdapter.itemCount > 0) {
            binding.recyclerView.post {
                binding.recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
            }
        }
    }

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