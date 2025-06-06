package com.spidroid.starry.adapters

import android.content.Context
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.databinding.ItemCommentBinding
import java.util.Date

class CommentAdapter(
    private val context: Context,
    private val postAuthorId: String?,
    private val listener: CommentInteractionListener
) : ListAdapter<CommentModel?, CommentViewHolder?>(CommentAdapter.Companion.DIFF_CALLBACK) {
    private val currentUserId: String?

    interface CommentInteractionListener {
        fun onLikeClicked(comment: CommentModel?)
        fun onReplyClicked(comment: CommentModel?)
        fun onAuthorClicked(userId: String?)
        fun onShowRepliesClicked(comment: CommentModel?)
        fun onDeleteComment(comment: CommentModel?)
        fun onReportComment(comment: CommentModel?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding =
            ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment: CommentModel? = getItem(position)
        if (comment != null) {
            holder.bind(comment)
        }
    }

    internal inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(
            binding.getRoot()
        ) {
        private val indentationMargin: Int

        init {
            this.indentationMargin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 24f, context.getResources().getDisplayMetrics()
            ).toInt()
        }

        fun bind(comment: CommentModel) {
            // تطبيق المسافة البادئة للردود
            val params: MarginLayoutParams =
                binding.getRoot().getLayoutParams() as MarginLayoutParams
            params.setMargins(
                indentationMargin * comment.getDepth(),
                params.topMargin,
                params.rightMargin,
                params.bottomMargin
            )
            binding.getRoot().setLayoutParams(params)

            // ربط البيانات
            binding.tvAuthor.setText(comment.getAuthorDisplayName())
            binding.tvUsername.setText("@" + comment.getAuthorUsername())
            binding.tvCommentText.setText(comment.getContent())
            binding.tvTimestamp.setText(formatTimestamp(comment.getJavaDate()))

            Glide.with(context)
                .load(comment.getAuthorAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .into(binding.ivAvatar)

            binding.ivVerified.setVisibility(if (comment.isAuthorVerified()) View.VISIBLE else View.GONE)

            // منطق الإعجاب
            binding.tvLikeCount.setText(comment.getLikeCount().toString())
            binding.btnLike.setImageResource(if (comment.isLiked()) R.drawable.ic_like_filled else R.drawable.ic_like_outline)
            binding.btnLike.setColorFilter(
                ContextCompat.getColor(
                    context,
                    if (comment.isLiked()) R.color.red else R.color.text_secondary
                )
            )

            // منطق إظهار/إخفاء الردود
            if (comment.getRepliesCount() > 0) {
                binding.btnShowReplies.setVisibility(View.VISIBLE)
                // الحالة ستتم إدارتها في الـ ViewModel
                // يمكنك إضافة خاصية isExpanded إلى CommentModel إذا أردت
                val buttonText =
                    context.getString(R.string.show_replies_format, comment.getRepliesCount())
                binding.btnShowReplies.setText(buttonText)
            } else {
                binding.btnShowReplies.setVisibility(View.GONE)
            }

            // إعداد مستمعي النقرات
            binding.btnLike.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onLikeClicked(
                    comment
                )
            })
            binding.btnReply.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onReplyClicked(
                    comment
                )
            })
            binding.btnShowReplies.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onShowRepliesClicked(
                    comment
                )
            })
            binding.ivAvatar.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onAuthorClicked(
                    comment.getAuthorId()
                )
            })
            binding.tvAuthor.setOnClickListener(View.OnClickListener { v: View? ->
                listener.onAuthorClicked(
                    comment.getAuthorId()
                )
            })

            // قائمة الخيارات عند الضغط المطول
            itemView.setOnLongClickListener(OnLongClickListener { v: View? ->
                showCommentMenu(comment, v!!)
                true
            })
        }

        private fun showCommentMenu(comment: CommentModel, anchor: View) {
            val popup = PopupMenu(context, anchor)
            popup.getMenuInflater().inflate(R.menu.comment_menu, popup.getMenu())

            // إظهار خيار الحذف فقط إذا كان المستخدم هو صاحب التعليق
            if (currentUserId != null && currentUserId == comment.getAuthorId()) {
                popup.getMenu().findItem(R.id.action_delete).setVisible(true)
            } else {
                popup.getMenu().findItem(R.id.action_delete).setVisible(false)
            }

            popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
                val itemId = item!!.getItemId()
                if (itemId == R.id.action_delete) {
                    listener.onDeleteComment(comment)
                    return@setOnMenuItemClickListener true
                } else if (itemId == R.id.action_report) {
                    listener.onReportComment(comment)
                    return@setOnMenuItemClickListener true
                }
                false
            })
            popup.show()
        }

        private fun formatTimestamp(date: Date?): String {
            if (date == null) return "Just now"
            return DateUtils.getRelativeTimeSpanString(
                date.getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }
    }

    init {
        this.currentUserId = FirebaseAuth.getInstance().getUid()
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<CommentModel?> =
            object : DiffUtil.ItemCallback<CommentModel?>() {
                override fun areItemsTheSame(
                    oldItem: CommentModel,
                    newItem: CommentModel
                ): Boolean {
                    return oldItem.getCommentId() == newItem.getCommentId()
                }

                override fun areContentsTheSame(
                    oldItem: CommentModel,
                    newItem: CommentModel
                ): Boolean {
                    return oldItem.getContent() == newItem.getContent() && oldItem.getLikeCount() == newItem.getLikeCount() && oldItem.isLiked() == newItem.isLiked() && oldItem.getRepliesCount() == newItem.getRepliesCount()
                }
            }
    }
}