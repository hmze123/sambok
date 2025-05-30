package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.spidroid.starry.R;
import com.spidroid.starry.models.ChatMessage;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.ui.messages.ContextMenuDialog;
import com.spidroid.starry.ui.messages.MessageContextMenuListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.GridLayout;
import com.spidroid.starry.ui.messages.MessageAdapter;
import com.spidroid.starry.ui.messages.PollDialog;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public class ChatActivity extends AppCompatActivity implements MessageAdapter.MessageClickListener {

  // Constants
  private static final int MAX_MESSAGE_LENGTH = 2000;
  private static final String TAG = "ChatActivity";
  private Set<String> userDeletedMessageIds = new HashSet<>();
  private ListenerRegistration userDeletedMessagesListener;

  // Firebase
  private FirebaseFirestore db;
  private FirebaseStorage storage;
  private String currentUserId;
  private ListenerRegistration messagesListener;

  // Views
  private ImageButton btnSend, btnAddMedia, btnAddGif, btnAddPoll, btnMenu, triggerButton;
  private EditText postInput;
  private FrameLayout mediaPreviewContainer;
  private ImageView mediaPreviewImage;
  private ProgressBar progressBar;
  private RecyclerView messagesRecycler;
  private MessageAdapter messageAdapter;
  private LinearLayout bottomToolbar;
  private TextView replyHeader, tvDisplayName;
  private CircleImageView ivAvatar;
  private ImageView ivVerified;
  private ConstraintLayout mainContent;

  // State
  private String chatId;
  private boolean isGroupChat = false;
  private Uri currentMediaUri;

  // Contracts
  private final ActivityResultLauncher<String> mediaPicker =
          registerForActivityResult(
                  new ActivityResultContracts.GetContent(),
                  uri -> {
                    if (uri != null) {
                      currentMediaUri = uri;
                      showMediaPreview(uri);
                    }
                  });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_chat);

    initializeFirebase();
    initializeViews();
    setupRecyclerView();
    setupInputBehavior();
    setupAppBar();
    loadChatDetails();
  }

  private void initializeFirebase() {
    db = FirebaseFirestore.getInstance();
    storage = FirebaseStorage.getInstance();
    currentUserId = FirebaseAuth.getInstance().getUid();

    if (currentUserId == null) {
      Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
      finish();
    }

    // Listen for user's deleted messages
    if (currentUserId != null) {
      userDeletedMessagesListener =
              db.collection("users")
                      .document(currentUserId)
                      .collection("deleted_messages")
                      .addSnapshotListener(
                              (snapshots, error) -> {
                                if (error != null) {
                                  Log.e(TAG, "Error listening to deleted messages", error);
                                  return;
                                }

                                userDeletedMessageIds.clear();
                                for (QueryDocumentSnapshot doc : snapshots) {
                                  userDeletedMessageIds.add(doc.getId());
                                }
                              });
    }
  }

  private void initializeViews() {
    // App bar views
    tvDisplayName = findViewById(R.id.tv_app_name);
    ivAvatar = findViewById(R.id.ivAvatar);
    ivVerified = findViewById(R.id.iv_verified);
    btnMenu = findViewById(R.id.btnMenu);

    // Input area
    btnSend = findViewById(R.id.btnSend);
    postInput = findViewById(R.id.postInput);
    mediaPreviewContainer = findViewById(R.id.mediaPreview);
    mediaPreviewImage = mediaPreviewContainer.findViewById(R.id.ivMedia);
    progressBar = findViewById(R.id.progressBar);
    bottomToolbar = findViewById(R.id.bottomToolbar);
    replyHeader = findViewById(R.id.replyHeader);
    mainContent = findViewById(R.id.mainContent);
    triggerButton = findViewById(R.id.triggerButton);

    // Media buttons
    btnAddMedia = findViewById(R.id.addPhoto);
    btnAddGif = findViewById(R.id.addGif);
    btnAddPoll = findViewById(R.id.addPoll);
    ImageButton btnRemoveMedia = mediaPreviewContainer.findViewById(R.id.btnRemove);
    btnRemoveMedia.setOnClickListener(v -> clearMediaPreview());
    ImageButton btnAddVideo = findViewById(R.id.addVideo);
    btnAddVideo.setOnClickListener(v -> mediaPicker.launch("video/*"));

    // Get intent data
    Intent intent = getIntent();
    isGroupChat = intent.getBooleanExtra("isGroup", false);
    chatId = intent.getStringExtra("chatId");

    if (chatId == null || chatId.isEmpty()) {
      Toast.makeText(this, "Invalid chat", Toast.LENGTH_SHORT).show();
      finish();
    }

    btnAddPoll.setVisibility(isGroupChat ? View.VISIBLE : View.GONE);
  }

  private void setupRecyclerView() {
    messagesRecycler = findViewById(R.id.recyclerView);
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    messagesRecycler.setLayoutManager(layoutManager);
    messageAdapter = new MessageAdapter(currentUserId, this);
    messagesRecycler.setAdapter(messageAdapter);

    // Real-time message listener
    messagesListener =
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener(
                            (snapshots, error) -> {
                              if (error != null) {
                                Log.e(TAG, "Messages listen failed", error);
                                return;
                              }

                              List<ChatMessage> messages = new ArrayList<>();
                              for (QueryDocumentSnapshot doc : snapshots) {
                                boolean isDeleted = Boolean.TRUE.equals(doc.getBoolean("deleted"));
                                boolean isDeletedForMe = userDeletedMessageIds.contains(doc.getId());
                                if (!isDeleted && !isDeletedForMe) {
                                  ChatMessage message = doc.toObject(ChatMessage.class);
                                  message.setMessageId(doc.getId()); // Ensure messageId is set
                                  messages.add(message);
                                }
                              }
                              runOnUiThread(
                                      () -> {
                                        messageAdapter.submitList(messages);
                                        scrollToBottom();
                                      });
                            });
  }

  private void setupInputBehavior() {
    triggerButton.setOnClickListener(v -> toggleMediaOptions());

    postInput.addTextChangedListener(
            new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

              @Override
              public void onTextChanged(CharSequence s, int start, int before, int count) {}

              @Override
              public void afterTextChanged(Editable s) {
                updateSendButtonState();
                toggleSendButtonVisibility();
              }
            });

    btnSend.setOnClickListener(v -> sendMessage());

    btnAddMedia.setOnClickListener(v -> mediaPicker.launch("image/*"));
    btnAddGif.setOnClickListener(v -> showGifPicker());
    btnAddPoll.setOnClickListener(v -> createPoll());

    postInput.setOnKeyListener(
            (v, keyCode, event) -> {
              if (event.getAction() == KeyEvent.ACTION_DOWN
                      && keyCode == KeyEvent.KEYCODE_ENTER
                      && !postInput.getText().toString().trim().isEmpty()) {
                sendMessage();
                return true;
              }
              return false;
            });
  }

  private void toggleSendButtonVisibility() {
    boolean hasText = !postInput.getText().toString().trim().isEmpty();
    boolean hasMedia = mediaPreviewContainer.getVisibility() == View.VISIBLE;
    boolean showSend = hasText || hasMedia;

    btnSend.setVisibility(showSend ? View.VISIBLE : View.GONE);
    findViewById(R.id.btnRecord).setVisibility(showSend ? View.GONE : View.VISIBLE);
  }

  private void updateSendButtonState() {
    boolean hasText = !postInput.getText().toString().trim().isEmpty();
    boolean hasMedia = mediaPreviewContainer.getVisibility() == View.VISIBLE;
    btnSend.setEnabled(hasText || hasMedia);
  }

  private void sendMessage() {
    String messageText = postInput.getText().toString().trim();

    if (messageText.length() > MAX_MESSAGE_LENGTH) {
      Toast.makeText(this, "Message exceeds character limit", Toast.LENGTH_SHORT).show();
      return;
    }

    if (messageText.isEmpty() && currentMediaUri == null) {
      Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
      return;
    }

    showProgress(true);

    if (currentMediaUri != null) {
      uploadMediaAndSendMessage(messageText);
    } else {
      ChatMessage message = new ChatMessage(currentUserId, messageText);
      message.setEdited(false);
      message.setTimestamp(new Date());
      saveMessageToFirestore(message);
    }
  }

  private void uploadMediaAndSendMessage(String messageText) {
    Uri fileUri = currentMediaUri;
    if (fileUri == null) {
      Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show();
      showProgress(false);
      return;
    }

    // Create storage reference
    StorageReference storageRef = storage.getReference();
    String fileName = "media/" + System.currentTimeMillis() + "_" + currentUserId;
    StorageReference fileRef = storageRef.child(fileName);

    // Detect MIME type and determine message type
    String mimeType = getContentResolver().getType(fileUri);
    final String messageType; // Make final for lambda usage

    if (mimeType != null) {
      if (mimeType.startsWith("video/")) {
        messageType = ChatMessage.TYPE_VIDEO;
      } else if (mimeType.equals("image/gif")) {
        messageType = ChatMessage.TYPE_GIF;
      } else {
        messageType = ChatMessage.TYPE_IMAGE;
      }
    } else {
      messageType = ChatMessage.TYPE_IMAGE;
    }

    // Upload file
    UploadTask uploadTask = fileRef.putFile(fileUri);
    uploadTask
            .addOnProgressListener(
                    taskSnapshot -> {
                      double progress =
                              (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                      Log.d(TAG, "Upload progress: " + progress + "%");
                    })
            .addOnSuccessListener(
                    taskSnapshot -> {
                      fileRef
                              .getDownloadUrl()
                              .addOnSuccessListener(
                                      uri -> {
                                        ChatMessage message = new ChatMessage(currentUserId, messageText);
                                        message.setTimestamp(new Date());
                                        message.setType(messageType);
                                        message.setMediaUrl(uri.toString());
                                        message.setThumbnailUrl(uri.toString());

                                        if (ChatMessage.TYPE_VIDEO.equals(messageType)) {
                                          // Video-specific handling
                                          message.setThumbnailUrl(uri.toString());
                                        }

                                        saveMessageToFirestore(message);
                                      });
                    })
            .addOnFailureListener(
                    e -> {
                      Log.e(TAG, "Upload failed", e);
                      Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                      showProgress(false);
                    });
  }

  private void saveMessageToFirestore(ChatMessage message) {
    db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message.toFirestoreMap())
            .addOnCompleteListener(
                    task -> {
                      showProgress(false);

                      if (task.isSuccessful()) {
                        // Set the generated message ID
                        DocumentReference docRef = task.getResult();
                        message.setMessageId(docRef.getId());
                        // Update Firestore document with messageId if necessary
                        docRef.update("messageId", docRef.getId());
                        clearInputFields();
                        updateChatLastMessage(message.getContent());
                        scrollToBottom();
                      } else {
                        Toast.makeText(
                                        ChatActivity.this,
                                        "Failed to send message: "
                                                + Objects.requireNonNull(task.getException()).getMessage(),
                                        Toast.LENGTH_SHORT)
                                .show();
                      }
                    });
  }

  private void clearInputFields() {
    postInput.setText("");
    mediaPreviewContainer.setVisibility(View.GONE);
    currentMediaUri = null;
    Glide.with(this).clear(mediaPreviewImage);
  }

  private void showEditMessageDialog(ChatMessage message) {
    // Security checks
    if (!message.getSenderId().equals(currentUserId)) {
      Toast.makeText(this, "Can't edit others' messages", Toast.LENGTH_SHORT).show();
      return;
    }

    if (!message.getType().equals(ChatMessage.TYPE_TEXT)) {
      Toast.makeText(this, "Only text messages can be edited", Toast.LENGTH_SHORT).show();
      return;
    }

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
    View dialogView = getLayoutInflater().inflate(R.layout.inputchatlayout, null);
    EditText input = dialogView.findViewById(R.id.postInput);
    input.setText(message.getContent());

    builder
            .setTitle("Edit Message")
            .setView(dialogView)
            .setPositiveButton(
                    "Save",
                    (dialog, which) -> {
                      String newText = input.getText().toString().trim();
                      if (validateEdit(message, newText)) {
                        updateMessageInFirestore(message, newText);
                      }
                    })
            .setNegativeButton("Cancel", null)
            .show();
  }

  private boolean validateEdit(ChatMessage original, String newText) {
    if (newText.isEmpty()) {
      Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
      return false;
    }

    if (newText.equals(original.getContent())) {
      Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
      return false;
    }

    if (newText.length() > MAX_MESSAGE_LENGTH) {
      Toast.makeText(this, "Message exceeds character limit", Toast.LENGTH_SHORT).show();
      return false;
    }

    return true;
  }

  private void updateMessageInFirestore(ChatMessage message, String newContent) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("content", newContent);
    updates.put("edited", true);
    updates.put("timestamp", FieldValue.serverTimestamp());

    db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(message.getMessageId())
            .update(updates)
            .addOnSuccessListener(
                    aVoid -> Toast.makeText(this, "Message updated", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(
                    e -> {
                      Log.e(TAG, "Error updating message", e);
                      Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    });
  }

  private void updateChatLastMessage(String message) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("lastMessage", message);
    updates.put("lastMessageTime", FieldValue.serverTimestamp());

    db.collection("chats")
            .document(chatId)
            .update(updates)
            .addOnFailureListener(e -> Log.e(TAG, "Error updating last message", e));
  }

  private void showProgress(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    btnSend.setEnabled(!show);
    postInput.setEnabled(!show);
  }

  private void showMediaPreview(Uri uri) {
    mediaPreviewContainer.setVisibility(View.VISIBLE);

    // Check if it's a video
    String mimeType = getContentResolver().getType(uri);
    if (mimeType != null && mimeType.startsWith("video/")) {
      // Load video thumbnail
      Glide.with(this).asBitmap().load(uri).into(mediaPreviewImage);
    } else {
      Glide.with(this)
              .load(uri)
              .placeholder(R.drawable.ic_cover_placeholder)
              .into(mediaPreviewImage);
    }

    // Add these lines to immediately update button visibility
    updateSendButtonState();
    toggleSendButtonVisibility();
  }

  private void toggleMediaOptions() {
    boolean show = bottomToolbar.getVisibility() != View.VISIBLE;
    TransitionManager.beginDelayedTransition(bottomToolbar);
    bottomToolbar.setVisibility(show ? View.VISIBLE : View.GONE);
    triggerButton.setImageResource(show ? R.drawable.ic_close : R.drawable.ic_add);
    postInput.clearFocus();
  }

  private void showGifPicker() {
    Toast.makeText(this, "GIF picker coming soon!", Toast.LENGTH_SHORT).show();
  }

  private void createPoll() {
    if (!isGroupChat) return;
    new PollDialog().show(getSupportFragmentManager(), "PollDialog");
  }

  private void scrollToBottom() {
    messagesRecycler.post(
            () -> {
              if (messageAdapter.getItemCount() > 0) {
                messagesRecycler.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
              }
            });
  }

  private void setupAppBar() {
    ImageView ivBack = findViewById(R.id.iv_back);
    ivBack.setOnClickListener(v -> finish());

    if (isGroupChat) {
      loadGroupDetails();
    } else {
      loadUserDetails();
    }
  }

  private void loadChatDetails() {
    db.collection("chats")
            .document(chatId)
            .addSnapshotListener(
                    (document, error) -> {
                      if (error != null || document == null) {
                        Log.e(TAG, "Chat details listen failed", error);
                        return;
                      }

                      if (isGroupChat) {
                        tvDisplayName.setText(document.getString("name"));
                        Glide.with(this).load(document.getString("avatarUrl")).into(ivAvatar);
                      } else {
                        List<String> participants = (List<String>) document.get("participants");
                        String otherUserId = getOtherUserId(participants);
                        loadOtherUserDetails(otherUserId);
                      }
                    });
  }

  private String getOtherUserId(List<String> participants) {
    for (String userId : participants) {
      if (!userId.equals(currentUserId)) return userId;
    }
    return "";
  }

  private void loadOtherUserDetails(String userId) {
    db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(
                    document -> {
                      UserModel user = document.toObject(UserModel.class);
                      if (user != null) {
                        tvDisplayName.setText(user.getDisplayName());
                        Glide.with(this).load(user.getProfileImageUrl()).into(ivAvatar);
                        ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
                      }
                    });
  }

  // MessageClickListener implementation
  @Override
  public void onMessageLongClick(ChatMessage message, int position) {
    showMessageContextMenu(message, position);
  }

  @Override
  public void onMediaClick(String mediaUrl, int position) {
    ArrayList<String> mediaList = new ArrayList<>();
    mediaList.add(mediaUrl);

    // Get the view holder safely
    RecyclerView.ViewHolder viewHolder =
            messagesRecycler.findViewHolderForAdapterPosition(position);
    if (viewHolder != null) {
      View mediaView = viewHolder.itemView.findViewById(R.id.imageContent);
      if (mediaView != null) {
        MediaViewerActivity.launch(this, mediaList, position, mediaView);
        return;
      }
    }

    // Fallback if view isn't available
    MediaViewerActivity.launch(this, mediaList, position, null);
  }

  @Override
  public void onReplyClick(String messageId) {
    // Implement reply functionality
  }

  @Override
  public void onPollVote(String pollId, int option) {
    // Handle poll voting
  }

  @Override
  public void onFileClick(String fileUrl) {
    // Handle file download
  }

  // Region: Placeholder methods for your existing implementations
  private void loadGroupDetails() {
    // Implement group details loading
  }

  private void loadUserDetails() {
    // Implement user details loading
  }

  private void showMessageContextMenu(ChatMessage message, int position) {
    View anchor = messagesRecycler.findViewHolderForAdapterPosition(position).itemView;
    ContextMenuDialog dialog =
            new ContextMenuDialog(this, anchor, message, new MessageMenuListener(message, position));
    dialog.show();
  }

  private class MessageMenuListener
          implements com.spidroid.starry.ui.messages.MessageContextMenuListener {

    private final ChatMessage message;
    private final int position;

    // Line 659: هذا هو دالة البناء (constructor) ولا تحتاج إلى نوع إرجاع
    public MessageMenuListener(ChatMessage message, int position) {
      this.message = message;
      this.position = position;
    }

    @Override
    public void onReplySelected() {
      showReplyUI(message);
    }

    @Override
    public void onEditSelected() {
      showEditMessageDialog(message);
    }

    @Override
    public void onDeleteSelected() {
      showDeleteOptionsDialog(message);
    }

    @Override
    public void onReactionSelected() {
      showReactionPicker(message);
    }

    // تم تعديل توقيع هذه الدالة لتتوافق مع الواجهة
    @Override
    public void onReportSelected(ChatMessage message) {
      startActivity(
              new Intent(ChatActivity.this, ReportActivity.class)
                      .putExtra("messageId", message.getMessageId()));
    }
  }

  private void showDeleteOptionsDialog(ChatMessage message) {
    boolean isCurrentUser = message.getSenderId().equals(currentUserId);
    List<String> options = new ArrayList<>();

    if (isCurrentUser) {
      options.add("Delete for everyone");
    }
    options.add("Delete for me");

    new MaterialAlertDialogBuilder(this)
            .setTitle("Delete message")
            .setItems(
                    options.toArray(new String[0]),
                    (dialog, which) -> {
                      boolean deleteForAll = isCurrentUser && which == 0;
                      deleteMessage(message, deleteForAll);
                    })
            .show();
  }

  private void showReactionPicker(ChatMessage message) {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View view = getLayoutInflater().inflate(R.layout.reaction_picker, null);
    dialog.setContentView(view);

    GridLayout grid = view.findViewById(R.id.reaction_grid);
    String[] reactions = {"❤️", "😂", "😮", "😢", "👍", "👎"};

    for (String reaction : reactions) {
      TextView emojiView = new TextView(this);
      emojiView.setText(reaction);
      emojiView.setTextSize(24);
      emojiView.setOnClickListener(
              v -> {
                addReaction(message, reaction);
                dialog.dismiss();
              });
      grid.addView(emojiView);
    }

    dialog.show();
  }

  private void addReaction(ChatMessage message, String reaction) {
    DocumentReference messageRef =
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(message.getMessageId());

    messageRef.update("reactions." + currentUserId, reaction);
  }

  private void deleteMessage(ChatMessage message, boolean forEveryone) {
    String messageId = message.getMessageId();
    if (messageId == null) {
      Log.e(TAG, "Message ID is null");
      return;
    }

    if (forEveryone) {
      // Delete for everyone
      db.collection("chats")
              .document(chatId)
              .collection("messages")
              .document(messageId)
              .update("deleted", true)
              .addOnSuccessListener(aVoid -> showToast("Message deleted for everyone"))
              .addOnFailureListener(e -> Log.e(TAG, "Delete for everyone failed", e));
    } else {
      // Delete for me
      db.collection("users")
              .document(currentUserId)
              .collection("deleted_messages")
              .document(messageId)
              .set(Collections.singletonMap("deleted", true)) // Store minimal data
              .addOnSuccessListener(aVoid -> showToast("Message deleted for you"))
              .addOnFailureListener(e -> Log.e(TAG, "Delete for me failed", e));
    }
  }

  private void showReplyUI(ChatMessage message) {
    // Implement reply UI
    replyHeader.setVisibility(View.VISIBLE);
    replyHeader.setText("Replying to: " + message.getContent());
    // Store reply reference
  }

  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    if (messagesListener != null) {
      messagesListener.remove();
    }
    if (userDeletedMessagesListener != null) {
      userDeletedMessagesListener.remove();
    }
    super.onDestroy();
  }

  private void clearMediaPreview() {
    mediaPreviewContainer.setVisibility(View.GONE);
    currentMediaUri = null;
    Glide.with(this).clear(mediaPreviewImage);
    updateSendButtonState();
    toggleSendButtonVisibility();
  }
}