package com.spidroid.starry.ui.messages

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.*
import com.spidroid.starry.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

interface MessageClickListener {
    fun onMessageLongClick(message: ChatMessage, position: Int)
    fun onMediaClick(mediaUrl: String, position: Int)
    fun onReplyClick(messageId: String)
    fun onPollVote(messageId: String, optionIndex: Int)
    fun onFileClick(fileUrl: String)
}

class MessageAdapter(
    private val currentUserId: String,
    private val context: Context,
    private val pollListener: MessageClickListener
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isSent = message.senderId == currentUserId

        return when (message.type) {
            ChatMessage.TYPE_TEXT -> if (isSent) VIEW_TYPE_TEXT_SENT else VIEW_TYPE_TEXT_RECEIVED
            ChatMessage.TYPE_IMAGE, ChatMessage.TYPE_GIF -> if (isSent) VIEW_TYPE_IMAGE_SENT else VIEW_TYPE_IMAGE_RECEIVED
            ChatMessage.TYPE_VIDEO -> if (isSent) VIEW_TYPE_VIDEO_SENT else VIEW_TYPE_VIDEO_RECEIVED
            ChatMessage.TYPE_POLL -> if (isSent) VIEW_TYPE_POLL_SENT else VIEW_TYPE_POLL_RECEIVED
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
            VIEW_TYPE_POLL_SENT -> PollViewHolder(ItemMessagePollSentBinding.inflate(inflater, parent, false))
            VIEW_TYPE_POLL_RECEIVED -> PollViewHolder(ItemMessagePollReceivedBinding.inflate(inflater, parent, false))
            // TODO: Add other view holders for video, files etc.
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentTextViewHolder -> holder.bind(message)
            is ReceivedTextViewHolder -> holder.bind(message)
            is SentImageViewHolder -> holder.bind(message)
            is ReceivedImageViewHolder -> holder.bind(message)
            is PollViewHolder -> holder.bind(message)
        }
    }

    // ... (ViewHolders for Text and Image remain the same) ...

    inner class SentTextViewHolder(private val binding: ItemMessageTextSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            // ... (Your existing code for SentTextViewHolder)
        }
    }

    inner class ReceivedTextViewHolder(private val binding: ItemMessageTextReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            // ... (Your existing code for ReceivedTextViewHolder)
        }
    }

    inner class SentImageViewHolder(val binding: ItemMessageImageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            // ... (Your existing code for SentImageViewHolder)
        }
    }

    inner class ReceivedImageViewHolder(val binding: ItemMessageImageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            // ... (Your existing code for ReceivedImageViewHolder)
        }
    }

    // --- ViewHolder الجديد للاستطلاعات ---
    inner class PollViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            val poll = message.poll ?: return
            val userHasVoted = poll.voters.containsKey(currentUserId)
            val userVote = poll.voters[currentUserId]

            val pollQuestionTextView: TextView
            val optionsContainer: LinearLayout
            val totalVotesTextView: TextView

            when (binding) {
                is ItemMessagePollSentBinding -> {
                    pollQuestionTextView = binding.pollQuestion
                    optionsContainer = binding.pollOptionsContainer
                    totalVotesTextView = binding.totalVotes
                }
                is ItemMessagePollReceivedBinding -> {
                    pollQuestionTextView = binding.pollQuestion
                    optionsContainer = binding.pollOptionsContainer
                    totalVotesTextView = binding.totalVotes
                    binding.textSender.text = message.senderName
                }
                else -> return
            }

            pollQuestionTextView.text = poll.question
            optionsContainer.removeAllViews()

            poll.options.forEachIndexed { index, option ->
                val optionView = LayoutInflater.from(context).inflate(R.layout.item_poll_option_view, optionsContainer, false)
                val optionText = optionView.findViewById<TextView>(R.id.poll_option_text)
                val optionProgress = optionView.findViewById<ProgressBar>(R.id.poll_option_progress)
                val optionCheck = optionView.findViewById<ImageView>(R.id.poll_option_check)

                if (userHasVoted) {
                    val percentage = if (poll.totalVotes > 0) (option.votes * 100) / poll.totalVotes else 0
                    optionProgress.progress = percentage
                    optionText.text = "${option.text} ($percentage%)"
                    optionCheck.visibility = if (userVote == index) View.VISIBLE else View.INVISIBLE
                    optionView.isClickable = false
                } else {
                    optionProgress.progress = 0
                    optionText.text = option.text
                    optionCheck.visibility = View.GONE
                    optionView.setOnClickListener {
                        message.messageId?.let { msgId -> pollListener.onPollVote(msgId, index) }
                    }
                }
                optionsContainer.addView(optionView)
            }
            totalVotesTextView.text = "${poll.totalVotes} votes"
        }
    }

    companion object {
        private const val VIEW_TYPE_TEXT_SENT = 1
        private const val VIEW_TYPE_TEXT_RECEIVED = 2
        private const val VIEW_TYPE_IMAGE_SENT = 3
        private const val VIEW_TYPE_IMAGE_RECEIVED = 4
        private const val VIEW_TYPE_VIDEO_SENT = 5
        private const val VIEW_TYPE_VIDEO_RECEIVED = 6
        private const val VIEW_TYPE_POLL_SENT = 7
        private const val VIEW_TYPE_POLL_RECEIVED = 8
        private const val VIEW_TYPE_FILE = 9

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