package com.spidroid.starry.activities

// ★ إضافة هذا إذا لم يكن موجودًا
// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
// ★ إضافة هذا
import com.google.firebase.auth.FirebaseAuth

class CreateStoryActivity : AppCompatActivity() {
    private var db: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var auth: FirebaseAuth? = null // تأكد من وجود هذا
    private val currentUserId: kotlin.String? = null // ★★ هذا هو التعريف المهم ★★
    private var currentUserModel: UserModel? = null
    private var cameraPreviewView: PreviewView? = null
    private var ivStoryImagePreview: android.widget.ImageView? = null
    private var storyVideoPreview: PlayerView? = null

    // ...
    private var btnCaptureStory: ImageButton? = null
    private var btnSwitchCamera: ImageButton? = null
    private var btnGallery: ImageButton? = null // ★ زر المعرض اسمه btnGallery

    // ... (باقي المتغيرات)
    private var editToolsContainer: LinearLayout? = null
    private var captureControlsContainer: LinearLayout? = null
    private var btnPublishStory: android.widget.Button? = null
    private var pbStoryUploadProgress: ProgressBar? = null

    private var selectedMediaUri: android.net.Uri? = null
    private var capturedImageUri: android.net.Uri? = null


    // private Uri recordedVideoUri; // لم يتم استخدامه بعد بشكل كامل
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider?>? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private val videoCapture: VideoCapture<androidx.camera.video.Recorder?>? = null
    private val currentRecording: androidx.camera.video.Recording? = null
    private var cameraExecutor: java.util.concurrent.ExecutorService? = null
    private var currentCameraSelector: Int = CameraSelector.LENS_FACING_BACK


    // Launcher جديد لـ pickMedia (يحل محل onActivityResult لـ PICK_MEDIA_REQUEST)
    private val pickMediaActivityResultLauncher: ActivityResultLauncher<Intent?> =
        registerForActivityResult<Intent?, androidx.activity.result.ActivityResult?>(
            StartActivityForResult(),
            ActivityResultCallback { result: androidx.activity.result.ActivityResult? ->
                if (result!!.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null && result.getData()!!
                        .getData() != null
                ) {
                    val uri = result.getData()!!.getData()
                    try {
                        val takeFlags = result.getData()!!
                            .getFlags() and (Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        if ((takeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                            getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                        selectedMediaUri = uri
                        capturedImageUri = null

                        // recordedVideoUri = null; // مسح أي فيديو مسجل بالكاميرا
                        val mimeType = getContentResolver().getType(selectedMediaUri)
                        android.util.Log.d(
                            CreateStoryActivity.Companion.TAG,
                            "Selected media from gallery: " + selectedMediaUri.toString() + ", MIME type: " + mimeType
                        )

                        if (mimeType != null && mimeType.startsWith("video/")) {
                            showVideoPreview(selectedMediaUri)
                        } else if (mimeType != null && mimeType.startsWith("image/")) {
                            showImagePreview(selectedMediaUri)
                        } else {
                            Toast.makeText(
                                this,
                                "Unsupported file type selected.",
                                Toast.LENGTH_LONG
                            ).show()
                            selectedMediaUri = null
                            if (ivStoryImagePreview != null) ivStoryImagePreview!!.setVisibility(
                                android.view.View.GONE
                            )
                            if (storyVideoPreview != null) storyVideoPreview.setVisibility(android.view.View.GONE)
                            if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
                            return@registerForActivityResult
                        }
                        // إظهار/إخفاء العناصر المناسبة
                        if (cameraPreviewView != null) cameraPreviewView.setVisibility(android.view.View.GONE)
                        if (captureControlsContainer != null) captureControlsContainer.setVisibility(
                            android.view.View.GONE
                        )
                        if (editToolsContainer != null) editToolsContainer.setVisibility(android.view.View.VISIBLE)
                        if (btnPublishStory != null) btnPublishStory!!.setEnabled(true)
                    } catch (e: java.lang.Exception) {
                        android.util.Log.e(
                            CreateStoryActivity.Companion.TAG,
                            "Error processing media from gallery: " + e.message,
                            e
                        )
                        Toast.makeText(
                            this,
                            "Could not load selected media. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        selectedMediaUri = null
                        if (ivStoryImagePreview != null) ivStoryImagePreview!!.setVisibility(android.view.View.GONE)
                        if (storyVideoPreview != null) storyVideoPreview.setVisibility(android.view.View.GONE)
                        if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
                    }
                }
            })


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_story)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        initializeViews()
        setupListeners()

        cameraExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        checkAndRequestCameraPermissions()
        loadCurrentUserDetails() // ★ جلب بيانات المستخدم الحالي
    }

    private fun initializeViews() {
        cameraPreviewView = findViewById<PreviewView?>(R.id.camera_preview_view)
        ivStoryImagePreview = findViewById<android.widget.ImageView?>(R.id.iv_story_image_preview)
        storyVideoPreview = findViewById<PlayerView?>(R.id.story_video_preview)
        btnCaptureStory = findViewById<ImageButton?>(R.id.btn_capture_story)
        btnSwitchCamera = findViewById<ImageButton?>(R.id.btn_switch_camera_story)
        btnGallery = findViewById<ImageButton?>(R.id.btn_gallery_story)
        editToolsContainer = findViewById<LinearLayout?>(R.id.edit_tools_container)
        captureControlsContainer = findViewById<LinearLayout?>(R.id.capture_controls_container)
        btnPublishStory = findViewById<android.widget.Button?>(R.id.btnPublishStory)
        pbStoryUploadProgress = findViewById<ProgressBar?>(R.id.pb_story_upload_progress)
        findViewById<android.view.View?>(R.id.btnCloseStory).setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> finish() })
    }

