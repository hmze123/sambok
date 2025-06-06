package com.spidroid.starry.ui.messages

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemMessageFileBinding
import com.spidroid.starry.databinding.ItemMessageImageReceivedBinding
import com.spidroid.starry.databinding.ItemMessageImageSentBinding
import com.spidroid.starry.databinding.ItemMessagePollBinding
import com.spidroid.starry.databinding.ItemMessageTextReceivedBinding
import com.spidroid.starry.databinding.ItemMessageTextSentBinding
import com.spidroid.starry.databinding.ItemMessageVideoReceivedBinding
import com.spidroid.starry.databinding.ItemMessageVideoSentBinding
import com.spidroid.starry.models.ChatMessage
import com.spidroid.starry.models.ChatMessage.PollOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

// import com.spidroid.starry.databinding.ItemMessageRecordingSentBinding; // إذا كنت تستخدمه
// import com.spidroid.starry.databinding.ItemMessageRecordingReceivedBinding; // إذا كنت تستخدمه
class MessageAdapter(
    private val currentUserId: String?,
    private val context: Context?,
    private val listener: MessageClickListener
) : ListAdapter<ChatMessage?, RecyclerView.ViewHolder?>(MessageDiffCallback()) {
    private var recyclerView: RecyclerView? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        if (message == null || message.getSenderId() == null || message.getType() == null) {
            Log.e(
                "MessageAdapter",
                "Message or its critical fields are null at position: " + position + ". Returning default view type."
            )
            return VIEW_TYPE_TEXT_RECEIVED // نوع افتراضي لتجنب التعطل
        }
        val isSent = message.getSenderId() == currentUserId

        when (message.getType()) {
            ChatMessage.Companion.TYPE_TEXT -> return if (isSent) VIEW_TYPE_TEXT_SENT else VIEW_TYPE_TEXT_RECEIVED
            ChatMessage.Companion.TYPE_IMAGE, ChatMessage.Companion.TYPE_GIF -> return if (isSent) VIEW_TYPE_IMAGE_SENT else VIEW_TYPE_IMAGE_RECEIVED
            ChatMessage.Companion.TYPE_VIDEO -> return if (isSent) VIEW_TYPE_VIDEO_SENT else VIEW_TYPE_VIDEO_RECEIVED
            ChatMessage.Companion.TYPE_POLL -> return VIEW_TYPE_POLL
            ChatMessage.Companion.TYPE_FILE -> return VIEW_TYPE_FILE
            else -> {
                Log.w(
                    "MessageAdapter",
                    "Unknown message type: " + message.getType() + " at position: " + position
                )
                return if (isSent) VIEW_TYPE_TEXT_SENT else VIEW_TYPE_TEXT_RECEIVED
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.getContext())

        when (viewType) {
            VIEW_TYPE_TEXT_SENT -> return SentTextViewHolder(
                ItemMessageTextSentBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            VIEW_TYPE_TEXT_RECEIVED -> return ReceivedTextViewHolder(
                ItemMessageTextReceivedBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_IMAGE_SENT -> return SentImageViewHolder(
                ItemMessageImageSentBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_IMAGE_RECEIVED -> return ReceivedImageViewHolder(
                ItemMessageImageReceivedBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_VIDEO_SENT -> return SentVideoViewHolder(
                ItemMessageVideoSentBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_VIDEO_RECEIVED -> return ReceivedVideoViewHolder(
                ItemMessageVideoReceivedBinding.inflate(inflater, parent, false)
            )

            VIEW_TYPE_POLL -> return PollViewHolder(
                ItemMessagePollBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            VIEW_TYPE_FILE -> return FileViewHolder(
                ItemMessageFileBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> {
                Log.e("MessageAdapter", "onCreateViewHolder received unknown viewType: " + viewType)
                return SentTextViewHolder(
                    ItemMessageTextSentBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (message == null) {
            Log.e("MessageAdapter", "Message is null in onBindViewHolder at position: " + position)
            holder.itemView.setVisibility(View.GONE)
            return
        }
        holder.itemView.setVisibility(View.VISIBLE)

        when (holder.getItemViewType()) {
            VIEW_TYPE_TEXT_SENT -> (holder as SentTextViewHolder).bind(message)
            VIEW_TYPE_TEXT_RECEIVED -> (holder as ReceivedTextViewHolder).bind(message)
            VIEW_TYPE_IMAGE_SENT -> (holder as SentImageViewHolder).bind(message)
            VIEW_TYPE_IMAGE_RECEIVED -> (holder as ReceivedImageViewHolder).bind(message)
            VIEW_TYPE_VIDEO_SENT -> (holder as SentVideoViewHolder).bind(message)
            VIEW_TYPE_VIDEO_RECEIVED -> (holder as ReceivedVideoViewHolder).bind(message)
            VIEW_TYPE_POLL -> (holder as PollViewHolder).bind(message)
            VIEW_TYPE_FILE -> (holder as FileViewHolder).bind(message)
            else -> {
                Log.e(
                    "MessageAdapter",
                    "onBindViewHolder encountered unknown viewType: " + holder.getItemViewType() + " for message: " + (if (message.getMessageId() != null) message.getMessageId() else "ID_NULL")
                )
                holder.itemView.setVisibility(View.GONE)
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage?>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            if (oldItem.getMessageId() == null || newItem.getMessageId() == null) {
                return oldItem === newItem
            }
            return oldItem.getMessageId() == newItem.getMessageId()
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.getContent() == newItem.getContent()
                    && oldItem.getTimestamp() == newItem.getTimestamp()
                    && oldItem.isEdited() == newItem.isEdited() && oldItem.getType() == newItem.getType()
                    && oldItem.isUploading() == newItem.isUploading() && oldItem.getMediaUrl() == newItem.getMediaUrl()
                    && oldItem.getReactions() == newItem.getReactions()
        }
    }

    internal abstract inner class BaseViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        abstract fun bind(message: ChatMessage?)
    }

    internal inner class SentTextViewHolder(val binding: ItemMessageTextSentBinding) :
        BaseViewHolder(
            binding.getRoot()
        ) {
        override fun bind(message: ChatMessage?) {
            if (message == null) {
                Log.e("SentTextViewHolder", "Message object is null in bind.")
                if (binding.textContent != null) binding.textContent.setText("")
                // قد ترغب في إخفاء العنصر بالكامل إذا كانت الرسالة null
                // itemView.setVisibility(View.GONE);
                return
            }

            // itemView.setVisibility(View.VISIBLE); // تأكد من أن العنصر ظاهر
            Log.d(
                "SentTextViewHolder",
                "Binding message: " + message.getMessageId() + ", Content: [" + message.getContent() + "]"
            )

            // ★★ إذا كنت تستخدم Data Binding لتعيين النص في XML, فهذا السطر ضروري ★★
            binding.setMessage(message)


            // إذا كنت لا تستخدم Data Binding لـ textContent أو تريد تعيينه برمجيًا أيضًا:
            // if (binding.textContent != null) {
            //     binding.textContent.setText(message.getContent() != null ? message.getContent() : "");
            // }
            if (binding.textTime != null) {
                binding.textTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            }
            if (binding.editedIndicator != null) {
                binding.editedIndicator.setVisibility(if (message.isEdited()) View.VISIBLE else View.GONE)
            }
            if (binding.statusIndicator != null) {
                if (message.getReadReceipts() != null && message.getReadReceipts().values().stream()
                        .anyMatch({ b -> b != null && b })
                ) {
                    binding.statusIndicator.setImageResource(R.drawable.ic_read)
                } else {
                    binding.statusIndicator.setImageResource(R.drawable.ic_sent)
                }
            }

            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) {
                    listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition())
                }
                true
            })

            if (binding.replyPreview != null) {
                if (message.getReplyToId() != null && message.getReplyPreview() != null) {
                    binding.replyPreview.setVisibility(View.VISIBLE)
                    binding.replyPreview.setText(message.getReplyPreview()) // يتم تعيينه برمجيًا
                    binding.replyPreview.setOnClickListener(View.OnClickListener { v: View? ->
                        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(
                                getAdapterPosition()
                            ) != null
                        ) {
                            listener.onReplyClick(getItem(getAdapterPosition())!!.getReplyToId())
                        }
                    })
                } else {
                    binding.replyPreview.setVisibility(View.GONE)
                }
            }
        }
    }

    internal inner class ReceivedTextViewHolder(val binding: ItemMessageTextReceivedBinding) :
        BaseViewHolder(
            binding.getRoot()
        ) {
        override fun bind(message: ChatMessage?) {
            if (message == null) {
                Log.e("ReceivedTextViewHolder", "Message object is null in bind.")
                if (binding.textContent != null) binding.textContent.setText("")
                if (binding.textSender != null) binding.textSender.setText("")
                return
            }
            Log.d(
                "ReceivedTextViewHolder",
                "Binding message: " + message.getMessageId() + ", Content: [" + message.getContent() + "], Sender: [" + message.getSenderName() + "]"
            )

            // ★★ إذا كنت تستخدم Data Binding لتعيين النص في XML, فهذا السطر ضروري ★★
            binding.setMessage(message)

            // إذا كنت لا تستخدم Data Binding لـ textContent أو تريد تعيينه برمجيًا أيضًا:
            // if (binding.textContent != null) {
            //    binding.textContent.setText(message.getContent() != null ? message.getContent() : "");
            // }
            if (binding.textSender != null) {
                binding.textSender.setText(
                    if (message.getSenderName() != null) message.getSenderName() else (if (context != null) context.getString(
                        R.string.unknown_user_display_name
                    ) else "User")
                )
            }
            if (binding.textTime != null) {
                binding.textTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            }
            if (binding.editedIndicator != null) {
                binding.editedIndicator.setVisibility(if (message.isEdited()) View.VISIBLE else View.GONE)
            }

            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) {
                    listener.onMessageLongClick(getItem(getAdapterPosition()), getAdapterPosition())
                }
                true
            })

            if (binding.replyPreview != null) {
                if (message.getReplyToId() != null && message.getReplyPreview() != null) {
                    binding.replyPreview.setVisibility(View.VISIBLE)
                    binding.replyPreview.setText(message.getReplyPreview()) // يتم تعيينه برمجيًا
                    binding.replyPreview.setOnClickListener(View.OnClickListener { v: View? ->
                        if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(
                                getAdapterPosition()
                            ) != null
                        ) {
                            listener.onReplyClick(getItem(getAdapterPosition())!!.getReplyToId())
                        }
                    })
                } else {
                    binding.replyPreview.setVisibility(View.GONE)
                }
            }

            if (context != null && binding.avatar != null) {
                if (message.getSenderAvatar() != null && !message.getSenderAvatar().isEmpty()) {
                    Glide.with(context)
                        .load(message.getSenderAvatar())
                        .circleCrop()
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .into(binding.avatar)
                } else {
                    binding.avatar.setImageResource(R.drawable.ic_default_avatar)
                }
            }
        }
    }

    internal inner class SentImageViewHolder(binding: ItemMessageImageSentBinding) :
        BaseViewHolder(binding.getRoot()) {
        val binding: ItemMessageImageSentBinding?

        init {
            this.binding = binding
        }

        override fun bind(message: ChatMessage) {
            val imageUrl =
                if (message.getThumbnailUrl() != null) message.getThumbnailUrl() else message.getMediaUrl()
            if (context != null && binding!!.imageContent != null) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(context).load(imageUrl).placeholder(R.drawable.ic_cover_placeholder)
                        .error(R.drawable.ic_cover_placeholder)
                        .transition(DrawableTransitionOptions.withCrossFade()).into(
                            binding.imageContent
                        )
                } else {
                    binding.imageContent.setImageResource(R.drawable.ic_cover_placeholder)
                }
            }
            if (binding!!.textTime != null) binding.textTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            if (binding.statusIndicator != null) {
                if (message.getReadReceipts() != null && message.getReadReceipts().values().stream()
                        .anyMatch({ b -> b != null && b })
                ) binding.statusIndicator.setImageResource(R.drawable.ic_read)
                else binding.statusIndicator.setImageResource(R.drawable.ic_sent)
            }
            if (binding.imageContent != null) binding.imageContent.setOnClickListener(View.OnClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(
                        getAdapterPosition()
                    )!!.getMediaUrl() != null
                ) listener.onMediaClick(
                    getItem(getAdapterPosition())!!.getMediaUrl(), getAdapterPosition()
                )
            })
            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(
                    getItem(getAdapterPosition()),
                    getAdapterPosition()
                )
                true
            })
            if (binding.progressBar != null) binding.progressBar.setVisibility(if (message.isUploading()) View.VISIBLE else View.GONE)
        }
    }

    internal inner class ReceivedImageViewHolder(binding: ItemMessageImageReceivedBinding) :
        BaseViewHolder(binding.getRoot()) {
        val binding: ItemMessageImageReceivedBinding?

        init {
            this.binding = binding
        }

        override fun bind(message: ChatMessage) {
            val imageUrl =
                if (message.getThumbnailUrl() != null) message.getThumbnailUrl() else message.getMediaUrl()
            if (context != null && binding!!.imageContent != null) {
                if (imageUrl != null && !imageUrl.isEmpty()) Glide.with(context).load(imageUrl)
                    .placeholder(R.drawable.ic_cover_placeholder)
                    .error(R.drawable.ic_cover_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade()).into(
                        binding.imageContent
                    )
                else binding.imageContent.setImageResource(R.drawable.ic_cover_placeholder)
            }
            if (binding!!.textTime != null) binding.textTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            if (binding.imageContent != null) binding.imageContent.setOnClickListener(View.OnClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(
                        getAdapterPosition()
                    )!!.getMediaUrl() != null
                ) listener.onMediaClick(
                    getItem(getAdapterPosition())!!.getMediaUrl(), getAdapterPosition()
                )
            })
            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(
                    getItem(getAdapterPosition()),
                    getAdapterPosition()
                )
                true
            })
            if (context != null && binding.avatar != null) {
                if (message.getSenderAvatar() != null && !message.getSenderAvatar()
                        .isEmpty()
                ) Glide.with(context).load(message.getSenderAvatar()).circleCrop()
                    .placeholder(R.drawable.ic_default_avatar).error(R.drawable.ic_default_avatar)
                    .into(
                        binding.avatar
                    )
                else binding.avatar.setImageResource(R.drawable.ic_default_avatar)
            }
        }
    }

    internal inner class SentVideoViewHolder(binding: ItemMessageVideoSentBinding) :
        BaseViewHolder(binding.getRoot()) {
        val binding: ItemMessageVideoSentBinding?

        init {
            this.binding = binding
        }

        override fun bind(message: ChatMessage) {
            if (context != null && binding!!.videoThumbnail != null) {
                if (message.getThumbnailUrl() != null && !message.getThumbnailUrl()
                        .isEmpty()
                ) Glide.with(context).load(message.getThumbnailUrl())
                    .placeholder(R.drawable.ic_cover_placeholder)
                    .error(R.drawable.ic_cover_placeholder).into(
                        binding.videoThumbnail
                    )
                else binding.videoThumbnail.setImageResource(R.drawable.ic_cover_placeholder)
            }
            if (binding!!.textTime != null) binding.textTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            if (binding.statusIndicator != null) {
                if (message.getReadReceipts() != null && message.getReadReceipts().values().stream()
                        .anyMatch({ b -> b != null && b })
                ) binding.statusIndicator.setImageResource(R.drawable.ic_read)
                else binding.statusIndicator.setImageResource(R.drawable.ic_sent)
            }
            if (binding.playButton != null) binding.playButton.setOnClickListener(View.OnClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(
                        getAdapterPosition()
                    )!!.getMediaUrl() != null
                ) listener.onMediaClick(
                    getItem(getAdapterPosition())!!.getMediaUrl(), getAdapterPosition()
                )
            })
            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(
                    getItem(getAdapterPosition()),
                    getAdapterPosition()
                )
                true
            })
            if (binding.durationText != null) binding.durationText.setText(
                formatDuration(
                    message.getVideoDuration()
                )
            )
            if (binding.progressBar != null) binding.progressBar.setVisibility(if (message.isUploading()) View.VISIBLE else View.GONE)
        }

        private fun formatDuration(milliseconds: Long): String {
            if (milliseconds <= 0) return "0:00"
            val seconds = (milliseconds / 1000) % 60
            val minutes = (milliseconds / (1000 * 60)) % 60
            return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
        }
    }

    internal inner class ReceivedVideoViewHolder(binding: ItemMessageVideoReceivedBinding) :
        BaseViewHolder(binding.getRoot()) {
        val binding: ItemMessageVideoReceivedBinding?

        init {
            this.binding = binding
        }

        override fun bind(message: ChatMessage) {
            if (context != null && binding!!.videoThumbnail != null) {
                if (message.getThumbnailUrl() != null && !message.getThumbnailUrl()
                        .isEmpty()
                ) Glide.with(context).load(message.getThumbnailUrl())
                    .placeholder(R.drawable.ic_cover_placeholder)
                    .error(R.drawable.ic_cover_placeholder).into(
                        binding.videoThumbnail
                    )
                else binding.videoThumbnail.setImageResource(R.drawable.ic_cover_placeholder)
            }
            if (binding!!.textSender != null) binding.textSender.setText(if (message.getSenderName() != null) message.getSenderName() else "User")
            if (binding.textTime != null) binding.textTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            if (binding.durationText != null) binding.durationText.setText(
                formatDuration(
                    message.getVideoDuration()
                )
            )
            if (binding.playButton != null) binding.playButton.setOnClickListener(View.OnClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(
                        getAdapterPosition()
                    )!!.getMediaUrl() != null
                ) listener.onMediaClick(
                    getItem(getAdapterPosition())!!.getMediaUrl(), getAdapterPosition()
                )
            })
            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(
                    getItem(getAdapterPosition()),
                    getAdapterPosition()
                )
                true
            })
            if (context != null && binding.avatar != null) {
                if (message.getSenderAvatar() != null && !message.getSenderAvatar()
                        .isEmpty()
                ) Glide.with(context).load(message.getSenderAvatar()).circleCrop()
                    .placeholder(R.drawable.ic_default_avatar).error(R.drawable.ic_default_avatar)
                    .into(
                        binding.avatar
                    )
                else binding.avatar.setImageResource(R.drawable.ic_default_avatar)
            }
        }

        private fun formatDuration(milliseconds: Long): String {
            if (milliseconds <= 0) return "0:00"
            val seconds = (milliseconds / 1000) % 60
            val minutes = (milliseconds / (1000 * 60)) % 60
            return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
        }
    }

    internal inner class PollViewHolder(val binding: ItemMessagePollBinding) : BaseViewHolder(
        binding.getRoot()
    ) {
        override fun bind(message: ChatMessage) {
            if (message.getPoll() == null) {
                Log.e(
                    "PollViewHolder",
                    "Poll data is null for message: " + (if (message.getMessageId() != null) message.getMessageId() else "ID_NULL")
                )
                itemView.setVisibility(View.GONE)
                return
            }
            itemView.setVisibility(View.VISIBLE)

            if (binding.pollQuestion != null) binding.pollQuestion.setText(
                message.getPoll().getQuestion()
            )
            setupPollOptions(message)

            if (binding.pollTotalVotes != null && itemView.getContext() != null) binding.pollTotalVotes.setText(
                itemView.getContext()
                    .getString(R.string.total_votes, message.getPoll().getTotalVotes())
            )
            if (binding.pollTime != null) binding.pollTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            if (binding.pollExpired != null) binding.pollExpired.setVisibility(
                if (message.getPoll().isExpired()) View.VISIBLE else View.GONE
            )
            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(
                    getItem(getAdapterPosition()),
                    getAdapterPosition()
                )
                true
            })
        }

        private fun setupPollOptions(message: ChatMessage) {
            if (message.getPoll() == null || message.getPoll().getOptions() == null) return
            val options: MutableList<PollOption?> = message.getPoll().getOptions()
            if (binding.option1Button != null) {
                binding.option1Button.setText("")
                binding.option1Button.setVisibility(View.GONE)
            }
            if (binding.option1Progress != null) binding.option1Progress.setVisibility(View.GONE)
            if (binding.option2Button != null) {
                binding.option2Button.setText("")
                binding.option2Button.setVisibility(View.GONE)
            }
            if (binding.option2Progress != null) binding.option2Progress.setVisibility(View.GONE)

            if (options.size > 0 && options.get(0) != null && binding.option1Button != null) {
                binding.option1Button.setText(options.get(0)!!.getText())
                binding.option1Button.setVisibility(View.VISIBLE)
            }
            if (options.size > 1 && options.get(1) != null && binding.option2Button != null) {
                binding.option2Button.setText(options.get(1)!!.getText())
                binding.option2Button.setVisibility(View.VISIBLE)
            }
            updatePollResults(message)
            if (message.getPoll().isVoted() || message.getPoll().isExpired()) disableVoting()
            else setVoteClickListeners(message.getMessageId())
        }

        private fun updatePollResults(message: ChatMessage) {
            if (message.getPoll() == null || message.getPoll().getOptions() == null) return
            val total: Int = message.getPoll().getTotalVotes()
            if (total > 0) {
                if (message.getPoll().getOptions().size() > 0 && message.getPoll().getOptions()
                        .get(0) != null && binding.option1Progress != null
                ) updateOptionProgress(
                    binding.option1Progress,
                    message.getPoll().getOptions().get(0),
                    total
                )
                if (message.getPoll().getOptions().size() > 1 && message.getPoll().getOptions()
                        .get(1) != null && binding.option2Progress != null
                ) updateOptionProgress(
                    binding.option2Progress,
                    message.getPoll().getOptions().get(1),
                    total
                )
            }
        }

        private fun updateOptionProgress(
            progressBar: ProgressBar?,
            option: PollOption?,
            total: Int
        ) {
            if (option == null || total <= 0 || progressBar == null) {
                if (progressBar != null) progressBar.setVisibility(View.GONE)
                return
            }
            val percentage = ((option.getVotes() / total.toFloat()) * 100).toInt()
            progressBar.setProgress(percentage)
            progressBar.setVisibility(View.VISIBLE)
        }

        private fun disableVoting() {
            if (binding.option1Button != null) binding.option1Button.setEnabled(false)
            if (binding.option2Button != null) binding.option2Button.setEnabled(false)
        }

        private fun setVoteClickListeners(pollId: String?) {
            if (binding.option1Button != null) binding.option1Button.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onPollVote(
                    pollId,
                    0
                )
            })
            if (binding.option2Button != null) binding.option2Button.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onPollVote(
                    pollId,
                    1
                )
            })
        }
    }

    internal inner class FileViewHolder(val binding: ItemMessageFileBinding) : BaseViewHolder(
        binding.getRoot()
    ) {
        override fun bind(message: ChatMessage) {
            if (binding.fileName != null) binding.fileName.setText(if (message.getFileName() != null) message.getFileName() else "File")
            if (binding.fileSize != null) binding.fileSize.setText(formatFileSize(message.getFileSize()))
            if (binding.fileIcon != null) binding.fileIcon.setImageResource(getFileIcon(message.getFileType()))
            if (binding.textTime != null) binding.textTime.setText(timeFormat.format(if (message.getTimestamp() != null) message.getTimestamp() else Date()))
            if (binding.progressBar != null) binding.progressBar.setVisibility(if (message.isUploading()) View.VISIBLE else View.GONE)
            if (binding.downloadButton != null) binding.downloadButton.setOnClickListener(View.OnClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null && getItem(
                        getAdapterPosition()
                    )!!.getMediaUrl() != null
                ) listener.onFileClick(
                    getItem(getAdapterPosition())!!.getMediaUrl()
                )
            })
            binding.getRoot().setOnLongClickListener(OnLongClickListener { v: View? ->
                if (getAdapterPosition() != RecyclerView.NO_POSITION && getItem(getAdapterPosition()) != null) listener.onMessageLongClick(
                    getItem(getAdapterPosition()),
                    getAdapterPosition()
                )
                true
            })
            if (binding.statusIndicator != null) {
                if (message.getReadReceipts() != null && message.getReadReceipts().values().stream()
                        .anyMatch({ b -> b != null && b })
                ) binding.statusIndicator.setImageResource(R.drawable.ic_read)
                else binding.statusIndicator.setImageResource(R.drawable.ic_sent)
                binding.statusIndicator.setVisibility(View.VISIBLE)
            }
        }

        private fun formatFileSize(size: Long): String {
            if (size <= 0) return "0 B"
            val units = arrayOf<String?>("B", "KB", "MB", "GB")
            var digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
            if (digitGroups < 0) digitGroups = 0
            if (digitGroups >= units.size) digitGroups = units.size - 1
            return String.format(
                Locale.getDefault(),
                "%.1f %s",
                size / 1024.0.pow(digitGroups.toDouble()),
                units[digitGroups]
            )
        }

        private fun getFileIcon(mimeType: String?): Int {
            if (mimeType == null || mimeType.isEmpty()) return R.drawable.ic_file_generic
            val primaryType =
                mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            when (primaryType.lowercase(Locale.getDefault())) {
                "image" -> return R.drawable.ic_file_image
                "video" -> return R.drawable.ic_file_video
                "audio" -> return R.drawable.ic_file_audio
                "application" -> {
                    if (mimeType.equals(
                            "application/pdf",
                            ignoreCase = true
                        )
                    ) return R.drawable.ic_file_pdf
                    return R.drawable.ic_file_generic
                }

                else -> return R.drawable.ic_file_generic
            }
        }
    }

    fun cleanup() {
        if (recyclerView == null || context == null) return
        for (i in 0..<recyclerView!!.getChildCount()) {
            val childView = recyclerView!!.getChildAt(i)
            if (childView == null) continue
            val holder = recyclerView!!.getChildViewHolder(childView)
            if (holder == null) continue

            if (holder is SentImageViewHolder && holder.binding != null) {
                Glide.with(context).clear(holder.binding.imageContent)
            } else if (holder is ReceivedImageViewHolder && holder.binding != null) {
                Glide.with(context).clear(holder.binding.imageContent)
            } else if (holder is SentVideoViewHolder && holder.binding != null) {
                Glide.with(context).clear(holder.binding.videoThumbnail)
            } else if (holder is ReceivedVideoViewHolder && holder.binding != null) {
                Glide.with(context).clear(holder.binding.videoThumbnail)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (context == null) return
        if (holder is SentImageViewHolder && holder.binding != null) {
            Glide.with(context).clear(holder.binding.imageContent)
        } else if (holder is ReceivedImageViewHolder && holder.binding != null) {
            Glide.with(context).clear(holder.binding.imageContent)
        } else if (holder is SentVideoViewHolder && holder.binding != null) {
            Glide.with(context).clear(holder.binding.videoThumbnail)
        } else if (holder is ReceivedVideoViewHolder && holder.binding != null) {
            Glide.with(context).clear(holder.binding.videoThumbnail)
        }
    }

    interface MessageClickListener {
        fun onMessageLongClick(message: ChatMessage?, position: Int)
        fun onMediaClick(mediaUrl: String?, position: Int)
        fun onReplyClick(messageId: String?)
        fun onPollVote(pollId: String?, option: Int)
        fun onFileClick(fileUrl: String?)
    }

    companion object {
        const val VIEW_TYPE_TEXT_SENT: Int = 1
        const val VIEW_TYPE_TEXT_RECEIVED: Int = 2
        const val VIEW_TYPE_IMAGE_SENT: Int = 3
        const val VIEW_TYPE_IMAGE_RECEIVED: Int = 4
        const val VIEW_TYPE_VIDEO_SENT: Int = 5
        const val VIEW_TYPE_VIDEO_RECEIVED: Int = 6
        const val VIEW_TYPE_POLL: Int = 7
        const val VIEW_TYPE_FILE: Int = 8
        const val VIEW_TYPE_RECORDING_SENT: Int = 9
        const val VIEW_TYPE_RECORDING_RECEIVED: Int = 10
    }
}