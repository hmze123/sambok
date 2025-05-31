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
import com.google.firebase.firestore.FieldValue; // ★ استيراد FieldValue
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
      // استخدم string resource بدلاً من النص المباشر
      Toast.makeText(this, getString(R.string.error_post_not_found), Toast.LENGTH_LONG).show();
      finish(); // إنهاء النشاط إذا لم يتم العثور على بيانات المنشور
      return;
    }

    // تحقق من أن postId ليس null قبل المتابعة
    if (post.getPostId() == null || post.getPostId().isEmpty()) {
      Log.e(TAG, "Post ID is null or empty in received PostModel. Author: " +
              (post.getAuthorUsername() != null ? post.getAuthorUsername() : "N/A") +
              ", Content: " + (post.getContent() != null ? post.getContent() : "N/A"));
      // استخدم string resource
      Toast.makeText(this, getString(R.string.error_invalid_post_id), Toast.LENGTH_LONG).show();
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
    commentViewModel.loadComments(post.getPostId());
  }

  private void setupToolbar() {
    binding.ivBack.setOnClickListener(v -> finish());
    // يمكنك إضافة المزيد من إعدادات Toolbar هنا إذا لزم الأمر
    // مثلاً، عنوان الـ Toolbar يمكن أن يكون اسم صاحب المنشور أو "Post"
    binding.tvAppName.setText(post != null && post.getAuthorDisplayName() != null ? post.getAuthorDisplayName() + "'s Post" : "Post");
  }

  private void bindPostData() {
    if (post == null) {
      Log.e(TAG, "PostModel is null in bindPostData. This should not happen.");
      return;
    }

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
    binding.includedPostLayout.interactionContainer.setVisibility(View.GONE);
  }

  private void setupCommentsRecyclerView() {
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
      if (comments != null && commentAdapter != null) {
        commentAdapter.submitList(comments);
        if (post != null) {
          // استخدم string resource مع placeholder
          binding.commentstxt.setText(getString(R.string.comments_count, comments.size()));
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
                  if (binding.inputSection.postButton != null && (binding.inputSection.postInput.getText().length() > 0)) {
                    binding.inputSection.postButton.setEnabled(true);
                  }
                } else {
                  Toast.makeText(this, getString(R.string.error_loading_profile), Toast.LENGTH_SHORT).show();
                }
              })
              .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load current user profile", e);
                Toast.makeText(this, getString(R.string.error_loading_profile_with_message, e.getMessage()), Toast.LENGTH_SHORT).show();
              });
    } else {
      if(binding.inputSection.postButton != null) binding.inputSection.postButton.setEnabled(false);
      if(binding.inputSection.postInput != null) binding.inputSection.postInput.setHint(getString(R.string.login_to_comment));
    }
  }

  private void setupInputSection() {
    binding.inputSection.charCounter.setText(String.valueOf(CommentModel.MAX_CONTENT_LENGTH));
    binding.inputSection.postButton.setEnabled(false);

    if (currentUserModel == null && currentUser != null) {
      binding.inputSection.postInput.setHint(getString(R.string.loading_user_info));
    } else if (currentUser == null) {
      binding.inputSection.postInput.setHint(getString(R.string.login_to_comment));
      binding.inputSection.postInput.setEnabled(false);
    }


    binding.inputSection.postInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        binding.inputSection.postButton.setEnabled(s.toString().trim().length() > 0 && currentUserModel != null);
        int remaining = CommentModel.MAX_CONTENT_LENGTH - s.length();
        binding.inputSection.charCounter.setText(String.valueOf(remaining));
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    binding.inputSection.postButton.setOnClickListener(v -> postComment());
    binding.inputSection.addMedia.setVisibility(View.GONE);
    binding.inputSection.addGif.setVisibility(View.GONE);
    binding.inputSection.addPoll.setVisibility(View.GONE);
  }

  private void postComment() {
    String content = binding.inputSection.postInput.getText().toString().trim();
    if (content.isEmpty()) {
      Toast.makeText(this, getString(R.string.comment_empty_error), Toast.LENGTH_SHORT).show();
      return;
    }
    if (currentUser == null || currentUserModel == null) {
      Toast.makeText(this, getString(R.string.login_to_comment_action), Toast.LENGTH_SHORT).show();
      return;
    }
    if (post == null || post.getPostId() == null) {
      Toast.makeText(this, getString(R.string.error_invalid_post_for_comment), Toast.LENGTH_SHORT).show();
      return;
    }

    Map<String, Object> commentData = new HashMap<>();
    commentData.put("content", content);
    commentData.put("authorId", currentUser.getUid());
    commentData.put("authorDisplayName", currentUserModel.getDisplayName());
    commentData.put("authorUsername", currentUserModel.getUsername());
    commentData.put("authorAvatarUrl", currentUserModel.getProfileImageUrl());
    commentData.put("authorVerified", currentUserModel.isVerified());
    commentData.put("timestamp", FieldValue.serverTimestamp());
    commentData.put("likeCount", 0);
    commentData.put("repliesCount", 0);
    commentData.put("parentPostId", post.getPostId());

    commentViewModel.addComment(post.getPostId(), commentData);
    binding.inputSection.postInput.setText("");
    binding.inputSection.postInput.clearFocus();
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
        Toast.makeText(PostDetailActivity.this, getString(R.string.error_liking_comment), Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    public void onReplyClicked(CommentModel comment) {
      binding.inputSection.postInput.setHint(getString(R.string.replying_to_user, comment.getAuthorUsername()));
      binding.inputSection.postInput.requestFocus();
      Toast.makeText(PostDetailActivity.this, getString(R.string.replying_to_user, comment.getAuthorUsername()), Toast.LENGTH_SHORT).show();
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
                .setTitle(getString(R.string.delete_comment_title))
                .setMessage(getString(R.string.delete_comment_confirmation))
                .setPositiveButton(getString(R.string.delete_button), (dialog, which) -> {
                  if (post != null && post.getPostId() != null && comment.getCommentId() != null) {
                    commentViewModel.deleteComment(post.getPostId(), comment);
                  } else {
                    Log.e(TAG, "Cannot delete comment: missing post or comment ID.");
                    Toast.makeText(PostDetailActivity.this, getString(R.string.error_deleting_comment), Toast.LENGTH_SHORT).show();
                  }
                })
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show();
      } else {
        Toast.makeText(PostDetailActivity.this, getString(R.string.error_delete_own_comment_only), Toast.LENGTH_SHORT).show();
      }
    }

    @Override
    public void onReportComment(CommentModel comment) {
      Toast.makeText(PostDetailActivity.this, getString(R.string.comment_reported_toast, comment.getCommentId()), Toast.LENGTH_SHORT).show();
      // يمكنك هنا فتح شاشة الإبلاغ أو إرسال البلاغ مباشرة
      Intent intent = new Intent(PostDetailActivity.this, ReportActivity.class);
      intent.putExtra("commentId", comment.getCommentId());
      intent.putExtra("postId", post.getPostId());
      startActivity(intent);
    }
  }
}