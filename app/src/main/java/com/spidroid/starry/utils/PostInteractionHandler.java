package com.spidroid.starry.utils;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.PostInteractionListener;
import com.spidroid.starry.models.PostModel;
import java.util.Locale;
import java.util.Objects;

public class PostInteractionHandler {
  private final ImageButton btnLike;
  private final ImageButton btnBookmark;
  private final ImageButton btnRepost;
  private final ImageButton btnComment;
  private final TextView tvLikeCount;
  private final TextView tvBookmarkCount;
  private final TextView tvRepostCount;
  private final TextView tvCommentCount;
  private final ImageView ivLikeReaction;
  private final Context context;
  private PostModel currentPost;
  private final PostInteractionListener listener;
  private String currentUserId;
  private final ImageButton btnMenu; // ★ إضافة المتغير لزر القائمة

  public PostInteractionHandler(
          @NonNull View rootView,
          @NonNull PostInteractionListener listener,
          @NonNull Context context,
          @NonNull String currentUserId) {
    this.context = context;
    this.listener = listener;
    this.currentUserId = currentUserId;

    btnLike = rootView.findViewById(R.id.btnLike);
    btnBookmark = rootView.findViewById(R.id.btnBookmark);
    btnRepost = rootView.findViewById(R.id.btnRepost);
    btnComment = rootView.findViewById(R.id.btnComment);
    tvLikeCount = rootView.findViewById(R.id.tvLikeCount);
    tvBookmarkCount = rootView.findViewById(R.id.tvBookmarkCount);
    tvRepostCount = rootView.findViewById(R.id.tvRepostCount);
    tvCommentCount = rootView.findViewById(R.id.tvCommentCount);
    ivLikeReaction = rootView.findViewById(R.id.ivLikeReaction);
    btnMenu = rootView.findViewById(R.id.btnMenu); // ★ تهيئة زر القائمة

    setupClickListeners();
  }

  public void bind(PostModel post) {
    currentPost = post;
    updateAllCounters();
    updateButtonStates();
    updateLikeReactionIcon();
  }

