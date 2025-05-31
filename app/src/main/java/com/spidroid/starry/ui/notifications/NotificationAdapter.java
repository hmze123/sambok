package com.spidroid.starry.ui.notifications; // أو الحزمة الصحيحة إذا كانت مختلفة

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.NotificationModel;

import java.util.Date;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationAdapter extends ListAdapter<NotificationModel, NotificationAdapter.NotificationViewHolder> {

    private final OnNotificationClickListener listener;
    private final Context context;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationModel notification);
    }

    public NotificationAdapter(Context context, OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = getItem(position);
        if (notification != null) {
            holder.bind(notification, listener);
        }
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvNotificationText;
        private final TextView tvTimestamp;
        private final ImageView ivUnreadIndicator;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_notification_avatar);
            tvNotificationText = itemView.findViewById(R.id.tv_notification_text);
            tvTimestamp = itemView.findViewById(R.id.tv_notification_timestamp);
            ivUnreadIndicator = itemView.findViewById(R.id.iv_notification_unread_indicator);
        }

        void bind(final NotificationModel notification, final OnNotificationClickListener listener) {
            // تحميل صورة المستخدم الذي قام بالإجراء
            if (notification.getFromUserAvatarUrl() != null && !notification.getFromUserAvatarUrl().isEmpty()) {
                Glide.with(context)
                        .load(notification.getFromUserAvatarUrl())
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_default_avatar);
            }

            // بناء نص الإشعار
            SpannableStringBuilder notificationTextBuilder = new SpannableStringBuilder();
            String fromUsername = notification.getFromUsername() != null ? notification.getFromUsername() : "Someone";

            // اسم المستخدم بخط عريض
            notificationTextBuilder.append(fromUsername);
            notificationTextBuilder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, fromUsername.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // نص الإشعار بناءً على النوع
            if (NotificationModel.TYPE_LIKE.equals(notification.getType())) {
                notificationTextBuilder.append(" liked your post.");
                // يمكنك إضافة: " " + notification.getPostContentPreview() إذا كان متاحًا
            } else if (NotificationModel.TYPE_COMMENT.equals(notification.getType())) {
                notificationTextBuilder.append(" commented on your post: ");
                // يمكنك إضافة: notification.getCommentContentPreview() إذا كان متاحًا
            } else if (NotificationModel.TYPE_FOLLOW.equals(notification.getType())) {
                notificationTextBuilder.append(" started following you.");
            } else {
                notificationTextBuilder.append(" sent you a notification.");
            }
            tvNotificationText.setText(notificationTextBuilder);

            // عرض الوقت النسبي
            if (notification.getTimestamp() != null) {
                tvTimestamp.setText(
                        DateUtils.getRelativeTimeSpanString(
                                notification.getTimestamp().getTime(),
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE
                        )
                );
            } else {
                tvTimestamp.setText("Just now");
            }

            // مؤشر "غير مقروء"
            ivUnreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // مستمع النقر على عنصر الإشعار
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<NotificationModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NotificationModel>() {
                @Override
                public boolean areItemsTheSame(@NonNull NotificationModel oldItem, @NonNull NotificationModel newItem) {
                    // إذا كان لديك ID فريد لكل إشعار، استخدمه هنا
                    // حاليًا، نفترض أننا سنعتمد على محتوى الإشعار والوقت
                    return Objects.equals(oldItem.getNotificationId(), newItem.getNotificationId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NotificationModel oldItem, @NonNull NotificationModel newItem) {
                    return Objects.equals(oldItem.getType(), newItem.getType()) &&
                            Objects.equals(oldItem.getFromUserId(), newItem.getFromUserId()) &&
                            Objects.equals(oldItem.getPostId(), newItem.getPostId()) &&
                            Objects.equals(oldItem.getCommentId(), newItem.getCommentId()) &&
                            oldItem.isRead() == newItem.isRead() &&
                            Objects.equals(oldItem.getTimestamp(), newItem.getTimestamp());
                }
            };
}