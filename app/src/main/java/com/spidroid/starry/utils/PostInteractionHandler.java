package com.spidroid.starry.utils;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.models.PostModel;

public class PostInteractionHandler {
  private final ImageButton btnLike;
  private final ImageButton btnBookmark;
  private final ImageButton btnRepost;
  private final ImageButton btnComment;
  private final TextView tvLikeCount;
  private final TextView tvBookmarkCount;
  private final TextView tvRepostCount;
  private final TextView tvCommentCount;
  private final Context context;
  private PostModel currentPost;
  private final PostAdapter.PostInteractionListener listener;

  public PostInteractionHandler(
      @NonNull View rootView,
      @NonNull PostAdapter.PostInteractionListener listener,
      @NonNull Context context) {
    this.context = context;
    this.listener = listener;

    // Initialize views
    btnLike = rootView.findViewById(R.id.btnLike);
    btnBookmark = rootView.findViewById(R.id.btnBookmark);
    btnRepost = rootView.findViewById(R.id.btnRepost);
    btnComment = rootView.findViewById(R.id.btnComment);
    tvLikeCount = rootView.findViewById(R.id.tvLikeCount);
    tvBookmarkCount = rootView.findViewById(R.id.tvBookmarkCount);
    tvRepostCount = rootView.findViewById(R.id.tvRepostCount);
    tvCommentCount = rootView.findViewById(R.id.tvCommentCount);

    setupClickListeners();
  }

  public void bind(PostModel post) {
    currentPost = post;
    updateAllCounters();
    updateButtonStates();
  }

  public void handlePayload(Bundle payload) {
    for (String key : payload.keySet()) {
      switch (key) {
        case "liked":
          currentPost.setLiked(payload.getBoolean(key));
          updateLikeButton();
          break;
        case "likeCount":
          currentPost.setLikeCount(payload.getLong(key));
          tvLikeCount.setText(formatCount(currentPost.getLikeCount()));
          break;
        case "reposted":
          currentPost.setReposted(payload.getBoolean(key));
          updateRepostButton();
          break;
        case "repostCount":
          currentPost.setRepostCount(payload.getLong(key));
          tvRepostCount.setText(formatCount(currentPost.getRepostCount()));
          break;
        case "bookmarked":
          currentPost.setBookmarked(payload.getBoolean(key));
          updateBookmarkButton();
          break;
        case "bookmarkCount":
          currentPost.setBookmarkCount(payload.getLong(key));
          tvBookmarkCount.setText(formatCount(currentPost.getBookmarkCount()));
          break;
        case "replyCount":
          currentPost.setReplyCount(payload.getLong(key));
          tvCommentCount.setText(formatCount(currentPost.getReplyCount()));
          break;
      }
    }
  }

  private void setupClickListeners() {
    btnLike.setOnClickListener(v -> toggleLike());
    btnBookmark.setOnClickListener(v -> toggleBookmark());
    btnRepost.setOnClickListener(v -> toggleRepost());
    btnComment.setOnClickListener(v -> listener.onCommentClicked(currentPost));
  }

  private void toggleLike() {
    currentPost.toggleLike();
    updateLikeButton();
    listener.onLikeClicked(currentPost);
  }

  private void toggleBookmark() {
    currentPost.toggleBookmark();
    updateBookmarkButton();
    listener.onBookmarkClicked(currentPost);
  }

  private void toggleRepost() {
    currentPost.toggleRepost();
    updateRepostButton();
    listener.onRepostClicked(currentPost);
  }

  private void updateAllCounters() {
    tvLikeCount.setText(formatCount(currentPost.getLikeCount()));
    tvBookmarkCount.setText(formatCount(currentPost.getBookmarkCount()));
    tvRepostCount.setText(formatCount(currentPost.getRepostCount()));
    tvCommentCount.setText(formatCount(currentPost.getReplyCount()));
  }

  private void updateButtonStates() {
    updateButtonState(btnLike, currentPost.isLiked(), R.drawable.ic_like_filled);
    updateButtonState(btnBookmark, currentPost.isBookmarked(), R.drawable.ic_bookmark_filled);
    updateButtonState(btnRepost, currentPost.isReposted(), R.drawable.ic_repost_filled);
  }

  private void updateLikeButton() {
    updateButtonState(btnLike, currentPost.isLiked(), R.drawable.ic_like_filled);
    tvLikeCount.setText(formatCount(currentPost.getLikeCount()));
  }

  private void updateBookmarkButton() {
    updateButtonState(btnBookmark, currentPost.isBookmarked(), R.drawable.ic_bookmark_filled);
    tvBookmarkCount.setText(formatCount(currentPost.getBookmarkCount()));
  }

  private void updateRepostButton() {
    updateButtonState(btnRepost, currentPost.isReposted(), R.drawable.ic_repost_filled);
    tvRepostCount.setText(formatCount(currentPost.getRepostCount()));
  }

  private void updateButtonState(ImageButton button, boolean isActive, int filledRes) {
    button.setImageResource(isActive ? filledRes : getOutlineRes(filledRes));
    int colorRes = isActive ? getActiveColor(filledRes) : R.color.text_secondary;
    button.setColorFilter(ContextCompat.getColor(context, colorRes));
  }

  private int getActiveColor(int filledRes) {
    if (filledRes == R.drawable.ic_like_filled) return R.color.red;
    if (filledRes == R.drawable.ic_repost_filled) return R.color.green;
    if (filledRes == R.drawable.ic_bookmark_filled) return R.color.yellow;
    return R.color.primary;
  }

  private int getOutlineRes(int filledRes) {
    if (filledRes == R.drawable.ic_like_filled) return R.drawable.ic_like_outline;
    if (filledRes == R.drawable.ic_repost_filled) return R.drawable.ic_repost_outline;
    if (filledRes == R.drawable.ic_bookmark_filled) return R.drawable.ic_bookmark_outline;
    return filledRes;
  }

  private String formatCount(long count) {
    if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000f);
    if (count >= 1_000) return String.format("%.1fK", count / 1_000f);
    return String.valueOf(count);
  }
}
