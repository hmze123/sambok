package com.spidroid.starry.activities;

import android.Manifest;
import android.annotation.SuppressLint; // ★ إضافة هذا إذا لم يكن موجودًا
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever; // ★ إضافة هذا
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap; // ★ إضافة هذا
import android.widget.Button; // ★ إضافة هذا
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout; // ★ إضافة هذا
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // ★ إضافة هذا
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.spidroid.starry.R;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date; // ★ إضافة هذا
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CreateStoryActivity extends AppCompatActivity {

    private static final String TAG = "CreateStoryActivity";
    // PICK_MEDIA_REQUEST لم يعد مستخدماً مع ActivityResultLauncher الجديد
    // private static final int PICK_MEDIA_REQUEST = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 101;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth; // تأكد من وجود هذا
    private String currentUserId; // ★★ هذا هو التعريف المهم ★★
    private UserModel currentUserModel;
    private PreviewView cameraPreviewView;
    private ImageView ivStoryImagePreview;
    private PlayerView storyVideoPreview;
    // ...
    private ImageButton btnCaptureStory, btnSwitchCamera, btnGallery; // ★ زر المعرض اسمه btnGallery

    // ... (باقي المتغيرات)

    private LinearLayout editToolsContainer, captureControlsContainer;
    private Button btnPublishStory;
    private ProgressBar pbStoryUploadProgress;

    private Uri selectedMediaUri;
    private Uri capturedImageUri;
    // private Uri recordedVideoUri; // لم يتم استخدامه بعد بشكل كامل


    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private ExecutorService cameraExecutor;
    private int currentCameraSelector = CameraSelector.LENS_FACING_BACK;


    // Launcher جديد لـ pickMedia (يحل محل onActivityResult لـ PICK_MEDIA_REQUEST)
    private final ActivityResultLauncher<Intent> pickMediaActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        final int takeFlags = result.getData().getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        if ((takeFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }

                        selectedMediaUri = uri;
                        capturedImageUri = null;
                        // recordedVideoUri = null; // مسح أي فيديو مسجل بالكاميرا

                        String mimeType = getContentResolver().getType(selectedMediaUri);
                        Log.d(TAG, "Selected media from gallery: " + selectedMediaUri.toString() + ", MIME type: " + mimeType);

                        if (mimeType != null && mimeType.startsWith("video/")) {
                            showVideoPreview(selectedMediaUri);
                        } else if (mimeType != null && mimeType.startsWith("image/")) {
                            showImagePreview(selectedMediaUri);
                        } else {
                            Toast.makeText(this, "Unsupported file type selected.", Toast.LENGTH_LONG).show();
                            selectedMediaUri = null;
                            if (ivStoryImagePreview != null) ivStoryImagePreview.setVisibility(View.GONE);
                            if (storyVideoPreview != null) storyVideoPreview.setVisibility(View.GONE);
                            if (btnPublishStory != null) btnPublishStory.setEnabled(false);
                            return;
                        }
                        // إظهار/إخفاء العناصر المناسبة
                        if (cameraPreviewView != null) cameraPreviewView.setVisibility(View.GONE);
                        if (captureControlsContainer != null) captureControlsContainer.setVisibility(View.GONE);
                        if (editToolsContainer != null) editToolsContainer.setVisibility(View.VISIBLE);
                        if (btnPublishStory != null) btnPublishStory.setEnabled(true);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing media from gallery: " + e.getMessage(), e);
                        Toast.makeText(this, "Could not load selected media. Please try again.", Toast.LENGTH_LONG).show();
                        selectedMediaUri = null;
                        if (ivStoryImagePreview != null) ivStoryImagePreview.setVisibility(View.GONE);
                        if (storyVideoPreview != null) storyVideoPreview.setVisibility(View.GONE);
                        if (btnPublishStory != null) btnPublishStory.setEnabled(false);
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_story);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        setupListeners();

        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        checkAndRequestCameraPermissions();
        loadCurrentUserDetails(); // ★ جلب بيانات المستخدم الحالي
    }

    private void initializeViews() {
        cameraPreviewView = findViewById(R.id.camera_preview_view);
        ivStoryImagePreview = findViewById(R.id.iv_story_image_preview);
        storyVideoPreview = findViewById(R.id.story_video_preview);
        btnCaptureStory = findViewById(R.id.btn_capture_story);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera_story);
        btnGallery = findViewById(R.id.btn_gallery_story);
        editToolsContainer = findViewById(R.id.edit_tools_container);
        captureControlsContainer = findViewById(R.id.capture_controls_container);
        btnPublishStory = findViewById(R.id.btnPublishStory);
        pbStoryUploadProgress = findViewById(R.id.pb_story_upload_progress);
        findViewById(R.id.btnCloseStory).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        if (btnCaptureStory != null) btnCaptureStory.setOnClickListener(v -> takePhoto()); // مؤقتًا لالتقاط الصور فقط
        if (btnSwitchCamera != null) btnSwitchCamera.setOnClickListener(v -> switchCamera());
        if (btnGallery != null) btnGallery.setOnClickListener(v -> openGallery());
        if (btnPublishStory != null) btnPublishStory.setOnClickListener(v -> publishStory());
    }

    private void loadCurrentUserDetails() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUserModel = documentSnapshot.toObject(UserModel.class);
                            if (currentUserModel == null) {
                                Log.e(TAG, "currentUserModel is null after fetching from Firestore.");
                                Toast.makeText(this, "Failed to load your profile data to create story.", Toast.LENGTH_SHORT).show();
                                btnPublishStory.setEnabled(false); // تعطيل النشر إذا لم يتم تحميل المستخدم
                            } else {
                                Log.d(TAG, "Current user details loaded for story creation.");
                                // يمكنك تفعيل زر النشر هنا إذا كان selectedMediaUri ليس null
                                if (selectedMediaUri != null && btnPublishStory != null) {
                                    btnPublishStory.setEnabled(true);
                                }
                            }
                        } else {
                            Log.e(TAG, "Current user document does not exist in Firestore for story creation.");
                            Toast.makeText(this, "Your profile data not found, cannot create story.", Toast.LENGTH_SHORT).show();
                            if (btnPublishStory != null) btnPublishStory.setEnabled(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load current user data for story: " + e.getMessage(), e);
                        Toast.makeText(this, "Error loading your profile data for story.", Toast.LENGTH_SHORT).show();
                        if (btnPublishStory != null) btnPublishStory.setEnabled(false);
                    });
        } else {
            if (btnPublishStory != null) btnPublishStory.setEnabled(false);
        }
    }


    private void checkAndRequestCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera(currentCameraSelector);
        }
    }

    private void checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openMediaPicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
            } else {
                openMediaPicker();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(currentCameraSelector);
            } else {
                Toast.makeText(this, "Camera and Audio permissions are required to use this feature.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openMediaPicker();
            } else {
                Toast.makeText(this, "Storage permission is required to select media.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera(int lensFacing) {
        if (cameraPreviewView == null) {
            Log.e(TAG, "cameraPreviewView is null in startCamera. Cannot start camera.");
            return;
        }
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (cameraProvider == null) {
                    Log.e(TAG, "Camera provider is null");
                    return;
                }
                bindCameraUseCases(lensFacing);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("MissingPermission")
    private void bindCameraUseCases(int lensFacing) {
        if (cameraProvider == null || cameraPreviewView == null) {
            Log.e(TAG, "Camera provider or PreviewView not available, cannot bind use cases.");
            return;
        }
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Recorder recorder = new Recorder.Builder()
        //         .setQualitySelector(QualitySelector.from(Quality.HD))
        //         .build();
        // videoCapture = VideoCapture.withOutput(recorder);

        try {
            // ★ مؤقتًا، سنربط فقط معاينة الكاميرا والتقاط الصور
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture /*, videoCapture */);
            Log.d(TAG, "Camera use cases bound successfully for lens: " + (lensFacing == CameraSelector.LENS_FACING_BACK ? "BACK" : "FRONT"));
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void switchCamera() {
        currentCameraSelector = (currentCameraSelector == CameraSelector.LENS_FACING_BACK) ?
                CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        startCamera(currentCameraSelector);
    }


    private void takePhoto() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture use case is null. Cannot take photo.");
            return;
        }
        String name = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StarryApp");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        capturedImageUri = outputFileResults.getSavedUri();
                        Log.d(TAG, "Photo capture succeeded: " + capturedImageUri);
                        if (capturedImageUri != null) {
                            selectedMediaUri = capturedImageUri;
                            // recordedVideoUri = null; // إذا كان هناك فيديو مسجل سابقًا
                            showImagePreview(capturedImageUri);
                            if (cameraPreviewView != null) cameraPreviewView.setVisibility(View.GONE);
                            if (captureControlsContainer != null) captureControlsContainer.setVisibility(View.GONE);
                            if (editToolsContainer != null) editToolsContainer.setVisibility(View.VISIBLE);
                            if (btnPublishStory != null) btnPublishStory.setEnabled(true);
                        } else {
                            Toast.makeText(CreateStoryActivity.this, "Failed to save image.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        Toast.makeText(CreateStoryActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openGallery() {
        checkAndRequestStoragePermissions();
    }
    private void openMediaPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*,video/*"); // ★ السماح بالصور والفيديو
        String[] mimeTypes = {"image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        pickMediaActivityResultLauncher.launch(intent);
    }

    private void showImagePreview(Uri uri) {
        if (ivStoryImagePreview == null || storyVideoPreview == null) return;
        ivStoryImagePreview.setVisibility(View.VISIBLE);
        storyVideoPreview.setVisibility(View.GONE);
        if (!isDestroyed() && !isFinishing()){ // تحقق قبل استخدام Glide
            Glide.with(this).load(uri).into(ivStoryImagePreview);
        }
    }

    private void showVideoPreview(Uri uri) {
        if (ivStoryImagePreview == null || storyVideoPreview == null) return;
        ivStoryImagePreview.setVisibility(View.GONE);
        storyVideoPreview.setVisibility(View.VISIBLE);
        // يمكنك تهيئة ExoPlayer هنا لعرض معاينة الفيديو
        // أو يمكنك عرض صورة مصغرة للفيديو مؤقتًا
        if (!isDestroyed() && !isFinishing()){
            Glide.with(this).load(uri).placeholder(R.drawable.ic_cover_placeholder).into(ivStoryImagePreview);
            ivStoryImagePreview.setVisibility(View.VISIBLE);
            storyVideoPreview.setVisibility(View.GONE);
        }
    }


    private void loadCurrentUserData() {
        if (currentUserId == null) { // currentUserId هو متغير عضو يجب تهيئته في initializeFirebase()
            Log.e(TAG, "Cannot load current user data, currentUserId is null.");
            // يمكنك هنا تعطيل زر النشر أو إظهار رسالة خطأ مناسبة
            if (btnPublishStory != null) {
                btnPublishStory.setEnabled(false);
                Toast.makeText(this, "User authentication error. Cannot create story.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserModel = documentSnapshot.toObject(UserModel.class);
                        if (currentUserModel == null) {
                            Log.e(TAG, "currentUserModel is null after fetching from Firestore in CreateStoryActivity.");
                            Toast.makeText(this, "Failed to load your profile data to create story.", Toast.LENGTH_SHORT).show();
                            if (btnPublishStory != null) btnPublishStory.setEnabled(false);
                        } else {
                            Log.d(TAG, "Current user data loaded for story creation: " +
                                    (currentUserModel.getUsername() != null ? currentUserModel.getUsername() : "N/A"));
                            // تمكين زر النشر إذا تم اختيار وسائط بالفعل وبيانات المستخدم متوفرة
                            if (selectedMediaUri != null && btnPublishStory != null) {
                                btnPublishStory.setEnabled(true);
                            }
                        }
                    } else {
                        Log.e(TAG, "Current user document does not exist in Firestore for story creation.");
                        Toast.makeText(this, "Your profile data not found, cannot create story.", Toast.LENGTH_SHORT).show();
                        if (btnPublishStory != null) btnPublishStory.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load current user data for story: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading your profile data for story.", Toast.LENGTH_SHORT).show();
                    if (btnPublishStory != null) btnPublishStory.setEnabled(false);
                });
    }

    private void publishStory() {
        if (selectedMediaUri == null) {
            Toast.makeText(this, "Please select media for your story", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser firebaseUser = auth.getCurrentUser(); // الحصول على المستخدم الحالي
        if (firebaseUser == null) {
            Toast.makeText(this, "You must be logged in to create a story", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserModel == null) { // ★ تحقق إضافي
            Toast.makeText(this, "User data not loaded yet. Cannot publish story.", Toast.LENGTH_SHORT).show();
            loadCurrentUserData(); // محاولة إعادة التحميل
            return;
        }

        showProgress(true);

        String userId = firebaseUser.getUid();
        String fileExtension = getFileExtension(selectedMediaUri);
        String fileName = "stories/" + userId + "/" + UUID.randomUUID().toString() + fileExtension;
        StorageReference mediaRef = storage.getReference().child(fileName);

        // ★★ تعريف وتهيئة mimeType هنا ★★
        String mimeType = getContentResolver().getType(selectedMediaUri);
        String storyMediaType = (mimeType != null && mimeType.startsWith("image")) ? StoryModel.MEDIA_TYPE_IMAGE : StoryModel.MEDIA_TYPE_VIDEO;

        long duration = 5000; // مدة افتراضية للصور
        if (StoryModel.MEDIA_TYPE_VIDEO.equals(storyMediaType)) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, selectedMediaUri);
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durationStr != null) {
                    duration = Long.parseLong(durationStr);
                }
                retriever.release();
            } catch (Exception e) {
                Log.e(TAG, "Failed to get video duration", e);
                // إذا فشل جلب المدة، استخدم مدة افتراضية معقولة للفيديو أو أظهر خطأ
            }
        }

        final long finalDuration = duration;

        UploadTask uploadTask = mediaRef.putFile(selectedMediaUri);
        uploadTask.addOnSuccessListener(taskSnapshot ->
                mediaRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String mediaUrl = uri.toString();
                    StoryModel story = new StoryModel(userId, mediaUrl, storyMediaType, finalDuration, mediaUrl); // استخدام mediaUrl كـ thumbnailUrl للفيديو مؤقتًا

                    // استخدام currentUserModel الذي تم تحميله مسبقًا
                    if (currentUserModel != null) {
                        story.setAuthorDisplayName(currentUserModel.getDisplayName() != null ? currentUserModel.getDisplayName() : currentUserModel.getUsername());
                        story.setAuthorUsername(currentUserModel.getUsername());
                        story.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl());
                        story.setAuthorVerified(currentUserModel.isVerified());
                    } else {
                        // حالة نادرة، لكن يجب التعامل معها
                        Log.w(TAG, "currentUserModel is null when creating story object in Firestore.");
                    }

                    db.collection("stories").add(story)
                            .addOnSuccessListener(docRef -> {
                                docRef.update("storyId", docRef.getId());
                                showProgress(false);
                                Toast.makeText(CreateStoryActivity.this, "Story published!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                showProgress(false);
                                Toast.makeText(CreateStoryActivity.this, "Failed to save story: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error publishing story", e);
                            });
                }).addOnFailureListener(e -> {
                    showProgress(false);
                    Toast.makeText(CreateStoryActivity.this, "Failed to get media URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error getting media URL", e);
                })
        ).addOnFailureListener(e -> {
            showProgress(false);
            String errorMessage = "Media upload failed: " + e.getMessage();
            if (e instanceof com.google.firebase.storage.StorageException) {
                com.google.firebase.storage.StorageException storageException = (com.google.firebase.storage.StorageException) e;
                if (storageException.getErrorCode() == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND ||
                        storageException.getErrorCode() == com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED ||
                        storageException.getErrorCode() == com.google.firebase.storage.StorageException.ERROR_PROJECT_NOT_FOUND) { // تحقق من كود خطأ آخر محتمل
                    errorMessage = "Upload failed. Please check Storage plan, rules, or App Check configuration.";
                    Log.e(TAG, "Upload failed due to Storage config/rules/plan. ErrorCode: " + storageException.getErrorCode());
                } else if (storageException.getErrorCode() == com.google.firebase.storage.StorageException.ERROR_CANCELED) {
                    errorMessage = "Upload cancelled.";
                }
            }
            Toast.makeText(CreateStoryActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error uploading media", e);
        });
    }

    private void showProgress(boolean show) {
        if (pbStoryUploadProgress != null) pbStoryUploadProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnPublishStory != null) btnPublishStory.setEnabled(!show);
        if (btnGallery != null) btnGallery.setEnabled(!show);
        if (btnCaptureStory != null) btnCaptureStory.setEnabled(!show);
        if (btnSwitchCamera != null) btnSwitchCamera.setEnabled(!show);
    }

    private String getFileExtension(Uri uri) {
        String extension = "";
        if (uri == null) {
            Log.w(TAG, "getFileExtension called with null URI");
            return ".jpg";
        }
        try {
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension != null && !extension.isEmpty()) {
                    return "." + extension;
                }
            }
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf(".");
                if (lastDot >= 0 && lastDot < path.length() - 1) {
                    return path.substring(lastDot);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file extension for URI: " + uri.toString(), e);
        }
        Log.w(TAG, "Could not determine file extension for URI: " + uri.toString() + ". Defaulting to .jpg");
        return ".jpg";
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) { // ★ إضافة إلغاء الربط بشكل صريح هنا
            cameraProvider.unbindAll();
        }
    }
}