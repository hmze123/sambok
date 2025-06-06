// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/CreateGroupActivity.kt
package com.spidroid.starry.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.spidroid.starry.R
import com.spidroid.starry.adapters.UserSelectionAdapter
import com.spidroid.starry.databinding.ActivityCreateGroupBinding
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class CreateGroupActivity : AppCompatActivity(), UserSelectionAdapter.OnUserSelectionChangedListener {

    private lateinit var binding: ActivityCreateGroupBinding
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val storage: FirebaseStorage by lazy { Firebase.storage }
    private var currentUser: FirebaseUser? = null

    private lateinit var userSelectionAdapter: UserSelectionAdapter
    private var groupImageUri: Uri? = null

    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Authentication required.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeLaunchers()
        setupUI()
        loadUsersForSelection()
    }

    private fun initializeLaunchers() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    groupImageUri = uri // ✨ تم تغيير 'it' إلى 'uri'
                    Glide.with(this).load(uri).into(binding.ivGroupImage)
                }
            }
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbarCreateGroup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarCreateGroup.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        // تهيئة userSelectionAdapter بشكل صحيح
        userSelectionAdapter = UserSelectionAdapter(this, this)
        binding.rvSelectMembers.layoutManager = LinearLayoutManager(this)
        binding.rvSelectMembers.adapter = userSelectionAdapter
    }

    private fun setupListeners() {
        binding.btnSelectGroupImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.etSearchMembers.doOnTextChanged { text, _, _, _ ->
            userSelectionAdapter.filter.filter(text)
        }

        binding.fabCreateGroup.setOnClickListener { createGroup() }

        binding.etGroupName.doOnTextChanged { _, _, _, _ -> validateInputs() }
        binding.etGroupDescription.doOnTextChanged { _, _, _, _ -> validateInputs() }
    }

    override fun onSelectionChanged(count: Int) {
        binding.tvSelectedMembersCount.text = getString(R.string.selected_members_count_format, count)
        validateInputs()
    }

    private fun validateInputs() {
        val groupName = binding.etGroupName.text.toString().trim()
        val groupDescription = binding.etGroupDescription.text.toString().trim()
        val isNameValid = groupName.isNotEmpty() && groupName.length <= MAX_GROUP_NAME_LENGTH
        val isDescriptionValid = groupDescription.length <= MAX_GROUP_DESC_LENGTH
        val areMembersSelected = userSelectionAdapter.selectedUserIds.isNotEmpty()

        binding.fabCreateGroup.isEnabled = isNameValid && isDescriptionValid && areMembersSelected
    }

    private fun loadUsersForSelection() {
        showLoading(true)
        val userId = currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { currentUserDoc ->
                val followingMap = currentUserDoc.get("following") as? Map<String, Boolean> ?: emptyMap()
                val userIdsToFetch = followingMap.keys.toList()

                if (userIdsToFetch.isEmpty()) {
                    showLoading(false)
                    Toast.makeText(this, "No users to add to group.", Toast.LENGTH_SHORT).show()
                    // يمكن هنا تحديث RecyclerView بقائمة فارغة لضمان عرض حالة عدم وجود مستخدمين
                    userSelectionAdapter.setData(emptyList())
                    return@addOnSuccessListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val allUsers = fetchUsersInChunks(userIdsToFetch)
                    withContext(Dispatchers.Main) {
                        userSelectionAdapter.setData(allUsers)
                        showLoading(false)
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Error loading user's connections", e)
                Toast.makeText(this, "Failed to load users for selection.", Toast.LENGTH_SHORT).show()
            }
    }

    private suspend fun fetchUsersInChunks(userIds: List<String>): MutableList<UserModel> {
        val allUsers = mutableListOf<UserModel>()
        userIds.chunked(10).forEach { chunk ->
            try {
                val usersSnapshot = db.collection("users").whereIn("userId", chunk).get().await()
                allUsers.addAll(usersSnapshot.toObjects(UserModel::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user chunk", e)
            }
        }
        return allUsers
    }

    private fun createGroup() {
        val groupName = binding.etGroupName.text.toString().trim()
        val groupDescription = binding.etGroupDescription.text.toString().trim()
        val selectedMemberIds = userSelectionAdapter.selectedUserIds.toMutableList()

        if (groupName.isEmpty()) {
            binding.etGroupName.error = "Group name is required."
            return
        }
        if (groupName.length > MAX_GROUP_NAME_LENGTH) {
            binding.etGroupName.error = "Group name too long (max $MAX_GROUP_NAME_LENGTH characters)."
            return
        }
        if (groupDescription.length > MAX_GROUP_DESC_LENGTH) {
            binding.etGroupDescription.error = "Group description too long (max $MAX_GROUP_DESC_LENGTH characters)."
            return
        }
        if (selectedMemberIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one member.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        binding.fabCreateGroup.isEnabled = false

        val creatorId = currentUser?.uid ?: run {
            handleFailure("Creator ID is missing. Please log in.")
            return
        }
        selectedMemberIds.add(creatorId)

        groupImageUri?.let {
            uploadGroupImageAndCreateGroup(groupName, groupDescription, it,
                selectedMemberIds
            )
        } ?: run {
            saveGroupToFirestore(groupName, groupDescription, null,
                selectedMemberIds
            )
        }
    }

    private fun uploadGroupImageAndCreateGroup(name: String, description: String, imageUri: Uri, members: MutableList<String>) {
        val storageRef = storage.reference.child("group_images/${UUID.randomUUID()}")
        storageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                storageRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                saveGroupToFirestore(name, description, downloadUri.toString(), members)
            }.addOnFailureListener { e ->
                handleFailure("Failed to upload image: ${e.message}")
            }
    }

    private fun saveGroupToFirestore(name: String, description: String, imageUrl: String?, members: MutableList<String>) {
        val creatorId = currentUser?.uid ?: return

        val newGroup = Chat(
            id = null,
            groupName = name,
            groupDescription = description.ifBlank { null },
            groupImage = imageUrl,
            creatorId = creatorId,
            admins = mutableListOf(creatorId),
            participants = members,
            isGroup = true,
            lastMessage = "Group created",
            lastMessageTime = null,
            createdAt = null
        )

        val groupDocRef = db.collection("chats").document()
        newGroup.id = groupDocRef.id

        groupDocRef.set(newGroup)
            .addOnSuccessListener {
                groupDocRef.update(
                    "createdAt", FieldValue.serverTimestamp(),
                    "lastMessageTime", FieldValue.serverTimestamp()
                ).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        showLoading(false)
                        Toast.makeText(this, "Group '$name' created successfully!", Toast.LENGTH_SHORT).show()
                        openChatActivity(groupDocRef.id)
                    } else {
                        handleFailure("Failed to set group timestamps: ${updateTask.exception?.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                handleFailure("Failed to create group: ${e.message}")
            }
    }

    private fun handleFailure(message: String) {
        showLoading(false)
        binding.fabCreateGroup.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.pbCreateGroupLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun openChatActivity(chatId: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("isGroup", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "CreateGroupActivity"
        private const val MAX_GROUP_NAME_LENGTH = 50
        private const val MAX_GROUP_DESC_LENGTH = 200
    }
}