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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.spidroid.starry.databinding.ActivityCreateStoryBinding
import com.spidroid.starry.models.StoryModel
import com.spidroid.starry.models.UserModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CreateStoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateStoryBinding
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var currentUserModel: UserModel? = null
    private var selectedMediaUri: Uri? = null
    private var mediaType: String? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentCameraSelector: Int = CameraSelector.LENS_FACING_BACK
    private var exoPlayer: ExoPlayer? = null

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
        binding = ActivityCreateStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        // Views are already available through binding
    }

    private fun setupListeners() {
        binding.btnCloseStory.setOnClickListener { finish() }
        binding.btnCaptureStory.setOnClickListener { takePhoto() }
        binding.btnSwitchCameraStory.setOnClickListener { switchCamera() }
        binding.btnGalleryStory.setOnClickListener { openGallery() }
        binding.btnPublishStory.setOnClickListener { publishStory() }
    }

    private fun loadCurrentUserDetails() {
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    currentUserModel = document.toObject(UserModel::class.java)
                    if (currentUserModel == null) {
                        Toast.makeText(this, "Failed to load profile data. Story cannot be published.", Toast.LENGTH_SHORT).show()
                        binding.btnPublishStory.isEnabled = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load user data.", e)
                    Toast.makeText(this, "Failed to load user data. Please try again.", Toast.LENGTH_SHORT).show()
                    binding.btnPublishStory.isEnabled = false
                }
        }
    }

    // ... (Camera and Permissions code remains mostly the same)

    private fun handleSelectedMedia(uri: Uri) {
        selectedMediaUri = uri
        val mime = contentResolver.getType(uri)
        mediaType = if (mime?.startsWith("video/") == true) StoryModel.MEDIA_TYPE_VIDEO else StoryModel.MEDIA_TYPE_IMAGE

        showPreview(uri, mediaType == StoryModel.MEDIA_TYPE_VIDEO)
        updateUIVisibility(true)
    }

    private fun showPreview(uri: Uri, isVideo: Boolean) {
        releaseExoPlayer()
        if (isVideo) {
            binding.storyVideoPreview.visibility = View.VISIBLE
            binding.ivStoryImagePreview.visibility = View.GONE
            exoPlayer = ExoPlayer.Builder(this).build().also { player ->
                binding.storyVideoPreview.player = player
                player.setMediaItem(MediaItem.fromUri(uri))
                player.playWhenReady = true
                player.prepare()
            }
        } else {
            binding.storyVideoPreview.visibility = View.GONE
            binding.ivStoryImagePreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(binding.ivStoryImagePreview)
        }
    }

    private fun updateUIVisibility(hasMedia: Boolean) {
        binding.captureControlsContainer.visibility = if (hasMedia) View.GONE else View.VISIBLE
        binding.cameraPreviewView.visibility = if (hasMedia) View.GONE else View.VISIBLE
        binding.editToolsContainer.visibility = if (hasMedia) View.VISIBLE else View.GONE
        binding.btnPublishStory.isEnabled = hasMedia && (currentUserModel != null)
    }

    private fun publishStory() {
        val mediaUri = selectedMediaUri ?: return
        val user = currentUserModel ?: return
        val type = mediaType ?: return

        showProgress(true)

        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(mediaUri)) ?: "jpg"
        val fileName = "stories/${user.userId}/${UUID.randomUUID()}.$fileExtension"
        val mediaRef = storage.reference.child(fileName)

        mediaRef.putFile(mediaUri)
            .addOnSuccessListener {
                mediaRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    createStoryInFirestore(downloadUrl.toString(), type)
                }
            }
            .addOnFailureListener { e -> handleUploadFailure(e) }
    }

    private fun createStoryInFirestore(mediaUrl: String, type: String) {
        val user = currentUserModel!!
        val duration = if (type == StoryModel.MEDIA_TYPE_VIDEO) getVideoDuration(selectedMediaUri) else StoryModel.DEFAULT_IMAGE_DURATION_MS

        val twentyFourHoursFromNow = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)

        val story = StoryModel(
            userId = user.userId,
            mediaUrl = mediaUrl,
            mediaType = type,
            duration = duration,
            thumbnailUrl = if (type == StoryModel.MEDIA_TYPE_VIDEO) null else mediaUrl,
            createdAt = Date(), // Set client-side for immediate sorting
            expiresAt = twentyFourHoursFromNow,
            authorUsername = user.username,
            authorDisplayName = user.displayName,
            authorAvatarUrl = user.profileImageUrl,
            isAuthorVerified = user.isVerified
        )

        db.collection("stories").add(story)
            .addOnSuccessListener { docRef ->
                docRef.update("storyId", docRef.id, "createdAt", FieldValue.serverTimestamp())
                showProgress(false)
                Toast.makeText(this, "Story published!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e -> handleUploadFailure(e, "Failed to save story") }
    }

    // ... (rest of the functions: getVideoDuration, handleUploadFailure, showProgress, etc.)
    // ... (Make sure they are correct as per previous steps)

    private fun handleAuthError() {
        Toast.makeText(this, "Authentication required to create a story.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun releaseExoPlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onPause() {
        super.onPause()
        releaseExoPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        releaseExoPlayer()
    }

    // ... (The rest of camera and permission logic should be here)
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
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startCamera(currentCameraSelector)
            } else {
                Toast.makeText(this, "Camera and Audio permissions are required.", Toast.LENGTH_LONG).show()
                finish()
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
            it.setSurfaceProvider(binding.cameraPreviewView.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder().build()

        try {
            provider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            binding.cameraPreviewView.visibility = View.VISIBLE
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
                    handleSelectedMedia(output.savedUri!!)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

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
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
        pickMediaActivityResultLauncher.launch(intent)
    }

    private fun getVideoDuration(uri: Uri?): Long {
        if (uri == null) return StoryModel.DEFAULT_IMAGE_DURATION_MS
        return try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(this, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: StoryModel.DEFAULT_IMAGE_DURATION_MS
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video duration", e)
            StoryModel.DEFAULT_IMAGE_DURATION_MS
        }
    }

    private fun handleUploadFailure(e: Exception, message: String = "Media upload failed") {
        showProgress(false)
        Toast.makeText(this, "$message: ${e.message}", Toast.LENGTH_LONG).show()
        Log.e(TAG, message, e)
    }

    private fun showProgress(show: Boolean) {
        binding.pbStoryUploadProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnPublishStory.isEnabled = !show
        binding.btnGalleryStory.isEnabled = !show
        binding.btnCaptureStory.isEnabled = !show
        binding.btnSwitchCameraStory.isEnabled = !show
    }

    companion object {
        private const val TAG = "CreateStoryActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        private const val STORAGE_PERMISSION_REQUEST_CODE = 102
    }
}