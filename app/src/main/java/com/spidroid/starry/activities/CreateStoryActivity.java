package com.spidroid.starry.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.spidroid.starry.R;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel; // استيراد UserModel

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

public class CreateStoryActivity extends AppCompatActivity {

    private static final int PICK_MEDIA_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final String TAG = "CreateStoryActivity";

    private ImageView ivPreviewMedia;
    private ImageButton btnSelectMedia;
    private ImageButton btnPublishStory;
    private ProgressBar progressBar;

    private Uri selectedMediaUri;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_story);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        setupListeners();

        checkAndRequestPermissions();
    }

    private void initializeViews() {
        ivPreviewMedia = findViewById(R.id.ivPreviewMedia);
        btnSelectMedia = findViewById(R.id.btnSelectMedia);
        btnPublishStory = findViewById(R.id.btnPublishStory);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnSelectMedia.setOnClickListener(v -> openMediaPicker());
        btnPublishStory.setOnClickListener(v -> publishStory());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void openMediaPicker() {
        if (checkStoragePermissions()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/* video/*");
            startActivityForResult(Intent.createChooser(intent, "Select Media"), PICK_MEDIA_REQUEST);
        } else {
            Toast.makeText(this, "Storage permission is required to select media.", Toast.LENGTH_LONG).show();
            requestStoragePermissions();
        }
    }

    private boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                    PERMISSION_REQUEST_CODE
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private void checkAndRequestPermissions() {
        if (!checkStoragePermissions()) {
            requestStoragePermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot select media.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MEDIA_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedMediaUri = data.getData();

            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(selectedMediaUri, takeFlags);

            try (InputStream inputStream = getContentResolver().openInputStream(selectedMediaUri)) {
                Glide.with(this).load(selectedMediaUri).into(ivPreviewMedia);
                ivPreviewMedia.setVisibility(View.VISIBLE);
                btnPublishStory.setEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, "Error opening URI InputStream, media might be inaccessible: " + e.getMessage());
                Toast.makeText(this, "Selected media is not accessible or corrupted. Please choose another file.", Toast.LENGTH_LONG).show();
                selectedMediaUri = null;
                ivPreviewMedia.setVisibility(View.GONE);
                btnPublishStory.setEnabled(false);
            }
        }
    }

    private void publishStory() {
        if (selectedMediaUri == null) {
            Toast.makeText(this, "Please select media for your story", Toast.LENGTH_SHORT).show();
            return;
        }
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to create a story", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!checkStoragePermissions()) {
            Toast.makeText(this, "Storage permission is required to publish story.", Toast.LENGTH_LONG).show();
            requestStoragePermissions();
            return;
        }

        showProgress(true);

        String userId = auth.getCurrentUser().getUid();
        String fileExtension = getFileExtension(selectedMediaUri);
        String fileName = "stories/" + userId + "/" + UUID.randomUUID().toString() + fileExtension;
        StorageReference mediaRef = storage.getReference().child(fileName);

        UploadTask uploadTask = mediaRef.putFile(selectedMediaUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            mediaRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String mediaUrl = uri.toString();
                String mimeType = getContentResolver().getType(selectedMediaUri);
                long duration = 0; // ستحتاج إلى حساب مدة الفيديو إذا كان فيديو

                StoryModel story = new StoryModel(userId, mediaUrl, mimeType != null && mimeType.startsWith("image") ? StoryModel.MEDIA_TYPE_IMAGE : StoryModel.MEDIA_TYPE_VIDEO, duration, null);

                // --- إضافة: جلب معلومات المستخدم قبل حفظ القصة ---
                db.collection("users").document(userId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            UserModel user = documentSnapshot.toObject(UserModel.class);
                            if (user != null) {
                                story.setAuthorDisplayName(user.getDisplayName());
                                story.setAuthorUsername(user.getUsername());
                                story.setAuthorAvatarUrl(user.getProfileImageUrl());
                                story.setAuthorVerified(user.isVerified());
                            }

                            db.collection("stories")
                                    .add(story)
                                    .addOnSuccessListener(documentReference -> {
                                        documentReference.update("storyId", documentReference.getId());
                                        showProgress(false);
                                        Toast.makeText(CreateStoryActivity.this, "Story published!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        showProgress(false);
                                        Toast.makeText(CreateStoryActivity.this, "Failed to publish story: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Error publishing story", e);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            showProgress(false);
                            Toast.makeText(CreateStoryActivity.this, "Failed to get user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error getting user info for story", e);
                        });
                // --- نهاية إضافة: جلب معلومات المستخدم ---

            });
        }).addOnFailureListener(e -> {
            showProgress(false);
            String errorMessage = "Media upload failed: " + e.getMessage();
            if (e instanceof com.google.firebase.storage.StorageException) {
                com.google.firebase.storage.StorageException storageException = (com.google.firebase.storage.StorageException) e;
                if (storageException.getErrorCode() == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND) {
                    errorMessage = "Failed to upload: Selected file not found or inaccessible.";
                } else if (storageException.getErrorCode() == com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED) {
                    errorMessage = "Failed to upload: Unauthorized. Check Firebase Storage Rules.";
                } else if (storageException.getErrorCode() == com.google.firebase.storage.StorageException.ERROR_CANCELED) {
                    errorMessage = "Upload cancelled.";
                }
            }
            Toast.makeText(CreateStoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error uploading media", e);
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPublishStory.setEnabled(!show);
        btnSelectMedia.setEnabled(!show);
    }

    private String getFileExtension(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) return ".jpg";
        switch (mimeType) {
            case "image/jpeg": return ".jpg";
            case "image/png": return ".png";
            case "video/mp4": return ".mp4";
            case "video/webm": return ".webm";
            case "image/gif": return ".gif";
            default: return "";
        }
    }
}