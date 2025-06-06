package com.spidroid.starry.ui.messages

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemMessageImageReceivedBinding
import com.spidroid.starry.databinding.ItemMessageImageSentBinding
import com.spidroid.starry.databinding.ItemMessageTextReceivedBinding
import com.spidroid.starry.databinding.ItemMessageTextSentBinding
import com.spidroid.starry.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// تم تعريف الواجهة هنا بالتوقيعات الصحيحة التي يجب على ChatActivity تطبيقها
interface MessageClickListener {
    fun onMessageLongClick(message: ChatMessage, position: Int)
    fun onMediaClick(mediaUrl: String, position: Int)
    fun onReplyClick(messageId: String)
    fun onPollVote(pollId: String, optionIndex: Int)
    fun onFileClick(fileUrl: String)
}

class MessageAdapter(
    private val currentUserId: String,
    private val context: Context,
    private val listener: MessageClickListener
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position) ?: return VIEW_TYPE_TEXT_RECEIVED
        val isSent = message.senderId == currentUserId

        return when (message.type) {
            ChatMessage.TYPE_TEXT -> if (isSent) VIEW_TYPE_TEXT_SENT else VIEW_TYPE_TEXT_RECEIVED
            ChatMessage.TYPE_IMAGE, ChatMessage.TYPE_GIF -> if (isSent) VIEW_TYPE_IMAGE_SENT else VIEW_TYPE_IMAGE_RECEIVED
            // أضف أنواع أخرى هنا إذا لزم الأمر
            else -> if (isSent) VIEW_TYPE_TEXT_SENT else VIEW_TYPE_TEXT_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TEXT_SENT -> SentTextViewHolder(ItemMessageTextSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_TEXT_RECEIVED -> ReceivedTextViewHolder(ItemMessageTextReceivedBinding.inflate(inflater, parent, false))
            VIEW_TYPE_IMAGE_SENT -> SentImageViewHolder(ItemMessageImageSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_IMAGE_RECEIVED -> ReceivedImageViewHolder(ItemMessageImageReceivedBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position) ?: return
        when (holder) {
            is SentTextViewHolder -> holder.bind(message)
            is ReceivedTextViewHolder -> holder.bind(message)
            is SentImageViewHolder -> holder.bind(message)
            is ReceivedImageViewHolder -> holder.bind(message)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        // تنظيف موارد Glide لمنع تحميل الصور بشكل خاطئ
        when (holder) {
            is SentImageViewHolder -> Glide.with(context).clear(holder.binding.imageContent)
            is ReceivedImageViewHolder -> Glide.with(context).clear(holder.binding.imageContent)
        }
    }

    // --- ViewHolders ---

    inner class SentTextViewHolder(private val binding: ItemMessageTextSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.executePendingBindings()
            if (message.replyToId != null) {
                binding.replyPreview.visibility = View.VISIBLE
                binding.replyPreview.text = message.replyPreview ?: "Replying to a message"
            } else {
                binding.replyPreview.visibility = View.GONE
            }
            if (message.deleted) {
                binding.textContent.text = context.getString(R.string.message_deleted) // يمكنك إضافة هذا النص في strings.xml
                binding.textContent.setTypeface(null, android.graphics.Typeface.ITALIC)
                binding.textContent.alpha = 0.7f
                binding.root.setOnLongClickListener(null) // لا يمكن التفاعل مع الرسائل المحذوفة
            } else {
                binding.textContent.text = message.content
                binding.textContent.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.textContent.alpha = 1.0f
                binding.root.setOnLongClickListener {
                    listener.onMessageLongClick(message, bindingAdapterPosition)
                    true
                }
            }
            binding.message = message
            binding.executePendingBindings() // تطبيق الربط فورًا
            binding.textTime.text = timeFormat.format(message.timestamp ?: Date())
            binding.editedIndicator.visibility = if (message.edited) View.VISIBLE else View.GONE
            binding.statusIndicator.setImageResource(
                if (message.readReceipts.any { it.value }) R.drawable.ic_read else R.drawable.ic_sent
            )
            binding.root.setOnLongClickListener {
                listener.onMessageLongClick(message, bindingAdapterPosition)
                true
            }
        }
    }

    inner class ReceivedTextViewHolder(private val binding: ItemMessageTextReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.executePendingBindings()
            if (message.replyToId != null) {
                binding.replyPreview.visibility = View.VISIBLE
                binding.replyPreview.text = message.replyPreview ?: "Replying to a message"
            } else {
                binding.replyPreview.visibility = View.GONE
            }
            if (message.deleted) {
                binding.textContent.text = context.getString(R.string.message_deleted)
                binding.textContent.setTypeface(null, android.graphics.Typeface.ITALIC)
                binding.textContent.alpha = 0.7f
                binding.root.setOnLongClickListener(null)
            } else {
                binding.textContent.text = message.content
                binding.textContent.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.textContent.alpha = 1.0f
                binding.root.setOnLongClickListener {
                    listener.onMessageLongClick(message, bindingAdapterPosition)
                    true
                }
            }
            binding.message = message
            binding.executePendingBindings()
            binding.textSender.text = message.senderName ?: context.getString(R.string.unknown_user_display_name)
            binding.textTime.text = timeFormat.format(message.timestamp ?: Date())
            Glide.with(context).load(message.senderAvatar).placeholder(R.drawable.ic_default_avatar).into(binding.avatar)

            binding.root.setOnLongClickListener {
                listener.onMessageLongClick(message, bindingAdapterPosition)
                true
            }
        }
    }

    inner class SentImageViewHolder(val binding: ItemMessageImageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            Glide.with(context).load(message.mediaUrl).placeholder(R.drawable.ic_cover_placeholder).into(binding.imageContent)
            binding.textTime.text = timeFormat.format(message.timestamp ?: Date())
            binding.progressBar.visibility = if (message.uploading) View.VISIBLE else View.GONE
            binding.imageContent.setOnClickListener {
                message.mediaUrl?.let { url -> listener.onMediaClick(url, bindingAdapterPosition) }
            }
        }
    }

    inner class ReceivedImageViewHolder(val binding: ItemMessageImageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            Glide.with(context).load(message.mediaUrl).placeholder(R.drawable.ic_cover_placeholder).into(binding.imageContent)
            binding.textTime.text = timeFormat.format(message.timestamp ?: Date())
            Glide.with(context).load(message.senderAvatar).placeholder(R.drawable.ic_default_avatar).into(binding.avatar)
            binding.imageContent.setOnClickListener {
                message.mediaUrl?.let { url -> listener.onMediaClick(url, bindingAdapterPosition) }
            }
        }
    }

    // --- Companion Object ---
    companion object {
        const val VIEW_TYPE_TEXT_SENT = 1
        const val VIEW_TYPE_TEXT_RECEIVED = 2
        const val VIEW_TYPE_IMAGE_SENT = 3
        const val VIEW_TYPE_IMAGE_RECEIVED = 4
        const val VIEW_TYPE_VIDEO_SENT = 5
        const val VIEW_TYPE_VIDEO_RECEIVED = 6
        const val VIEW_TYPE_POLL = 7
        const val VIEW_TYPE_FILE = 8

        class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem.messageId == newItem.messageId
            }
            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem == newItem
            }
        }
    }
}
