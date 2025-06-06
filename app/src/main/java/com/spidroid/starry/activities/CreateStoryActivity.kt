package com.spidroid.starry.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.spidroid.starry.R
import com.spidroid.starry.models.StoryModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.ui.common.BottomSheetPostOptions.Companion.TAG
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CreateStoryActivity : AppCompatActivity() {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var currentUserModel: UserModel? = null

    private lateinit var cameraPreviewView: PreviewView
    private lateinit var ivStoryImagePreview: ImageView
    private lateinit var storyVideoPreview: PlayerView
    private lateinit var btnCaptureStory: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnGallery: ImageButton
    private lateinit var editToolsContainer: LinearLayout
    private lateinit var captureControlsContainer: LinearLayout
    private lateinit var btnPublishStory: Button
    private lateinit var pbStoryUploadProgress: ProgressBar

    private var selectedMediaUri: Uri? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentCameraSelector: Int = CameraSelector.LENS_FACING_BACK

    private val pickMediaActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleSelectedMedia(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_story)

        if (auth.currentUser == null) {
            handleAuthError()
            return
        }

        initializeViews()
        setupListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkAndRequestCameraPermissions()
        loadCurrentUserDetails()
    }

    private fun initializeViews() {
        cameraPreviewView = findViewById(R.id.camera_preview_view)
        ivStoryImagePreview = findViewById(R.id.iv_story_image_preview)
        storyVideoPreview = findViewById(R.id.story_video_preview)
        btnCaptureStory = findViewById(R.id.btn_capture_story)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera_story)
        btnGallery = findViewById(R.id.btn_gallery_story)
        editToolsContainer = findViewById(R.id.edit_tools_container)
        captureControlsContainer = findViewById(R.id.capture_controls_container)
        btnPublishStory = findViewById(R.id.btnPublishStory)
        pbStoryUploadProgress = findViewById(R.id.pb_story_upload_progress)
        findViewById<View>(R.id.btnCloseStory).setOnClickListener { finish() }
    }

    private fun setupListeners() {
        btnCaptureStory.setOnClickListener { takePhoto() }
        btnSwitchCamera.setOnClickListener { switchCamera() }
        btnGallery.setOnClickListener { openGallery() }
        btnPublishStory.setOnClickListener { publishStory() }
    }

    private fun loadCurrentUserDetails() {
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    currentUserModel = document.toObject(UserModel::class.java)
                    if (currentUserModel == null) {
                        Toast.makeText(this, "Failed to load profile data.", Toast.LENGTH_SHORT).show()
                        btnPublishStory.isEnabled = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load user data.", e)
                    btnPublishStory.isEnabled = false
                }
        }
    }

    // Camera and Permissions
    private fun checkAndRequestCameraPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera(currentCameraSelector)
        } else {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera(currentCameraSelector)
            } else {
                Toast.makeText(this, "Camera and Audio permissions are required.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                openMediaPicker()
            } else {
                Toast.makeText(this, "Storage permission is required.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCamera(lensFacing: Int) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lensFacing)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission")
    private fun bindCameraUseCases(lensFacing: Int) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder().build()

        try {
            provider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun switchCamera() {
        currentCameraSelector = if (currentCameraSelector == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT
        else
            CameraSelector.LENS_FACING_BACK
        startCamera(currentCameraSelector)
    }

    private fun takePhoto() {
        val localImageCapture = imageCapture ?: return

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/StarryApp")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        localImageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    handleSelectedMedia(output.savedUri)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Media Selection
    private fun openGallery() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            openMediaPicker()
        } else {
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_REQUEST_CODE)
        }
    }

    private fun openMediaPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*, image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
        pickMediaActivityResultLauncher.launch(intent)
    }

    private fun handleSelectedMedia(uri: Uri?) {
        selectedMediaUri = uri ?: return
        val mimeType = contentResolver.getType(selectedMediaUri!!)

        val isVideo = mimeType?.startsWith("video/") == true
        showPreview(selectedMediaUri!!, isVideo)
        updateUIVisibility(true)
    }

    private fun showPreview(uri: Uri, isVideo: Boolean) {
        storyVideoPreview.visibility = if (isVideo) View.VISIBLE else View.GONE
        ivStoryImagePreview.visibility = if (isVideo) View.GONE else View.VISIBLE

        if(isVideo){
            // setup ExoPlayer for video preview if needed
        } else {
            Glide.with(this).load(uri).into(ivStoryImagePreview)
        }
    }

    private fun updateUIVisibility(hasMedia: Boolean) {
        captureControlsContainer.visibility = if (hasMedia) View.GONE else View.VISIBLE
        cameraPreviewView.visibility = if (hasMedia) View.GONE else View.VISIBLE
        editToolsContainer.visibility = if (hasMedia) View.VISIBLE else View.GONE
        btnPublishStory.isEnabled = hasMedia && (currentUserModel != null)
    }

    // Publishing Story
    private fun publishStory() {
        val mediaUri = selectedMediaUri ?: run {
            Toast.makeText(this, "Please select media for your story", Toast.LENGTH_SHORT).show()
            return
        }
        val user = currentUserModel ?: run {
            Toast.makeText(this, "User data not loaded yet.", Toast.LENGTH_SHORT).show()
            return
        }

        showProgress(true)
        val mimeType = contentResolver.getType(mediaUri)
        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        val fileName = "stories/${user.userId}/${UUID.randomUUID()}.$fileExtension"
        val mediaRef = storage.reference.child(fileName)

        mediaRef.putFile(mediaUri)
            .addOnSuccessListener {
                mediaRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    createStoryInFirestore(downloadUrl.toString(), mimeType)
                }
            }
            .addOnFailureListener { e ->
                handleUploadFailure(e)
            }
    }

    private fun createStoryInFirestore(mediaUrl: String, mimeType: String?) {
        val user = currentUserModel ?: return
        val storyType = if (mimeType?.startsWith("video/") == true) StoryModel.MEDIA_TYPE_VIDEO else StoryModel.MEDIA_TYPE_IMAGE
        val duration = if (storyType == StoryModel.MEDIA_TYPE_VIDEO) getVideoDuration(selectedMediaUri) else 5000L

        val story = StoryModel(
            userId = user.userId,
            mediaUrl = mediaUrl,
            mediaType = storyType,
            duration = duration,
            thumbnailUrl = mediaUrl // Can be improved to generate actual thumbnails
        )

        db.collection("stories").add(story)
            .addOnSuccessListener {
                showProgress(false)
                Toast.makeText(this, "Story published!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                handleUploadFailure(e, "Failed to save story")
            }
    }

    private fun getVideoDuration(uri: Uri?): Long {
        if (uri == null) return 5000L
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(this, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 5000L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video duration", e)
            5000L
        }
    }

    private fun handleUploadFailure(e: Exception, message: String = "Media upload failed") {
        showProgress(false)
        Toast.makeText(this, "$message: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e(TAG, message, e)
    }

    private fun showProgress(show: Boolean) {
        pbStoryUploadProgress.visibility = if (show) View.VISIBLE else View.GONE
        btnPublishStory.isEnabled = !show
        btnGallery.isEnabled = !show
        btnCaptureStory.isEnabled = !show
        btnSwitchCamera.isEnabled = !show
    }

    private fun handleAuthError() {
        Toast.makeText(this, "Authentication required to create a story.", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}