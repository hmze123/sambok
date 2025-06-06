package com.spidroid.starry.ui.notifications

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.models.NotificationModel
import com.spidroid.starry.ui.notifications.NotificationAdapter.NotificationViewHolder
import de.hdodenhof.circleimageview.CircleImageView

// أو الحزمة الصحيحة إذا كانت مختلفة

class NotificationAdapter(
    private val context: Context,
    private val listener: OnNotificationClickListener?
) : ListAdapter<NotificationModel?, NotificationViewHolder?>(NotificationAdapter.Companion.DIFF_CALLBACK) {
    interface OnNotificationClickListener {
        fun onNotificationClick(notification: NotificationModel?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        if (notification != null) {
            holder.bind(notification, listener)
        }
    }

    internal inner class NotificationViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: CircleImageView
        private val tvNotificationText: TextView
        private val tvTimestamp: TextView
        private val ivUnreadIndicator: ImageView

        init {
            ivAvatar = itemView.findViewById<CircleImageView>(R.id.iv_notification_avatar)
            tvNotificationText = itemView.findViewById<TextView>(R.id.tv_notification_text)
            tvTimestamp = itemView.findViewById<TextView>(R.id.tv_notification_timestamp)
            ivUnreadIndicator =
                itemView.findViewById<ImageView>(R.id.iv_notification_unread_indicator)
        }

        fun bind(notification: NotificationModel, listener: OnNotificationClickListener?) {
            // تحميل صورة المستخدم الذي قام بالإجراء
            if (notification.getFromUserAvatarUrl() != null && !notification.getFromUserAvatarUrl()
                    .isEmpty()
            ) {
                Glide.with(context)
                    .load(notification.getFromUserAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            // بناء نص الإشعار
            val notificationTextBuilder = SpannableStringBuilder()
            val fromUsername =
                if (notification.getFromUsername() != null) notification.getFromUsername() else "Someone"

            // اسم المستخدم بخط عريض
            notificationTextBuilder.append(fromUsername)
            notificationTextBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                fromUsername.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // نص الإشعار بناءً على النوع
            if (NotificationModel.Companion.TYPE_LIKE == notification.getType()) {
                notificationTextBuilder.append(" liked your post.")
                // يمكنك إضافة: " " + notification.getPostContentPreview() إذا كان متاحًا
            } else if (NotificationModel.Companion.TYPE_COMMENT == notification.getType()) {
                notificationTextBuilder.append(" commented on your post: ")
                // يمكنك إضافة: notification.getCommentContentPreview() إذا كان متاحًا
            } else if (NotificationModel.Companion.TYPE_FOLLOW == notification.getType()) {
                notificationTextBuilder.append(" started following you.")
            } else {
                notificationTextBuilder.append(" sent you a notification.")
            }
            tvNotificationText.setText(notificationTextBuilder)

            // عرض الوقت النسبي
            if (notification.getTimestamp() != null) {
                tvTimestamp.setText(
                    DateUtils.getRelativeTimeSpanString(
                        notification.getTimestamp().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                    )
                )
            } else {
                tvTimestamp.setText("Just now")
            }

            // مؤشر "غير مقروء"
            ivUnreadIndicator.setVisibility(if (notification.isRead()) View.GONE else View.VISIBLE)

            // مستمع النقر على عنصر الإشعار
            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    listener.onNotificationClick(notification)
                }
            })
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<NotificationModel?> =
            object : DiffUtil.ItemCallback<NotificationModel?>() {
                override fun areItemsTheSame(
                    oldItem: NotificationModel,
                    newItem: NotificationModel
                ): Boolean {
                    // إذا كان لديك ID فريد لكل إشعار، استخدمه هنا
                    // حاليًا، نفترض أننا سنعتمد على محتوى الإشعار والوقت
                    return oldItem.getNotificationId() == newItem.getNotificationId()
                }

                override fun areContentsTheSame(
                    oldItem: NotificationModel,
                    newItem: NotificationModel
                ): Boolean {
                    return oldItem.getType() == newItem.getType() &&
                            oldItem.getFromUserId() == newItem.getFromUserId() &&
                            oldItem.getPostId() == newItem.getPostId() &&
                            oldItem.getCommentId() == newItem.getCommentId() && oldItem.isRead() == newItem.isRead() &&
                            oldItem.getTimestamp() == newItem.getTimestamp()
                }
            }
    }
}