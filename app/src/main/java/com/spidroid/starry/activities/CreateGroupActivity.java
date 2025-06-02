package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.UserSelectionAdapter;
import com.spidroid.starry.models.Chat; // استخدام نموذج Chat
import com.spidroid.starry.models.UserModel;
import java.util.ArrayList;
import java.util.Arrays; // استيراد Arrays
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import de.hdodenhof.circleimageview.CircleImageView;

public class CreateGroupActivity extends AppCompatActivity implements UserSelectionAdapter.OnUserSelectionChangedListener {

    private static final String TAG = "CreateGroupActivity";
    private static final int MAX_GROUP_NAME_LENGTH = 50;
    private static final int MAX_GROUP_DESC_LENGTH = 200;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    private Toolbar toolbar;
    private CircleImageView ivGroupImage;
    private Button btnSelectGroupImage;
    private EditText etGroupName, etGroupDescription, etSearchMembers;
    private RecyclerView rvSelectMembers;
    private TextView tvSelectedMembersCount;
    private ProgressBar pbLoading;
    private FloatingActionButton fabCreateGroup;

    private UserSelectionAdapter userSelectionAdapter;
    private List<UserModel> allUsersForSelection = new ArrayList<>();
    private Uri groupImageUri;

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Authentication required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadUsersForSelection();

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        groupImageUri = result.getData().getData();
                        Glide.with(this).load(groupImageUri).into(ivGroupImage);
                    }
                });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_create_group);
        ivGroupImage = findViewById(R.id.iv_group_image);
        btnSelectGroupImage = findViewById(R.id.btn_select_group_image);
        etGroupName = findViewById(R.id.et_group_name);
        etGroupDescription = findViewById(R.id.et_group_description);
        etSearchMembers = findViewById(R.id.et_search_members);
        rvSelectMembers = findViewById(R.id.rv_select_members);
        tvSelectedMembersCount = findViewById(R.id.tv_selected_members_count);
        pbLoading = findViewById(R.id.pb_create_group_loading);
        fabCreateGroup = findViewById(R.id.fab_create_group);
        fabCreateGroup.setEnabled(false);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        userSelectionAdapter = new UserSelectionAdapter(this, new ArrayList<>(), this);
        rvSelectMembers.setLayoutManager(new LinearLayoutManager(this));
        rvSelectMembers.setAdapter(userSelectionAdapter);
    }

    private void setupListeners() {
        btnSelectGroupImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        etSearchMembers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (userSelectionAdapter != null) {
                    userSelectionAdapter.getFilter().filter(s);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabCreateGroup.setOnClickListener(v -> createGroup());

        etGroupName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateInputs(); }
        });
    }

    @Override
    public void onSelectionChanged(int count) {
        // تأكد من أن لديك string resource لهذا
        tvSelectedMembersCount.setText(getString(R.string.selected_members_count_format, count));
        validateInputs();
    }

    private void validateInputs() {
        String groupName = etGroupName.getText().toString().trim();
        boolean isNameValid = !TextUtils.isEmpty(groupName) && groupName.length() <= MAX_GROUP_NAME_LENGTH;
        boolean areMembersSelected = userSelectionAdapter != null && !userSelectionAdapter.getSelectedUserIds().isEmpty();
        fabCreateGroup.setEnabled(isNameValid && areMembersSelected);
    }

    private void loadUsersForSelection() {
        pbLoading.setVisibility(View.VISIBLE);
        // جلب قائمة المستخدمين الذين يتابعهم المستخدم الحالي أو يتابعونه
        // هذا مثال مبسط، قد تحتاج إلى منطق أكثر تعقيدًا
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(currentUserDoc -> {
                    UserModel currentUserData = currentUserDoc.toObject(UserModel.class);
                    if (currentUserData == null) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to load your data.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> potentialMemberIds = new ArrayList<>();
                    if (currentUserData.getFollowing() != null) {
                        potentialMemberIds.addAll(currentUserData.getFollowing().keySet());
                    }
                    // يمكنك إضافة المتابعين أيضًا إذا أردت، أو أي منطق آخر لاختيار المستخدمين
                    // if (currentUserData.getFollowers() != null) {
                    //     potentialMemberIds.addAll(currentUserData.getFollowers().keySet());
                    // }

                    // إزالة التكرار وإزالة المستخدم الحالي إذا كان موجودًا
                    List<String> distinctIds = new ArrayList<>(new HashSet<>(potentialMemberIds));
                    distinctIds.remove(currentUser.getUid());

                    if (distinctIds.isEmpty()) {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "No users to add to group.", Toast.LENGTH_SHORT).show();
                        if (userSelectionAdapter != null) {
                            userSelectionAdapter.setData(new ArrayList<>());
                        }
                        return;
                    }

                    // جلب تفاصيل المستخدمين
                    // Firestore 'in' query is limited to 10 elements. Chunk if necessary.
                    // For simplicity, we'll fetch all if less than or equal to 10.
                    // In a real app, you'd implement pagination or chunking for larger lists.
                    List<List<String>> chunks = chunkList(distinctIds, 10);
                    allUsersForSelection.clear();
                    final int[] chunksProcessed = {0};

                    if (chunks.isEmpty()){
                        pbLoading.setVisibility(View.GONE);
                        if (userSelectionAdapter != null) {
                            userSelectionAdapter.setData(new ArrayList<>());
                        }
                        return;
                    }


                    for (List<String> chunk : chunks) {
                        if (chunk.isEmpty()) { // تجاوز Chunk فارغ
                            chunksProcessed[0]++;
                            if (chunksProcessed[0] == chunks.size()) {
                                pbLoading.setVisibility(View.GONE);
                                if (userSelectionAdapter != null) {
                                    userSelectionAdapter.setData(allUsersForSelection);
                                }
                            }
                            continue;
                        }
                        db.collection("users").whereIn("userId", chunk).get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        UserModel user = document.toObject(UserModel.class);
                                        if (user != null) {
                                            user.setUserId(document.getId());
                                            allUsersForSelection.add(user);
                                        }
                                    }
                                    chunksProcessed[0]++;
                                    if (chunksProcessed[0] == chunks.size()) {
                                        pbLoading.setVisibility(View.GONE);
                                        if (userSelectionAdapter != null) {
                                            userSelectionAdapter.setData(allUsersForSelection);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    chunksProcessed[0]++;
                                    Log.e(TAG, "Error loading users for selection chunk", e);
                                    if (chunksProcessed[0] == chunks.size()) { // فقط إذا كان هذا آخر chunk فاشل
                                        pbLoading.setVisibility(View.GONE);
                                        Toast.makeText(CreateGroupActivity.this, "Failed to load some users.", Toast.LENGTH_SHORT).show();
                                        if (userSelectionAdapter != null) {
                                            userSelectionAdapter.setData(allUsersForSelection); // عرض ما تم تحميله
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading current user's following list", e);
                    Toast.makeText(this, "Failed to load your connections: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        if (list == null || list.isEmpty() || chunkSize <= 0) return chunks;
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + chunkSize))));
        }
        return chunks;
    }

    private void createGroup() {
        String groupName = etGroupName.getText().toString().trim();
        String groupDescription = etGroupDescription.getText().toString().trim();

        if (userSelectionAdapter == null) {
            Toast.makeText(this, "Error: Members adapter not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }
        Set<String> selectedMemberIdsSet = userSelectionAdapter.getSelectedUserIds();

        if (TextUtils.isEmpty(groupName)) {
            etGroupName.setError("Group name is required.");
            return;
        }
        if (groupName.length() > MAX_GROUP_NAME_LENGTH) {
            etGroupName.setError("Group name is too long (max " + MAX_GROUP_NAME_LENGTH + " chars).");
            return;
        }
        if (groupDescription.length() > MAX_GROUP_DESC_LENGTH) {
            etGroupDescription.setError("Description is too long (max " + MAX_GROUP_DESC_LENGTH + " chars).");
            return;
        }
        if (selectedMemberIdsSet.isEmpty()) {
            Toast.makeText(this, "Please select at least one member.", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        fabCreateGroup.setEnabled(false);

        List<String> memberIds = new ArrayList<>(selectedMemberIdsSet);
        memberIds.add(currentUser.getUid());

        if (groupImageUri != null) {
            uploadGroupImageAndCreateGroup(groupName, groupDescription, memberIds);
        } else {
            saveGroupToFirestore(groupName, groupDescription, null, memberIds);
        }
    }

    private void uploadGroupImageAndCreateGroup(String groupName, String groupDescription, List<String> memberIdsList) {
        StorageReference storageRef = storage.getReference().child("group_images/" + UUID.randomUUID().toString());
        storageRef.putFile(groupImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveGroupToFirestore(groupName, groupDescription, imageUrl, memberIdsList);
                        })
                        .addOnFailureListener(e -> {
                            pbLoading.setVisibility(View.GONE);
                            fabCreateGroup.setEnabled(true);
                            Log.e(TAG, "Failed to get group image URL", e);
                            Toast.makeText(CreateGroupActivity.this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    fabCreateGroup.setEnabled(true);
                    Log.e(TAG, "Failed to upload group image", e);
                    Toast.makeText(CreateGroupActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveGroupToFirestore(String groupName, String groupDescription, @Nullable String groupImageUrl, List<String> memberIdsList) {
        DocumentReference groupDocRef = db.collection("chats").document(); // استخدام collection "chats"

        Chat newGroup = new Chat();
        newGroup.setId(groupDocRef.getId());
        newGroup.setGroupName(groupName);
        if (!TextUtils.isEmpty(groupDescription)) {
            newGroup.setGroupDescription(groupDescription);
        }
        if (groupImageUrl != null) {
            newGroup.setGroupImage(groupImageUrl);
        }
        newGroup.setCreatorId(currentUser.getUid());
        newGroup.setAdmins(Arrays.asList(currentUser.getUid()));
        newGroup.setParticipants(memberIdsList);
        newGroup.setGroup(true); // isGroup = true
        newGroup.setLastMessageTime(new Date()); // أو FieldValue.serverTimestamp() إذا كنت ستضبطه كـ Map
        newGroup.setLastMessage("Group created");


        // تحويل كائن Chat إلى Map ليتم حفظه
        Map<String, Object> groupDataMap = new HashMap<>();
        groupDataMap.put("id", newGroup.getId());
        groupDataMap.put("groupName", newGroup.getGroupName());
        if (newGroup.getGroupDescription() != null) groupDataMap.put("groupDescription", newGroup.getGroupDescription());
        if (newGroup.getGroupImage() != null) groupDataMap.put("groupImage", newGroup.getGroupImage());
        groupDataMap.put("creatorId", newGroup.getCreatorId());
        groupDataMap.put("admins", newGroup.getAdmins());
        groupDataMap.put("participants", newGroup.getParticipants());
        groupDataMap.put("isGroup", newGroup.isGroup());
        groupDataMap.put("createdAt", FieldValue.serverTimestamp()); // استخدام الطابع الزمني للخادم
        groupDataMap.put("lastMessage", newGroup.getLastMessage());
        groupDataMap.put("lastMessageTime", FieldValue.serverTimestamp());


        groupDocRef.set(groupDataMap) // استخدام set مع Map
                .addOnSuccessListener(aVoid -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(CreateGroupActivity.this, "Group '" + groupName + "' created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CreateGroupActivity.this, ChatActivity.class);
                    intent.putExtra("chatId", groupDocRef.getId());
                    intent.putExtra("isGroup", true);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    fabCreateGroup.setEnabled(true);
                    Log.e(TAG, "Error creating group", e);
                    Toast.makeText(CreateGroupActivity.this, "Failed to create group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}