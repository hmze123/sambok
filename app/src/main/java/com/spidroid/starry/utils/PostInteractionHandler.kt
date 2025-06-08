package com.spidroid.starry.utils

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.spidroid.starry.R
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.models.PostModel
import java.util.Locale

class PostInteractionHandler(
    rootView: View,
    private val listener: PostInteractionListener?,
    private val context: Context?,
    private val currentUserId: String?
) {
    private val btnLike: ImageButton = rootView.findViewById(R.id.btnLike)
    private val btnBookmark: ImageButton = rootView.findViewById(R.id.btnBookmark)
    private val btnRepost: ImageButton = rootView.findViewById(R.id.btnRepost)
    private val btnComment: ImageButton = rootView.findViewById(R.id.btnComment)
    private val tvLikeCount: TextView = rootView.findViewById(R.id.tvLikeCount)
    private val tvBookmarkCount: TextView = rootView.findViewById(R.id.tvBookmarkCount)
    private val tvRepostCount: TextView = rootView.findViewById(R.id.tvRepostCount)
    private val tvCommentCount: TextView = rootView.findViewById(R.id.tvCommentCount)
    private val ivLikeReaction: ImageView? = rootView.findViewById(R.id.ivLikeReaction)
    private var currentPost: PostModel? = null
    private val btnMenu: ImageButton? = rootView.findViewById(R.id.btnMenu)

    init {
        setupClickListeners()
    }

    fun bind(post: PostModel) {
        currentPost = post
        updateAllCounters()
        updateButtonStates()
        updateLikeReactionIcon()
    }

    private fun setupClickListeners() {
        // --- تم تعديل هذه الدوال ---
        // الآن هي تستدعي الـ listener مباشرة بدلاً من محاولة تعديل المنشور
        btnLike.setOnClickListener {
            currentPost?.let { listener?.onLikeClicked(it) }
        }
        btnBookmark.setOnClickListener {
            currentPost?.let { listener?.onBookmarkClicked(it) }
        }
        btnRepost.setOnClickListener {
            currentPost?.let { listener?.onRepostClicked(it) }
        }
        // --- نهاية التعديل ---

        btnLike.setOnLongClickListener {
            currentPost?.let { post ->
                listener?.onLikeButtonLongClicked(post, btnLike)
            }
            true
        }
        btnComment.setOnClickListener {
            currentPost?.let { listener?.onCommentClicked(it) }
        }
        btnMenu?.setOnClickListener {
            currentPost?.let { post -> listener?.onMenuClicked(post, btnMenu) }
        }
    }

    private fun updateAllCounters() {
        val post = currentPost ?: return
        tvLikeCount.text = formatCount(post.likeCount)
        tvBookmarkCount.text = formatCount(post.bookmarkCount)
        tvRepostCount.text = formatCount(post.repostCount)
        tvCommentCount.text = formatCount(post.replyCount)
    }

    private fun updateButtonStates() {
        currentPost ?: return
        updateLikeButton()
        updateBookmarkButton()
        updateRepostButton()
    }

    private fun updateLikeButton() {
        val post = currentPost ?: return
        updateButtonState(
            btnLike,
            post.isLiked,
            R.drawable.ic_like_filled,
            R.color.red,
            R.drawable.ic_like_outline
        )
    }

    private fun updateBookmarkButton() {
        val post = currentPost ?: return
        updateButtonState(
            btnBookmark,
            post.isBookmarked,
            R.drawable.ic_bookmark_filled,
            R.color.yellow,
            R.drawable.ic_bookmark_outline
        )
    }

    private fun updateRepostButton() {
        val post = currentPost ?: return
        updateButtonState(
            btnRepost,
            post.isReposted,
            R.drawable.ic_repost_filled,
            R.color.green,
            R.drawable.ic_repost_outline
        )
    }

    private fun updateButtonState(
        button: ImageButton,
        isActive: Boolean,
        filledRes: Int,
        activeColorRes: Int,
        outlineRes: Int
    ) {
        context ?: return
        button.setImageResource(if (isActive) filledRes else outlineRes)
        val color = ContextCompat.getColor(context, if (isActive) activeColorRes else R.color.text_secondary)
        button.setColorFilter(color)
    }

    private fun updateLikeReactionIcon() {
        val post = currentPost
        val reactionIcon = ivLikeReaction
        val localContext = context
        val userId = currentUserId
        if (post == null || reactionIcon == null || localContext == null || userId == null) {
            reactionIcon?.visibility = View.GONE
            return
        }

        // --- تم تعديل هذا الجزء ---
        val userReaction = post.reactions[userId]

        if (post.isLiked && userReaction != null && userReaction != "❤️") {
            val drawableId = getDrawableIdForEmoji(userReaction, true)
            if (drawableId != 0 && drawableId != R.drawable.ic_emoji) {
                reactionIcon.setImageResource(drawableId)
                reactionIcon.clearColorFilter()
                reactionIcon.visibility = View.VISIBLE
            } else {
                reactionIcon.visibility = View.GONE
            }
        } else {
            reactionIcon.visibility = View.GONE
        }
    }

    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000_000 -> String.format(Locale.getDefault(), "%.1fB", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.getDefault(), "%.0fK", count / 1_000.0)
            else -> count.toString()
        }
    }

    companion object {
        fun getDrawableIdForEmoji(reactionEmoji: String?, isSmallIcon: Boolean): Int {
            if (reactionEmoji == null) return R.drawable.ic_emoji
            return when (reactionEmoji) {
                "❤️" -> R.drawable.ic_like_filled_red
                "😂" -> R.drawable.ic_emoji_laugh_small
                "😮" -> R.drawable.ic_emoji
                "😢" -> R.drawable.ic_emoji
                "👍" -> R.drawable.ic_like_filled
                "👎" -> R.drawable.ic_emoji
                else -> R.drawable.ic_emoji
            }
        }
    }
}