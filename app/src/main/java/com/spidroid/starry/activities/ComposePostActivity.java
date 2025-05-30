package com.spidroid.starry.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ActivityComposePostBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import java.util.Date;

public class ComposePostActivity extends AppCompatActivity {

  private ActivityComposePostBinding binding;
  private FirebaseAuth auth;
  private FirebaseFirestore db;
  private UserModel currentUserModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityComposePostBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    initializeFirebase();
    setupListeners();

    // تعطيل زر النشر في البداية حتى يتم تحميل بيانات المستخدم
    binding.btnPost.setEnabled(false);
    binding.btnPost.setText("Loading...");

    loadCurrentUser();
  }

  private void initializeFirebase() {
    auth = FirebaseAuth.getInstance();
    db = FirebaseFirestore.getInstance();
  }

  private void loadCurrentUser() {
    FirebaseUser user = auth.getCurrentUser();
    if (user == null) {
      Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot != null && documentSnapshot.exists()) {
                currentUserModel = documentSnapshot.toObject(UserModel.class);
                // تفعيل الزر فقط بعد تحميل البيانات بنجاح
                if (currentUserModel != null) {
                  binding.btnPost.setEnabled(true);
                  binding.btnPost.setText(R.string.post);
                } else {
                  Toast.makeText(this, "Could not deserialize user model.", Toast.LENGTH_SHORT).show();
                  binding.btnPost.setText("Error");
                }
              } else {
                Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                binding.btnPost.setText("Error");
              }
            })
            .addOnFailureListener(e -> {
              Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
              binding.btnPost.setText("Error");
            });
  }

  private void setupListeners() {
    binding.btnClose.setOnClickListener(v -> finish());
    binding.btnPost.setOnClickListener(v -> createNewPost());
  }

  private void createNewPost() {
    String content = binding.etContent.getText().toString().trim();
    if (content.isEmpty()) {
      Toast.makeText(this, "Post cannot be empty", Toast.LENGTH_SHORT).show();
      return;
    }

    // تحقق أخير للتأكد من أن بيانات المستخدم موجودة
    if (currentUserModel == null) {
      Toast.makeText(this, "User data not loaded yet, please wait.", Toast.LENGTH_SHORT).show();
      return;
    }

    binding.progressContainer.setVisibility(View.VISIBLE);
    binding.btnPost.setEnabled(false);

    PostModel post = new PostModel(currentUserModel.getUserId(), content);
    post.setAuthorUsername(currentUserModel.getUsername());
    post.setAuthorDisplayName(currentUserModel.getDisplayName());
    post.setAuthorAvatarUrl(currentUserModel.getProfileImageUrl());
    post.setAuthorVerified(currentUserModel.isVerified());
    post.setCreatedAt(new Date()); // Ensure createdAt is set

    db.collection("posts").add(post)
            .addOnSuccessListener(documentReference -> {
              binding.progressContainer.setVisibility(View.GONE);
              Toast.makeText(this, "Posted successfully", Toast.LENGTH_SHORT).show();
              finish();
            })
            .addOnFailureListener(e -> {
              binding.progressContainer.setVisibility(View.GONE);
              binding.btnPost.setEnabled(true);
              Toast.makeText(this, "Post failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
  }
}