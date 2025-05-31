package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.MediaPreviewAdapter;
import com.spidroid.starry.databinding.ActivityComposePostBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.utils.LinkPreviewFetcher;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComposePostActivity extends AppCompatActivity implements MediaPreviewAdapter.MediaRemoveListener {

  private static final String TAG = "ComposePostActivity";
  private ActivityComposePostBinding binding;
  private FirebaseAuth auth;
  private FirebaseFirestore db;
  private FirebaseStorage storage;
  private UserModel currentUserModel;

  private MediaPreviewAdapter mediaPreviewAdapter;
  private List<Uri> selectedMediaUris = new ArrayList<>();
  private List<String> existingMediaUrls = new ArrayList<>();

  private Pattern urlPattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
  private PostModel.LinkPreview currentLinkPreview;

  private final ActivityResultLauncher<String[]> pickMultipleMedia =
          registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
            if (uris != null && !uris.isEmpty()) {
              if (selectedMediaUris.size() + existingMediaUrls.size() + uris.size() > 4) {
                Toast.makeText(this, getString(R.string.max_media_limit, 4), Toast.LENGTH_SHORT).show();
                return;
              }
              for(Uri uri : uris){
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null) {
                  if (mimeType.startsWith("image/")) {
                    handleMediaSelection(uri, PostModel.TYPE_IMAGE);
                  } else if (mimeType.startsWith("video/")) {
                    if (selectedMediaUris.stream().anyMatch(u -> {
                      String innerMimeType = getContentResolver().getType(u);
                      return innerMimeType != null && innerMimeType.startsWith("video/");
                    }) || existingMediaUrls.stream().anyMatch(url -> url.toLowerCase().matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$"))){
                      Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show();
                      return;
                    }
                    handleMediaSelection(uri, PostModel.TYPE_VIDEO);
                  } else {
                    Toast.makeText(this, getString(R.string.unsupported_file_type, mimeType), Toast.LENGTH_SHORT).show();
                  }
                }
              }
            }
          });


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityComposePostBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    initializeFirebase();
    setupListeners();
    setupMediaPreviewRecyclerView();

    binding.btnPost.setEnabled(false);
    binding.btnPost.setText(getString(R.string.loading_text));

    loadCurrentUser();
  }

  private void initializeFirebase() {
    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
    storage = FirebaseStorage.getInstance();
  }

  private void loadCurrentUser() {
    FirebaseUser user = auth.getCurrentUser();
    if (user == null) {
      Log.e(TAG, "User not authenticated. Finishing activity.");
      Toast.makeText(this, getString(R.string.user_not_authenticated_error), Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot != null && documentSnapshot.exists()) {
                currentUserModel = documentSnapshot.toObject(UserModel.class);
                if (currentUserModel != null) {
                  Log.d(TAG, "Current user model loaded: " + (currentUserModel.getUsername() != null ? currentUserModel.getUsername() : "N/A") +
                          ", DisplayName: " + (currentUserModel.getDisplayName() != null ? currentUserModel.getDisplayName() : "N/A"));
                  updatePostButtonState();
                } else {
                  Log.e(TAG, "currentUserModel is null after deserialization in Compose.");
                  Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show();
                  binding.btnPost.setText(getString(R.string.error_button_text));
                  binding.btnPost.setEnabled(false);
                }
              } else {
                Log.e(TAG, "User profile document does not exist in Compose.");
                Toast.makeText(this, getString(R.string.user_profile_not_found), Toast.LENGTH_SHORT).show();
                binding.btnPost.setText(getString(R.string.error_button_text));
                binding.btnPost.setEnabled(false);
              }
            })
            .addOnFailureListener(e -> {
              Log.e(TAG, "Failed to load user data in Compose: " + e.getMessage());
              Toast.makeText(this, getString(R.string.error_loading_user_data_generic), Toast.LENGTH_SHORT).show();
              binding.btnPost.setText(getString(R.string.error_button_text));
              binding.btnPost.setEnabled(false);
            });
  }

  private void setupListeners() {
    binding.btnClose.setOnClickListener(v -> finish());
    binding.btnPost.setOnClickListener(v -> createNewPost());

    binding.btnAddPhoto.setOnClickListener(v -> pickMultipleMedia.launch(new String[]{"image/*"}));
    binding.btnAddVideo.setOnClickListener(v -> {
      boolean hasVideo = selectedMediaUris.stream().anyMatch(uri -> {
        String mimeType = getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
      }) || existingMediaUrls.stream().anyMatch(url ->
              url.toLowerCase().matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$")
      );

      if (hasVideo) {
        Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show();
      } else {
        pickMultipleMedia.launch(new String[]{"video/*"});
      }
    });

    binding.etContent.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        updateCharCount(s.length());
        checkAndFetchLinkPreview(s.toString());
      }

      @Override
      public void afterTextChanged(Editable s) {
        updatePostButtonState();
      }
    });

    binding.layoutLinkPreview.btnRemoveLink.setOnClickListener(v -> removeLinkPreview());
  }

  private void setupMediaPreviewRecyclerView() {
    mediaPreviewAdapter = new MediaPreviewAdapter(selectedMediaUris, existingMediaUrls, this);
    binding.rvMediaPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    binding.rvMediaPreview.setAdapter(mediaPreviewAdapter);
  }

  private void handleMediaSelection(Uri uri, String type) {
    if (uri == null) return;

    if (selectedMediaUris.size() + existingMediaUrls.size() >= 4) {
      Toast.makeText(this, getString(R.string.max_media_limit, 4), Toast.LENGTH_SHORT).show();
      return;
    }

    if (PostModel.TYPE_VIDEO.equals(type)) {
      boolean videoExists = selectedMediaUris.stream().anyMatch(u -> {
        String mimeType = getContentResolver().getType(u);
        return mimeType != null && mimeType.startsWith("video/");
      }) || existingMediaUrls.stream().anyMatch(url ->
              url.toLowerCase().matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$")
      );
      if (videoExists) {
        Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show();
        return;
      }
    }

    final int takeFlags = getIntent().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
    try {
      if (takeFlags != 0) {
        getContentResolver().takePersistableUriPermission(uri, takeFlags);
      }
    } catch (SecurityException e) {
      Log.e(TAG, "Failed to take persistable URI permission: " + e.getMessage() + " for URI: " + uri.toString());
    }

    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
      if (inputStream == null) {
        throw new Exception("Could not open InputStream for URI.");
      }
      selectedMediaUris.add(uri);
      mediaPreviewAdapter.notifyItemInserted(selectedMediaUris.size() -1 + existingMediaUrls.size());
      binding.rvMediaPreview.setVisibility(View.VISIBLE);
    } catch (Exception e) {
      Log.e(TAG, "Error opening URI InputStream, media might be inaccessible: " + e.getMessage() + " for URI: " + uri.toString());
      Toast.makeText(this, getString(R.string.media_access_error), Toast.LENGTH_LONG).show();
      return;
    }
    updatePostButtonState();
  }


  @Override
  public void onMediaRemoved(int position) {
    if (position < existingMediaUrls.size()) {
      existingMediaUrls.remove(position);
    } else {
      int uriPosition = position - existingMediaUrls.size();
      if (uriPosition >= 0 && uriPosition < selectedMediaUris.size()) {
        selectedMediaUris.remove(uriPosition);
      }
    }
    mediaPreviewAdapter.notifyItemRemoved(position);
    if (selectedMediaUris.isEmpty() && existingMediaUrls.isEmpty()) {
      binding.rvMediaPreview.setVisibility(View.GONE);
    }
    updatePostButtonState();
  }

  private void updateCharCount(int count) {
    binding.tvCharCount.setText(getString(R.string.char_count_format, count, PostModel.MAX_CONTENT_LENGTH));
    if (count > PostModel.MAX_CONTENT_LENGTH) {
      binding.tvCharCount.setTextColor(getResources().getColor(R.color.error_red, getTheme()));
    } else {
      binding.tvCharCount.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
    }
  }

  private void checkAndFetchLinkPreview(String content) {
    Matcher matcher = urlPattern.matcher(content);
    if (matcher.find()) {
      String url = matcher.group();
      if (currentLinkPreview != null && url.equals(currentLinkPreview.getUrl())) {
        return;
      }
      binding.layoutLinkPreview.getRoot().setVisibility(View.VISIBLE);
      binding.layoutLinkPreview.tvLinkTitle.setText(R.string.loading_preview);
      binding.layoutLinkPreview.tvLinkDescription.setText("");
      binding.layoutLinkPreview.tvLinkDomain.setText("");
      binding.layoutLinkPreview.ivLinkImage.setImageResource(R.drawable.ic_cover_placeholder);

      LinkPreviewFetcher.fetch(url, new LinkPreviewFetcher.LinkPreviewCallback() {
        @Override
        public void onPreviewReceived(PostModel.LinkPreview preview) {
          if (preview != null && preview.getTitle() != null) {
            currentLinkPreview = preview;
            binding.layoutLinkPreview.tvLinkTitle.setText(preview.getTitle());
            binding.layoutLinkPreview.tvLinkDescription.setText(preview.getDescription());
            if (preview.getUrl() != null) { // التحقق من أن الرابط ليس null قبل استخدامه
              binding.layoutLinkPreview.tvLinkDomain.setText(Uri.parse(preview.getUrl()).getHost());
            }
            if (preview.getImageUrl() != null && !preview.getImageUrl().isEmpty()) {
              Glide.with(ComposePostActivity.this)
                      .load(preview.getImageUrl())
                      .placeholder(R.drawable.ic_cover_placeholder)
                      .into(binding.layoutLinkPreview.ivLinkImage);
            }
          } else {
            onLinkPreviewError(getString(R.string.no_title));
          }
        }
        @Override
        public void onError(String errorMsg) {
          onLinkPreviewError(getString(R.string.link_preview_failed));
        }
      });
    } else {
      removeLinkPreview();
    }
  }

  private void onLinkPreviewError(String errorMsg) {
    Log.w(TAG, "Link preview error: " + errorMsg);
    currentLinkPreview = null;
    binding.layoutLinkPreview.getRoot().setVisibility(View.GONE);
  }

  private void removeLinkPreview() {
    currentLinkPreview = null;
    binding.layoutLinkPreview.getRoot().setVisibility(View.GONE);
  }

  private void updatePostButtonState() {
    boolean hasContent = binding.etContent.getText().toString().trim().length() > 0;
    boolean hasMedia = !selectedMediaUris.isEmpty() || !existingMediaUrls.isEmpty();
    boolean contentWithinLimit = binding.etContent.getText().toString().length() <= PostModel.MAX_CONTENT_LENGTH;

    if (currentUserModel != null) {
      binding.btnPost.setEnabled((hasContent || hasMedia) && contentWithinLimit);
      binding.btnPost.setText(R.string.post);
    } else {
      binding.btnPost.setEnabled(false);
    }
  }

  private void createNewPost() {
    String content = binding.etContent.getText().toString().trim();

    if (currentUserModel == null) {
      Toast.makeText(this, getString(R.string.error_user_data_not_loaded_wait), Toast.LENGTH_SHORT).show();
      return;
    }

    if (content.isEmpty() && selectedMediaUris.isEmpty() && existingMediaUrls.isEmpty() && currentLinkPreview == null) {
      Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show();
      return;
    }
    if (content.length() > PostModel.MAX_CONTENT_LENGTH) {
      Toast.makeText(this, R.string.char_limit_exceeded, Toast.LENGTH_SHORT).show();
      return;
    }

    showProgress(true);

    if (!selectedMediaUris.isEmpty()) {
      uploadMediaAndCreatePost(content);
    } else {
      // إذا لم تكن هناك وسائط جديدة، ولكن قد تكون هناك وسائط موجودة أو رابط
      savePostToFirestore(content, new ArrayList<>(existingMediaUrls), PostModel.TYPE_TEXT); // النوع قد يتغير إذا كان هناك رابط فقط
    }
  }

  private void uploadMediaAndCreatePost(String content) {
    List<Task<Uri>> uploadTasks = new ArrayList<>();
    List<String> uploadedMediaUrls = new ArrayList<>(existingMediaUrls);
    final String[] finalContentType = {PostModel.TYPE_TEXT}; // Default to text

    for (Uri fileUri : selectedMediaUris) {
      String mimeType = getContentResolver().getType(fileUri);
      String fileExtension = getFileExtension(fileUri);

      boolean isVideoType = (mimeType != null && mimeType.startsWith("video/")) ||
              (fileExtension != null && PostModel.VIDEO_EXTENSIONS.contains(fileExtension.replace(".", "").toLowerCase()));
      boolean isImageType = (mimeType != null && mimeType.startsWith("image/")) ||
              (fileExtension != null && (fileExtension.equalsIgnoreCase(".jpg") || fileExtension.equalsIgnoreCase(".jpeg") || fileExtension.equalsIgnoreCase(".png") || fileExtension.equalsIgnoreCase(".gif")));


      if (isVideoType) {
        finalContentType[0] = PostModel.TYPE_VIDEO;
      } else if (isImageType) {
        if (!PostModel.TYPE_VIDEO.equals(finalContentType[0])) {
          finalContentType[0] = PostModel.TYPE_IMAGE;
        }
      } else {
        Log.w(TAG, "Could not determine media type for URI: " + fileUri + ". Defaulting or skipping.");
        // قد ترغب في إظهار رسالة خطأ للمستخدم هنا أو تخطي هذا الملف
        // continue;
      }

      String userId = currentUserModel.getUserId();
      String fileName = "post_media/" + userId + "/" + UUID.randomUUID().toString() + fileExtension;
      StorageReference mediaRef = storage.getReference().child(fileName);
      UploadTask uploadTask = mediaRef.putFile(fileUri);

      Task<Uri> getUrlTask = uploadTask.continueWithTask(task -> {
        if (!task.isSuccessful()) {
          if (task.getException() != null) {
            throw task.getException();
          }
          throw new Exception("Upload failed without specific exception for " + fileUri);
        }
        return mediaRef.getDownloadUrl();
      });
      uploadTasks.add(getUrlTask);
    }


    Tasks.whenAllSuccess(uploadTasks).addOnSuccessListener(results -> {
      for (Object result : results) {
        if (result instanceof Uri) {
          uploadedMediaUrls.add(((Uri) result).toString());
        }
      }

      String postContentType = finalContentType[0];
      if (content.isEmpty() && !uploadedMediaUrls.isEmpty()) {
        if (uploadedMediaUrls.stream().anyMatch(url -> url.toLowerCase().matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$"))) {
          postContentType = PostModel.TYPE_VIDEO;
        } else if (uploadedMediaUrls.stream().anyMatch(url -> url.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)(\\?.*)?$"))) {
          postContentType = PostModel.TYPE_IMAGE;
        }
      } else if (content.isEmpty() && uploadedMediaUrls.isEmpty() && currentLinkPreview == null) {
        showProgress(false);
        Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show();
        return;
      } else if (!content.isEmpty()){ // إذا كان هناك نص، فيمكن أن يكون نوع المنشور نصيًا حتى لو كانت هناك وسائط
        postContentType = PostModel.TYPE_TEXT;
      }
      // إذا كان هناك رابط فقط وبدون محتوى نصي أو وسائط، فيمكنك اعتباره من نوع TYPE_TEXT مع linkPreview

      savePostToFirestore(content, uploadedMediaUrls, postContentType);

    }).addOnFailureListener(e -> {
      showProgress(false);
      Toast.makeText(ComposePostActivity.this, getString(R.string.media_upload_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
      Log.e(TAG, "Media upload failed: " + e.getMessage(), e);
    });
  }


  private void savePostToFirestore(String content, List<String> mediaUrls, String contentType) {
    Log.d(TAG, "Attempting to save post to Firestore.");
    Log.d(TAG, "Author ID: " + (currentUserModel != null ? currentUserModel.getUserId() : "currentUserModel is NULL"));
    Log.d(TAG, "Author Username: " + (currentUserModel != null && currentUserModel.getUsername() != null ? currentUserModel.getUsername() : "N/A"));
    Log.d(TAG, "Author DisplayName: " + (currentUserModel != null && currentUserModel.getDisplayName() != null ? currentUserModel.getDisplayName() : "N/A"));
    Log.d(TAG, "Content: [" + content + "]");
    Log.d(TAG, "Content Type: " + contentType);
    Log.d(TAG, "Media URLs: " + (mediaUrls != null ? mediaUrls.toString() : "null"));


    if (currentUserModel == null || currentUserModel.getUserId() == null) {
      Log.e(TAG, "Cannot save post: currentUserModel or its ID is null.");
      Toast.makeText(this, getString(R.string.error_user_data_cannot_save_post), Toast.LENGTH_LONG).show();
      showProgress(false);
      return;
    }

    // التحقق الأساسي: إذا لم يكن هناك محتوى نصي ولا وسائط ولا رابط، فلا تنشئ المنشور
    if (content.isEmpty() && (mediaUrls == null || mediaUrls.isEmpty()) && currentLinkPreview == null) {
      Log.e(TAG, "Attempting to save an empty post.");
      Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show();
      showProgress(false);
      return;
    }

    PostModel post = new PostModel(currentUserModel.getUserId(), content);

    String displayNameToSave = (currentUserModel.getDisplayName() != null && !currentUserModel.getDisplayName().isEmpty())
            ? currentUserModel.getDisplayName()
            : (currentUserModel.getUsername() != null ? currentUserModel.getUsername() : "Unknown User");
    post.setAuthorDisplayName(displayNameToSave);

    post.setAuthorUsername(currentUserModel.getUsername() != null ? currentUserModel.getUsername() : "unknown_user");
    post.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl());
    post.setAuthorVerified(currentUserModel.isVerified());
    // سيتم تعيين createdAt بواسطة Firestore باستخدام FieldValue.serverTimestamp()
    post.setMediaUrls(mediaUrls != null ? mediaUrls : new ArrayList<>());

    // تحديد contentType بشكل أدق
    if (mediaUrls != null && !mediaUrls.isEmpty()) {
      // إذا كان هناك فيديو، فالنوع هو فيديو
      if (mediaUrls.stream().anyMatch(url -> url.toLowerCase().matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$"))) {
        post.setContentType(PostModel.TYPE_VIDEO);
      } else { // وإلا، إذا كانت هناك وسائط، فهي صور
        post.setContentType(PostModel.TYPE_IMAGE);
      }
    } else { // إذا لم تكن هناك وسائط، فالنوع هو نص (وقد يحتوي على رابط)
      post.setContentType(PostModel.TYPE_TEXT);
    }


    if (currentLinkPreview != null) {
      List<PostModel.LinkPreview> previews = new ArrayList<>();
      previews.add(currentLinkPreview);
      post.setLinkPreviews(previews);
    }

    db.collection("posts").add(post)
            .addOnSuccessListener(documentReference -> {
              String generatedPostId = documentReference.getId();
              Log.d(TAG, "Post added with ID: " + generatedPostId);
              Map<String, Object> updates = new HashMap<>();
              updates.put("postId", generatedPostId);
              updates.put("createdAt", FieldValue.serverTimestamp()); // استخدام الطابع الزمني للخادم

              documentReference.update(updates)
                      .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Post ID and server timestamp updated successfully for " + generatedPostId);
                        showProgress(false);
                        Toast.makeText(ComposePostActivity.this, R.string.post_success, Toast.LENGTH_SHORT).show();
                        clearMediaPreview();
                        binding.etContent.setText("");
                        removeLinkPreview();
                        finish();
                      })
                      .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating post with postId and server timestamp for " + generatedPostId, e);
                        Toast.makeText(ComposePostActivity.this, getString(R.string.post_created_finalize_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                        showProgress(false);
                        // finish(); // قد لا ترغب في الإغلاق هنا للسماح بإعادة المحاولة
                      });
            })
            .addOnFailureListener(e -> {
              showProgress(false);
              Toast.makeText(this, getString(R.string.post_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
              Log.e(TAG, "Error adding post to Firestore: " + e.getMessage(),e);
            });
  }

  private void showProgress(boolean show) {
    binding.progressContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    binding.btnPost.setEnabled(!show);
    binding.btnAddPhoto.setEnabled(!show);
    binding.btnAddVideo.setEnabled(!show);
    binding.etContent.setEnabled(!show);
  }

  private void clearMediaPreview() {
    selectedMediaUris.clear();
    existingMediaUrls.clear();
    if (mediaPreviewAdapter != null) {
      mediaPreviewAdapter.notifyDataSetChanged();
    }
    binding.rvMediaPreview.setVisibility(View.GONE);
    updatePostButtonState();
  }

  private String getFileExtension(Uri uri) {
    String mimeType = getContentResolver().getType(uri);
    String extension = ".jpg"; // Default extension
    if (mimeType != null) {
      String extFromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
      if (extFromMime != null && !extFromMime.equalsIgnoreCase("null")) {
        extension = "." + extFromMime;
      } else {
        if (mimeType.startsWith("image/jpeg")) extension = ".jpg";
        else if (mimeType.startsWith("image/png")) extension = ".png";
        else if (mimeType.startsWith("image/gif")) extension = ".gif";
        else if (mimeType.startsWith("video/mp4")) extension = ".mp4";
        else if (mimeType.startsWith("video/webm")) extension = ".webm";
        else if (mimeType.startsWith("video/quicktime")) extension = ".mov";
        else if (mimeType.startsWith("video/x-matroska")) extension = ".mkv";
        else if (mimeType.startsWith("video/3gpp")) extension = ".3gp";
        else if (mimeType.startsWith("video/x-msvideo")) extension = ".avi";
      }
    } else {
      String path = uri.getPath();
      if (path != null) {
        int lastDot = path.lastIndexOf(".");
        if (lastDot >= 0 && lastDot < path.length() - 1) {
          extension = path.substring(lastDot);
        }
      }
    }
    return extension;
  }
}