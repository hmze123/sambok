package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.firestore.DocumentSnapshot;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ActivityEditProfileBinding;
import com.spidroid.starry.models.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private UserModel originalUser;
    private Uri profileImageUri, coverImageUri;
    private boolean hasChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        setupUI();
        loadUserData();
        setupTextWatchers();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnSave.setOnClickListener(v -> saveProfile());
        binding.btnAddSocial.setOnClickListener(v -> showAddSocialDialog());
        binding.btnChangePhoto.setOnClickListener(v -> openImagePicker("profile"));
        binding.btnChangeCover.setOnClickListener(v -> openImagePicker("cover"));
    }

    private void loadUserData() {
        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(
                        documentSnapshot -> {
                            originalUser = documentSnapshot.toObject(UserModel.class);
                            if (originalUser != null) {
                                populateFields(originalUser);
                            }
                        });
    }

    private void populateFields(UserModel user) {
        // Profile Image
        Glide.with(this)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .into(binding.ivProfile);

        // Cover Image
        Glide.with(this)
                .load(user.getCoverImageUrl())
                .placeholder(R.drawable.ic_cover_placeholder)
                .into(binding.ivCover);

        // Text Fields
        binding.etDisplayName.setText(user.getDisplayName());
        binding.etUsername.setText(user.getUsername());
        binding.etBio.setText(user.getBio());

        // Privacy Settings
        binding.switchPrivate.setChecked(user.getPrivacySettings().isPrivateAccount());
        binding.switchActivity.setChecked(user.getPrivacySettings().isShowActivityStatus());

        // Social Links
        updateSocialLinksUI(user.getSocialLinks());
    }

    private void updateSocialLinksUI(Map<String, String> socialLinks) {
        binding.layoutSocialLinks.removeAllViews();
        for (Map.Entry<String, String> entry : socialLinks.entrySet()) {
            addSocialLinkView(entry.getKey(), entry.getValue());
        }
    }

    private void addSocialLinkView(String platform, String url) {
        View linkView = LayoutInflater.from(this).inflate(R.layout.item_social_link, null);
        linkView.setTag(platform);

        ImageView icon = linkView.findViewById(R.id.ivPlatform);
        EditText etUrl = linkView.findViewById(R.id.etUrl);
        ImageButton btnRemove = linkView.findViewById(R.id.btnRemove);

        icon.setImageResource(getPlatformIcon(platform));
        etUrl.setText(url);

        btnRemove.setOnClickListener(
                v -> {
                    binding.layoutSocialLinks.removeView(linkView);
                    hasChanges = true;
                });

        etUrl.addTextChangedListener(
                new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        hasChanges = true;
                    }

                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                });

        binding.layoutSocialLinks.addView(linkView);
    }

    private int getPlatformIcon(String platform) {
        switch (platform.toLowerCase()) {
                //            case "twitter": return R.drawable.ic_twitter;
                //            case "instagram": return R.drawable.ic_instagram;
                //            case "facebook": return R.drawable.ic_facebook;
                //            case "linkedin": return R.drawable.ic_linkedin;
            default:
                return R.drawable.ic_link;
        }
    }

    private void showAddSocialDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Social Link")
                .setItems(
                        new String[] {"Twitter", "Instagram", "Facebook", "LinkedIn"},
                        (dialog, which) -> {
                            String platform = "";
                            switch (which) {
                                case 0:
                                    platform = "twitter";
                                    break;
                                case 1:
                                    platform = "instagram";
                                    break;
                                case 2:
                                    platform = "facebook";
                                    break;
                                case 3:
                                    platform = "linkedin";
                                    break;
                            }
                            addSocialLinkView(platform, "");
                            hasChanges = true;
                        })
                .show();
    }

    private void openImagePicker(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, type.equals("profile") ? 100 : 101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (requestCode == 100) {
                binding.ivProfile.setImageURI(uri);
                profileImageUri = uri;
            } else if (requestCode == 101) {
                binding.ivCover.setImageURI(uri);
                coverImageUri = uri;
            }
            hasChanges = true;
        }
    }

    private void setupTextWatchers() {
        TextWatcher watcher =
                new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        hasChanges = true;
                    }

                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                };

        binding.etDisplayName.addTextChangedListener(watcher);
        binding.etUsername.addTextChangedListener(watcher);
        binding.etBio.addTextChangedListener(watcher);
    }

    @Override
    public void onBackPressed() {
        if (hasChanges) {
            showDiscardDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showDiscardDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setPositiveButton("Discard", (dialog, which) -> finish())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveProfile() {
        if (!validateInputs()) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        // Upload images first if changed
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        if (profileImageUri != null) {
            uploadTasks.add(uploadImage(profileImageUri, "profile"));
        }
        if (coverImageUri != null) {
            uploadTasks.add(uploadImage(coverImageUri, "cover"));
        }

        // Proceed after all uploads succeed
        Tasks.whenAllSuccess(uploadTasks)
                .addOnSuccessListener(
                        urls -> {
                            String profileImageUrl = null;
                            String coverImageUrl = null;
                            int index = 0;

                            // Extract URLs based on upload order
                            if (profileImageUri != null && !urls.isEmpty()) {
                                profileImageUrl = ((Uri) urls.get(index)).toString();
                                index++;
                            }
                            if (coverImageUri != null && !urls.isEmpty() && index < urls.size()) {
                                coverImageUrl = ((Uri) urls.get(index)).toString();
                            }

                            // Update originalUser with new image URLs
                            if (profileImageUrl != null) {
                                originalUser.setProfileImageUrl(profileImageUrl);
                            }
                            if (coverImageUrl != null) {
                                originalUser.setCoverImageUrl(coverImageUrl);
                            }

                            // Update other fields from UI
                            updateOriginalUserFields();
                            updateUserPosts(originalUser);

                            // Save changes to Firestore with merge to avoid overwriting
                            db.collection("users")
                                    .document(originalUser.getUserId())
                                    .set(originalUser, SetOptions.merge())
                                    .addOnSuccessListener(
                                            aVoid -> {
                                                Toast.makeText(
                                                                this,
                                                                "Profile updated",
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                                finish();
                                            })
                                    .addOnFailureListener(
                                            e -> {
                                                binding.progressBar.setVisibility(View.GONE);
                                                Toast.makeText(
                                                                this,
                                                                "Update failed: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT)
                                                        .show();
                                            });
                        })
                .addOnFailureListener(
                        e -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(
                                            this,
                                            "Image upload failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT)
                                    .show();
                        });
    }

    private void updateOriginalUserFields() {
        // Update username, display name, and bio
        originalUser.setUsername(binding.etUsername.getText().toString().trim());
        originalUser.setDisplayName(binding.etDisplayName.getText().toString().trim());
        originalUser.setBio(binding.etBio.getText().toString().trim());

        // Update social links
        Map<String, String> socialLinks = new HashMap<>();
        for (int i = 0; i < binding.layoutSocialLinks.getChildCount(); i++) {
            View view = binding.layoutSocialLinks.getChildAt(i);
            EditText etUrl = view.findViewById(R.id.etUrl);
            String platform =
                    (String) view.getTag(); // Ensure platform is set as view tag when adding
            String url = etUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                socialLinks.put(platform, url);
            }
        }
        originalUser.setSocialLinks(socialLinks);

        // Update privacy settings
        UserModel.PrivacySettings privacySettings = originalUser.getPrivacySettings();
        privacySettings.setPrivateAccount(binding.switchPrivate.isChecked());
        privacySettings.setShowActivityStatus(binding.switchActivity.isChecked());
        originalUser.setPrivacySettings(privacySettings);
    }

    private Task<Uri> uploadImage(Uri uri, String type) {
        StorageReference ref =
                storage.getReference()
                        .child(type + "_images")
                        .child(auth.getCurrentUser().getUid())
                        .child(System.currentTimeMillis() + ".jpg");

        return ref.putFile(uri).continueWithTask(task -> ref.getDownloadUrl());
    }

    private UserModel createUpdatedUser() {
        UserModel user =
                new UserModel(
                        auth.getCurrentUser().getUid(),
                        binding.etUsername.getText().toString(),
                        originalUser.getEmail());

        // Basic info
        user.setDisplayName(binding.etDisplayName.getText().toString());
        user.setBio(binding.etBio.getText().toString());

        // Social links
        Map<String, String> socialLinks = new HashMap<>();
        for (int i = 0; i < binding.layoutSocialLinks.getChildCount(); i++) {
            View view = binding.layoutSocialLinks.getChildAt(i);
            EditText etUrl = view.findViewById(R.id.etUrl);
            String platform = (String) view.getTag();
            if (!etUrl.getText().toString().isEmpty()) {
                socialLinks.put(platform, etUrl.getText().toString());
            }
        }
        user.setSocialLinks(socialLinks);

        // Privacy settings
        UserModel.PrivacySettings privacy = originalUser.getPrivacySettings();
        privacy.setPrivateAccount(binding.switchPrivate.isChecked());
        privacy.setShowActivityStatus(binding.switchActivity.isChecked());
        user.setPrivacySettings(privacy);

        return user;
    }

    private boolean validateInputs() {
        String username = binding.etUsername.getText().toString().trim();
        if (username.isEmpty() || username.length() < 4) {
            binding.etUsername.setError("Invalid username");
            return false;
        }
        // Add more validation as needed
        return true;
    }

    private void updateUserPosts(UserModel updatedUser) {
        db.collection("posts")
                .whereEqualTo("authorId", updatedUser.getUserId())
                .get()
                .addOnSuccessListener(
                        queryDocumentSnapshots -> {
                            for (DocumentSnapshot document :
                                    queryDocumentSnapshots.getDocuments()) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("authorUsername", updatedUser.getUsername());
                                updates.put("authorDisplayName", updatedUser.getDisplayName());
                                updates.put("authorAvatarUrl", updatedUser.getProfileImageUrl());

                                document.getReference().update(updates);
                            }
                        });
    }
}
