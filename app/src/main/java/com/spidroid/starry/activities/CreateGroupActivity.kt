package com.spidroid.starry.activities

// استخدام نموذج Chat
// استيراد Arrays
import com.google.firebase.auth.FirebaseAuth

class CreateGroupActivity : AppCompatActivity(), OnUserSelectionChangedListener {
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var storage: FirebaseStorage? = null
    private var currentUser: FirebaseUser? = null

    private var toolbar: androidx.appcompat.widget.Toolbar? = null
    private var ivGroupImage: de.hdodenhof.circleimageview.CircleImageView? = null
    private var btnSelectGroupImage: android.widget.Button? = null
    private var etGroupName: EditText? = null
    private var etGroupDescription: EditText? = null
    private var etSearchMembers: EditText? = null
    private var rvSelectMembers: RecyclerView? = null
    private var tvSelectedMembersCount: TextView? = null
    private var pbLoading: ProgressBar? = null
    private var fabCreateGroup: FloatingActionButton? = null

    private var userSelectionAdapter: UserSelectionAdapter? = null
    private val allUsersForSelection: kotlin.collections.MutableList<UserModel?> =
        java.util.ArrayList<UserModel?>()
    private var groupImageUri: android.net.Uri? = null

    private var pickImageLauncher: ActivityResultLauncher<Intent?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        currentUser = auth.getCurrentUser()

