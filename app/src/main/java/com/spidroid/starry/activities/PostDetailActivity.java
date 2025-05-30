package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log; // استيراد Log
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.CommentAdapter;
import com.spidroid.starry.databinding.ActivityPostDetailBinding;
import com.spidroid.starry.models.CommentModel;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.viewmodels.CommentViewModel;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects; // استيراد Objects

public class PostDetailActivity extends AppCompatActivity {

  public static final String EXTRA_POST = "post";
  private static final String TAG = "PostDetailActivity"; // لـ Log

  private ActivityPostDetailBinding binding;
  private CommentViewModel commentViewModel;
  private CommentAdapter commentAdapter;
  private PostModel post;
  private FirebaseUser currentUser;
  private UserModel currentUserModel; // افترض أنك ستحتاج إليه لإضافة تعليقات

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    // --- ★★ بداية التعديل والتحقق ★★ ---
    post = getIntent().getParcelableExtra(EXTRA_POST);
    if (post == null) {
      Log.e(TAG, "PostModel received is null.");
      Toast.makeText(this, "Error: Post data not found.", Toast.LENGTH_LONG).show();
      finish(); // إنهاء النشاط إذا لم يتم العثور على بيانات المنشور
      return;
    }

    // تحقق من أن postId ليس null قبل المتابعة
    // هذا هو التحقق الأساسي الذي قد يحل المشكلة المباشرة للـ NullPointerException
    if (post.getPostId() == null || post.getPostId().isEmpty()) {
      Log.e(TAG, "Post ID is null or empty in received PostModel. Author: " + post.getAuthorUsername() + ", Content: " + post.getContent());
      Toast.makeText(this, "Error: Invalid post ID.", Toast.LENGTH_LONG).show();
      finish(); // إنهاء النشاط إذا كان معرّف المنشور غير صالح
      return;
    }
    // --- ★★ نهاية التعديل والتحقق ★★ ---

