package com.spidroid.starry.adapters

import android.content.Context
import android.text.format.DateUtils
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
import com.spidroid.starry.adapters.GroupAdapter.GroupViewHolder
import com.spidroid.starry.models.Chat
import de.hdodenhof.circleimageview.CircleImageView

// أو الحزمة المناسبة

// سنستخدم نموذج Chat للمجموعات أيضًا
class GroupAdapter // ★ تعديل المُنشئ
// ★ تهيئة Context
    (// ★ إضافة Context
    private val context: Context?, private val listener: GroupClickListener
) : ListAdapter<Chat, GroupViewHolder?>(GroupAdapter.Companion.DIFF_CALLBACK) {
    interface GroupClickListener {
        fun onGroupClick(group: Chat?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        // يمكنك استخدام نفس item_chat أو إنشاء item_group مخصص إذا أردت عرض مختلف للمجموعات
        val view =
            LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        holder.bind(group, listener)
    }

    internal inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: CircleImageView
        private val tvUserName: TextView // سنستخدمه لاسم المجموعة
        private val ivVerified: ImageView // قد لا نحتاجه للمجموعات
        private val tvLastMessage: TextView
        private val tvTime: TextView
        private val tvUnreadCount: TextView // قد لا يكون منطقيًا للمجموعات بنفس الطريقة

        init {
            ivAvatar = itemView.findViewById<CircleImageView>(R.id.ivAvatar)
            tvUserName = itemView.findViewById<TextView>(R.id.tvUserName)
            ivVerified = itemView.findViewById<ImageView>(R.id.ivVerified)
            tvLastMessage = itemView.findViewById<TextView>(R.id.tvLastMessage)
            tvTime = itemView.findViewById<TextView>(R.id.tvTime)
            tvUnreadCount = itemView.findViewById<TextView>(R.id.tvUnreadCount)
        }

        fun bind(group: Chat, listener: GroupClickListener) {
            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onGroupClick(
                    group
                )
            })

            tvUserName.setText(group.getGroupName()) // عرض اسم المجموعة
            ivVerified.setVisibility(View.GONE) // المجموعات لا تحتاج لعلامة توثيق عادةً

            if (group.getGroupImage() != null && !group.getGroupImage()
                    .isEmpty() && context != null
            ) {
                Glide.with(context)
                    .load(group.getGroupImage())
                    .placeholder(R.drawable.ic_default_group) // أيقونة افتراضية للمجموعات
                    .error(R.drawable.ic_default_group)
                    .into(ivAvatar)
            } else if (context != null) {
                ivAvatar.setImageResource(R.drawable.ic_default_group)
            }


            if (group.getLastMessageTime() != null) {
                tvTime.setText(
                    DateUtils.formatDateTime(
                        itemView.getContext(),
                        group.getLastMessageTime().getTime(),
                        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_TIME
                    )
                )
            } else {
                tvTime.setText("")
            }

            if (group.getLastMessage() != null) {
                // يمكنك إضافة اسم المرسل هنا إذا أردت، مثلاً: "أحمد: أهلاً"
                tvLastMessage.setText(group.getLastMessage())
            } else {
                tvLastMessage.setText("No messages yet.")
            }

            // منطق عدد الرسائل غير المقروءة للمجموعات قد يكون مختلفًا أو غير مطلوب
            tvUnreadCount.setVisibility(View.GONE)
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<Chat?> =
            object : DiffUtil.ItemCallback<Chat?>() {
                override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                    return oldItem.getId() == newItem.getId()
                }

                override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                    return oldItem.getLastMessageTimestamp() == newItem.getLastMessageTimestamp() && oldItem.getLastMessage() == newItem.getLastMessage()
                            && oldItem.getGroupName() == newItem.getGroupName() // مقارنة اسم المجموعة
                            && oldItem.getGroupImage() == newItem.getGroupImage() // مقارنة صورة المجموعة
                }
            }
    }
}