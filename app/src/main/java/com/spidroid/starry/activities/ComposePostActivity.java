package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
// *** التعديل هنا: إضافة استيراد Glide ***
import com.bumptech.glide.Glide;

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
  private Uri currentMediaUri;

  private Pattern urlPattern = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
  private PostModel.LinkPreview currentLinkPreview;

  private final ActivityResultLauncher<String> pickImage =
          registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
              handleMediaSelection(uri, PostModel.TYPE_IMAGE);
            }
          });

  private final ActivityResultLauncher<String> pickVideo =
          registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
              handleMediaSelection(uri, PostModel.TYPE_VIDEO);
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
    binding.btnPost.setText("Loading...");

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
      Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot != null && documentSnapshot.exists()) {
                currentUserModel = documentSnapshot.toObject(UserModel.class);
                if (currentUserModel != null) {
                  binding.btnPost.setEnabled(true);
                  binding.btnPost.setText(R.string.post);
                } else {
                  Toast.makeText(this, "Could not deserialize user model.", Toast.LENGTH_SHORT).show();
                  binding.btnPost.setText("Error");
                }
              } else {
                Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                binding.btnPost.setText("Error");
              }
            })
            .addOnFailureListener(e -> {
              Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
              binding.btnPost.setText("Error");
            });
  }

  private void setupListeners() {
    binding.btnClose.setOnClickListener(v -> finish());
    binding.btnPost.setOnClickListener(v -> createNewPost());

    binding.btnAddPhoto.setOnClickListener(v -> pickImage.launch("image/*"));
    binding.btnAddVideo.setOnClickListener(v -> pickVideo.launch("video/*"));

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

  /**
   * يتعامل مع اختيار الوسائط (صورة أو فيديو).
   * يدعم حاليًا وسيطة واحدة فقط. إذا تم اختيار وسيطة جديدة، يتم استبدال السابقة.
   *
   * @param uri URI الوسيطة المختارة.
   * @param type نوع الوسيطة (صورة أو فيديو).
   */
  private void handleMediaSelection(Uri uri, String type) {
    if (uri == null) return;

    // مسح أي وسائط سابقة (للسماح بواحدة فقط حاليًا)
    selectedMediaUris.clear();
    existingMediaUrls.clear(); // تأكد من مسح أي URLs موجودة أيضًا
    currentMediaUri = uri; // تخزين URI الوسيطة الحالية

    selectedMediaUris.add(uri); // إضافة الوسيطة الجديدة
    mediaPreviewAdapter.notifyDataSetChanged(); // تحديث RecyclerView
    binding.rvMediaPreview.setVisibility(View.VISIBLE); // إظهار RecyclerView

    updatePostButtonState(); // تحديث حالة زر النشر
  }

  /**
   * دالة CallBack عندما يقوم المستخدم بإزالة وسيطة من المعاينة.
   *
   * @param position الموضع الذي تمت إزالته.
   */
  @Override
  public void onMediaRemoved(int position) {
    // إذا كانت الوسيطة من القائمة الموجودة (existingMediaUrls)
    if (position < existingMediaUrls.size()) {
      existingMediaUrls.remove(position);
    } else {
      // إذا كانت الوسيطة من القائمة الجديدة (selectedMediaUris)
      int uriPosition = position - existingMediaUrls.size();
      selectedMediaUris.remove(uriPosition);
    }
    mediaPreviewAdapter.notifyDataSetChanged();
    if (selectedMediaUris.isEmpty() && existingMediaUrls.isEmpty()) {
      binding.rvMediaPreview.setVisibility(View.GONE);
    }
    updatePostButtonState();
  }

  private void updateCharCount(int count) {
    binding.tvCharCount.setText(getString(R.string.char_count_format, count, PostModel.MAX_CONTENT_LENGTH));
    if (count > PostModel.MAX_CONTENT_LENGTH) {
      binding.tvCharCount.setTextColor(getResources().getColor(R.color.error_red));
    } else {
      binding.tvCharCount.setTextColor(getResources().getColor(R.color.text_secondary));
    }
  }

  /**
   * تتحقق من وجود رابط في المحتوى وتجلب معاينته.
   *
   * @param content المحتوى النصي للمنشور.
   */
  private void checkAndFetchLinkPreview(String content) {
    Matcher matcher = urlPattern.matcher(content);
    if (matcher.find()) {
      String url = matcher.group();
      // تجنب جلب المعاينة لنفس الرابط مرتين
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
            onLinkPreviewError("No title found");
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
    currentLinkPreview = null;
    binding.layoutLinkPreview.getRoot().setVisibility(View.GONE);
  }

  private void removeLinkPreview() {
    currentLinkPreview = null;
    binding.layoutLinkPreview.getRoot().setVisibility(View.GONE);
  }

  /**
   * تحديث حالة زر النشر بناءً على المحتوى والوسائط المختارة.
   */
  private void updatePostButtonState() {
    boolean hasContent = binding.etContent.getText().toString().trim().length() > 0;
    boolean hasMedia = !selectedMediaUris.isEmpty();
    boolean contentWithinLimit = binding.etContent.getText().toString().length() <= PostModel.MAX_CONTENT_LENGTH;

    binding.btnPost.setEnabled((hasContent || hasMedia) && contentWithinLimit && currentUserModel != null);
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

    if (!selectedMediaUris.isEmpty()) {
      uploadMediaAndCreatePost(content);
    } else {
      savePostToFirestore(content, new ArrayList<>(), PostModel.TYPE_TEXT);
    }
  }

  /**
   * تقوم بتحميل الوسائط إلى Firebase Storage ثم تنشئ المنشور.
   * يدعم حاليًا تحميل وسيطة واحدة فقط.
   *
   * @param content المحتوى النصي للمنشور.
   */
  private void uploadMediaAndCreatePost(String content) {
    if (selectedMediaUris.isEmpty()) {
      Toast.makeText(this, "No media selected for upload.", Toast.LENGTH_SHORT).show();
      showProgress(false);
      return;
    }

    Uri fileUri = selectedMediaUris.get(0);
    String mimeType = getContentResolver().getType(fileUri);
    String fileExtension = getFileExtension(fileUri);
    String type = (mimeType != null && mimeType.startsWith("video")) ? PostModel.TYPE_VIDEO : PostModel.TYPE_IMAGE;

    if (type.equals(PostModel.TYPE_VIDEO) && selectedMediaUris.size() > 1) {
      Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show();
      showProgress(false);
      return;
    }

    showProgress(true);

    String userId = currentUserModel.getUserId();
    String fileName = "post_media/" + userId + "/" + UUID.randomUUID().toString() + fileExtension;
    StorageReference mediaRef = storage.getReference().child(fileName);

    UploadTask uploadTask = mediaRef.putFile(fileUri);
    uploadTask.addOnSuccessListener(taskSnapshot -> {
      mediaRef.getDownloadUrl().addOnSuccessListener(uri -> {
        List<String> mediaUrls = new ArrayList<>();
        mediaUrls.add(uri.toString());
        savePostToFirestore(content, mediaUrls, type);
      }).addOnFailureListener(e -> {
        showProgress(false);
        Toast.makeText(ComposePostActivity.this, R.string.media_upload_failed, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Failed to get download URL: " + e.getMessage());
      });
    }).addOnFailureListener(e -> {
      showProgress(false);
      Toast.makeText(ComposePostActivity.this, R.string.media_upload_failed, Toast.LENGTH_SHORT).show();
      Log.e(TAG, "Media upload failed: " + e.getMessage());
    });
  }

  /**
   * يحفظ المنشور في Firestore.
   *
   * @param content   المحتوى النصي للمنشور.
   * @param mediaUrls قائمة بـ URLs الوسائط (إذا وجدت).
   * @param contentType نوع المحتوى (نص، صورة، فيديو).
   */
  private void savePostToFirestore(String content, List<String> mediaUrls, String contentType) {
    PostModel post = new PostModel(currentUserModel.getUserId(), content);
    post.setAuthorUsername(currentUserModel.getUsername());
    post.setAuthorDisplayName(currentUserModel.getDisplayName());
    post.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl());
    post.setAuthorVerified(currentUserModel.isVerified());
    post.setCreatedAt(new Date());
    post.setMediaUrls(mediaUrls);
    post.setContentType(contentType);

    if (currentLinkPreview != null) {
      List<PostModel.LinkPreview> previews = new ArrayList<>();
      previews.add(currentLinkPreview);
      post.setLinkPreviews(previews);
    }

    db.collection("posts").add(post)
            .addOnSuccessListener(documentReference -> {
              Map<String, Object> updates = new HashMap<>();
              updates.put("postId", documentReference.getId());
              documentReference.update(updates)
                      .addOnSuccessListener(aVoid -> {
                        showProgress(false);
                        Toast.makeText(ComposePostActivity.this, R.string.post_success, Toast.LENGTH_SHORT).show();
                        clearMediaPreview();
                        binding.etContent.setText("");
                        removeLinkPreview();
                        finish();
                      })
                      .addOnFailureListener(e -> {
                        Toast.makeText(ComposePostActivity.this, R.string.post_updated + ", but failed to save Post ID: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating post with postId: " + e.getMessage());
                        showProgress(false);
                        clearMediaPreview();
                        binding.etContent.setText("");
                        removeLinkPreview();
                        finish();
                      });
            })
            .addOnFailureListener(e -> {
              binding.progressContainer.setVisibility(View.GONE);
              binding.btnPost.setEnabled(true);
              Toast.makeText(this, R.string.post_failed + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
              Log.e(TAG, "Error adding post: " + e.getMessage());
            });
  }

  private void showProgress(boolean show) {
    binding.progressContainer.setVisibility(show ? View.VISIBLE : View.GONE);
    binding.btnPost.setEnabled(!show);
    binding.btnAddPhoto.setEnabled(!show);
    binding.btnAddVideo.setEnabled(!show);
  }
  private void clearMediaPreview() {
    selectedMediaUris.clear();
    existingMediaUrls.clear(); // إذا كنت تستخدمها من مكان آخر، وإلا يمكن إزالتها إذا كانت فقط للاستخدام المستقبلي
    if (mediaPreviewAdapter != null) {
      mediaPreviewAdapter.notifyDataSetChanged();
    }
    binding.rvMediaPreview.setVisibility(View.GONE);
    currentMediaUri = null; // إعادة تعيين الوسيطة الحالية
    updatePostButtonState(); // تحديث حالة زر النشر
    // Toast.makeText(this, "Media preview cleared", Toast.LENGTH_SHORT).show(); // يمكنك إبقاء هذا أو إزالته
  }
  private String getFileExtension(Uri uri) {
    String mimeType = getContentResolver().getType(uri);
    if (mimeType == null) return ".jpg";
    switch (mimeType) {
      case "image/jpeg": return ".jpg";
      case "image/png": return ".png";
      case "image/gif": return ".gif";
      case "video/mp4": return ".mp4";
      case "video/webm": return ".webm";
      default: return "";
    }
  }
}