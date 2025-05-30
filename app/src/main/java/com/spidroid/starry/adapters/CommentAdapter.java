package com.spidroid.starry.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ItemCommentBinding;
import com.spidroid.starry.models.CommentModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CommentAdapter extends ListAdapter<CommentModel, CommentAdapter.CommentViewHolder> {

  private final CommentInteractionListener listener;
  private final String currentUserId;
  private final String postAuthorId;
  private final Context context;

  public interface CommentInteractionListener {
    void onLikeClicked(CommentModel comment);
    void onReplyClicked(CommentModel comment);
    void onAuthorClicked(String userId);
    void onShowRepliesClicked(CommentModel comment);
    void onDeleteComment(CommentModel comment);
    void onReportComment(CommentModel comment);
  }

  public CommentAdapter(@NonNull Context context, String postAuthorId, CommentInteractionListener listener) {
    super(DIFF_CALLBACK);
    this.context = context;
    this.postAuthorId = postAuthorId;
    this.listener = listener;
    this.currentUserId = FirebaseAuth.getInstance().getUid();
  }

  @NonNull
  @Override
  public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemCommentBinding binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new CommentViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
    CommentModel comment = getItem(position);
    if (comment != null) {
      holder.bind(comment);
    }
  }

  class CommentViewHolder extends RecyclerView.ViewHolder {
    private final ItemCommentBinding binding;
    private final int indentationMargin;

    CommentViewHolder(@NonNull ItemCommentBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
      this.indentationMargin = (int) TypedValue.applyDimension(
              TypedValue.COMPLEX_UNIT_DIP, 24, context.getResources().getDisplayMetrics());
    }

    void bind(final CommentModel comment) {
      // تطبيق المسافة البادئة للردود
      ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) binding.getRoot().getLayoutParams();
      params.setMargins(indentationMargin * comment.getDepth(), params.topMargin, params.rightMargin, params.bottomMargin);
      binding.getRoot().setLayoutParams(params);

      // ربط البيانات
      binding.tvAuthor.setText(comment.getAuthorDisplayName());
      binding.tvUsername.setText("@" + comment.getAuthorUsername());
      binding.tvCommentText.setText(comment.getContent());
      binding.tvTimestamp.setText(formatTimestamp(comment.getJavaDate()));

      Glide.with(context)
              .load(comment.getAuthorAvatarUrl())
              .placeholder(R.drawable.ic_default_avatar)
              .into(binding.ivAvatar);

      binding.ivVerified.setVisibility(comment.isAuthorVerified() ? View.VISIBLE : View.GONE);

      // منطق الإعجاب
      binding.tvLikeCount.setText(String.valueOf(comment.getLikeCount()));
      binding.btnLike.setImageResource(comment.isLiked() ? R.drawable.ic_like_filled : R.drawable.ic_like_outline);
      binding.btnLike.setColorFilter(ContextCompat.getColor(context, comment.isLiked() ? R.color.red : R.color.text_secondary));

      // منطق إظهار/إخفاء الردود
      if (comment.getRepliesCount() > 0) {
        binding.btnShowReplies.setVisibility(View.VISIBLE);
        // الحالة ستتم إدارتها في الـ ViewModel
        // يمكنك إضافة خاصية isExpanded إلى CommentModel إذا أردت
        String buttonText = context.getString(R.string.show_replies_format, comment.getRepliesCount());
        binding.btnShowReplies.setText(buttonText);
      } else {
        binding.btnShowReplies.setVisibility(View.GONE);
      }

      // إعداد مستمعي النقرات
      binding.btnLike.setOnClickListener(v -> listener.onLikeClicked(comment));
      binding.btnReply.setOnClickListener(v -> listener.onReplyClicked(comment));
      binding.btnShowReplies.setOnClickListener(v -> listener.onShowRepliesClicked(comment));
      binding.ivAvatar.setOnClickListener(v -> listener.onAuthorClicked(comment.getAuthorId()));
      binding.tvAuthor.setOnClickListener(v -> listener.onAuthorClicked(comment.getAuthorId()));

      // قائمة الخيارات عند الضغط المطول
      itemView.setOnLongClickListener(v -> {
        showCommentMenu(comment, v);
        return true;
      });
    }

    private void showCommentMenu(CommentModel comment, View anchor) {
      PopupMenu popup = new PopupMenu(context, anchor);
      popup.getMenuInflater().inflate(R.menu.comment_menu, popup.getMenu());

      // إظهار خيار الحذف فقط إذا كان المستخدم هو صاحب التعليق
      if (currentUserId != null && currentUserId.equals(comment.getAuthorId())) {
        popup.getMenu().findItem(R.id.action_delete).setVisible(true);
      } else {
        popup.getMenu().findItem(R.id.action_delete).setVisible(false);
      }

      popup.setOnMenuItemClickListener(item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete) {
          listener.onDeleteComment(comment);
          return true;
        } else if (itemId == R.id.action_report) {
          listener.onReportComment(comment);
          return true;
        }
        return false;
      });
      popup.show();
    }

    private String formatTimestamp(Date date) {
      if (date == null) return "Just now";
      return DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    }
  }

  private static final DiffUtil.ItemCallback<CommentModel> DIFF_CALLBACK = new DiffUtil.ItemCallback<CommentModel>() {
    @Override
    public boolean areItemsTheSame(@NonNull CommentModel oldItem, @NonNull CommentModel newItem) {
      return Objects.equals(oldItem.getCommentId(), newItem.getCommentId());
    }

    @Override
    public boolean areContentsTheSame(@NonNull CommentModel oldItem, @NonNull CommentModel newItem) {
      return oldItem.getContent().equals(newItem.getContent()) &&
              oldItem.getLikeCount() == newItem.getLikeCount() &&
              oldItem.isLiked() == newItem.isLiked() &&
              oldItem.getRepliesCount() == newItem.getRepliesCount();
    }
  };
}