package com.spidroid.starry.adapters

import android.content.Context
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemCommentBinding
import com.spidroid.starry.models.CommentModel
import java.util.Date

class CommentAdapter(
    private val context: Context,
    private val postAuthorId: String?,
    private val listener: CommentInteractionListener
) : ListAdapter<CommentModel, CommentAdapter.CommentViewHolder>(DIFF_CALLBACK) {

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    interface CommentInteractionListener {
        fun onLikeClicked(comment: CommentModel)
        fun onReplyClicked(comment: CommentModel)
        fun onAuthorClicked(userId: String)
        fun onShowRepliesClicked(comment: CommentModel)
        fun onDeleteComment(comment: CommentModel)
        fun onReportComment(comment: CommentModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        if (comment != null) {
            holder.bind(comment)
        }
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {

        private val indentationMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics
        ).toInt()

        fun bind(comment: CommentModel) {
            // Apply indentation for nested replies
            val params = (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)
            params?.marginStart = indentationMargin * comment.depth
            binding.root.layoutParams = params

            // Bind data to views
            binding.tvAuthor.text = comment.authorDisplayName ?: "Unknown"
            binding.tvUsername.text = "@${comment.authorUsername ?: "unknown"}"
            binding.tvCommentText.text = comment.content
            binding.tvTimestamp.text = formatTimestamp(comment.javaDate)
            binding.ivVerified.visibility = if (comment.authorVerified) View.VISIBLE else View.GONE

            Glide.with(context)
                .load(comment.authorAvatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(binding.ivAvatar)

            // Like button state
            binding.tvLikeCount.text = comment.likeCount.toString()
            binding.btnLike.setImageResource(if (comment.isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline)
            binding.btnLike.setColorFilter(
                ContextCompat.getColor(context, if (comment.isLiked) R.color.red else R.color.text_secondary)
            )

            // Replies button state
            if (comment.repliesCount > 0) {
                binding.btnShowReplies.visibility = View.VISIBLE
                binding.btnShowReplies.text = context.getString(R.string.show_replies_format, comment.repliesCount)
            } else {
                binding.btnShowReplies.visibility = View.GONE
            }

            setupClickListeners(comment)
        }

        private fun setupClickListeners(comment: CommentModel) {
            binding.btnLike.setOnClickListener { listener.onLikeClicked(comment) }
            binding.btnReply.setOnClickListener { listener.onReplyClicked(comment) }
            binding.btnShowReplies.setOnClickListener { listener.onShowRepliesClicked(comment) }
            binding.ivAvatar.setOnClickListener { comment.authorId?.let { listener.onAuthorClicked(it) } }
            binding.tvAuthor.setOnClickListener { comment.authorId?.let { listener.onAuthorClicked(it) } }

            itemView.setOnLongClickListener {
                showCommentMenu(comment, it)
                true
            }
        }

        private fun showCommentMenu(comment: CommentModel, anchor: View) {
            val popup = PopupMenu(context, anchor)
            popup.menuInflater.inflate(R.menu.comment_menu, popup.menu)

            popup.menu.findItem(R.id.action_delete).isVisible = (currentUserId == comment.authorId)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete -> {
                        listener.onDeleteComment(comment)
                        true
                    }
                    R.id.action_report -> {
                        listener.onReportComment(comment)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun formatTimestamp(date: Date?): String {
            if (date == null) return "Just now"
            return DateUtils.getRelativeTimeSpanString(
                date.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CommentModel>() {
            override fun areItemsTheSame(oldItem: CommentModel, newItem: CommentModel): Boolean {
                return oldItem.commentId == newItem.commentId
            }

            override fun areContentsTheSame(oldItem: CommentModel, newItem: CommentModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