  public void handlePayload(Bundle payload) {
    if (currentPost == null) return;

    for (String key : payload.keySet()) {
      switch (key) {
        case "liked":
          currentPost.setLiked(payload.getBoolean(key));
          updateLikeButton();
          updateLikeReactionIcon();
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
    btnLike.setOnClickListener(v -> {
      if (currentPost != null && listener != null) toggleLike();
    });
    // ★ إضافة مستمع الضغط المطول لزر الإعجاب (إذا كنت قد أضفت الدالة للواجهة)
    btnLike.setOnLongClickListener(v -> {
      if (currentPost != null && listener != null) {
        listener.onLikeButtonLongClicked(currentPost, btnLike);
        return true;
      }
      return false;
    });
    btnBookmark.setOnClickListener(v -> {
      if (currentPost != null && listener != null) toggleBookmark();
    });
    btnRepost.setOnClickListener(v -> {
      if (currentPost != null && listener != null) toggleRepost();
    });
    btnComment.setOnClickListener(v -> {
      if (listener != null && currentPost != null) listener.onCommentClicked(currentPost);
    });
    // ★★★ إضافة مستمع النقر لزر القائمة ★★★
    if (btnMenu != null) {
      btnMenu.setOnClickListener(v -> {
        if (listener != null && currentPost != null) {
          listener.onMenuClicked(currentPost, btnMenu);
        }
      });
    }
  }

  private void toggleLike() {
    currentPost.toggleLike();
    updateLikeButton();
    if (listener != null) {
      listener.onLikeClicked(currentPost);
    }
    updateLikeReactionIcon();
  }

  private void toggleBookmark() {
    currentPost.toggleBookmark();
    updateBookmarkButton();
    if (listener != null) listener.onBookmarkClicked(currentPost);
  }

  private void toggleRepost() {
    currentPost.toggleRepost();
    updateRepostButton();
    if (listener != null) listener.onRepostClicked(currentPost);
  }

  private void updateAllCounters() {
    if (currentPost == null) return;
    tvLikeCount.setText(formatCount(currentPost.getLikeCount()));
    tvBookmarkCount.setText(formatCount(currentPost.getBookmarkCount()));
    tvRepostCount.setText(formatCount(currentPost.getRepostCount()));
    tvCommentCount.setText(formatCount(currentPost.getReplyCount()));
  }

  private void updateButtonStates() {
    if (currentPost == null) return;
    updateLikeButton();
    updateButtonState(btnBookmark, currentPost.isBookmarked(), R.drawable.ic_bookmark_filled, R.color.yellow, R.drawable.ic_bookmark_outline);
    updateButtonState(btnRepost, currentPost.isReposted(), R.drawable.ic_repost_filled, R.color.green, R.drawable.ic_repost_outline);
  }

  private void updateLikeButton() {
    if (currentPost == null) return;
    updateButtonState(btnLike, currentPost.isLiked(), R.drawable.ic_like_filled, R.color.red, R.drawable.ic_like_outline);
    tvLikeCount.setText(formatCount(currentPost.getLikeCount()));
  }

  private void updateBookmarkButton() {
    if (currentPost == null) return;
    updateButtonState(btnBookmark, currentPost.isBookmarked(), R.drawable.ic_bookmark_filled, R.color.yellow, R.drawable.ic_bookmark_outline);
    tvBookmarkCount.setText(formatCount(currentPost.getBookmarkCount()));
  }

  private void updateRepostButton() {
    if (currentPost == null) return;
    updateButtonState(btnRepost, currentPost.isReposted(), R.drawable.ic_repost_filled, R.color.green, R.drawable.ic_repost_outline);
    tvRepostCount.setText(formatCount(currentPost.getRepostCount()));
  }

  private void updateButtonState(ImageButton button, boolean isActive, int filledRes, int activeColorRes, int outlineRes) {
    if (context == null || button == null) return;
    button.setImageResource(isActive ? filledRes : outlineRes);
    int colorRes = isActive ? activeColorRes : R.color.text_secondary;
    button.setColorFilter(ContextCompat.getColor(context, colorRes));
  }

  private void updateLikeReactionIcon() {
    if (currentPost == null || ivLikeReaction == null || context == null || currentUserId == null) return;

    String userReaction = currentPost.getUserReaction(currentUserId);

    if (currentPost.isLiked() && "❤️".equals(userReaction)) {
      ivLikeReaction.setImageResource(R.drawable.ic_like_filled_red);
      ivLikeReaction.setColorFilter(ContextCompat.getColor(context, R.color.red));
      ivLikeReaction.setVisibility(View.VISIBLE);
    } else if (currentPost.isLiked()) {
      ivLikeReaction.setVisibility(View.GONE);
    }
    else {
      ivLikeReaction.setVisibility(View.GONE);
    }
  }

  private String formatCount(long count) {
    if (count >= 1_000_000_000) return String.format(Locale.getDefault(), "%.1fB", count / 1_000_000_000.0);
    if (count >= 1_000_000) return String.format(Locale.getDefault(), "%.1fM", count / 1_000_000.0);
    if (count >= 1_000) return String.format(Locale.getDefault(), "%.0fK", count / 1_000.0);
    return String.valueOf(count);
  }

  public static int getDrawableIdForEmoji(String reactionEmoji, boolean isSmallIcon) {
    if (reactionEmoji == null) {
      return R.drawable.ic_emoji;
    }
    switch (reactionEmoji) {
      case "❤️":
        return R.drawable.ic_like_filled_red;
      case "😂":
        return R.drawable.ic_emoji_laugh_small; // تأكد أن هذا الـ drawable موجود ومُعرَّف
      case "😮":
        return R.drawable.ic_emoji; // مؤقتًا
      case "😢":
        return R.drawable.ic_emoji; // مؤقتًا
      case "👍":
        return R.drawable.ic_like_filled;
      case "👎":
        return R.drawable.ic_emoji; // مؤقتًا
      default:
        return R.drawable.ic_emoji;
    }
  }
}