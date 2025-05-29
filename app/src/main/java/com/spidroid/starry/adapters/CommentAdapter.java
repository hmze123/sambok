package com.spidroid.starry.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.CommentModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Date;
import java.util.List;
import java.util.Map;
import android.content.Context;
import androidx.cardview.widget.CardView;
import android.widget.Button;
import java.util.ArrayList;
import java.util.HashMap;
import com.google.firebase.auth.FirebaseAuth;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import androidx.appcompat.widget.PopupMenu;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
  private static final int INDENTATION_DP = 24;
  private final List<CommentModel> visibleComments = new ArrayList<>();
  private final Map<String, List<CommentModel>> replyMap = new HashMap<>();
  private final CommentInteractionListener listener;
  private final String currentUserId;
  private final String postAuthorId;
  private final String postId;
  private final int indentationMargin;
  private final Set<String> expandedCommentIds = new HashSet<>();

  public interface CommentInteractionListener {
    void onLikeClicked(CommentModel comment);

    void onReplyClicked(CommentModel comment);

    void onShowRepliesClicked(CommentModel comment);

    void onAuthorClicked(String userId);
  }

  public CommentAdapter(
      CommentInteractionListener listener, Context context, String postAuthorId, String postId) {
    this.listener = listener;
    this.postId = postId;
    this.currentUserId = FirebaseAuth.getInstance().getUid();
    this.postAuthorId = postAuthorId;
    this.indentationMargin =
        (int)
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                INDENTATION_DP,
                context.getResources().getDisplayMetrics());
  }

  public void updateComments(List<CommentModel> allComments) {
    visibleComments.clear();
    replyMap.clear();

    // Separate top-level comments and replies
    for (CommentModel comment : allComments) {
      if (comment.isTopLevel()) {
        visibleComments.add(comment);
      } else {
        String parentId = comment.getParentCommentId();
        if (!replyMap.containsKey(parentId)) {
          replyMap.put(parentId, new ArrayList<>());
        }
        replyMap.get(parentId).add(comment);
      }
    }

    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
    return new CommentViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
    CommentModel comment = visibleComments.get(position);
    boolean hasReplies = replyMap.containsKey(comment.getCommentId());

    holder.bind(comment, hasReplies);
    setupIndentation(holder.itemView, comment.getDepth());
  }

  @Override
  public int getItemCount() {
    return visibleComments.size();
  }

  private void setupIndentation(View view, int depth) {
    int margin = indentationMargin * depth;
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    params.setMargins(margin, params.topMargin, params.rightMargin, params.bottomMargin);
    view.setLayoutParams(params);
  }

  public void toggleReplies(CommentModel parentComment) {
    List<CommentModel> replies = replyMap.get(parentComment.getCommentId());
    if (replies == null) return;

    int parentPosition = visibleComments.indexOf(parentComment);
    if (parentPosition == -1) return;

    if (expandedCommentIds.contains(parentComment.getCommentId())) {
      // Collapse replies
      int removeCount = 0;
      while (parentPosition + 1 < visibleComments.size()
          && visibleComments.get(parentPosition + 1).isReply()) {
        visibleComments.remove(parentPosition + 1);
        removeCount++;
      }
      expandedCommentIds.remove(parentComment.getCommentId());
      notifyItemRangeRemoved(parentPosition + 1, removeCount);
    } else {
      // Expand replies
      visibleComments.addAll(parentPosition + 1, replies);
      expandedCommentIds.add(parentComment.getCommentId());
      notifyItemRangeInserted(parentPosition + 1, replies.size());
    }

    // Update the show/hide button text
    notifyItemChanged(parentPosition);
  }

  class CommentViewHolder extends RecyclerView.ViewHolder {
    private final CardView cardView;
    private final CircleImageView ivAvatar;
    private final TextView tvAuthor;
    private final ImageView ivVerified;
    private final TextView tvTimestamp;
    private final TextView tvContent;
    private final ImageButton btnLike;
    private final TextView tvLikeCount;
    private final ImageButton btnReply;
    private final Button btnShowReplies;
    private LinearLayout replyingContainer;
    private TextView tvReplyingTarget;

    CommentViewHolder(@NonNull View itemView) {
      super(itemView);
      cardView = itemView.findViewById(R.id.cardView);
      ivAvatar = itemView.findViewById(R.id.ivAvatar);
      tvAuthor = itemView.findViewById(R.id.tv_author);
      ivVerified = itemView.findViewById(R.id.ivVerified);
      tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
      tvContent = itemView.findViewById(R.id.tvCommentText);
      btnLike = itemView.findViewById(R.id.btnLike);
      tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
      btnReply = itemView.findViewById(R.id.btnReply);
      btnShowReplies = itemView.findViewById(R.id.btnShowReplies);
      replyingContainer = itemView.findViewById(R.id.replyingToContainer);
      tvReplyingTarget = itemView.findViewById(R.id.tvReplyingToTarget);
    }

    private String formatCount(int count) {
      if (count >= 1000000) return (count / 1000000) + "M";
      if (count >= 1000) return (count / 1000) + "K";
      return String.valueOf(count);
    }

    void bind(CommentModel comment, boolean hasReplies) {
      // Author Info
      tvAuthor.setText(comment.getAuthorDisplayName());
      tvTimestamp.setText(formatTimestamp(comment.getJavaDate()));
      Glide.with(itemView)
          .load(comment.getAuthorAvatarUrl())
          .placeholder(R.drawable.ic_default_avatar)
          .error(R.drawable.ic_default_avatar)
          .into(ivAvatar);

      // Verified Badge
      ivVerified.setVisibility(comment.isAuthorVerified() ? View.VISIBLE : View.GONE);

      if (comment.getParentAuthorId() != null) {
        replyingContainer.setVisibility(View.VISIBLE);

        if (comment.isReplyToAuthor(postAuthorId)) {
          tvReplyingTarget.setText(R.string.author);
        } else {
          tvReplyingTarget.setText("@" + comment.getParentAuthorUsername());
        }

      } else {
        replyingContainer.setVisibility(View.GONE);
      }

      // Content
      tvContent.setText(comment.getContent());

      // Likes
      tvLikeCount.setText(formatCount(comment.getLikeCount()));
      btnLike.setImageResource(
          comment.isLiked() ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
      btnLike.setColorFilter(
          ContextCompat.getColor(
              itemView.getContext(), comment.isLiked() ? Color.RED : R.color.text_secondary));

      // Replies
      boolean showReplyButton = !comment.isReply();
      btnReply.setVisibility(showReplyButton ? View.VISIBLE : View.GONE);

      // Show replies button
      if (hasReplies && comment.getRepliesCount() > 0) {
        btnShowReplies.setVisibility(View.VISIBLE);
        boolean isExpanded = expandedCommentIds.contains(comment.getCommentId());
        String buttonText =
            isExpanded
                ? itemView.getResources().getString(R.string.hide_replies)
                : itemView
                    .getResources()
                    .getString(R.string.show_replies_format, comment.getRepliesCount());
        btnShowReplies.setText(buttonText);
      } else {
        btnShowReplies.setVisibility(View.GONE);
      }

      // Click Listeners
      ivAvatar.setOnClickListener(v -> listener.onAuthorClicked(comment.getAuthorId()));
      btnLike.setOnClickListener(v -> listener.onLikeClicked(comment));
      btnReply.setOnClickListener(v -> listener.onReplyClicked(comment));
      btnShowReplies.setOnClickListener(v -> listener.onShowRepliesClicked(comment));
      cardView.setOnLongClickListener(
          v -> {
            showCommentMenu(comment);
            return true;
          });

      Date commentDate = comment.getJavaDate();
      tvTimestamp.setText(formatTimestamp(commentDate));
    }

    private String formatTimestamp(Date date) {
      if (date == null) {
        return "Just now";
      }
      return DateUtils.getRelativeTimeSpanString(
              date.getTime(),
              System.currentTimeMillis(),
              DateUtils.MINUTE_IN_MILLIS,
              DateUtils.FORMAT_ABBREV_RELATIVE)
          .toString();
    }

    private void showCommentMenu(CommentModel comment) {
      PopupMenu menu = new PopupMenu(itemView.getContext(), cardView);
      menu.inflate(R.menu.comment_menu);

      // Only show delete if current user is author
      menu.getMenu()
          .findItem(R.id.action_delete)
          .setVisible(comment.getAuthorId().equals(currentUserId));

      menu.setOnMenuItemClickListener(
          item -> {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
              deleteComment(comment);
              return true;
            } else if (id == R.id.action_report) {
              reportComment(comment);
              return true;
            }
            return false;
          });
      menu.show();
    }

    private void deleteComment(CommentModel comment) {
      FirebaseFirestore.getInstance()
          .collection("posts")
          .document(postId)
          .collection("comments")
          .document(comment.getCommentId())
          .delete()
          .addOnSuccessListener(
              unused -> {
                // Find position safely
                int position = visibleComments.indexOf(comment);
                if (position != -1) {
                  visibleComments.remove(position);
                  notifyItemRemoved(position);

                  // Optional: Update replies count for parent comment
                  if (comment.getParentCommentId() != null) {
                    updateParentRepliesCount(comment.getParentCommentId(), -1);
                  }
                }
              });
    }

    // To update parent replies count
    private void updateParentRepliesCount(String parentCommentId, int delta) {
      for (int i = 0; i < visibleComments.size(); i++) {
        CommentModel potentialParent = visibleComments.get(i);
        if (potentialParent.getCommentId().equals(parentCommentId)) {
          potentialParent.setRepliesCount(potentialParent.getRepliesCount() + delta);
          notifyItemChanged(i);
          break;
        }
      }
    }

    private void reportComment(CommentModel comment) {
      // Implement report logic
    }
  }
}
