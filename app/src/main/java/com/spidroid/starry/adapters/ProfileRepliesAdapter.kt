package com.spidroid.starry.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spidroid.starry.R
import com.spidroid.starry.models.CommentModel
import java.util.Date

class ProfileRepliesAdapter(
    private val context: Context,
    private val listener: OnReplyClickListener
) : ListAdapter<CommentModel, ProfileRepliesAdapter.ReplyViewHolder>(DIFF_CALLBACK) {

    interface OnReplyClickListener {
        fun onReplyClicked(comment: CommentModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_reply, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRepliedToInfo: TextView = itemView.findViewById(R.id.tvRepliedToInfo)
        private val tvReplyContent: TextView = itemView.findViewById(R.id.tvReplyContent)
        private val tvReplyTimestamp: TextView = itemView.findViewById(R.id.tvReplyTimestamp)

        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onReplyClicked(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(comment: CommentModel) {
            tvReplyContent.text = comment.content

            val repliedToText = if (!comment.parentAuthorUsername.isNullOrBlank()) {
                context.getString(R.string.replying_to_user, "@${comment.parentAuthorUsername}")
            } else {
                context.getString(R.string.replying_to_author)
            }
            tvRepliedToInfo.text = repliedToText

            tvReplyTimestamp.text = comment.javaDate?.let {
                DateUtils.getRelativeTimeSpanString(
                    it.time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString()
            } ?: ""
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CommentModel>() {
            override fun areItemsTheSame(oldItem: CommentModel, newItem: CommentModel): Boolean {
                return oldItem.commentId == newItem.commentId
            }

            override fun areContentsTheSame(oldItem: CommentModel, newItem: CommentModel): Boolean {
                return oldItem.content == newItem.content && oldItem.timestamp == newItem.timestamp
            }
        }
    }
}