    currentUser = FirebaseAuth.getInstance().getCurrentUser();
    commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);

    setupToolbar();
    bindPostData();
    setupCommentsRecyclerView();
    setupCommentObservers();
    setupInputSection();

    loadCurrentUserProfile();
    commentViewModel.loadComments(post.getPostId()); // الآن postId مضمون أنه ليس null
  }

  private void setupToolbar() {
    binding.ivBack.setOnClickListener(v -> finish());
    // يمكنك إضافة المزيد من إعدادات Toolbar هنا إذا لزم الأمر
    // مثلاً، عنوان الـ Toolbar يمكن أن يكون اسم صاحب المنشور أو "Post"
  }

  private void bindPostData() {
    // تأكد من أن post ليس null مرة أخرى هنا احتياطًا، على الرغم من التحقق في onCreate
    if (post == null) {
      Log.e(TAG, "PostModel is null in bindPostData. This should not happen.");
      return;
    }

    // استخدام binding للوصول إلى عناصر includedPostLayout
    binding.includedPostLayout.tvAuthorName.setText(post.getAuthorDisplayName() != null ? post.getAuthorDisplayName() : post.getAuthorUsername());
    binding.includedPostLayout.tvUsername.setText("@" + (post.getAuthorUsername() != null ? post.getAuthorUsername() : "unknown"));
    binding.includedPostLayout.tvPostContent.setText(post.getContent() != null ? post.getContent() : "");
    // يمكنك إضافة عرض التاريخ والوقت هنا إذا أردت
    // binding.includedPostLayout.tvTimestamp.setText(formatTimestamp(post.getCreatedAt()));

    if (post.getAuthorAvatarUrl() != null && !post.getAuthorAvatarUrl().isEmpty()) {
      Glide.with(this)
              .load(post.getAuthorAvatarUrl())
              .placeholder(R.drawable.ic_default_avatar)
              .error(R.drawable.ic_default_avatar)
              .into(binding.includedPostLayout.ivAuthorAvatar);
    } else {
      binding.includedPostLayout.ivAuthorAvatar.setImageResource(R.drawable.ic_default_avatar);
    }

    binding.includedPostLayout.ivVerified.setVisibility(post.isAuthorVerified() ? View.VISIBLE : View.GONE);

    // إخفاء عناصر التفاعل في النسخة المضمنة لأنها ستكون خاصة بالشاشة الرئيسية
    binding.includedPostLayout.interactionContainer.setVisibility(View.GONE);
    // أو يمكنك إعدادها لتعمل بشكل مستقل هنا إذا أردت
  }

  private void setupCommentsRecyclerView() {
    // تأكد أن post و postId ليسا null
    if (post == null || post.getPostId() == null) {
      Log.e(TAG, "Cannot setup Comments RecyclerView: post or postId is null.");
      return;
    }
    commentAdapter = new CommentAdapter(this, post.getAuthorId(), new CommentInteractionListenerImpl());
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.recyclerView.setAdapter(commentAdapter);
  }

  private void setupCommentObservers() {
    commentViewModel.getVisibleComments().observe(this, comments -> {
      if (comments != null && commentAdapter != null) { // تحقق من أن commentAdapter ليس null
        commentAdapter.submitList(comments);
        // تحديث عدد التعليقات في شريط عنوان المنشور المضمن إذا لزم الأمر
        if (post != null) { // تحقق من أن post ليس null
          binding.commentstxt.setText(String.format(Locale.getDefault(), "%d Comments", comments.size()));
        }
      }
    });
  }

  private void loadCurrentUserProfile() {
    if (currentUser != null) {
      FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
              .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                  currentUserModel = documentSnapshot.toObject(UserModel.class);
                  // يمكنك تمكين زر إرسال التعليق هنا إذا كان معطلاً مبدئيًا
                  if (binding.inputSection.postButton != null && (binding.inputSection.postInput.getText().length() > 0)) {
                    binding.inputSection.postButton.setEnabled(true);
                  }
                } else {
                  Toast.makeText(this, "Could not load your user profile.", Toast.LENGTH_SHORT).show();
                }
              })
              .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load current user profile", e);
                Toast.makeText(this, "Failed to load your profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
              });
    } else {
      // المستخدم غير مسجل الدخول، قد ترغب في تعطيل إمكانية إضافة تعليق
      if(binding.inputSection.postButton != null) binding.inputSection.postButton.setEnabled(false);
      if(binding.inputSection.postInput != null) binding.inputSection.postInput.setHint("Log in to comment");
    }
  }

  private void setupInputSection() {
    binding.inputSection.charCounter.setText(String.valueOf(CommentModel.MAX_CONTENT_LENGTH)); // افترض وجود MAX_CONTENT_LENGTH في CommentModel
    binding.inputSection.postButton.setEnabled(false); // تعطيل مبدئي حتى يتم تحميل بيانات المستخدم أو إدخال نص

    if (currentUserModel == null && currentUser != null) { // إذا لم يتم تحميل بيانات المستخدم بعد ولكن المستخدم مسجل
      binding.inputSection.postInput.setHint("Loading user info...");
    } else if (currentUser == null) {
      binding.inputSection.postInput.setHint("Log in to comment");
      binding.inputSection.postInput.setEnabled(false);
    }


    binding.inputSection.postInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        // تمكين زر الإرسال فقط إذا كان هناك نص والمستخدم قد تم تحميله
        binding.inputSection.postButton.setEnabled(s.toString().trim().length() > 0 && currentUserModel != null);
        int remaining = CommentModel.MAX_CONTENT_LENGTH - s.length(); // افترض وجود MAX_CONTENT_LENGTH
        binding.inputSection.charCounter.setText(String.valueOf(remaining));
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    binding.inputSection.postButton.setOnClickListener(v -> postComment());

    // إخفاء الأزرار غير المستخدمة في سياق التعليقات إذا لزم الأمر
    binding.inputSection.addMedia.setVisibility(View.GONE); // أو أي زر آخر غير مناسب للتعليقات
    binding.inputSection.addGif.setVisibility(View.GONE);
    binding.inputSection.addPoll.setVisibility(View.GONE);
    // يمكنك إظهار زر لإرفاق صورة إذا كنت تدعم الصور في التعليقات
  }

  private void postComment() {
    String content = binding.inputSection.postInput.getText().toString().trim();
    if (content.isEmpty()) {
      Toast.makeText(this, "Comment cannot be empty.", Toast.LENGTH_SHORT).show();
      return;
    }
    if (currentUser == null || currentUserModel == null) {
      Toast.makeText(this, "You need to be logged in to comment.", Toast.LENGTH_SHORT).show();
      return;
    }
    if (post == null || post.getPostId() == null) {
      Toast.makeText(this, "Cannot comment on an invalid post.", Toast.LENGTH_SHORT).show();
      return;
    }

    Map<String, Object> commentData = new HashMap<>();
    commentData.put("content", content);
    commentData.put("authorId", currentUser.getUid());
    commentData.put("authorDisplayName", currentUserModel.getDisplayName());
    commentData.put("authorUsername", currentUserModel.getUsername());
    commentData.put("authorAvatarUrl", currentUserModel.getProfileImageUrl());
    commentData.put("authorVerified", currentUserModel.isVerified());
    commentData.put("timestamp", FieldValue.serverTimestamp()); // استخدام FieldValue.serverTimestamp()
    commentData.put("likeCount", 0);
    commentData.put("repliesCount", 0);
    commentData.put("parentPostId", post.getPostId()); // إضافة parentPostId
    // parentCommentId سيكون null للتعليقات الأساسية

    commentViewModel.addComment(post.getPostId(), commentData);
    binding.inputSection.postInput.setText("");
    binding.inputSection.postInput.clearFocus(); // إزالة التركيز من حقل الإدخال
    // إخفاء لوحة المفاتيح
    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (imm != null && getCurrentFocus() != null) {
      imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
  }

  private class CommentInteractionListenerImpl implements CommentAdapter.CommentInteractionListener {
    @Override
    public void onLikeClicked(CommentModel comment) {
      if (currentUser != null && comment != null && comment.getCommentId() != null && post != null && post.getPostId() != null) {
        commentViewModel.toggleLike(post.getPostId(), comment, currentUser.getUid());
      } else {
        Log.e(TAG, "Cannot toggle like: missing data. CurrentUser: " + (currentUser != null) +
                ", Comment: " + (comment != null ? comment.getCommentId() : "null") +
                ", Post: " + (post != null ? post.getPostId() : "null"));
        Toast.makeText(PostDetailActivity.this, "Error liking comment.", Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    public void onReplyClicked(CommentModel comment) {
      // منطق الرد على تعليق
      // يمكنك تعديل واجهة المستخدم لإظهار أنك ترد على تعليق معين
      // وتمرير parentCommentId عند إرسال الرد الجديد
      binding.inputSection.postInput.setHint("Replying to " + comment.getAuthorUsername());
      binding.inputSection.postInput.requestFocus();
      // يمكنك تخزين comment.getCommentId() لاستخدامه كـ parentCommentId للتعليق الجديد
      // (ستحتاج لإضافة حقل في PostDetailActivity لتخزين parentCommentId مؤقتًا)
      Toast.makeText(PostDetailActivity.this, "Replying to: " + comment.getAuthorUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthorClicked(String authorId) {
      if (authorId != null && !authorId.isEmpty()) {
        Intent intent = new Intent(PostDetailActivity.this, ProfileActivity.class);
        intent.putExtra("userId", authorId);
        startActivity(intent);
      }
    }

    @Override
    public void onShowRepliesClicked(CommentModel comment) {
      if (comment != null && comment.getCommentId() != null) {
        commentViewModel.toggleReplies(comment);
      }
    }

    @Override
    public void onDeleteComment(CommentModel comment) {
      if (currentUser != null && comment != null && comment.getAuthorId() != null && comment.getAuthorId().equals(currentUser.getUid())) {
        new android.app.AlertDialog.Builder(PostDetailActivity.this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                  if (post != null && post.getPostId() != null && comment.getCommentId() != null) {
                    commentViewModel.deleteComment(post.getPostId(), comment);
                  } else {
                    Log.e(TAG, "Cannot delete comment: missing post or comment ID.");
                    Toast.makeText(PostDetailActivity.this, "Error deleting comment.", Toast.LENGTH_SHORT).show();
                  }
                })
                .setNegativeButton("Cancel", null)
                .show();
      } else {
        Toast.makeText(PostDetailActivity.this, "You can only delete your own comments.", Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    public void onReportComment(CommentModel comment) {
      Toast.makeText(PostDetailActivity.this, "Reported comment: " + comment.getCommentId(), Toast.LENGTH_SHORT).show();
      // يمكنك هنا فتح شاشة الإبلاغ أو إرسال البلاغ مباشرة
    }
  }
}