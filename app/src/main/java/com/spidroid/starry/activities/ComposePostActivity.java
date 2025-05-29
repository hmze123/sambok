package com.spidroid.starry.activities;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.MediaPreviewAdapter;
import com.spidroid.starry.auth.CircularProgressBar;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.utils.LinkPreviewFetcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComposePostActivity extends AppCompatActivity {
  // Constants
  private static final int MAX_CHARS = 600;
  private static final int MAX_MEDIA = 4;
  private static final int PICK_MEDIA_REQUEST = 100;
  public static final String EXTRA_EDIT_MODE = "edit_mode";
  public static final String EXTRA_POST_ID = "post_id";
  public static final String EXTRA_CONTENT = "content";
  public static final String EXTRA_MEDIA_URLS = "media_urls";

  // Views
  private EditText etContent;
  private RecyclerView rvMediaPreview;
  private TextView tvCharCount;
  private View layoutLinkPreview;
  private TextView tvLinkTitle;
  private TextView tvLinkDescription;
  private TextView tvLinkDomain;
  private ImageView ivLinkImage;
  private CardView progressContainer;
  private CircularProgressBar circularProgressBar;

  // Firebase
  private FirebaseAuth auth;
  private FirebaseFirestore db;
  private FirebaseStorage storage;

  // Adapters & Data
  private MediaPreviewAdapter mediaAdapter;
  private TextWatcher textWatcher;
  private UserModel currentUserModel;

  // State
  private boolean isEditMode = false;
  private String existingPostId;
  private List<String> existingMediaUrls = new ArrayList<>();
  private List<String> originalMediaUrls = new ArrayList<>();
  private List<Uri> selectedMediaUris = new ArrayList<>();
  private List<PostModel.LinkPreview> currentPostLinkPreviews = new ArrayList<>();
  private ValueAnimator progressAnimator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_compose_post);

    initializeViews();
    initializeFirebase();
    setupViews();
    setupMediaAdapter();
    loadCurrentUser();
    handleIntentAndSavedState(savedInstanceState);
  }

  private void initializeViews() {
    etContent = findViewById(R.id.etContent);
    rvMediaPreview = findViewById(R.id.rvMediaPreview);
    tvCharCount = findViewById(R.id.tvCharCount);
    layoutLinkPreview = findViewById(R.id.layoutLinkPreview);
    tvLinkTitle = findViewById(R.id.tvLinkTitle);
    tvLinkDescription = findViewById(R.id.tvLinkDescription);
    tvLinkDomain = findViewById(R.id.tvLinkDomain);
    ivLinkImage = findViewById(R.id.ivLinkImage);
    progressContainer = findViewById(R.id.progressContainer);
    circularProgressBar = findViewById(R.id.circularProgressBar);
  }

  private void initializeFirebase() {
    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    storage = FirebaseStorage.getInstance();
  }

  private void setupViews() {
    setupTextWatcher();
    setupClickListeners();
    updateCharacterCount(0);
  }

  private void setupTextWatcher() {
    textWatcher =
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void afterTextChanged(Editable s) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateCharacterCount(s.length());
            detectLinks(s.toString());
          }
        };
    etContent.addTextChangedListener(textWatcher);
  }

  private void updateCharacterCount(int length) {
    tvCharCount.setText(getString(R.string.char_count_format, length, MAX_CHARS));

    int colorRes;
    if (length == 0) {
      colorRes = R.color.error_red;
    } else if (length > 550) {
      colorRes = R.color.warning_orange;
    } else {
      colorRes = R.color.text_secondary;
    }

    tvCharCount.setTextColor(ContextCompat.getColor(this, colorRes));
  }

  private void setupClickListeners() {
    findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    findViewById(R.id.btnPost).setOnClickListener(v -> validateAndPost());
    findViewById(R.id.btnAddPhoto).setOnClickListener(v -> openMediaPicker("image/*"));
    findViewById(R.id.btnAddVideo).setOnClickListener(v -> openMediaPicker("video/*"));
  }

  private void setupMediaAdapter() {
    mediaAdapter =
        new MediaPreviewAdapter(
            selectedMediaUris,
            existingMediaUrls,
            new MediaPreviewAdapter.MediaRemoveListener() {
              @Override
              public void onMediaRemoved(int position) {
                if (position < existingMediaUrls.size()) {
                  existingMediaUrls.remove(position);
                } else {
                  selectedMediaUris.remove(position - existingMediaUrls.size());
                }
                mediaAdapter.notifyDataSetChanged();
              }
            });

    rvMediaPreview.setLayoutManager(
        new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    rvMediaPreview.setAdapter(mediaAdapter);
  }

  private void loadCurrentUser() {
    FirebaseUser user = auth.getCurrentUser();
    if (user == null) {
      finish();
      return;
    }

    db.collection("users")
        .document(user.getUid())
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              currentUserModel = documentSnapshot.toObject(UserModel.class);
            })
        .addOnFailureListener(e -> showToast(R.string.user_load_failed));
  }

  private void handleIntentAndSavedState(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      restoreFromSavedState(savedInstanceState);
    } else if (getIntent() != null) {
      handleEditModeIntent();
    }
  }

  private void restoreFromSavedState(Bundle savedInstanceState) {
    isEditMode = savedInstanceState.getBoolean(EXTRA_EDIT_MODE, false);
    existingPostId = savedInstanceState.getString(EXTRA_POST_ID);
    existingMediaUrls = savedInstanceState.getStringArrayList(EXTRA_MEDIA_URLS);
    originalMediaUrls = new ArrayList<>(existingMediaUrls);
    etContent.setText(savedInstanceState.getString(EXTRA_CONTENT));

    ArrayList<Uri> savedUris = savedInstanceState.getParcelableArrayList("selectedMediaUris");
    if (savedUris != null) {
      selectedMediaUris.addAll(savedUris);
    }
    mediaAdapter.notifyDataSetChanged();
  }

  private void handleEditModeIntent() {
    isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
    if (isEditMode) {
      setupEditMode();
    }
  }

  private void setupEditMode() {
    setTitle(R.string.edit_post);
    existingPostId = getIntent().getStringExtra(EXTRA_POST_ID);

    String content = getIntent().getStringExtra(EXTRA_CONTENT);
    etContent.setText(content);
    updateCharacterCount(content.length());

    ArrayList<String> mediaUrls = getIntent().getStringArrayListExtra(EXTRA_MEDIA_URLS);
    if (mediaUrls != null) {
      existingMediaUrls.addAll(mediaUrls);
      originalMediaUrls.addAll(mediaUrls);
      mediaAdapter.notifyDataSetChanged();
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(EXTRA_EDIT_MODE, isEditMode);
    outState.putString(EXTRA_POST_ID, existingPostId);
    outState.putStringArrayList(EXTRA_MEDIA_URLS, new ArrayList<>(existingMediaUrls));
    outState.putString(EXTRA_CONTENT, etContent.getText().toString());
    outState.putParcelableArrayList("selectedMediaUris", new ArrayList<>(selectedMediaUris));
  }

  private void openMediaPicker(String mimeType) {
    if (selectedMediaUris.size() + existingMediaUrls.size() >= MAX_MEDIA) {
      showToast(R.string.max_media_limit);
      return;
    }

    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType(mimeType);
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    startActivityForResult(intent, PICK_MEDIA_REQUEST);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_MEDIA_REQUEST && resultCode == RESULT_OK && data != null) {
      handleSelectedMedia(data);
    }
  }

  private void handleSelectedMedia(Intent data) {
    List<Uri> newUris = new ArrayList<>();
    if (data.getClipData() != null) {
      int count = data.getClipData().getItemCount();
      for (int i = 0; i < count; i++) {
        newUris.add(data.getClipData().getItemAt(i).getUri());
      }
    } else if (data.getData() != null) {
      newUris.add(data.getData());
    }

    validateAndAddMedia(newUris);
    mediaAdapter.notifyDataSetChanged();
  }

  private void validateAndAddMedia(List<Uri> newUris) {
    boolean hasExistingVideo =
        existingMediaUrls.stream().anyMatch(url -> url.matches(".*\\.(mp4|mov|avi|mkv|webm|3gp)$"));

    boolean hasNewVideo =
        newUris.stream()
            .anyMatch(
                uri -> {
                  String mimeType = getContentResolver().getType(uri);
                  return mimeType != null && mimeType.startsWith("video/");
                });

    if (hasExistingVideo && hasNewVideo) {
      showToast(R.string.multiple_videos_error);
      return;
    }

    if (hasNewVideo) {
      if (!selectedMediaUris.isEmpty() || !existingMediaUrls.isEmpty()) {
        showToast(R.string.video_post_single_media);
        return;
      }
      if (newUris.size() > 1) {
        showToast(R.string.single_video_allowed);
        return;
      }
    } else {
      if (hasExistingVideo) {
        showToast(R.string.cant_add_images_to_video_post);
        return;
      }
      int totalMedia = existingMediaUrls.size() + selectedMediaUris.size() + newUris.size();
      if (totalMedia > MAX_MEDIA) {
        showToast(getString(R.string.max_images_error, MAX_MEDIA));
        return;
      }
    }

    selectedMediaUris.addAll(newUris);
  }
    
   private void showToast(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
}

  private void validateAndPost() {
    String content = etContent.getText().toString().trim();

    if (content.isEmpty() && selectedMediaUris.isEmpty() && existingMediaUrls.isEmpty()) {
      showToast(R.string.post_empty_error);
      return;
    }

    if (content.length() > MAX_CHARS) {
      showToast(R.string.char_limit_exceeded);
      return;
    }

    if (!verifyMediaAccess()) return;

    showProgress(true);

    if (isEditMode) {
      handleEditPost(content);
    } else {
      createNewPost(content);
    }
  }

  private boolean verifyMediaAccess() {
    for (Uri uri : selectedMediaUris) {
      try {
        getContentResolver().openInputStream(uri).close();
      } catch (Exception e) {
        showToast(R.string.media_access_error);
        return false;
      }
    }
    return true;
  }

  private void handleEditPost(String content) {
    boolean mediaChanged = !existingMediaUrls.equals(originalMediaUrls);
    boolean contentChanged = !content.equals(getIntent().getStringExtra(EXTRA_CONTENT));

    if (selectedMediaUris.isEmpty()) {
      if (mediaChanged || contentChanged) {
        if (mediaChanged) {
          updatePostContentAndMedia(content, existingMediaUrls);
        } else {
          updatePostContent(content);
        }
      } else {
        showProgress(false);
        showToast(R.string.no_changes_detected);
        finish();
      }
    } else {
      uploadMediaAndUpdatePost(content);
    }
  }

  private void createNewPost(String content) {
    if (selectedMediaUris.isEmpty()) {
      createPostDocument(content, new ArrayList<>());
    } else {
      uploadMediaAndCreatePost(content);
    }
  }

  private void uploadMediaAndCreatePost(String content) {
    List<Task<Uri>> uploadTasks = createMediaUploadTasks();

    Tasks.whenAllComplete(uploadTasks)
        .addOnCompleteListener(
            task -> {
              if (task.isSuccessful()) {
                List<String> mediaUrls = extractDownloadUrls(uploadTasks);
                createPostDocument(content, mediaUrls);
              } else {
                showProgress(false);
                handleUploadError(task.getException());
              }
            });
  }

  private List<Task<Uri>> createMediaUploadTasks() {
    List<Task<Uri>> tasks = new ArrayList<>();
    for (Uri mediaUri : selectedMediaUris) {
      String mimeType = getContentResolver().getType(mediaUri);
      String extension = getFileExtension(mediaUri);
      String fileName = createUniqueFileName(mimeType, extension);

      StorageReference mediaRef =
          storage.getReference().child("posts").child(auth.getUid()).child(fileName);

      tasks.add(
          mediaRef
              .putFile(mediaUri)
              .continueWithTask(
                  task ->
                      task.isSuccessful()
                          ? mediaRef.getDownloadUrl()
                          : Tasks.forException(task.getException())));
    }
    return tasks;
  }

  private String getFileExtension(Uri uri) {
    String path = uri.getLastPathSegment();
    return path != null && path.contains(".") ? path.substring(path.lastIndexOf(".")) : "";
  }

  private String createUniqueFileName(String mimeType, String extension) {
    String typePrefix = mimeType.startsWith("video/") ? "_vid" : "";
    return System.currentTimeMillis()
        + typePrefix
        + "_"
        + UUID.randomUUID().toString().substring(0, 8)
        + extension;
  }

  private List<String> extractDownloadUrls(List<Task<Uri>> tasks) {
    List<String> urls = new ArrayList<>();
    for (Task<Uri> task : tasks) {
      if (task.isSuccessful() && task.getResult() != null) {
        urls.add(task.getResult().toString());
      }
    }
    return urls;
  }

  private void createPostDocument(String content, List<String> mediaUrls) {
    PostModel post = new PostModel(currentUserModel.getUserId(), content);
    post.setAuthorUsername(currentUserModel.getUsername());
    post.setAuthorDisplayName(currentUserModel.getDisplayName());
    post.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl());
    post.setMediaUrls(mediaUrls);
    post.setLinkPreviews(currentPostLinkPreviews);
    post.setContentType(determineContentType(mediaUrls));

    db.collection("posts")
        .add(post)
        .addOnCompleteListener(
            task -> {
              showProgress(false);
              if (task.isSuccessful()) {
                showToast(R.string.post_success);
                setResult(RESULT_OK);
                finish();
              } else {
                showToast(R.string.post_failed);
              }
            });
  }

  private void uploadMediaAndUpdatePost(String content) {
    List<Task<Uri>> uploadTasks = createMediaUploadTasks();

    Tasks.whenAllComplete(uploadTasks)
        .addOnCompleteListener(
            task -> {
              if (task.isSuccessful()) {
                List<String> newMediaUrls = new ArrayList<>(existingMediaUrls);
                newMediaUrls.addAll(extractDownloadUrls(uploadTasks));
                updatePostContentAndMedia(content, newMediaUrls);
              } else {
                showProgress(false);
                handleUploadError(task.getException());
              }
            });
  }

  private void updatePostContentAndMedia(String content, List<String> mediaUrls) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("content", content);
    updates.put("mediaUrls", mediaUrls);
    updates.put("updatedAt", FieldValue.serverTimestamp());
    updates.put("contentType", determineContentType(mediaUrls));

    db.collection("posts")
        .document(existingPostId)
        .update(updates)
        .addOnCompleteListener(
            task -> {
              showProgress(false);
              if (task.isSuccessful()) {
                showToast(R.string.post_updated);
                setResult(RESULT_OK);
                finish();
              } else {
                showToast(R.string.update_failed);
              }
            });
  }

  private void updatePostContent(String content) {
    db.collection("posts")
        .document(existingPostId)
        .update("content", content, "updatedAt", FieldValue.serverTimestamp())
        .addOnCompleteListener(
            task -> {
              showProgress(false);
              if (task.isSuccessful()) {
                showToast(R.string.post_updated);
                setResult(RESULT_OK);
                finish();
              } else {
                showToast(R.string.update_failed);
              }
            });
  }

  private String determineContentType(List<String> mediaUrls) {
    if (mediaUrls.isEmpty()) return PostModel.TYPE_TEXT;
    return mediaUrls.stream().anyMatch(url -> url.matches(".*\\.(mp4|mov|avi|mkv|webm|3gp)$"))
        ? PostModel.TYPE_VIDEO
        : PostModel.TYPE_IMAGE;
  }

  private void handleUploadError(Exception exception) {
    showProgress(false);
    showToast(R.string.media_upload_failed);
  }

  private void detectLinks(String text) {
    Spannable spannable = new SpannableString(text);
    Pattern pattern = Patterns.WEB_URL;
    Matcher matcher = pattern.matcher(text);
    boolean hasLinks = false;

    int selectionStart = etContent.getSelectionStart();
    int selectionEnd = etContent.getSelectionEnd();

    currentPostLinkPreviews.clear();

    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      String url = text.substring(start, end);

      if (!pattern.matcher(url).matches()) continue;

      spannable.setSpan(
          new ForegroundColorSpan(ContextCompat.getColor(this, R.color.link_color)),
          start,
          end,
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      spannable.setSpan(new URLSpan(url), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

      if (!hasLinks) {
        fetchLinkPreview(url);
        hasLinks = true;
      }
    }

    if (!etContent.getText().toString().equals(spannable.toString())) {
      etContent.removeTextChangedListener(textWatcher);
      etContent.setText(spannable);
      etContent.setMovementMethod(LinkMovementMethod.getInstance());
      etContent.setSelection(
          Math.min(selectionStart, spannable.length()), Math.min(selectionEnd, spannable.length()));
      etContent.addTextChangedListener(textWatcher);
    }
  }

  private void fetchLinkPreview(String url) {
    layoutLinkPreview.setVisibility(View.VISIBLE);
    tvLinkTitle.setText(R.string.loading_preview);
    tvLinkDescription.setText("");
    tvLinkDomain.setText(Uri.parse(url).getHost());
    Glide.with(this).load(R.drawable.ic_cover_placeholder).into(ivLinkImage);

    LinkPreviewFetcher.fetch(
        url,
        new LinkPreviewFetcher.LinkPreviewCallback() {
          @Override
          public void onPreviewReceived(PostModel.LinkPreview preview) {
            runOnUiThread(() -> updateLinkPreviewUI(preview));
          }

          @Override
          public void onError(String error) {
            runOnUiThread(
                () -> {
                  layoutLinkPreview.setVisibility(View.GONE);
                  currentPostLinkPreviews.clear();
                  showToast(R.string.link_preview_failed);
                });
          }
        });
  }

  private void updateLinkPreviewUI(PostModel.LinkPreview preview) {
    tvLinkTitle.setText(
        preview.getTitle() != null ? preview.getTitle() : getString(R.string.no_title));

    if (preview.getDescription() != null && !preview.getDescription().isEmpty()) {
      tvLinkDescription.setText(preview.getDescription());
      tvLinkDescription.setVisibility(View.VISIBLE);
    } else {
      tvLinkDescription.setVisibility(View.GONE);
    }

    if (preview.getImageUrl() != null) {
      Glide.with(this)
          .load(preview.getImageUrl())
          .placeholder(R.drawable.ic_cover_placeholder)
          .into(ivLinkImage);
    }

    currentPostLinkPreviews.clear();
    currentPostLinkPreviews.add(preview);
  }

  private void showProgress(boolean show) {
    progressContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    if (show) {
      startProgressAnimation();
    } else {
      stopProgressAnimation();
    }
  }

  private void startProgressAnimation() {
    if (progressAnimator != null) {
      progressAnimator.cancel();
    }

    progressAnimator = ValueAnimator.ofFloat(0, 100);
    progressAnimator.setDuration(2000);
    progressAnimator.setRepeatCount(ValueAnimator.INFINITE);
    progressAnimator.addUpdateListener(
        animation -> {
          float progress = (float) animation.getAnimatedValue();
          circularProgressBar.setProgress(progress);
        });
    progressAnimator.start();
  }

  private void stopProgressAnimation() {
    if (progressAnimator != null) {
      progressAnimator.cancel();
      progressAnimator = null;
    }
    circularProgressBar.setProgress(0);
  }

  private void showToast(int resId) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    stopProgressAnimation();
    if (etContent != null) {
      etContent.removeTextChangedListener(textWatcher);
    }
    if (!isDestroyed()) {
      Glide.with(this).clear(ivLinkImage);
    }
  }
}