        if (currentUser == null) {
            Toast.makeText(this, "Authentication required.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadUsersForSelection()

        pickImageLauncher =
            registerForActivityResult<Intent?, androidx.activity.result.ActivityResult?>(
                StartActivityForResult(),
                ActivityResultCallback { result: androidx.activity.result.ActivityResult? ->
                    if (result!!.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData()!!
                            .getData() != null
                    ) {
                        groupImageUri = result.getData()!!.getData()
                        Glide.with(this).load(groupImageUri).into(ivGroupImage)
                    }
                })
    }

    private fun initializeViews() {
        toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_create_group)
        ivGroupImage =
            findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.iv_group_image)
        btnSelectGroupImage = findViewById<android.widget.Button>(R.id.btn_select_group_image)
        etGroupName = findViewById<EditText>(R.id.et_group_name)
        etGroupDescription = findViewById<EditText>(R.id.et_group_description)
        etSearchMembers = findViewById<EditText>(R.id.et_search_members)
        rvSelectMembers = findViewById<RecyclerView>(R.id.rv_select_members)
        tvSelectedMembersCount = findViewById<TextView>(R.id.tv_selected_members_count)
        pbLoading = findViewById<ProgressBar>(R.id.pb_create_group_loading)
        fabCreateGroup = findViewById<FloatingActionButton>(R.id.fab_create_group)
        fabCreateGroup.setEnabled(false)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
            getSupportActionBar().setDisplayShowHomeEnabled(true)
        }
        toolbar!!.setNavigationOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> finish() })
    }

    private fun setupRecyclerView() {
        userSelectionAdapter = UserSelectionAdapter(this, java.util.ArrayList<UserModel?>(), this)
        rvSelectMembers.setLayoutManager(LinearLayoutManager(this))
        rvSelectMembers.setAdapter(userSelectionAdapter)
    }

    private fun setupListeners() {
        btnSelectGroupImage!!.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            val intent: Intent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        })

        etSearchMembers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (userSelectionAdapter != null) {
                    userSelectionAdapter.getFilter().filter(s)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        fabCreateGroup.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> createGroup() })

        etGroupName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })
    }

    override fun onSelectionChanged(count: Int) {
        // تأكد من أن لديك string resource لهذا
        tvSelectedMembersCount.setText(getString(R.string.selected_members_count_format, count))
        validateInputs()
    }

    private fun validateInputs() {
        val groupName = etGroupName.getText().toString().trim { it <= ' ' }
        val isNameValid =
            !TextUtils.isEmpty(groupName) && groupName.length <= CreateGroupActivity.Companion.MAX_GROUP_NAME_LENGTH
        val areMembersSelected =
            userSelectionAdapter != null && !userSelectionAdapter.getSelectedUserIds().isEmpty()
        fabCreateGroup.setEnabled(isNameValid && areMembersSelected)
    }

    private fun loadUsersForSelection() {
        pbLoading.setVisibility(android.view.View.VISIBLE)
        // جلب قائمة المستخدمين الذين يتابعهم المستخدم الحالي أو يتابعونه
        // هذا مثال مبسط، قد تحتاج إلى منطق أكثر تعقيدًا
        db.collection("users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener({ currentUserDoc ->
                val currentUserData: UserModel? = currentUserDoc.toObject(UserModel::class.java)
                if (currentUserData == null) {
                    pbLoading.setVisibility(android.view.View.GONE)
                    Toast.makeText(this, "Failed to load your data.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val potentialMemberIds: kotlin.collections.MutableList<kotlin.String?> =
                    java.util.ArrayList<kotlin.String?>()
                if (currentUserData.getFollowing() != null) {
                    potentialMemberIds.addAll(currentUserData.getFollowing().keys)
                }

                // يمكنك إضافة المتابعين أيضًا إذا أردت، أو أي منطق آخر لاختيار المستخدمين
                // if (currentUserData.getFollowers() != null) {
                //     potentialMemberIds.addAll(currentUserData.getFollowers().keySet());
                // }

                // إزالة التكرار وإزالة المستخدم الحالي إذا كان موجودًا
                val distinctIds: kotlin.collections.MutableList<kotlin.String?> =
                    java.util.ArrayList<kotlin.String?>(
                        java.util.HashSet<kotlin.String?>(potentialMemberIds)
                    )
                distinctIds.remove(currentUser.getUid())

                if (distinctIds.isEmpty()) {
                    pbLoading.setVisibility(android.view.View.GONE)
                    Toast.makeText(this, "No users to add to group.", Toast.LENGTH_SHORT).show()
                    if (userSelectionAdapter != null) {
                        userSelectionAdapter.setData(java.util.ArrayList<UserModel?>())
                    }
                    return@addOnSuccessListener
                }

                // جلب تفاصيل المستخدمين
                // Firestore 'in' query is limited to 10 elements. Chunk if necessary.
                // For simplicity, we'll fetch all if less than or equal to 10.
                // In a real app, you'd implement pagination or chunking for larger lists.
                val chunks = chunkList<kotlin.String?>(distinctIds, 10)
                allUsersForSelection.clear()
                val chunksProcessed = kotlin.intArrayOf(0)

                if (chunks.isEmpty()) {
                    pbLoading.setVisibility(android.view.View.GONE)
                    if (userSelectionAdapter != null) {
                        userSelectionAdapter.setData(java.util.ArrayList<UserModel?>())
                    }
                    return@addOnSuccessListener
                }
                for (chunk in chunks) {
                    if (chunk.isEmpty()) { // تجاوز Chunk فارغ
                        chunksProcessed[0]++
                        if (chunksProcessed[0] == chunks.size) {
                            pbLoading.setVisibility(android.view.View.GONE)
                            if (userSelectionAdapter != null) {
                                userSelectionAdapter.setData(allUsersForSelection)
                            }
                        }
                        continue
                    }
                    db.collection("users").whereIn("userId", chunk).get()
                        .addOnSuccessListener({ queryDocumentSnapshots ->
                            for (document in queryDocumentSnapshots) {
                                val user: UserModel? = document.toObject(UserModel::class.java)
                                if (user != null) {
                                    user.setUserId(document.getId())
                                    allUsersForSelection.add(user)
                                }
                            }
                            chunksProcessed[0]++
                            if (chunksProcessed[0] == chunks.size) {
                                pbLoading.setVisibility(android.view.View.GONE)
                                if (userSelectionAdapter != null) {
                                    userSelectionAdapter.setData(allUsersForSelection)
                                }
                            }
                        })
                        .addOnFailureListener({ e ->
                            chunksProcessed[0]++
                            android.util.Log.e(
                                CreateGroupActivity.Companion.TAG,
                                "Error loading users for selection chunk",
                                e
                            )
                            if (chunksProcessed[0] == chunks.size) { // فقط إذا كان هذا آخر chunk فاشل
                                pbLoading.setVisibility(android.view.View.GONE)
                                Toast.makeText(
                                    this@CreateGroupActivity,
                                    "Failed to load some users.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (userSelectionAdapter != null) {
                                    userSelectionAdapter.setData(allUsersForSelection) // عرض ما تم تحميله
                                }
                            }
                        })
                }
            })
            .addOnFailureListener({ e ->
                pbLoading.setVisibility(android.view.View.GONE)
                android.util.Log.e(
                    CreateGroupActivity.Companion.TAG,
                    "Error loading current user's following list",
                    e
                )
                Toast.makeText(
                    this,
                    "Failed to load your connections: " + e.getMessage(),
                    Toast.LENGTH_SHORT
                ).show()
            })
    }

    private fun <T> chunkList(
        list: kotlin.collections.MutableList<T?>?,
        chunkSize: Int
    ): kotlin.collections.MutableList<kotlin.collections.MutableList<T?>> {
        val chunks: kotlin.collections.MutableList<kotlin.collections.MutableList<T?>> =
            java.util.ArrayList<kotlin.collections.MutableList<T?>>()
        if (list == null || list.isEmpty() || chunkSize <= 0) return chunks
        var i = 0
        while (i < list.size) {
            chunks.add(
                java.util.ArrayList<T?>(
                    list.subList(
                        i,
                        kotlin.math.min(list.size.toDouble(), (i + chunkSize).toDouble()).toInt()
                    )
                )
            )
            i += chunkSize
        }
        return chunks
    }

    private fun createGroup() {
        val groupName = etGroupName.getText().toString().trim { it <= ' ' }
        val groupDescription = etGroupDescription.getText().toString().trim { it <= ' ' }

        if (userSelectionAdapter == null) {
            Toast.makeText(this, "Error: Members adapter not initialized.", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val selectedMemberIdsSet = userSelectionAdapter.getSelectedUserIds()

        if (TextUtils.isEmpty(groupName)) {
            etGroupName.setError("Group name is required.")
            return
        }
        if (groupName.length > CreateGroupActivity.Companion.MAX_GROUP_NAME_LENGTH) {
            etGroupName.setError("Group name is too long (max " + CreateGroupActivity.Companion.MAX_GROUP_NAME_LENGTH + " chars).")
            return
        }
        if (groupDescription.length > CreateGroupActivity.Companion.MAX_GROUP_DESC_LENGTH) {
            etGroupDescription.setError("Description is too long (max " + CreateGroupActivity.Companion.MAX_GROUP_DESC_LENGTH + " chars).")
            return
        }
        if (selectedMemberIdsSet.isEmpty()) {
            Toast.makeText(this, "Please select at least one member.", Toast.LENGTH_SHORT).show()
            return
        }

        pbLoading.setVisibility(android.view.View.VISIBLE)
        fabCreateGroup.setEnabled(false)

        val memberIds: kotlin.collections.MutableList<kotlin.String?> =
            java.util.ArrayList<kotlin.String?>(selectedMemberIdsSet)
        memberIds.add(currentUser.getUid())

        if (groupImageUri != null) {
            uploadGroupImageAndCreateGroup(groupName, groupDescription, memberIds)
        } else {
            saveGroupToFirestore(groupName, groupDescription, null, memberIds)
        }
    }

    private fun uploadGroupImageAndCreateGroup(
        groupName: kotlin.String?,
        groupDescription: kotlin.String?,
        memberIdsList: kotlin.collections.MutableList<kotlin.String?>?
    ) {
        val storageRef: StorageReference =
            storage.getReference().child("group_images/" + java.util.UUID.randomUUID().toString())
        storageRef.putFile(groupImageUri)
            .addOnSuccessListener({ taskSnapshot ->
                storageRef.getDownloadUrl()
                    .addOnSuccessListener({ uri ->
                        val imageUrl: kotlin.String? = uri.toString()
                        saveGroupToFirestore(groupName, groupDescription, imageUrl, memberIdsList)
                    })
                    .addOnFailureListener({ e ->
                        pbLoading.setVisibility(android.view.View.GONE)
                        fabCreateGroup.setEnabled(true)
                        android.util.Log.e(
                            CreateGroupActivity.Companion.TAG,
                            "Failed to get group image URL",
                            e
                        )
                        Toast.makeText(
                            this@CreateGroupActivity,
                            "Failed to get image URL: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                        ).show()
                    })
            })
            .addOnFailureListener({ e ->
                pbLoading.setVisibility(android.view.View.GONE)
                fabCreateGroup.setEnabled(true)
                android.util.Log.e(
                    CreateGroupActivity.Companion.TAG,
                    "Failed to upload group image",
                    e
                )
                Toast.makeText(
                    this@CreateGroupActivity,
                    "Failed to upload image: " + e.getMessage(),
                    Toast.LENGTH_SHORT
                ).show()
            })
    }

    private fun saveGroupToFirestore(
        groupName: kotlin.String?,
        groupDescription: kotlin.String?,
        groupImageUrl: kotlin.String?,
        memberIdsList: kotlin.collections.MutableList<kotlin.String?>?
    ) {
        val groupDocRef: DocumentReference =
            db.collection("chats").document() // استخدام collection "chats"

        val newGroup: Chat = Chat()
        newGroup.setId(groupDocRef.getId())
        newGroup.setGroupName(groupName)
        if (!TextUtils.isEmpty(groupDescription)) {
            newGroup.setGroupDescription(groupDescription)
        }
        if (groupImageUrl != null) {
            newGroup.setGroupImage(groupImageUrl)
        }
        newGroup.setCreatorId(currentUser.getUid())
        newGroup.setAdmins(java.util.Arrays.asList(currentUser.getUid()))
        newGroup.setParticipants(memberIdsList)
        newGroup.setGroup(true) // isGroup = true
        newGroup.setLastMessageTime(java.util.Date()) // أو FieldValue.serverTimestamp() إذا كنت ستضبطه كـ Map
        newGroup.setLastMessage("Group created")


        // تحويل كائن Chat إلى Map ليتم حفظه
        val groupDataMap: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?> =
            java.util.HashMap<kotlin.String?, kotlin.Any?>()
        groupDataMap.put("id", newGroup.getId())
        groupDataMap.put("groupName", newGroup.getGroupName())
        if (newGroup.getGroupDescription() != null) groupDataMap.put(
            "groupDescription",
            newGroup.getGroupDescription()
        )
        if (newGroup.getGroupImage() != null) groupDataMap.put(
            "groupImage",
            newGroup.getGroupImage()
        )
        groupDataMap.put("creatorId", newGroup.getCreatorId())
        groupDataMap.put("admins", newGroup.getAdmins())
        groupDataMap.put("participants", newGroup.getParticipants())
        groupDataMap.put("isGroup", newGroup.isGroup())
        groupDataMap.put("createdAt", FieldValue.serverTimestamp()) // استخدام الطابع الزمني للخادم
        groupDataMap.put("lastMessage", newGroup.getLastMessage())
        groupDataMap.put("lastMessageTime", FieldValue.serverTimestamp())


        groupDocRef.set(groupDataMap) // استخدام set مع Map
            .addOnSuccessListener({ aVoid ->
                pbLoading.setVisibility(android.view.View.GONE)
                Toast.makeText(
                    this@CreateGroupActivity,
                    "Group '" + groupName + "' created successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                val intent: Intent = Intent(this@CreateGroupActivity, ChatActivity::class.java)
                intent.putExtra("chatId", groupDocRef.getId())
                intent.putExtra("isGroup", true)
                startActivity(intent)
                finish()
            })
            .addOnFailureListener({ e ->
                pbLoading.setVisibility(android.view.View.GONE)
                fabCreateGroup.setEnabled(true)
                android.util.Log.e(CreateGroupActivity.Companion.TAG, "Error creating group", e)
                Toast.makeText(
                    this@CreateGroupActivity,
                    "Failed to create group: " + e.getMessage(),
                    Toast.LENGTH_SHORT
                ).show()
            })
    }

    companion object {
        private const val TAG = "CreateGroupActivity"
        private const val MAX_GROUP_NAME_LENGTH = 50
        private const val MAX_GROUP_DESC_LENGTH = 200
    }
}