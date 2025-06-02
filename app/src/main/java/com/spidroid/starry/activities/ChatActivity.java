package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.annotation.Nullable; // ★ إضافة هذا إذا كان ناقصًا
import androidx.appcompat.app.AppCompatActivity;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.spidroid.starry.R;
import com.spidroid.starry.models.Chat;
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
import java.util.UUID;
import androidx.recyclerview.widget.LinearLayoutManager; // ★ تأكد من وجود هذا
import androidx.recyclerview.widget.RecyclerView;      // ★ تأكد من وجود هذا


public class ChatActivity extends AppCompatActivity implements MessageAdapter.MessageClickListener {

  private static final int MAX_MESSAGE_LENGTH = 2000;
  private static final String TAG = "ChatActivity";
  private Set<String> userDeletedMessageIds = new HashSet<>();
  private ListenerRegistration userDeletedMessagesListener;
  private ListenerRegistration chatDetailsListener;

  private FirebaseFirestore db;
  private FirebaseStorage storage;
  private FirebaseAuth auth;
  private String currentUserId;
  private UserModel currentUserModel;
  private ListenerRegistration messagesListener;

  private ImageButton btnSend, btnAddMedia, btnAddVideo, btnAddGif, btnAddPoll, btnMenu, triggerButton;
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

  private String chatId;
  private boolean isGroupChat = false;
  private Uri currentMediaUri;

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

    auth = FirebaseAuth.getInstance();
    initializeFirebase();

    if (currentUserId == null) {
      Log.e(TAG, "Current user ID is null after initializeFirebase. Finishing activity.");
      Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    initializeViews();

    if (chatId == null || chatId.isEmpty()) {
      Toast.makeText(this, getString(R.string.invalid_chat_id_error), Toast.LENGTH_LONG).show();
      Log.e(TAG, "Chat ID is null or empty in onCreate. Finishing activity.");
      finish();
      return;
    }

    loadCurrentUserData();
    setupRecyclerView();
    setupInputBehavior();
    setupAppBar();
    loadChatDetails();
  }

