package com.spidroid.starry.activities

import com.google.firebase.auth.FirebaseAuth

class ComposePostActivity : AppCompatActivity(), MediaRemoveListener {
    private var binding: com.spidroid.starry.databinding.ActivityComposePostBinding? = null
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var storage: FirebaseStorage? = null
    private var currentUserModel: UserModel? = null

    private var mediaPreviewAdapter: MediaPreviewAdapter? = null
    private val selectedMediaUris: kotlin.collections.MutableList<android.net.Uri> =
        java.util.ArrayList<android.net.Uri>()
    private val existingMediaUrls: kotlin.collections.MutableList<kotlin.String?> =
        java.util.ArrayList<kotlin.String?>()

    private val urlPattern: java.util.regex.Pattern =
        java.util.regex.Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)")
    private var currentLinkPreview: LinkPreview? = null

    private val pickMultipleMedia: ActivityResultLauncher<kotlin.Array<kotlin.String?>?>? = null
    fun registerForActivityResult()

    init {
        if (uris != null && !uris.isEmpty()) {
            if (selectedMediaUris.size + existingMediaUrls.size + uris.size() > 4) {
                Toast.makeText(this, getString(R.string.max_media_limit, 4), Toast.LENGTH_SHORT)
                    .show()
                return
            }
            for (uri in uris) {
                val mimeType = getContentResolver().getType(uri)
                if (mimeType != null) {
                    if (mimeType.startsWith("image/")) {
                        handleMediaSelection(uri, PostModel.Companion.TYPE_IMAGE)
                    } else if (mimeType.startsWith("video/")) {
                        if (selectedMediaUris.stream().anyMatch { u: android.net.Uri? ->
                                val innerMimeType = getContentResolver().getType(u)
                                innerMimeType != null && innerMimeType.startsWith("video/")
                            } || existingMediaUrls.stream().anyMatch { url: kotlin.String? ->
                                url!!.lowercase(java.util.Locale.getDefault())
                                    .matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$".toRegex())
                            }) {
                            Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG)
                                .show()
                            return
                        }
                        handleMediaSelection(uri, PostModel.Companion.TYPE_VIDEO)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.unsupported_file_type, mimeType),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            com.spidroid.starry.databinding.ActivityComposePostBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        initializeFirebase()
        setupListeners()
        setupMediaPreviewRecyclerView()

        binding!!.btnPost.setEnabled(false)
        binding!!.btnPost.setText(getString(R.string.loading_text))

        loadCurrentUser()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
    }

    private fun loadCurrentUser() {
        val user: FirebaseUser? = auth.getCurrentUser()
        if (user == null) {
            android.util.Log.e(
                ComposePostActivity.Companion.TAG,
                "User not authenticated. Finishing activity."
            )
            Toast.makeText(
                this,
                getString(R.string.user_not_authenticated_error),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener({ documentSnapshot ->
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    currentUserModel = documentSnapshot.toObject(UserModel::class.java)
                    if (currentUserModel != null) {
                        android.util.Log.d(
                            ComposePostActivity.Companion.TAG,
                            "Current user model loaded: " + (if (currentUserModel.getUsername() != null) currentUserModel.getUsername() else "N/A") +
                                    ", DisplayName: " + (if (currentUserModel.getDisplayName() != null) currentUserModel.getDisplayName() else "N/A")
                        )
                        updatePostButtonState()
                    } else {
                        android.util.Log.e(
                            ComposePostActivity.Companion.TAG,
                            "currentUserModel is null after deserialization in Compose."
                        )
                        Toast.makeText(
                            this,
                            getString(R.string.error_loading_profile),
                            Toast.LENGTH_SHORT
                        ).show()
                        binding!!.btnPost.setText(getString(R.string.error_button_text))
                        binding!!.btnPost.setEnabled(false)
                    }
                } else {
                    android.util.Log.e(
                        ComposePostActivity.Companion.TAG,
                        "User profile document does not exist in Compose."
                    )
                    Toast.makeText(
                        this,
                        getString(R.string.user_profile_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding!!.btnPost.setText(getString(R.string.error_button_text))
                    binding!!.btnPost.setEnabled(false)
                }
            })
            .addOnFailureListener({ e ->
                android.util.Log.e(
                    ComposePostActivity.Companion.TAG,
                    "Failed to load user data in Compose: " + e.getMessage()
                )
                Toast.makeText(
                    this,
                    getString(R.string.error_loading_user_data_generic),
                    Toast.LENGTH_SHORT
                ).show()
                binding!!.btnPost.setText(getString(R.string.error_button_text))
                binding!!.btnPost.setEnabled(false)
            })
    }

    private fun setupListeners() {
        binding!!.btnClose.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> finish() })
        binding!!.btnPost.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> createNewPost() })

        binding!!.btnAddPhoto.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            pickMultipleMedia.launch(
                kotlin.arrayOf<kotlin.String>("image/*")
            )
        })
        binding!!.btnAddVideo.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            val hasVideo = selectedMediaUris.stream().anyMatch { uri: android.net.Uri? ->
                val mimeType = getContentResolver().getType(uri)
                mimeType != null && mimeType.startsWith("video/")
            } || existingMediaUrls.stream().anyMatch { url: kotlin.String? ->
                url!!.lowercase(java.util.Locale.getDefault())
                    .matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$".toRegex())
            }
            if (hasVideo) {
                Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show()
            } else {
                pickMultipleMedia.launch(kotlin.arrayOf<kotlin.String>("video/*"))
            }
        })

        binding!!.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: kotlin.CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                updateCharCount(s.length)
                checkAndFetchLinkPreview(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                updatePostButtonState()
            }
        })

        binding!!.layoutLinkPreview.btnRemoveLink.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> removeLinkPreview() })
    }

    private fun setupMediaPreviewRecyclerView() {
        mediaPreviewAdapter = MediaPreviewAdapter(selectedMediaUris, existingMediaUrls, this)
        binding!!.rvMediaPreview.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        binding!!.rvMediaPreview.setAdapter(mediaPreviewAdapter)
    }

    private fun handleMediaSelection(uri: android.net.Uri?, type: kotlin.String?) {
        if (uri == null) return

        if (selectedMediaUris.size + existingMediaUrls.size >= 4) {
            Toast.makeText(this, getString(R.string.max_media_limit, 4), Toast.LENGTH_SHORT).show()
            return
        }

        if (PostModel.Companion.TYPE_VIDEO == type) {
            val videoExists = selectedMediaUris.stream().anyMatch { u: android.net.Uri? ->
                val mimeType = getContentResolver().getType(u)
                mimeType != null && mimeType.startsWith("video/")
            } || existingMediaUrls.stream().anyMatch { url: kotlin.String? ->
                url!!.lowercase(java.util.Locale.getDefault())
                    .matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$".toRegex())
            }
            if (videoExists) {
                Toast.makeText(this, R.string.single_video_allowed, Toast.LENGTH_LONG).show()
                return
            }
        }

        val takeFlags = getIntent().getFlags() and (Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            if (takeFlags != 0) {
                getContentResolver().takePersistableUriPermission(uri, takeFlags)
            }
        } catch (e: java.lang.SecurityException) {
            android.util.Log.e(
                ComposePostActivity.Companion.TAG,
                "Failed to take persistable URI permission: " + e.message + " for URI: " + uri.toString()
            )
        }

        try {
            getContentResolver().openInputStream(uri).use { inputStream ->
                if (inputStream == null) {
                    throw java.lang.Exception("Could not open InputStream for URI.")
                }
                selectedMediaUris.add(uri)
                mediaPreviewAdapter.notifyItemInserted(selectedMediaUris.size - 1 + existingMediaUrls.size)
                binding!!.rvMediaPreview.setVisibility(android.view.View.VISIBLE)
            }
        } catch (e: java.lang.Exception) {
            android.util.Log.e(
                ComposePostActivity.Companion.TAG,
                "Error opening URI InputStream, media might be inaccessible: " + e.message + " for URI: " + uri.toString()
            )
            Toast.makeText(this, getString(R.string.media_access_error), Toast.LENGTH_LONG).show()
            return
        }
        updatePostButtonState()
    }


    override fun onMediaRemoved(position: Int) {
        if (position < existingMediaUrls.size) {
            existingMediaUrls.removeAt(position)
        } else {
            val uriPosition = position - existingMediaUrls.size
            if (uriPosition >= 0 && uriPosition < selectedMediaUris.size) {
                selectedMediaUris.removeAt(uriPosition)
            }
        }
        mediaPreviewAdapter.notifyItemRemoved(position)
        if (selectedMediaUris.isEmpty() && existingMediaUrls.isEmpty()) {
            binding!!.rvMediaPreview.setVisibility(android.view.View.GONE)
        }
        updatePostButtonState()
    }

    private fun updateCharCount(count: Int) {
        binding!!.tvCharCount.setText(
            getString(
                R.string.char_count_format,
                count,
                PostModel.Companion.MAX_CONTENT_LENGTH
            )
        )
        if (count > PostModel.Companion.MAX_CONTENT_LENGTH) {
            binding!!.tvCharCount.setTextColor(
                getResources().getColor(
                    R.color.error_red,
                    getTheme()
                )
            )
        } else {
            binding!!.tvCharCount.setTextColor(
                getResources().getColor(
                    R.color.text_secondary,
                    getTheme()
                )
            )
        }
    }

    private fun checkAndFetchLinkPreview(content: kotlin.String) {
        val matcher = urlPattern.matcher(content)
        if (matcher.find()) {
            val url = matcher.group()
            if (currentLinkPreview != null && url == currentLinkPreview.getUrl()) {
                return
            }
            binding!!.layoutLinkPreview.getRoot().setVisibility(android.view.View.VISIBLE)
            binding!!.layoutLinkPreview.tvLinkTitle.setText(R.string.loading_preview)
            binding!!.layoutLinkPreview.tvLinkDescription.setText("")
            binding!!.layoutLinkPreview.tvLinkDomain.setText("")
            binding!!.layoutLinkPreview.ivLinkImage.setImageResource(R.drawable.ic_cover_placeholder)

            LinkPreviewFetcher.fetch(url, object : LinkPreviewCallback {
                override fun onPreviewReceived(preview: LinkPreview?) {
                    if (preview != null && preview.getTitle() != null) {
                        currentLinkPreview = preview
                        binding!!.layoutLinkPreview.tvLinkTitle.setText(preview.getTitle())
                        binding!!.layoutLinkPreview.tvLinkDescription.setText(preview.getDescription())
                        if (preview.getUrl() != null) { // التحقق من أن الرابط ليس null قبل استخدامه
                            binding!!.layoutLinkPreview.tvLinkDomain.setText(
                                android.net.Uri.parse(
                                    preview.getUrl()
                                ).getHost()
                            )
                        }
                        if (preview.getImageUrl() != null && !preview.getImageUrl().isEmpty()) {
                            Glide.with(this@ComposePostActivity)
                                .load(preview.getImageUrl())
                                .placeholder(R.drawable.ic_cover_placeholder)
                                .into(binding!!.layoutLinkPreview.ivLinkImage)
                        }
                    } else {
                        onLinkPreviewError(getString(R.string.no_title))
                    }
                }

                override fun onError(errorMsg: kotlin.String?) {
                    onLinkPreviewError(getString(R.string.link_preview_failed))
                }
            })
        } else {
            removeLinkPreview()
        }
    }

    private fun onLinkPreviewError(errorMsg: kotlin.String?) {
        android.util.Log.w(ComposePostActivity.Companion.TAG, "Link preview error: " + errorMsg)
        currentLinkPreview = null
        binding!!.layoutLinkPreview.getRoot().setVisibility(android.view.View.GONE)
    }

    private fun removeLinkPreview() {
        currentLinkPreview = null
        binding!!.layoutLinkPreview.getRoot().setVisibility(android.view.View.GONE)
    }

    private fun updatePostButtonState() {
        val hasContent = binding!!.etContent.getText().toString().trim { it <= ' ' }.length > 0
        val hasMedia = !selectedMediaUris.isEmpty() || !existingMediaUrls.isEmpty()
        val contentWithinLimit = binding!!.etContent.getText()
            .toString().length <= PostModel.Companion.MAX_CONTENT_LENGTH

        if (currentUserModel != null) {
            binding!!.btnPost.setEnabled((hasContent || hasMedia) && contentWithinLimit)
            binding!!.btnPost.setText(R.string.post)
        } else {
            binding!!.btnPost.setEnabled(false)
        }
    }

    private fun createNewPost() {
        val content = binding!!.etContent.getText().toString().trim { it <= ' ' }

        if (currentUserModel == null) {
            Toast.makeText(
                this,
                getString(R.string.error_user_data_not_loaded_wait),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (content.isEmpty() && selectedMediaUris.isEmpty() && existingMediaUrls.isEmpty() && currentLinkPreview == null) {
            Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show()
            return
        }
        if (content.length > PostModel.Companion.MAX_CONTENT_LENGTH) {
            Toast.makeText(this, R.string.char_limit_exceeded, Toast.LENGTH_SHORT).show()
            return
        }

        showProgress(true)

        if (!selectedMediaUris.isEmpty()) {
            uploadMediaAndCreatePost(content)
        } else {
            // إذا لم تكن هناك وسائط جديدة، ولكن قد تكون هناك وسائط موجودة أو رابط
            savePostToFirestore(
                content,
                java.util.ArrayList<kotlin.String?>(existingMediaUrls),
                PostModel.Companion.TYPE_TEXT
            ) // النوع قد يتغير إذا كان هناك رابط فقط
        }
    }

    private fun uploadMediaAndCreatePost(content: kotlin.String) {
        val uploadTasks: kotlin.collections.MutableList<com.google.android.gms.tasks.Task<android.net.Uri?>?> =
            java.util.ArrayList<com.google.android.gms.tasks.Task<android.net.Uri?>?>()
        val uploadedMediaUrls: kotlin.collections.MutableList<kotlin.String?> =
            java.util.ArrayList<kotlin.String?>(existingMediaUrls)
        val finalContentType =
            kotlin.arrayOf<kotlin.String?>(PostModel.Companion.TYPE_TEXT) // Default to text

        for (fileUri in selectedMediaUris) {
            val mimeType = getContentResolver().getType(fileUri)
            val fileExtension = getFileExtension(fileUri)

            val isVideoType = (mimeType != null && mimeType.startsWith("video/")) ||
                    (fileExtension != null && PostModel.Companion.VIDEO_EXTENSIONS.contains(
                        fileExtension.replace(".", "").lowercase(java.util.Locale.getDefault())
                    ))
            val isImageType = (mimeType != null && mimeType.startsWith("image/")) ||
                    (fileExtension != null && (fileExtension.equals(
                        ".jpg",
                        ignoreCase = true
                    ) || fileExtension.equals(
                        ".jpeg",
                        ignoreCase = true
                    ) || fileExtension.equals(
                        ".png",
                        ignoreCase = true
                    ) || fileExtension.equals(".gif", ignoreCase = true)))


            if (isVideoType) {
                finalContentType[0] = PostModel.Companion.TYPE_VIDEO
            } else if (isImageType) {
                if (PostModel.Companion.TYPE_VIDEO != finalContentType[0]) {
                    finalContentType[0] = PostModel.Companion.TYPE_IMAGE
                }
            } else {
                android.util.Log.w(
                    ComposePostActivity.Companion.TAG,
                    "Could not determine media type for URI: " + fileUri + ". Defaulting or skipping."
                )
                // قد ترغب في إظهار رسالة خطأ للمستخدم هنا أو تخطي هذا الملف
                // continue;
            }

            val userId = currentUserModel.getUserId()
            val fileName = "post_media/" + userId + "/" + java.util.UUID.randomUUID()
                .toString() + fileExtension
            val mediaRef: StorageReference = storage.getReference().child(fileName)
            val uploadTask: UploadTask = mediaRef.putFile(fileUri)

            val getUrlTask: com.google.android.gms.tasks.Task<android.net.Uri?>? =
                uploadTask.continueWithTask({ task ->
                    if (!task.isSuccessful()) {
                        if (task.getException() != null) {
                            throw task.getException()
                        }
                        throw java.lang.Exception("Upload failed without specific exception for " + fileUri)
                    }
                    mediaRef.getDownloadUrl()
                })
            uploadTasks.add(getUrlTask)
        }


        Tasks.whenAllSuccess<kotlin.Any?>(uploadTasks)
            .addOnSuccessListener(OnSuccessListener { results: kotlin.collections.MutableList<kotlin.Any?>? ->
                for (result in results!!) {
                    if (result is android.net.Uri) {
                        uploadedMediaUrls.add(result.toString())
                    }
                }
                var postContentType = finalContentType[0]
                if (content.isEmpty() && !uploadedMediaUrls.isEmpty()) {
                    if (uploadedMediaUrls.stream().anyMatch { url: kotlin.String? ->
                            url!!.lowercase(java.util.Locale.getDefault())
                                .matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$".toRegex())
                        }) {
                        postContentType = PostModel.Companion.TYPE_VIDEO
                    } else if (uploadedMediaUrls.stream().anyMatch { url: kotlin.String? ->
                            url!!.lowercase(java.util.Locale.getDefault())
                                .matches(".*\\.(jpg|jpeg|png|gif)(\\?.*)?$".toRegex())
                        }) {
                        postContentType = PostModel.Companion.TYPE_IMAGE
                    }
                } else if (content.isEmpty() && uploadedMediaUrls.isEmpty() && currentLinkPreview == null) {
                    showProgress(false)
                    Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                } else if (!content.isEmpty()) { // إذا كان هناك نص، فيمكن أن يكون نوع المنشور نصيًا حتى لو كانت هناك وسائط
                    postContentType = PostModel.Companion.TYPE_TEXT
                }

                // إذا كان هناك رابط فقط وبدون محتوى نصي أو وسائط، فيمكنك اعتباره من نوع TYPE_TEXT مع linkPreview
                savePostToFirestore(content, uploadedMediaUrls, postContentType)
            }).addOnFailureListener(OnFailureListener { e: java.lang.Exception? ->
                showProgress(false)
                Toast.makeText(
                    this@ComposePostActivity,
                    getString(R.string.media_upload_failed) + ": " + e!!.message,
                    Toast.LENGTH_LONG
                ).show()
                android.util.Log.e(
                    ComposePostActivity.Companion.TAG,
                    "Media upload failed: " + e.message,
                    e
                )
            })
    }


    private fun savePostToFirestore(
        content: kotlin.String,
        mediaUrls: kotlin.collections.MutableList<kotlin.String?>?,
        contentType: kotlin.String?
    ) {
        android.util.Log.d(
            ComposePostActivity.Companion.TAG,
            "Attempting to save post to Firestore."
        )
        android.util.Log.d(
            ComposePostActivity.Companion.TAG,
            "Author ID: " + (if (currentUserModel != null) currentUserModel.getUserId() else "currentUserModel is NULL")
        )
        android.util.Log.d(
            ComposePostActivity.Companion.TAG,
            "Author Username: " + (if (currentUserModel != null && currentUserModel.getUsername() != null) currentUserModel.getUsername() else "N/A")
        )
        android.util.Log.d(
            ComposePostActivity.Companion.TAG,
            "Author DisplayName: " + (if (currentUserModel != null && currentUserModel.getDisplayName() != null) currentUserModel.getDisplayName() else "N/A")
        )
        android.util.Log.d(ComposePostActivity.Companion.TAG, "Content: [" + content + "]")
        android.util.Log.d(ComposePostActivity.Companion.TAG, "Content Type: " + contentType)
        android.util.Log.d(
            ComposePostActivity.Companion.TAG,
            "Media URLs: " + (if (mediaUrls != null) mediaUrls.toString() else "null")
        )


        if (currentUserModel == null || currentUserModel.getUserId() == null) {
            android.util.Log.e(
                ComposePostActivity.Companion.TAG,
                "Cannot save post: currentUserModel or its ID is null."
            )
            Toast.makeText(
                this,
                getString(R.string.error_user_data_cannot_save_post),
                Toast.LENGTH_LONG
            ).show()
            showProgress(false)
            return
        }

        // التحقق الأساسي: إذا لم يكن هناك محتوى نصي ولا وسائط ولا رابط، فلا تنشئ المنشور
        if (content.isEmpty() && (mediaUrls == null || mediaUrls.isEmpty()) && currentLinkPreview == null) {
            android.util.Log.e(
                ComposePostActivity.Companion.TAG,
                "Attempting to save an empty post."
            )
            Toast.makeText(this, R.string.post_empty_error, Toast.LENGTH_SHORT).show()
            showProgress(false)
            return
        }

        val post: PostModel = PostModel(currentUserModel.getUserId(), content)

        val displayNameToSave =
            if (currentUserModel.getDisplayName() != null && !currentUserModel.getDisplayName()
                    .isEmpty()
            )
                currentUserModel.getDisplayName()
            else
                (if (currentUserModel.getUsername() != null) currentUserModel.getUsername() else "Unknown User")
        post.setAuthorDisplayName(displayNameToSave)

        post.setAuthorUsername(if (currentUserModel.getUsername() != null) currentUserModel.getUsername() else "unknown_user")
        post.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl())
        post.setAuthorVerified(currentUserModel.isVerified())
        // سيتم تعيين createdAt بواسطة Firestore باستخدام FieldValue.serverTimestamp()
        post.setMediaUrls(if (mediaUrls != null) mediaUrls else java.util.ArrayList<kotlin.String?>())

        // تحديد contentType بشكل أدق
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            // إذا كان هناك فيديو، فالنوع هو فيديو
            if (mediaUrls.stream().anyMatch { url: kotlin.String? ->
                    url!!.lowercase(java.util.Locale.getDefault())
                        .matches(".*\\.(mp4|mov|mkv|webm|3gp|avi)(\\?.*)?$".toRegex())
                }) {
                post.setContentType(PostModel.Companion.TYPE_VIDEO)
            } else { // وإلا، إذا كانت هناك وسائط، فهي صور
                post.setContentType(PostModel.Companion.TYPE_IMAGE)
            }
        } else { // إذا لم تكن هناك وسائط، فالنوع هو نص (وقد يحتوي على رابط)
            post.setContentType(PostModel.Companion.TYPE_TEXT)
        }


        if (currentLinkPreview != null) {
            val previews: kotlin.collections.MutableList<LinkPreview?> =
                java.util.ArrayList<LinkPreview?>()
            previews.add(currentLinkPreview)
            post.setLinkPreviews(previews)
        }

        db.collection("posts").add(post)
            .addOnSuccessListener({ documentReference ->
                val generatedPostId: kotlin.String? = documentReference.getId()
                android.util.Log.d(
                    ComposePostActivity.Companion.TAG,
                    "Post added with ID: " + generatedPostId
                )
                val updates: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?> =
                    java.util.HashMap<kotlin.String?, kotlin.Any?>()
                updates.put("postId", generatedPostId)
                updates.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
                ) // استخدام الطابع الزمني للخادم
                documentReference.update(updates)
                    .addOnSuccessListener({ aVoid ->
                        android.util.Log.d(
                            ComposePostActivity.Companion.TAG,
                            "Post ID and server timestamp updated successfully for " + generatedPostId
                        )
                        showProgress(false)
                        Toast.makeText(
                            this@ComposePostActivity,
                            R.string.post_success,
                            Toast.LENGTH_SHORT
                        ).show()
                        clearMediaPreview()
                        binding!!.etContent.setText("")
                        removeLinkPreview()
                        finish()
                    })
                    .addOnFailureListener({ e ->
                        android.util.Log.e(
                            ComposePostActivity.Companion.TAG,
                            "Error updating post with postId and server timestamp for " + generatedPostId,
                            e
                        )
                        Toast.makeText(
                            this@ComposePostActivity,
                            getString(R.string.post_created_finalize_failed, e.getMessage()),
                            Toast.LENGTH_LONG
                        ).show()
                        showProgress(false)
                    })
            })
            .addOnFailureListener({ e ->
                showProgress(false)
                Toast.makeText(
                    this,
                    getString(R.string.post_failed) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG
                ).show()
                android.util.Log.e(
                    ComposePostActivity.Companion.TAG,
                    "Error adding post to Firestore: " + e.getMessage(),
                    e
                )
            })
    }

    private fun showProgress(show: kotlin.Boolean) {
        binding!!.progressContainer.setVisibility(if (show) android.view.View.VISIBLE else android.view.View.GONE)
        binding!!.btnPost.setEnabled(!show)
        binding!!.btnAddPhoto.setEnabled(!show)
        binding!!.btnAddVideo.setEnabled(!show)
        binding!!.etContent.setEnabled(!show)
    }

    private fun clearMediaPreview() {
        selectedMediaUris.clear()
        existingMediaUrls.clear()
        if (mediaPreviewAdapter != null) {
            mediaPreviewAdapter.notifyDataSetChanged()
        }
        binding!!.rvMediaPreview.setVisibility(android.view.View.GONE)
        updatePostButtonState()
    }

    private fun getFileExtension(uri: android.net.Uri): kotlin.String {
        val mimeType = getContentResolver().getType(uri)
        var extension = ".jpg" // Default extension
        if (mimeType != null) {
            val extFromMime: kotlin.String? =
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extFromMime != null && !extFromMime.equals("null", ignoreCase = true)) {
                extension = "." + extFromMime
            } else {
                if (mimeType.startsWith("image/jpeg")) extension = ".jpg"
                else if (mimeType.startsWith("image/png")) extension = ".png"
                else if (mimeType.startsWith("image/gif")) extension = ".gif"
                else if (mimeType.startsWith("video/mp4")) extension = ".mp4"
                else if (mimeType.startsWith("video/webm")) extension = ".webm"
                else if (mimeType.startsWith("video/quicktime")) extension = ".mov"
                else if (mimeType.startsWith("video/x-matroska")) extension = ".mkv"
                else if (mimeType.startsWith("video/3gpp")) extension = ".3gp"
                else if (mimeType.startsWith("video/x-msvideo")) extension = ".avi"
            }
        } else {
            val path = uri.getPath()
            if (path != null) {
                val lastDot = path.lastIndexOf(".")
                if (lastDot >= 0 && lastDot < path.length - 1) {
                    extension = path.substring(lastDot)
                }
            }
        }
        return extension
    }

    companion object {
        private const val TAG = "ComposePostActivity"
    }
}