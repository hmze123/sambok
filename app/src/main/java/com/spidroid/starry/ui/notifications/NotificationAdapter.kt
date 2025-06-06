// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/notifications/NotificationAdapter.kt
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
import de.hdodenhof.circleimageview.CircleImageView

class NotificationAdapter(
    private val context: Context,
    private val listener: OnNotificationClickListener?
) : ListAdapter<NotificationModel, NotificationAdapter.NotificationViewHolder>(DIFF_CALLBACK) { // ✨ تم تغيير ListAdapter<NotificationModel?> إلى ListAdapter<NotificationModel>
    interface OnNotificationClickListener {
        fun onNotificationClick(notification: NotificationModel?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context) // ✨ تم تغيير parent.getContext() إلى parent.context
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        if (notification != null) {
            holder.bind(notification, listener)
        }
    }

    inner class NotificationViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: CircleImageView
        private val tvNotificationText: TextView
        private val tvTimestamp: TextView
        private val ivUnreadIndicator: ImageView

        init {
            ivAvatar = itemView.findViewById(R.id.iv_notification_avatar)
            tvNotificationText = itemView.findViewById(R.id.tv_notification_text)
            tvTimestamp = itemView.findViewById(R.id.tv_notification_timestamp)
            ivUnreadIndicator = itemView.findViewById(R.id.iv_notification_unread_indicator)
        }

        fun bind(notification: NotificationModel, listener: OnNotificationClickListener?) {
            // تحميل صورة المستخدم الذي قام بالإجراء
            if (!notification.fromUserAvatarUrl.isNullOrEmpty()) { // ✨ تم تغيير .getFromUserAvatarUrl().isEmpty() إلى .isNullOrEmpty()
                Glide.with(context)
                    .load(notification.fromUserAvatarUrl) // ✨ تم تغيير .getFromUserAvatarUrl()
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            // بناء نص الإشعار
            val notificationTextBuilder = SpannableStringBuilder()
            val fromUsername = notification.fromUsername ?: "Someone" // ✨ تم تغيير .getFromUsername()

            // اسم المستخدم بخط عريض
            notificationTextBuilder.append(fromUsername)
            notificationTextBuilder.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                fromUsername.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // نص الإشعار بناءً على النوع
            if (NotificationModel.TYPE_LIKE == notification.type) { // ✨ تم تغيير .getType()
                notificationTextBuilder.append(" liked your post.")
                // يمكنك إضافة: " " + notification.getPostContentPreview() إذا كان متاحًا
            } else if (NotificationModel.TYPE_COMMENT == notification.type) { // ✨ تم تغيير .getType()
                notificationTextBuilder.append(" commented on your post: ")
                // يمكنك إضافة: notification.getCommentContentPreview() إذا كان متاحًا
            } else if (NotificationModel.TYPE_FOLLOW == notification.type) { // ✨ تم تغيير .getType()
                notificationTextBuilder.append(" started following you.")
            } else {
                notificationTextBuilder.append(" sent you a notification.")
            }
            tvNotificationText.text = notificationTextBuilder // ✨ تم تغيير .setText() إلى .text

            // عرض الوقت النسبي
            if (notification.timestamp != null) { // ✨ تم تغيير .getTimestamp()
                tvTimestamp.text = DateUtils.getRelativeTimeSpanString( // ✨ تم تغيير .setText() إلى .text
                    notification.timestamp!!.time, // ✨ تم تغيير .getTimestamp().getTime()
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
            } else {
                tvTimestamp.text = "Just now" // ✨ تم تغيير .setText() إلى .text
            }

            // مؤشر "غير مقروء"
            ivUnreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE // ✨ تم تغيير .isRead() و .setVisibility()
            // مستمع النقر على عنصر الإشعار
            itemView.setOnClickListener { v: View? ->
                listener?.onNotificationClick(notification)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<NotificationModel> = // ✨ تم تغيير <NotificationModel?> إلى <NotificationModel>
            object : DiffUtil.ItemCallback<NotificationModel>() { // ✨ تم تغيير <NotificationModel?> إلى <NotificationModel>
                override fun areItemsTheSame(
                    oldItem: NotificationModel,
                    newItem: NotificationModel
                ): Boolean {
                    // إذا كان لديك ID فريد لكل إشعار، استخدمه هنا
                    // حاليًا، نفترض أننا سنعتمد على محتوى الإشعار والوقت
                    return oldItem.notificationId == newItem.notificationId // ✨ تم تغيير .getNotificationId()
                }

                override fun areContentsTheSame(
                    oldItem: NotificationModel,
                    newItem: NotificationModel
                ): Boolean {
                    return oldItem.type == newItem.type && // ✨ تم تغيير .getType()
                            oldItem.fromUserId == newItem.fromUserId && // ✨ تم تغيير .getFromUserId()
                            oldItem.postId == newItem.postId && // ✨ تم تغيير .getPostId()
                            oldItem.commentId == newItem.commentId &&
                            oldItem.isRead == newItem.isRead && // ✨ تم تغيير .isRead()
                            oldItem.timestamp == newItem.timestamp // ✨ تم تغيير .getTimestamp()
                }
            }
    }
}