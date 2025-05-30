package com.spidroid.starry.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

  public static final String EXTRA_POST = "post";

  private ActivityPostDetailBinding binding; // استخدام ViewBinding
  private CommentViewModel commentViewModel;
  private CommentAdapter commentAdapter;
  private PostModel post;
  private FirebaseUser currentUser;
  private UserModel currentUserModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // تهيئة ViewBinding
    binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    post = getIntent().getParcelableExtra(EXTRA_POST);
    if (post == null) {
      finish();
      return;
    }

    currentUser = FirebaseAuth.getInstance().getCurrentUser();
    commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);

    setupToolbar();
    bindPostData();
    setupCommentsRecyclerView();
    setupCommentObservers();
    setupInputSection();

    // جلب بيانات المستخدم الحالي والتعليقات
    loadCurrentUserProfile();
    commentViewModel.loadComments(post.getPostId());
  }

  private void setupToolbar() {
    binding.ivBack.setOnClickListener(v -> finish());
  }

  private void bindPostData() {
    // ... (منطق عرض بيانات المنشور الأصلي، يمكن نسخه من الكود السابق)
    // مثال:
    binding.includedPostLayout.tvAuthorName.setText(post.getAuthorDisplayName());
    Glide.with(this).load(post.getAuthorAvatarUrl()).into(binding.includedPostLayout.ivAuthorAvatar);
    // ... الخ
  }

  private void setupCommentsRecyclerView() {
    commentAdapter = new CommentAdapter(this, post.getAuthorId(), new CommentInteractionListenerImpl());
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
    binding.recyclerView.setAdapter(commentAdapter);
  }

  private void setupCommentObservers() {
    commentViewModel.getVisibleComments().observe(this, comments -> {
      if (comments != null) {
        commentAdapter.submitList(comments);
        binding.includedPostLayout.tvCommentCount.setText(String.valueOf(comments.size()));
      }
    });
  }

  private void loadCurrentUserProfile() {
    if (currentUser != null) {
      // ... (منطق جلب بيانات المستخدم الحالي وتخزينها في currentUserModel)
    }
  }

  private void setupInputSection() {
    binding.inputSection.charCounter.setText(String.valueOf(500));
    binding.inputSection.postButton.setEnabled(false);

    binding.inputSection.postInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        binding.inputSection.postButton.setEnabled(s.toString().trim().length() > 0);
      }
      @Override
      public void afterTextChanged(Editable s) {}
    });

    binding.inputSection.postButton.setOnClickListener(v -> postComment());
  }

  private void postComment() {
    String content = binding.inputSection.postInput.getText().toString().trim();
    if (content.isEmpty() || currentUser == null || currentUserModel == null) {
      return;
    }

    Map<String, Object> commentData = new HashMap<>();
    commentData.put("content", content);
    commentData.put("authorId", currentUser.getUid());
    commentData.put("authorDisplayName", currentUserModel.getDisplayName());
    commentData.put("authorUsername", currentUserModel.getUsername());
    commentData.put("authorAvatarUrl", currentUserModel.getProfileImageUrl());
    commentData.put("authorVerified", currentUserModel.isVerified());
    commentData.put("timestamp", new Date());
    commentData.put("likeCount", 0);
    commentData.put("repliesCount", 0);

    commentViewModel.addComment(post.getPostId(), commentData);
    binding.inputSection.postInput.setText(""); // تفريغ حقل الإدخال
  }

  // تطبيق واجهة التفاعل مع التعليقات
  private class CommentInteractionListenerImpl implements CommentAdapter.CommentInteractionListener {
    @Override
    public void onLikeClicked(CommentModel comment) {
      if (currentUser != null) {
        commentViewModel.toggleLike(post.getPostId(), comment, currentUser.getUid());
      }
    }

    @Override
    public void onReplyClicked(CommentModel comment) {
      // منطق الرد على تعليق
      Toast.makeText(PostDetailActivity.this, "Reply to: " + comment.getAuthorUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthorClicked(String userId) {
      Intent intent = new Intent(PostDetailActivity.this, ProfileActivity.class);
      intent.putExtra("userId", userId);
      startActivity(intent);
    }

    @Override
    public void onShowRepliesClicked(CommentModel comment) {
      commentViewModel.toggleReplies(comment);
    }

    @Override
    public void onDeleteComment(CommentModel comment) {
      commentViewModel.deleteComment(post.getPostId(), comment);
    }

    @Override
    public void onReportComment(CommentModel comment) {
      Toast.makeText(PostDetailActivity.this, "Reported comment: " + comment.getCommentId(), Toast.LENGTH_SHORT).show();
    }
  }
}