    private fun setupListeners() {
        if (btnCaptureStory != null) btnCaptureStory.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> takePhoto() }) // مؤقتًا لالتقاط الصور فقط

        if (btnSwitchCamera != null) btnSwitchCamera.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> switchCamera() })
        if (btnGallery != null) btnGallery.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> openGallery() })
        if (btnPublishStory != null) btnPublishStory!!.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> publishStory() })
    }

    private fun loadCurrentUserDetails() {
        val firebaseUser: FirebaseUser? = auth.getCurrentUser()
        if (firebaseUser != null) {
            db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener({ documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        currentUserModel = documentSnapshot.toObject(UserModel::class.java)
                        if (currentUserModel == null) {
                            android.util.Log.e(
                                CreateStoryActivity.Companion.TAG,
                                "currentUserModel is null after fetching from Firestore."
                            )
                            Toast.makeText(
                                this,
                                "Failed to load your profile data to create story.",
                                Toast.LENGTH_SHORT
                            ).show()
                            btnPublishStory!!.setEnabled(false) // تعطيل النشر إذا لم يتم تحميل المستخدم
                        } else {
                            android.util.Log.d(
                                CreateStoryActivity.Companion.TAG,
                                "Current user details loaded for story creation."
                            )
                            // يمكنك تفعيل زر النشر هنا إذا كان selectedMediaUri ليس null
                            if (selectedMediaUri != null && btnPublishStory != null) {
                                btnPublishStory!!.setEnabled(true)
                            }
                        }
                    } else {
                        android.util.Log.e(
                            CreateStoryActivity.Companion.TAG,
                            "Current user document does not exist in Firestore for story creation."
                        )
                        Toast.makeText(
                            this,
                            "Your profile data not found, cannot create story.",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
                    }
                })
                .addOnFailureListener({ e ->
                    android.util.Log.e(
                        CreateStoryActivity.Companion.TAG,
                        "Failed to load current user data for story: " + e.getMessage(),
                        e
                    )
                    Toast.makeText(
                        this,
                        "Error loading your profile data for story.",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
                })
        } else {
            if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
        }
    }


    private fun checkAndRequestCameraPermissions() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                kotlin.arrayOf<kotlin.String>(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                ),
                CreateStoryActivity.Companion.CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startCamera(currentCameraSelector)
        }
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    kotlin.arrayOf<kotlin.String>(
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    ),
                    CreateStoryActivity.Companion.STORAGE_PERMISSION_REQUEST_CODE
                )
            } else {
                openMediaPicker()
            }
        } else {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    kotlin.arrayOf<kotlin.String>(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    CreateStoryActivity.Companion.STORAGE_PERMISSION_REQUEST_CODE
                )
            } else {
                openMediaPicker()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: kotlin.Array<kotlin.String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CreateStoryActivity.Companion.CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(currentCameraSelector)
            } else {
                Toast.makeText(
                    this,
                    "Camera and Audio permissions are required to use this feature.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        } else if (requestCode == CreateStoryActivity.Companion.STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openMediaPicker()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to select media.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startCamera(lensFacing: Int) {
        if (cameraPreviewView == null) {
            android.util.Log.e(
                CreateStoryActivity.Companion.TAG,
                "cameraPreviewView is null in startCamera. Cannot start camera."
            )
            return
        }
        cameraProviderFuture.addListener(java.lang.Runnable {
            try {
                cameraProvider = cameraProviderFuture.get()
                if (cameraProvider == null) {
                    android.util.Log.e(CreateStoryActivity.Companion.TAG, "Camera provider is null")
                    return@addListener
                }
                bindCameraUseCases(lensFacing)
            } catch (e: java.lang.Exception) {
                android.util.Log.e(CreateStoryActivity.Companion.TAG, "Error starting camera", e)
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission")
    private fun bindCameraUseCases(lensFacing: Int) {
        if (cameraProvider == null || cameraPreviewView == null) {
            android.util.Log.e(
                CreateStoryActivity.Companion.TAG,
                "Camera provider or PreviewView not available, cannot bind use cases."
            )
            return
        }
        cameraProvider.unbindAll()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val preview = androidx.camera.core.Preview.Builder().build()
        preview.setSurfaceProvider(cameraPreviewView.getSurfaceProvider())

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // Recorder recorder = new Recorder.Builder()
        //         .setQualitySelector(QualitySelector.from(Quality.HD))
        //         .build();
        // videoCapture = VideoCapture.withOutput(recorder);
        try {
            // ★ مؤقتًا، سنربط فقط معاينة الكاميرا والتقاط الصور
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture /*, videoCapture */
            )
            android.util.Log.d(
                CreateStoryActivity.Companion.TAG,
                "Camera use cases bound successfully for lens: " + (if (lensFacing == CameraSelector.LENS_FACING_BACK) "BACK" else "FRONT")
            )
        } catch (e: java.lang.Exception) {
            android.util.Log.e(CreateStoryActivity.Companion.TAG, "Use case binding failed", e)
        }
    }

    private fun switchCamera() {
        currentCameraSelector =
            if (currentCameraSelector == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera(currentCameraSelector)
    }


    private fun takePhoto() {
        if (imageCapture == null) {
            android.util.Log.e(
                CreateStoryActivity.Companion.TAG,
                "ImageCapture use case is null. Cannot take photo."
            )
            return
        }
        val name = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.lang.System.currentTimeMillis())
        val contentValues: ContentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StarryApp")
        }

        val outputOptions: OutputFileOptions = OutputFileOptions.Builder(
            getContentResolver(),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
            .build()

        imageCapture.takePicture(
            outputOptions, androidx.core.content.ContextCompat.getMainExecutor(this),
            object : OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: OutputFileResults) {
                    capturedImageUri = outputFileResults.getSavedUri()
                    android.util.Log.d(
                        CreateStoryActivity.Companion.TAG,
                        "Photo capture succeeded: " + capturedImageUri
                    )
                    if (capturedImageUri != null) {
                        selectedMediaUri = capturedImageUri
                        // recordedVideoUri = null; // إذا كان هناك فيديو مسجل سابقًا
                        showImagePreview(capturedImageUri)
                        if (cameraPreviewView != null) cameraPreviewView.setVisibility(android.view.View.GONE)
                        if (captureControlsContainer != null) captureControlsContainer.setVisibility(
                            android.view.View.GONE
                        )
                        if (editToolsContainer != null) editToolsContainer.setVisibility(android.view.View.VISIBLE)
                        if (btnPublishStory != null) btnPublishStory!!.setEnabled(true)
                    } else {
                        Toast.makeText(
                            this@CreateStoryActivity,
                            "Failed to save image.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    android.util.Log.e(
                        CreateStoryActivity.Companion.TAG,
                        "Photo capture failed: " + exception.message,
                        exception
                    )
                    Toast.makeText(
                        this@CreateStoryActivity,
                        "Photo capture failed: " + exception.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun openGallery() {
        checkAndRequestStoragePermissions()
    }

    private fun openMediaPicker() {
        val intent: Intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("image/*,video/*") // ★ السماح بالصور والفيديو
        val mimeTypes = kotlin.arrayOf<kotlin.String?>("image/*", "video/*")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        pickMediaActivityResultLauncher.launch(intent)
    }

    private fun showImagePreview(uri: android.net.Uri?) {
        if (ivStoryImagePreview == null || storyVideoPreview == null) return
        ivStoryImagePreview!!.setVisibility(android.view.View.VISIBLE)
        storyVideoPreview.setVisibility(android.view.View.GONE)
        if (!isDestroyed() && !isFinishing()) { // تحقق قبل استخدام Glide
            Glide.with(this).load(uri).into(ivStoryImagePreview)
        }
    }

    private fun showVideoPreview(uri: android.net.Uri?) {
        if (ivStoryImagePreview == null || storyVideoPreview == null) return
        ivStoryImagePreview!!.setVisibility(android.view.View.GONE)
        storyVideoPreview.setVisibility(android.view.View.VISIBLE)
        // يمكنك تهيئة ExoPlayer هنا لعرض معاينة الفيديو
        // أو يمكنك عرض صورة مصغرة للفيديو مؤقتًا
        if (!isDestroyed() && !isFinishing()) {
            Glide.with(this).load(uri).placeholder(R.drawable.ic_cover_placeholder)
                .into(ivStoryImagePreview)
            ivStoryImagePreview!!.setVisibility(android.view.View.VISIBLE)
            storyVideoPreview.setVisibility(android.view.View.GONE)
        }
    }


    private fun loadCurrentUserData() {
        if (currentUserId == null) { // currentUserId هو متغير عضو يجب تهيئته في initializeFirebase()
            android.util.Log.e(
                CreateStoryActivity.Companion.TAG,
                "Cannot load current user data, currentUserId is null."
            )
            // يمكنك هنا تعطيل زر النشر أو إظهار رسالة خطأ مناسبة
            if (btnPublishStory != null) {
                btnPublishStory!!.setEnabled(false)
                Toast.makeText(
                    this,
                    "User authentication error. Cannot create story.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener({ documentSnapshot ->
                if (documentSnapshot.exists()) {
                    currentUserModel = documentSnapshot.toObject(UserModel::class.java)
                    if (currentUserModel == null) {
                        android.util.Log.e(
                            CreateStoryActivity.Companion.TAG,
                            "currentUserModel is null after fetching from Firestore in CreateStoryActivity."
                        )
                        Toast.makeText(
                            this,
                            "Failed to load your profile data to create story.",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
                    } else {
                        android.util.Log.d(
                            CreateStoryActivity.Companion.TAG,
                            "Current user data loaded for story creation: " +
                                    (if (currentUserModel.getUsername() != null) currentUserModel.getUsername() else "N/A")
                        )
                        // تمكين زر النشر إذا تم اختيار وسائط بالفعل وبيانات المستخدم متوفرة
                        if (selectedMediaUri != null && btnPublishStory != null) {
                            btnPublishStory!!.setEnabled(true)
                        }
                    }
                } else {
                    android.util.Log.e(
                        CreateStoryActivity.Companion.TAG,
                        "Current user document does not exist in Firestore for story creation."
                    )
                    Toast.makeText(
                        this,
                        "Your profile data not found, cannot create story.",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
                }
            })
            .addOnFailureListener({ e ->
                android.util.Log.e(
                    CreateStoryActivity.Companion.TAG,
                    "Failed to load current user data for story: " + e.getMessage(),
                    e
                )
                Toast.makeText(
                    this,
                    "Error loading your profile data for story.",
                    Toast.LENGTH_SHORT
                ).show()
                if (btnPublishStory != null) btnPublishStory!!.setEnabled(false)
            })
    }

    private fun publishStory() {
        if (selectedMediaUri == null) {
            Toast.makeText(this, "Please select media for your story", Toast.LENGTH_SHORT).show()
            return
        }
        val firebaseUser: FirebaseUser? = auth.getCurrentUser() // الحصول على المستخدم الحالي
        if (firebaseUser == null) {
            Toast.makeText(this, "You must be logged in to create a story", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (currentUserModel == null) { // ★ تحقق إضافي
            Toast.makeText(
                this,
                "User data not loaded yet. Cannot publish story.",
                Toast.LENGTH_SHORT
            ).show()
            loadCurrentUserData() // محاولة إعادة التحميل
            return
        }

        showProgress(true)

        val userId: kotlin.String = firebaseUser.getUid()
        val fileExtension = getFileExtension(selectedMediaUri)
        val fileName =
            "stories/" + userId + "/" + java.util.UUID.randomUUID().toString() + fileExtension
        val mediaRef: StorageReference = storage.getReference().child(fileName)

        // ★★ تعريف وتهيئة mimeType هنا ★★
        val mimeType = getContentResolver().getType(selectedMediaUri)
        val storyMediaType: kotlin.String =
            if (mimeType != null && mimeType.startsWith("image")) StoryModel.Companion.MEDIA_TYPE_IMAGE else StoryModel.Companion.MEDIA_TYPE_VIDEO

        var duration: kotlin.Long = 5000 // مدة افتراضية للصور
        if (StoryModel.Companion.MEDIA_TYPE_VIDEO == storyMediaType) {
            try {
                val retriever: MediaMetadataRetriever = MediaMetadataRetriever()
                retriever.setDataSource(this, selectedMediaUri)
                val durationStr: kotlin.String? =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (durationStr != null) {
                    duration = durationStr.toLong()
                }
                retriever.release()
            } catch (e: java.lang.Exception) {
                android.util.Log.e(
                    CreateStoryActivity.Companion.TAG,
                    "Failed to get video duration",
                    e
                )
                // إذا فشل جلب المدة، استخدم مدة افتراضية معقولة للفيديو أو أظهر خطأ
            }
        }

        val finalDuration = duration

        val uploadTask: UploadTask = mediaRef.putFile(selectedMediaUri)
        uploadTask.addOnSuccessListener({ taskSnapshot ->
            mediaRef.getDownloadUrl().addOnSuccessListener({ uri ->
                val mediaUrl: kotlin.String? = uri.toString()
                val story: StoryModel = StoryModel(
                    userId,
                    mediaUrl,
                    storyMediaType,
                    finalDuration,
                    mediaUrl
                ) // استخدام mediaUrl كـ thumbnailUrl للفيديو مؤقتًا

                // استخدام currentUserModel الذي تم تحميله مسبقًا
                if (currentUserModel != null) {
                    story.setAuthorDisplayName(if (currentUserModel.getDisplayName() != null) currentUserModel.getDisplayName() else currentUserModel.getUsername())
                    story.setAuthorUsername(currentUserModel.getUsername())
                    story.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl())
                    story.setAuthorVerified(currentUserModel.isVerified())
                } else {
                    // حالة نادرة، لكن يجب التعامل معها
                    android.util.Log.w(
                        CreateStoryActivity.Companion.TAG,
                        "currentUserModel is null when creating story object in Firestore."
                    )
                }
                db.collection("stories").add(story)
                    .addOnSuccessListener({ docRef ->
                        docRef.update("storyId", docRef.getId())
                        showProgress(false)
                        Toast.makeText(
                            this@CreateStoryActivity,
                            "Story published!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    })
                    .addOnFailureListener({ e ->
                        showProgress(false)
                        Toast.makeText(
                            this@CreateStoryActivity,
                            "Failed to save story: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                        ).show()
                        android.util.Log.e(
                            CreateStoryActivity.Companion.TAG,
                            "Error publishing story",
                            e
                        )
                    })
            }).addOnFailureListener({ e ->
                showProgress(false)
                Toast.makeText(
                    this@CreateStoryActivity,
                    "Failed to get media URL: " + e.getMessage(),
                    Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e(CreateStoryActivity.Companion.TAG, "Error getting media URL", e)
            })
        }
        ).addOnFailureListener({ e ->
            showProgress(false)
            var errorMessage = "Media upload failed: " + e.getMessage()
            if (e is com.google.firebase.storage.StorageException) {
                val storageException: com.google.firebase.storage.StorageException =
                    e as com.google.firebase.storage.StorageException
                if (storageException.getErrorCode() === com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND || storageException.getErrorCode() === com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED || storageException.getErrorCode() === com.google.firebase.storage.StorageException.ERROR_PROJECT_NOT_FOUND) { // تحقق من كود خطأ آخر محتمل
                    errorMessage =
                        "Upload failed. Please check Storage plan, rules, or App Check configuration."
                    android.util.Log.e(
                        CreateStoryActivity.Companion.TAG,
                        "Upload failed due to Storage config/rules/plan. ErrorCode: " + storageException.getErrorCode()
                    )
                } else if (storageException.getErrorCode() === com.google.firebase.storage.StorageException.ERROR_CANCELED) {
                    errorMessage = "Upload cancelled."
                }
            }
            Toast.makeText(this@CreateStoryActivity, errorMessage, Toast.LENGTH_LONG).show()
            android.util.Log.e(CreateStoryActivity.Companion.TAG, "Error uploading media", e)
        })
    }

    private fun showProgress(show: kotlin.Boolean) {
        if (pbStoryUploadProgress != null) pbStoryUploadProgress.setVisibility(if (show) android.view.View.VISIBLE else android.view.View.GONE)
        if (btnPublishStory != null) btnPublishStory!!.setEnabled(!show)
        if (btnGallery != null) btnGallery.setEnabled(!show)
        if (btnCaptureStory != null) btnCaptureStory.setEnabled(!show)
        if (btnSwitchCamera != null) btnSwitchCamera.setEnabled(!show)
    }

    private fun getFileExtension(uri: android.net.Uri?): kotlin.String {
        var extension: kotlin.String? = ""
        if (uri == null) {
            android.util.Log.w(
                CreateStoryActivity.Companion.TAG,
                "getFileExtension called with null URI"
            )
            return ".jpg"
        }
        try {
            val mimeType = getContentResolver().getType(uri)
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (extension != null && !extension.isEmpty()) {
                    return "." + extension
                }
            }
            val path = uri.getPath()
            if (path != null) {
                val lastDot = path.lastIndexOf(".")
                if (lastDot >= 0 && lastDot < path.length - 1) {
                    return path.substring(lastDot)
                }
            }
        } catch (e: java.lang.Exception) {
            android.util.Log.e(
                CreateStoryActivity.Companion.TAG,
                "Error getting file extension for URI: " + uri.toString(),
                e
            )
        }
        android.util.Log.w(
            CreateStoryActivity.Companion.TAG,
            "Could not determine file extension for URI: " + uri.toString() + ". Defaulting to .jpg"
        )
        return ".jpg"
    }


    override fun onDestroy() {
        super.onDestroy()
        if (cameraExecutor != null) {
            cameraExecutor!!.shutdown()
        }
        if (cameraProvider != null) { // ★ إضافة إلغاء الربط بشكل صريح هنا
            cameraProvider.unbindAll()
        }
    }

    companion object {
        private const val TAG = "CreateStoryActivity"

        // PICK_MEDIA_REQUEST لم يعد مستخدماً مع ActivityResultLauncher الجديد
        // private static final int PICK_MEDIA_REQUEST = 1;
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val STORAGE_PERMISSION_REQUEST_CODE = 101
    }
}