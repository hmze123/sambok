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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // ★ تأكد من وجود هذا الاستيراد
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue; // ★ استيراد FieldValue
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

  private static final String TAG = "ComposePostActivity"; // ★ تعريف TAG لـ Log
  private ActivityComposePostBinding binding;
  private FirebaseAuth auth;
  private FirebaseFirestore db;
  private FirebaseStorage storage;
  private UserModel currentUserModel;

  private MediaPreviewAdapter mediaPreviewAdapter;
  private List<Uri> selectedMediaUris = new ArrayList<>();
  private List<String> existingMediaUrls = new ArrayList<>(); // إذا كنت تستخدمها من مكان آخر
  // private Uri currentMediaUri; // لم تعد هناك حاجة للاحتفاظ بوسيطة واحدة فقط بهذه الطريقة

  private Pattern urlPattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
  private PostModel.LinkPreview currentLinkPreview;

  private final ActivityResultLauncher<String[]> pickMultipleMedia =
          registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
            if (uris != null && !uris.isEmpty()) {
              // بما أننا ندعم وسيطة واحدة حاليًا (صورة أو فيديو)، سنأخذ أول عنصر فقط
              // يمكنك تعديل هذا المنطق لاحقًا لدعم وسائط متعددة
              if (selectedMediaUris.size() + existingMediaUrls.size() + uris.size() > 4) { // حد 4 وسائط
                Toast.makeText(this, getString(R.string.max_media_limit, 4), Toast.LENGTH_SHORT).show();
                return;
              }
              for(Uri uri : uris){
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null) {
                  if (mimeType.startsWith("image/")) {
                    handleMediaSelection(uri, PostModel.TYPE_IMAGE);
                  } else if (mimeType.startsWith("video/")) {
                    if (selectedMediaUris.stream().anyMatch(u -> getContentResolver().getType(u).startsWith("video/")) ||
                            existingMediaUrls.stream().anyMatch(url -> url.toLowerCase().matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$"))){
                      Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show();
                      return; // لا تسمح بإضافة فيديو إذا كان هناك فيديو آخر
                    }
                    handleMediaSelection(uri, PostModel.TYPE_VIDEO);
                  } else {
                    Toast.makeText(this, "Unsupported file type: " + mimeType, Toast.LENGTH_SHORT).show();
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

    // تعطيل زر النشر مبدئيًا حتى يتم تحميل بيانات المستخدم أو إدخال محتوى
    binding.btnPost.setEnabled(false);
    binding.btnPost.setText("Loading..."); // أو أي نص مناسب

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
      Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot != null && documentSnapshot.exists()) {
                currentUserModel = documentSnapshot.toObject(UserModel.class);
                if (currentUserModel != null) {
                  Log.d(TAG, "Current user model loaded: " + currentUserModel.getUsername() + ", DisplayName: " + currentUserModel.getDisplayName());
                  // تمكين زر النشر بعد تحميل بيانات المستخدم بنجاح
                  updatePostButtonState(); // استدعاء هذه الدالة لتحديث حالة الزر
                } else {
                  Log.e(TAG, "currentUserModel is null after deserialization in Compose.");
                  Toast.makeText(this, "Could not load user profile.", Toast.LENGTH_SHORT).show();
                  binding.btnPost.setText("Error"); // أو يمكنك تعطيله
                  binding.btnPost.setEnabled(false);
                }
              } else {
                Log.e(TAG, "User profile document does not exist in Compose.");
                Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                binding.btnPost.setText("Error");
                binding.btnPost.setEnabled(false);
              }
            })
            .addOnFailureListener(e -> {
              Log.e(TAG, "Failed to load user data in Compose: " + e.getMessage());
              Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
              binding.btnPost.setText("Error");
              binding.btnPost.setEnabled(false);
            });
  }

  private void setupListeners() {
    binding.btnClose.setOnClickListener(v -> finish());
    binding.btnPost.setOnClickListener(v -> createNewPost());

    binding.btnAddPhoto.setOnClickListener(v -> pickMultipleMedia.launch("image/*"));
    binding.btnAddVideo.setOnClickListener(v -> {
      // تحقق مما إذا كان هناك فيديو بالفعل
      boolean hasVideo = selectedMediaUris.stream().anyMatch(uri -> {
        String mimeType = getContentResolver().getType(uri);
        return mimeType != null && mimeType.startsWith("video/");
      }) || existingMediaUrls.stream().anyMatch(url ->
              url.toLowerCase().matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$")
      );

      if (hasVideo) {
        Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show();
      } else {
        pickMultipleMedia.launch("video/*");
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

    // التحقق من عدد الوسائط الحالية (الجديدة + الموجودة)
    if (selectedMediaUris.size() + existingMediaUrls.size() >= 4) {
      Toast.makeText(this, getString(R.string.max_media_limit, 4), Toast.LENGTH_SHORT).show();
      return;
    }

    // إذا كان النوع فيديو، تأكد من عدم وجود فيديو آخر
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


    final int takeFlags = getIntent().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    try {
      // محاولة أخذ أذونات دائمة إذا لم تكن موجودة بالفعل
      getContentResolver().takePersistableUriPermission(uri, takeFlags);
    } catch (SecurityException e) {
      Log.e(TAG, "Failed to take persistable URI permission: " + e.getMessage());
      // لا يزال بإمكاننا محاولة القراءة إذا كان لدينا إذن مؤقت
    }


    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
      // إذا نجح فتح InputStream، فهذا يعني أن لدينا وصولاً للقراءة
      selectedMediaUris.add(uri);
      mediaPreviewAdapter.notifyItemInserted(selectedMediaUris.size() -1 + existingMediaUrls.size());
      binding.rvMediaPreview.setVisibility(View.VISIBLE);
    } catch (Exception e) {
      Log.e(TAG, "Error opening URI InputStream, media might be inaccessible: " + e.getMessage());
      Toast.makeText(this, "Selected media is not accessible. Please choose another file.", Toast.LENGTH_LONG).show();
      return; // لا تضف الـ URI إذا لم نتمكن من الوصول إليه
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
    // mediaPreviewAdapter.notifyItemRangeChanged(position, getItemCount()); // لتحديث المواضع
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
            binding.layoutLinkPreview.tvLinkDomain.setText(Uri.parse(preview.getUrl()).getHost());
            if (preview.getImageUrl() != null && !preview.getImageUrl().isEmpty()) {
              Glide.with(ComposePostActivity.this)
                      .load(preview.getImageUrl())
                      .placeholder(R.drawable.ic_cover_placeholder)
                      .into(binding.layoutLinkPreview.ivLinkImage);
            }
          } else {
            onLinkPreviewError("No title found for preview.");
          }
        }
        @Override
        public void onError(String errorMsg) {
          onLinkPreviewError(errorMsg);
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

    // تمكين زر النشر فقط إذا كان المستخدم قد تم تحميله، وهناك محتوى أو وسائط، والمحتوى ضمن الحد المسموح به
    if (currentUserModel != null) {
      binding.btnPost.setEnabled((hasContent || hasMedia) && contentWithinLimit);
      binding.btnPost.setText(R.string.post); // إعادة النص إلى "Post"
    } else {
      binding.btnPost.setEnabled(false); // يبقى معطلاً إذا لم يتم تحميل المستخدم
      // يمكن ترك النص "Loading..." أو تغييره إلى "Error" إذا فشل التحميل
    }
  }

  private void createNewPost() {
    String content = binding.etContent.getText().toString().trim();

    if (currentUserModel == null) {
      Toast.makeText(this, "User data not loaded yet, please wait.", Toast.LENGTH_SHORT).show();
      return;
    }

    if (content.isEmpty() && selectedMediaUris.isEmpty() && existingMediaUrls.isEmpty()) {
      Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show();
      return;
    }
    if (content.length() > PostModel.MAX_CONTENT_LENGTH) {
      Toast.makeText(this, R.string.char_limit_exceeded, Toast.LENGTH_SHORT).show();
      return;
    }

    showProgress(true); // إظهار مؤشر التقدم

    if (!selectedMediaUris.isEmpty()) {
      uploadMediaAndCreatePost(content);
    } else {
      // إذا لم تكن هناك وسائط جديدة ليتم تحميلها، ولكن قد تكون هناك وسائط موجودة (في حالة التعديل)
      // حاليًا، هذا الكود للإنشاء فقط، لذا existingMediaUrls ستكون فارغة
      savePostToFirestore(content, new ArrayList<>(existingMediaUrls), PostModel.TYPE_TEXT);
    }
  }

  private void uploadMediaAndCreatePost(String content) {
    List<Task<Uri>> uploadTasks = new ArrayList<>();
    List<String> uploadedMediaUrls = new ArrayList<>(existingMediaUrls); // ابدأ بالوسائط الموجودة
    final String[] finalContentType = {PostModel.TYPE_TEXT}; // نوع المحتوى النهائي للمنشور

    for (Uri fileUri : selectedMediaUris) {
      String mimeType = getContentResolver().getType(fileUri);
      String fileExtension = getFileExtension(fileUri);
      if (mimeType != null && mimeType.startsWith("video/")) {
        finalContentType[0] = PostModel.TYPE_VIDEO; // تحديث نوع المحتوى إذا كان هناك فيديو
      } else if (mimeType != null && mimeType.startsWith("image/")) {
        if (!PostModel.TYPE_VIDEO.equals(finalContentType[0])) { // لا تغير النوع إذا كان فيديو بالفعل
          finalContentType[0] = PostModel.TYPE_IMAGE;
        }
      }


      String userId = currentUserModel.getUserId();
      String fileName = "post_media/" + userId + "/" + UUID.randomUUID().toString() + fileExtension;
      StorageReference mediaRef = storage.getReference().child(fileName);
      UploadTask uploadTask = mediaRef.putFile(fileUri);

      Task<Uri> getUrlTask = uploadTask.continueWithTask(task -> {
        if (!task.isSuccessful()) {
          throw task.getException();
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
      savePostToFirestore(content, uploadedMediaUrls, finalContentType[0]);
    }).addOnFailureListener(e -> {
      showProgress(false);
      Toast.makeText(ComposePostActivity.this, R.string.media_upload_failed + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
      Log.e(TAG, "Media upload failed: " + e.getMessage(), e);
    });
  }


  private void savePostToFirestore(String content, List<String> mediaUrls, String contentType) {
    Log.d(TAG, "Attempting to save post to Firestore.");
    Log.d(TAG, "Author ID: " + (currentUserModel != null ? currentUserModel.getUserId() : "currentUserModel is NULL"));
    Log.d(TAG, "Author Username: " + (currentUserModel != null ? currentUserModel.getUsername() : "currentUserModel is NULL"));
    Log.d(TAG, "Author DisplayName: " + (currentUserModel != null ? currentUserModel.getDisplayName() : "currentUserModel is NULL"));
    Log.d(TAG, "Content: " + content);
    Log.d(TAG, "Content Type: " + contentType);
    Log.d(TAG, "Media URLs: " + (mediaUrls != null ? mediaUrls.toString() : "null"));


    if (currentUserModel == null || currentUserModel.getUserId() == null) {
      Log.e(TAG, "Cannot save post: currentUserModel or its ID is null.");
      Toast.makeText(this, "Error: User data not available. Cannot save post.", Toast.LENGTH_LONG).show();
      showProgress(false);
      return;
    }

    PostModel post = new PostModel(currentUserModel.getUserId(), content);
    post.setAuthorUsername(currentUserModel.getUsername());
    post.setAuthorDisplayName(currentUserModel.getDisplayName());
    post.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl());
    post.setAuthorVerified(currentUserModel.isVerified());
    post.setCreatedAt(new Date()); // أو استخدم FieldValue.serverTimestamp() إذا كنت تفضل ذلك
    post.setMediaUrls(mediaUrls);
    post.setContentType(contentType); // تعيين نوع المحتوى

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
              updates.put("createdAt", FieldValue.serverTimestamp()); // ★ استخدام الطابع الزمني للخادم هنا

              documentReference.update(updates)
                      .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Post ID and server timestamp updated successfully for " + generatedPostId);
                        showProgress(false);
                        Toast.makeText(ComposePostActivity.this, R.string.post_success, Toast.LENGTH_SHORT).show();
                        clearMediaPreview();
                        binding.etContent.setText("");
                        removeLinkPreview();
                        finish(); // أغلق النشاط بعد النجاح
                      })
                      .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating post with postId and server timestamp for " + generatedPostId, e);
                        // لا يزال المنشور قد تم إنشاؤه، ولكن بدون postId أو الطابع الزمني للخادم المحدث
                        Toast.makeText(ComposePostActivity.this, "Post created, but failed to finalize: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showProgress(false);
                        finish();
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
    binding.etContent.setEnabled(!show); // تعطيل حقل النص أيضًا أثناء التحميل
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
    String extension = ".jpg"; // افتراضي
    if (mimeType != null) {
      extension = "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }
    return extension;
  }
}