  private void initializeFirebase() {
    db = FirebaseFirestore.getInstance();
    storage = FirebaseStorage.getInstance();
    FirebaseUser user = auth.getCurrentUser();
    if (user != null) {
      currentUserId = user.getUid();
    } else {
      if (!isFinishing()) {
        Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Current user is null in initializeFirebase (called from onCreate).");
        finishActivityCleanup();
      }
      return;
    }

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
                              if (snapshots != null) {
                                userDeletedMessageIds.clear();
                                for (QueryDocumentSnapshot doc : snapshots) {
                                  userDeletedMessageIds.add(doc.getId());
                                }
                              }
                            });
  }

  private void finishActivityCleanup() {
    finish();
  }


  private void loadCurrentUserData() {
    if (currentUserId == null) return;
    db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot.exists()) {
                currentUserModel = documentSnapshot.toObject(UserModel.class);
                if (currentUserModel == null) {
                  Log.e(TAG, "currentUserModel is null after deserialization from Firestore.");
                  Toast.makeText(this, "Failed to load your profile data.", Toast.LENGTH_SHORT).show();
                } else {
                  Log.d(TAG, "Current user data loaded: " + (currentUserModel.getUsername() != null ? currentUserModel.getUsername() : "N/A"));
                  updateSendButtonState();
                }
              } else {
                Log.e(TAG, "Current user document does not exist in Firestore.");
                Toast.makeText(this, "Your profile data not found.", Toast.LENGTH_SHORT).show();
              }
            })
            .addOnFailureListener(e -> {
              Log.e(TAG, "Failed to load current user data.", e);
              Toast.makeText(this, "Error loading your profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
  }


  private void initializeViews() {
    tvDisplayName = findViewById(R.id.tv_app_name);
    ivAvatar = findViewById(R.id.ivAvatar);
    ivVerified = findViewById(R.id.iv_verified);
    btnMenu = findViewById(R.id.btnMenu);
    btnSend = findViewById(R.id.btnSend);
    postInput = findViewById(R.id.postInput);
    mediaPreviewContainer = findViewById(R.id.mediaPreview);
    if (mediaPreviewContainer != null) {
      mediaPreviewImage = mediaPreviewContainer.findViewById(R.id.ivMedia);
      ImageButton btnRemoveMedia = mediaPreviewContainer.findViewById(R.id.btnRemove);
      if (btnRemoveMedia != null) {
        btnRemoveMedia.setOnClickListener(v -> clearMediaPreview());
      }
    }
    progressBar = findViewById(R.id.progressBar); // افترض وجود ProgressBar بال ID progressBar
    bottomToolbar = findViewById(R.id.bottomToolbar);
    replyHeader = findViewById(R.id.replyHeader);
    triggerButton = findViewById(R.id.triggerButton);
    btnAddMedia = findViewById(R.id.addPhoto);
    btnAddVideo = findViewById(R.id.addVideo);
    btnAddGif = findViewById(R.id.addGif);
    btnAddPoll = findViewById(R.id.addPoll);

    if (btnAddVideo != null) {
      btnAddVideo.setOnClickListener(v -> mediaPicker.launch("video/*"));
    }

    Intent intent = getIntent();
    isGroupChat = intent.getBooleanExtra("isGroup", false);
    chatId = intent.getStringExtra("chatId");

    if (btnAddPoll != null) {
      btnAddPoll.setVisibility(isGroupChat ? View.VISIBLE : View.GONE);
    }
  }


  private void setupRecyclerView() {
    messagesRecycler = findViewById(R.id.recyclerView);
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    messagesRecycler.setLayoutManager(layoutManager);
    messageAdapter = new MessageAdapter(currentUserId, this, this);
    messagesRecycler.setAdapter(messageAdapter);

    if (chatId == null || chatId.isEmpty()) {
      Log.e(TAG, "Cannot setup messages listener, chatId is null or empty for RecyclerView.");
      return;
    }
    if (messagesListener != null) {
      messagesListener.remove();
    }
    messagesListener =
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener(
                            (snapshots, error) -> {
                              if (error != null) {
                                Log.e(TAG, "Messages listen failed for chatId: " + chatId, error);
                                return;
                              }
                              List<ChatMessage> messages = new ArrayList<>();
                              if (snapshots != null) {
                                for (QueryDocumentSnapshot doc : snapshots) {
                                  boolean isDeleted = Boolean.TRUE.equals(doc.getBoolean("deleted"));
                                  String docId = doc.getId();
                                  boolean isDeletedForMe = userDeletedMessageIds.contains(docId);

                                  if (!isDeleted && !isDeletedForMe) {
                                    ChatMessage message = doc.toObject(ChatMessage.class);
                                    message.setMessageId(docId);
                                    Log.d(TAG, "Fetched message: ID=" + message.getMessageId() + ", Content=[" + message.getContent() + "], SenderName=[" + message.getSenderName()+"]");
                                    messages.add(message);
                                  }
                                }
                              }
                              runOnUiThread(() -> {
                                if (messageAdapter != null) {
                                  messageAdapter.submitList(messages);
                                  scrollToBottom();
                                }
                              });
                            });
  }

  private void setupInputBehavior() {
    if (triggerButton != null) triggerButton.setOnClickListener(v -> toggleMediaOptions());
    if (postInput == null) return;

    postInput.addTextChangedListener(
            new TextWatcher() {
              @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
              @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
              @Override public void afterTextChanged(Editable s) {
                updateSendButtonState();
                toggleSendButtonVisibility();
              }
            });

    if (btnSend != null) btnSend.setOnClickListener(v -> sendMessage());
    if (btnAddMedia != null) btnAddMedia.setOnClickListener(v -> mediaPicker.launch("image/*"));
    // btnAddVideo listener is already set in initializeViews
    if (btnAddGif != null) btnAddGif.setOnClickListener(v -> showGifPicker());
    if (btnAddPoll != null) btnAddPoll.setOnClickListener(v -> createPoll());

    postInput.setOnKeyListener(
            (v, keyCode, event) -> {
              if (postInput != null && event.getAction() == KeyEvent.ACTION_DOWN
                      && keyCode == KeyEvent.KEYCODE_ENTER
                      && !postInput.getText().toString().trim().isEmpty()) {
                sendMessage();
                return true;
              }
              return false;
            });
  }

  private void toggleSendButtonVisibility() {
    View btnRecordView = findViewById(R.id.btnRecord);
    if (postInput == null || mediaPreviewContainer == null || btnSend == null || btnRecordView == null) return;

    boolean hasText = !postInput.getText().toString().trim().isEmpty();
    boolean hasMedia = mediaPreviewContainer.getVisibility() == View.VISIBLE;
    boolean showSend = hasText || hasMedia;

    btnSend.setVisibility(showSend ? View.VISIBLE : View.GONE);
    btnRecordView.setVisibility(showSend ? View.GONE : View.VISIBLE);
  }

  private void updateSendButtonState() {
    if (postInput == null || mediaPreviewContainer == null || btnSend == null) return;
    boolean hasText = !postInput.getText().toString().trim().isEmpty();
    boolean hasMedia = mediaPreviewContainer.getVisibility() == View.VISIBLE;
    btnSend.setEnabled((hasText || hasMedia) && currentUserModel != null);
  }

  private void sendMessage() {
    if (postInput == null) return;
    String messageText = postInput.getText().toString().trim();
    Object replyToTag = postInput.getTag(R.id.tag_reply_to_message_id);
    String replyToMessageId = (replyToTag instanceof String) ? (String) replyToTag : null;

    if (messageText.length() > MAX_MESSAGE_LENGTH) {
      Toast.makeText(this, "Message exceeds character limit (" + MAX_MESSAGE_LENGTH + ")", Toast.LENGTH_SHORT).show();
      return;
    }
    if (messageText.isEmpty() && currentMediaUri == null) {
      Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
      return;
    }
    if (currentUserId == null) {
      Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_SHORT).show();
      return;
    }
    if (currentUserModel == null) {
      Toast.makeText(this, "User data not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
      loadCurrentUserData();
      return;
    }

    showProgress(true);

    if (currentMediaUri != null) {
      uploadMediaAndSendMessage(messageText, replyToMessageId);
    } else {
      ChatMessage message = new ChatMessage(currentUserId, messageText);
      message.setSenderName(currentUserModel.getDisplayName() != null && !currentUserModel.getDisplayName().isEmpty() ? currentUserModel.getDisplayName() : currentUserModel.getUsername());
      message.setSenderAvatar(currentUserModel.getProfileImageUrl());
      message.setEdited(false);
      if (replyToMessageId != null && replyHeader != null && replyHeader.getText() != null) {
        message.setReplyToId(replyToMessageId);
        message.setReplyPreview(replyHeader.getText().toString());
      }
      saveMessageToFirestore(message);
    }
  }

  private void uploadMediaAndSendMessage(String messageText, @Nullable String replyToMessageId) {
    Uri fileUri = currentMediaUri;
    if (fileUri == null) {
      Toast.makeText(this, "No media selected", Toast.LENGTH_SHORT).show();
      showProgress(false);
      return;
    }
    if (currentUserId == null || currentUserModel == null) {
      Toast.makeText(this, getString(R.string.authentication_required_error), Toast.LENGTH_SHORT).show();
      showProgress(false);
      return;
    }

    StorageReference storageRef = storage.getReference();
    String fileName = "chat_media/" + chatId + "/" + UUID.randomUUID().toString(); // استخدام UUID لاسم فريد
    StorageReference fileRef = storageRef.child(fileName);

    String mimeType = getContentResolver().getType(fileUri);
    final String messageType;

    if (mimeType != null) {
      if (mimeType.startsWith("video/")) messageType = ChatMessage.TYPE_VIDEO;
      else if (mimeType.equals("image/gif")) messageType = ChatMessage.TYPE_GIF;
      else messageType = ChatMessage.TYPE_IMAGE;
    } else {
      messageType = ChatMessage.TYPE_IMAGE;
    }

    UploadTask uploadTask = fileRef.putFile(fileUri);
    uploadTask
            .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                      ChatMessage message = new ChatMessage(currentUserId, messageText);
                      message.setSenderName(currentUserModel.getDisplayName() != null && !currentUserModel.getDisplayName().isEmpty() ? currentUserModel.getDisplayName() : currentUserModel.getUsername());
                      message.setSenderAvatar(currentUserModel.getProfileImageUrl());
                      message.setType(messageType);
                      message.setMediaUrl(uri.toString());
                      message.setThumbnailUrl(uri.toString());
                      if (replyToMessageId != null && replyHeader != null && replyHeader.getText() != null) {
                        message.setReplyToId(replyToMessageId);
                        message.setReplyPreview(replyHeader.getText().toString());
                      }
                      saveMessageToFirestore(message);
                    })
                    .addOnFailureListener(e -> {
                      Log.e(TAG, "Failed to get download URL", e);
                      Toast.makeText(this, "Failed to get media URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                      showProgress(false);
                    }))
            .addOnFailureListener(e -> {
              Log.e(TAG, "Upload failed", e);
              Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
              showProgress(false);
            });
  }

  private void saveMessageToFirestore(ChatMessage message) {
    if (chatId == null || chatId.isEmpty()) {
      Log.e(TAG, "Cannot save message, chatId is null or empty.");
      showProgress(false);
      Toast.makeText(this, "Error sending message: Invalid chat.", Toast.LENGTH_SHORT).show();
      return;
    }

    Map<String, Object> messageMap = message.toFirestoreMap();
    messageMap.put("timestamp", FieldValue.serverTimestamp());
    // التأكد من تعيين اسم المرسل وصورته إذا لم يتم تعيينهما من قبل (كإجراء احترازي)
    if (!messageMap.containsKey("senderName") && currentUserModel != null) {
      messageMap.put("senderName", currentUserModel.getDisplayName() != null && !currentUserModel.getDisplayName().isEmpty() ? currentUserModel.getDisplayName() : currentUserModel.getUsername());
    }
    if (!messageMap.containsKey("senderAvatar") && currentUserModel != null) {
      messageMap.put("senderAvatar", currentUserModel.getProfileImageUrl());
    }


    db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(messageMap)
            .addOnCompleteListener(
                    task -> {
                      showProgress(false);
                      if (task.isSuccessful()) {
                        DocumentReference docRef = task.getResult();
                        if (docRef != null) {
                          Log.d(TAG, "Message sent with ID: " + docRef.getId());
                          docRef.update("messageId", docRef.getId());
                          clearInputFields();
                          String lastMsgContent = message.getContent();
                          if (message.getReplyToId() != null && message.getReplyPreview() != null) {
                            lastMsgContent = message.getReplyPreview(); // أو نص مختصر للرد
                          } else if (ChatMessage.TYPE_IMAGE.equals(message.getType()) || ChatMessage.TYPE_GIF.equals(message.getType())) {
                            lastMsgContent = "[Image]";
                          } else if (ChatMessage.TYPE_VIDEO.equals(message.getType())) {
                            lastMsgContent = "[Video]";
                          } else if (ChatMessage.TYPE_FILE.equals(message.getType())) {
                            lastMsgContent = "[File: " + message.getFileName() + "]";
                          } else if (ChatMessage.TYPE_POLL.equals(message.getType()) && message.getPoll() != null) {
                            lastMsgContent = "[Poll: " + message.getPoll().getQuestion() + "]";
                          }
                          updateChatLastMessage(lastMsgContent, message.getSenderName(), message.getType());
                          scrollToBottom();
                        } else {
                          Log.e(TAG, "Failed to send message, DocumentReference is null.");
                          Toast.makeText(ChatActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                        }
                      } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Failed to send message: " + errorMessage, task.getException());
                        Toast.makeText(ChatActivity.this, "Failed to send message: " + errorMessage, Toast.LENGTH_SHORT).show();
                      }
                    });
  }

  private void updateChatLastMessage(String messageContent, String senderName, String messageType) {
    if (chatId == null || chatId.isEmpty()) {
      Log.e(TAG, "Cannot update last message, chatId is null or empty.");
      return;
    }
    Map<String, Object> updates = new HashMap<>();
    updates.put("lastMessage", messageContent);
    updates.put("lastMessageSender", senderName != null ? senderName : (currentUserModel != null && currentUserModel.getDisplayName() != null ? currentUserModel.getDisplayName() : "User"));
    updates.put("lastMessageType", messageType);
    updates.put("lastMessageTime", FieldValue.serverTimestamp());

    db.collection("chats")
            .document(chatId)
            .update(updates)
            .addOnFailureListener(e -> Log.e(TAG, "Error updating last message for chat: " + chatId, e));
  }


  private void clearInputFields() {
    if (postInput != null) postInput.setText("");
    if (mediaPreviewContainer != null) mediaPreviewContainer.setVisibility(View.GONE);
    currentMediaUri = null;
    if (mediaPreviewImage != null && !isDestroyed() && !isFinishing()){
      Glide.with(this).clear(mediaPreviewImage);
    }
    clearReplyUI();
    updateSendButtonState();
    toggleSendButtonVisibility();
  }


  private void showEditMessageDialog(ChatMessage message) {
    if (currentUserId == null || !message.getSenderId().equals(currentUserId)) {
      Toast.makeText(this, "You can only edit your own messages.", Toast.LENGTH_SHORT).show();
      return;
    }
    if (!ChatMessage.TYPE_TEXT.equals(message.getType())) {
      Toast.makeText(this, "Only text messages can be edited.", Toast.LENGTH_SHORT).show();
      return;
    }

    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
    EditText input = dialogView.findViewById(R.id.et_edit_message_input);
    input.setText(message.getContent());
    input.setSelection(message.getContent() != null ? message.getContent().length() : 0);

    builder
            .setTitle("Edit Message")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
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
      Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
      return false;
    }
    if (newText.equals(original.getContent())) {
      return false;
    }
    if (newText.length() > MAX_MESSAGE_LENGTH) {
      Toast.makeText(this, "Message exceeds character limit (" + MAX_MESSAGE_LENGTH + ")", Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  private void updateMessageInFirestore(ChatMessage message, String newContent) {
    if (chatId == null || message.getMessageId() == null) {
      Log.e(TAG, "Cannot update message, chatId or messageId is null.");
      return;
    }
    Map<String, Object> updates = new HashMap<>();
    updates.put("content", newContent);
    updates.put("edited", true);
    updates.put("lastUpdated", FieldValue.serverTimestamp());

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
                      Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
  }

  private void showProgress(boolean show) {
    if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    if (btnSend != null) btnSend.setEnabled(!show);
    if (postInput != null) postInput.setEnabled(!show);
    if (btnAddMedia != null) btnAddMedia.setEnabled(!show);
    if (btnAddVideo != null) btnAddVideo.setEnabled(!show);
    if (btnAddGif != null) btnAddGif.setEnabled(!show);
    if (btnAddPoll != null) btnAddPoll.setEnabled(!show);
    if (triggerButton != null) triggerButton.setEnabled(!show);
  }

  private void showMediaPreview(Uri uri) {
    if (mediaPreviewContainer == null || mediaPreviewImage == null) return;
    mediaPreviewContainer.setVisibility(View.VISIBLE);
    Glide.with(this).load(uri).placeholder(R.drawable.ic_cover_placeholder).into(mediaPreviewImage);
    updateSendButtonState();
    toggleSendButtonVisibility();
  }

  private void toggleMediaOptions() {
    if (bottomToolbar == null || triggerButton == null || postInput == null) return;
    boolean show = bottomToolbar.getVisibility() != View.VISIBLE;
    bottomToolbar.setVisibility(show ? View.VISIBLE : View.GONE);
    triggerButton.setImageResource(show ? R.drawable.ic_close : R.drawable.ic_add);
    if (show) {
      Utils.hideKeyboard(this);
    }
  }

  private void showGifPicker() {
    Toast.makeText(this, "GIF picker coming soon!", Toast.LENGTH_SHORT).show();
  }

  private void createPoll() {
    if (!isGroupChat) {
      Toast.makeText(this, "Polls are only available in group chats.", Toast.LENGTH_SHORT).show();
      return;
    }
    PollDialog pollDialog = new PollDialog();
    pollDialog.setOnPollCreatedListener((question, optionsList) -> {
      if (currentUserId == null || currentUserModel == null) {
        Toast.makeText(this, "Cannot create poll: User data not loaded.", Toast.LENGTH_SHORT).show();
        return;
      }
      ChatMessage.Poll poll = new ChatMessage.Poll();
      poll.setQuestion(question);
      List<ChatMessage.PollOption> pollOptions = new ArrayList<>();
      for(String optText : optionsList) {
        pollOptions.add(new ChatMessage.PollOption(optText));
      }
      poll.setOptions(pollOptions);

      ChatMessage message = new ChatMessage(currentUserId, poll);
      message.setSenderName(currentUserModel.getDisplayName() != null ? currentUserModel.getDisplayName() : currentUserModel.getUsername());
      message.setSenderAvatar(currentUserModel.getProfileImageUrl());
      message.setContent(question);
      saveMessageToFirestore(message);
    });
    pollDialog.show(getSupportFragmentManager(), "PollDialog");
  }


  private void scrollToBottom() {
    if (messagesRecycler == null || messageAdapter == null) return;
    messagesRecycler.post(() -> {
      if (messageAdapter.getItemCount() > 0) {
        messagesRecycler.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
      }
    });
  }

  private void setupAppBar() {
    ImageView ivBack = findViewById(R.id.iv_back);
    if (ivBack != null) ivBack.setOnClickListener(v -> finish());

    if (tvDisplayName != null) tvDisplayName.setText("Loading...");
    if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_default_avatar);
    if (ivVerified != null) ivVerified.setVisibility(View.GONE);
  }

  private void loadChatDetails() {
    if (chatId == null || chatId.isEmpty()) {
      Log.e(TAG, "Cannot load chat details, chatId is null or empty for AppBar.");
      Toast.makeText(this, getString(R.string.invalid_chat_id_error), Toast.LENGTH_SHORT).show();
      finishActivityCleanup();
      return;
    }
    if (chatDetailsListener != null) {
      chatDetailsListener.remove();
    }
    chatDetailsListener = db.collection("chats")
            .document(chatId)
            .addSnapshotListener(
                    (document, error) -> {
                      if (!isActivityValid()) return;

                      if (error != null ) {
                        Log.e(TAG, "Chat details listen failed for chatId: " + chatId, error);
                        if (tvDisplayName != null) tvDisplayName.setText("Error");
                        return;
                      }
                      if (document == null || !document.exists()){
                        Log.w(TAG, "Chat document does not exist for chatId: " + chatId);
                        if (tvDisplayName != null) tvDisplayName.setText("Chat not found");
                        if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                        if (ivVerified != null) ivVerified.setVisibility(View.GONE);
                        return;
                      }

                      Chat chat = document.toObject(Chat.class);
                      if (chat != null) {
                        chat.setId(document.getId());
                        Log.d(TAG, "Chat details loaded/updated for AppBar: " + chatId + ", Group: " + chat.isGroup() + ", Name: " + chat.getGroupName());
                        if (isGroupChat) {
                          if (tvDisplayName != null) tvDisplayName.setText(chat.getGroupName() != null ? chat.getGroupName() : "Group");
                          if (ivAvatar != null) {
                            if (chat.getGroupImage() != null && !chat.getGroupImage().isEmpty()) {
                              Glide.with(this).load(chat.getGroupImage()).placeholder(R.drawable.ic_default_group).error(R.drawable.ic_default_group).into(ivAvatar);
                            } else {
                              ivAvatar.setImageResource(R.drawable.ic_default_group);
                            }
                          }
                          if (ivVerified != null) ivVerified.setVisibility(View.GONE);
                        } else {
                          List<String> participants = chat.getParticipants();
                          String otherUserId = getOtherUserId(participants);
                          if (otherUserId != null && !otherUserId.isEmpty()) {
                            loadOtherUserDetails(otherUserId);
                          } else {
                            Log.w(TAG, "Could not find other user ID for direct chat (AppBar): " + chatId);
                            if (tvDisplayName != null) tvDisplayName.setText(getString(R.string.unknown_user_display_name));
                            if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                            if (ivVerified != null) ivVerified.setVisibility(View.GONE);
                          }
                        }
                      } else {
                        Log.e(TAG, "Chat object is null after deserialization (AppBar) for chatId: " + chatId);
                        if (tvDisplayName != null) tvDisplayName.setText("Error Loading Chat");
                      }
                    });
  }

  private boolean isActivityValid() {
    return !isFinishing() && !isDestroyed();
  }

  private String getOtherUserId(List<String> participants) {
    if (participants == null || currentUserId == null) return null;
    for (String userId : participants) {
      if (!userId.equals(currentUserId)) return userId;
    }
    return null;
  }

  private void loadOtherUserDetails(String userId) {
    if (userId == null || userId.isEmpty()) {
      Log.e(TAG, "Cannot load other user details for AppBar, userId is null or empty.");
      if (tvDisplayName != null) tvDisplayName.setText(getString(R.string.unknown_user_display_name));
      if (ivAvatar != null) ivAvatar.setImageResource(R.drawable.ic_default_avatar);
      if (ivVerified != null) ivVerified.setVisibility(View.GONE);
      return;
    }
    db.collection("users").document(userId).get()
            .addOnSuccessListener(document -> {
              if (!isActivityValid()) return;

              if (document.exists()) {
                UserModel user = document.toObject(UserModel.class);
                if (user != null) {
                  if (tvDisplayName != null) tvDisplayName.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : user.getUsername());
                  if (ivAvatar != null) {
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                      Glide.with(ChatActivity.this).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_default_avatar).error(R.drawable.ic_default_avatar).into(ivAvatar);
                    } else {
                      ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                    }
                  }
                  if (ivVerified != null) ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
                } else {
                  Log.e(TAG, "UserModel is null after deserialization (AppBar) for other user: " + userId);
                  if (tvDisplayName != null) tvDisplayName.setText(getString(R.string.unknown_user_display_name));
                }
              } else {
                Log.w(TAG, "User document not found (AppBar) for other user ID: " + userId);
                if (tvDisplayName != null) tvDisplayName.setText(getString(R.string.unknown_user_display_name));
              }
            })
            .addOnFailureListener(e -> {
              if (!isActivityValid()) return;
              Log.e(TAG, "Failed to load other user details for AppBar, userId: " + userId, e);
              if (tvDisplayName != null) tvDisplayName.setText(getString(R.string.unknown_user_display_name));
            });
  }

  @Override
  public void onMessageLongClick(ChatMessage message, int position) {
    if (messagesRecycler == null) return;
    RecyclerView.ViewHolder viewHolder = messagesRecycler.findViewHolderForAdapterPosition(position);
    if (viewHolder != null && viewHolder.itemView != null) {
      showMessageContextMenu(message, viewHolder.itemView);
    } else {
      Log.w(TAG, "ViewHolder not found for position: " + position + " during long click.");
    }
  }

  @Override
  public void onMediaClick(String mediaUrl, int position) {
    if (mediaUrl == null || mediaUrl.isEmpty()) {
      Toast.makeText(this, "Media URL is invalid.", Toast.LENGTH_SHORT).show();
      return;
    }
    ArrayList<String> mediaList = new ArrayList<>();
    mediaList.add(mediaUrl);
    View mediaView = null;
    if (messagesRecycler != null) {
      RecyclerView.ViewHolder viewHolder = messagesRecycler.findViewHolderForAdapterPosition(position);
      if (viewHolder != null && viewHolder.itemView != null) {
        mediaView = viewHolder.itemView.findViewById(R.id.imageContent);
        if (mediaView == null && messageAdapter != null) {
          int itemViewType = messageAdapter.getItemViewType(position);
          if (itemViewType == MessageAdapter.VIEW_TYPE_VIDEO_SENT || itemViewType == MessageAdapter.VIEW_TYPE_VIDEO_RECEIVED) {
            mediaView = viewHolder.itemView.findViewById(R.id.videoThumbnail);
          }
        }
      }
    }
    MediaViewerActivity.launch(this, mediaList, 0, mediaView);
  }

  @Override
  public void onReplyClick(String messageId) {
    if (chatId == null || messageId == null || replyHeader == null || postInput == null) return;
    db.collection("chats").document(chatId).collection("messages").document(messageId).get()
            .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot.exists()) {
                ChatMessage repliedMessage = documentSnapshot.toObject(ChatMessage.class);
                if (repliedMessage != null) {
                  String senderName = repliedMessage.getSenderName() != null ? repliedMessage.getSenderName() : "User";
                  String contentPreview = "[Message]";
                  if (ChatMessage.TYPE_TEXT.equals(repliedMessage.getType()) && repliedMessage.getContent() != null) {
                    contentPreview = repliedMessage.getContent().length() > 30 ? repliedMessage.getContent().substring(0, 30) + "..." : repliedMessage.getContent();
                  } else if (ChatMessage.TYPE_IMAGE.equals(repliedMessage.getType()) || ChatMessage.TYPE_GIF.equals(repliedMessage.getType())) {
                    contentPreview = "[Image]";
                  } else if (ChatMessage.TYPE_VIDEO.equals(repliedMessage.getType())) {
                    contentPreview = "[Video]";
                  } else if (ChatMessage.TYPE_FILE.equals(repliedMessage.getType())) {
                    contentPreview = "[File: " + (repliedMessage.getFileName() != null ? repliedMessage.getFileName() : "") + "]";
                  } else if (ChatMessage.TYPE_POLL.equals(repliedMessage.getType()) && repliedMessage.getPoll() != null) {
                    contentPreview = "[Poll: " + (repliedMessage.getPoll().getQuestion() != null ? repliedMessage.getPoll().getQuestion() : "") + "]";
                  }

                  replyHeader.setText(getString(R.string.replying_to_user, senderName) + ": " + contentPreview);
                  replyHeader.setVisibility(View.VISIBLE);
                  postInput.setTag(R.id.tag_reply_to_message_id, repliedMessage.getMessageId());
                }
              }
            });
  }

  @Override
  public void onPollVote(String pollId, int optionIndex) {
    if (chatId == null || pollId == null || currentUserId == null) return;

    DocumentReference messageRef = db.collection("chats").document(chatId)
            .collection("messages").document(pollId);
    db.runTransaction(transaction -> {
              DocumentSnapshot snapshot = transaction.get(messageRef);
              ChatMessage message = snapshot.toObject(ChatMessage.class);
              if (message == null || message.getPoll() == null || message.getPoll().getOptions() == null || message.getPoll().isVoted() || message.getPoll().isExpired()) {
                return null;
              }
              if (optionIndex < 0 || optionIndex >= message.getPoll().getOptions().size()) {
                Log.e(TAG, "Invalid poll option index: " + optionIndex);
                return null;
              }

              List<ChatMessage.PollOption> options = message.getPoll().getOptions();
              ChatMessage.PollOption selectedOption = options.get(optionIndex);
              selectedOption.setVotes(selectedOption.getVotes() + 1);
              message.getPoll().setTotalVotes(message.getPoll().getTotalVotes() + 1);
              message.getPoll().setVoted(true);

              transaction.update(messageRef, "poll", message.getPoll());
              return null;
            }).addOnSuccessListener(aVoid -> Log.d(TAG, "Poll vote successful for option " + optionIndex))
            .addOnFailureListener(e -> Log.e(TAG, "Poll vote failed", e));
  }


  @Override
  public void onFileClick(String fileUrl) {
    if (fileUrl != null && !fileUrl.isEmpty()) {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
      try {
        startActivity(intent);
      } catch (android.content.ActivityNotFoundException e) {
        Toast.makeText(this, "No application can handle this file type.", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "No activity found to handle file URL: " + fileUrl, e);
      }
    } else {
      Toast.makeText(this, "File URL is invalid.", Toast.LENGTH_SHORT).show();
    }
  }


  private void showMessageContextMenu(ChatMessage message, View anchorView) {
    ContextMenuDialog dialog =
            new ContextMenuDialog(this, anchorView, message, new MessageMenuListener(message));
    dialog.show();
  }

  private class MessageMenuListener implements MessageContextMenuListener {
    private final ChatMessage message;
    public MessageMenuListener(ChatMessage message) {
      this.message = message;
    }
    @Override
    public void onReplySelected() { onReplyClick(message.getMessageId()); }
    @Override
    public void onEditSelected() { showEditMessageDialog(message); }
    @Override
    public void onDeleteSelected() { showDeleteOptionsDialog(message); }
    @Override
    public void onReactionSelected() { showReactionPicker(message); }
    @Override
    public void onReportSelected(ChatMessage msg) {
      Intent intent = new Intent(ChatActivity.this, ReportActivity.class);
      intent.putExtra(ReportActivity.EXTRA_REPORTED_ITEM_ID, msg.getMessageId());
      intent.putExtra(ReportActivity.EXTRA_REPORT_TYPE, "message");
      intent.putExtra(ReportActivity.EXTRA_REPORTED_AUTHOR_ID, msg.getSenderId());
      startActivity(intent);
    }
  }

  private void showDeleteOptionsDialog(ChatMessage message) {
    if (currentUserId == null) return;
    boolean isCurrentUserMessage = message.getSenderId() != null && message.getSenderId().equals(currentUserId);
    List<String> options = new ArrayList<>();

    if (isCurrentUserMessage) {
      options.add("Delete for everyone");
    }
    options.add("Delete for me");

    new MaterialAlertDialogBuilder(this)
            .setTitle("Delete message")
            .setItems(options.toArray(new String[0]), (dialog, which) -> {
              boolean deleteForAll = isCurrentUserMessage && which == 0;
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
    if (grid != null) {
      for (String reaction : reactions) {
        TextView emojiView = (TextView) LayoutInflater.from(this).inflate(R.layout.item_emoji_reaction, grid, false);
        emojiView.setText(reaction);
        emojiView.setOnClickListener(v -> {
          addReaction(message, reaction);
          dialog.dismiss();
        });
        grid.addView(emojiView);
      }
    }
    dialog.show();
  }

  private void addReaction(ChatMessage message, String reaction) {
    if (chatId == null || message.getMessageId() == null || currentUserId == null) {
      Log.e(TAG, "Cannot add reaction, critical ID is null.");
      return;
    }
    db.collection("chats").document(chatId).collection("messages")
            .document(message.getMessageId())
            .update("reactions." + currentUserId, reaction);
  }

  private void deleteMessage(ChatMessage message, boolean forEveryone) {
    String messageId = message.getMessageId();
    if (messageId == null || chatId == null || currentUserId == null) {
      Log.e(TAG, "Cannot delete message, critical ID is null.");
      return;
    }
    if (forEveryone) {
      db.collection("chats").document(chatId).collection("messages")
              .document(messageId)
              .update("deleted", true)
              .addOnSuccessListener(aVoid -> showToast("Message deleted for everyone."))
              .addOnFailureListener(e -> Log.e(TAG, "Delete for everyone failed for message: " + messageId, e));
    } else {
      db.collection("users").document(currentUserId).collection("deleted_messages")
              .document(messageId)
              .set(Collections.singletonMap("chatId", chatId))
              .addOnSuccessListener(aVoid -> showToast("Message deleted for you."))
              .addOnFailureListener(e -> Log.e(TAG, "Delete for me failed for message: " + messageId, e));
    }
  }


  private void clearReplyUI() {
    if (replyHeader == null || postInput == null) return;
    replyHeader.setVisibility(View.GONE);
    replyHeader.setText("");
    postInput.setTag(R.id.tag_reply_to_message_id, null);
  }


  private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (messagesListener != null) {
      messagesListener.remove();
    }
    if (userDeletedMessagesListener != null) {
      userDeletedMessagesListener.remove();
    }
    if (chatDetailsListener != null) {
      chatDetailsListener.remove();
    }
  }

  private void clearMediaPreview() {
    if (mediaPreviewContainer == null || mediaPreviewImage == null) return;
    mediaPreviewContainer.setVisibility(View.GONE);
    currentMediaUri = null;
    if (!isDestroyed() && !isFinishing()){
      Glide.with(this).clear(mediaPreviewImage);
    }
    updateSendButtonState();
    toggleSendButtonVisibility();
  }

  public static class Utils {
    public static void hideKeyboard(@NonNull AppCompatActivity activity) {
      View view = activity.getCurrentFocus();
      if (view != null) {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
          imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
      }
    }
  }
}