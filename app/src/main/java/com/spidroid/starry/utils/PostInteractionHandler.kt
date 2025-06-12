package com.spidroid.starry.utils

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.spidroid.starry.R
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.databinding.ItemPostBinding
import com.spidroid.starry.models.PostModel
import java.util.Locale

class PostInteractionHandler(
    private val binding: ItemPostBinding, // الكائن الذي يحتوي على كل عناصر الواجهة
    private val listener: PostInteractionListener?,
    private val context: Context?,
    private val currentUserId: String?
) {
    // **[تم التعديل هنا]** تعريف كل عنصر كمتغير مستقل لضمان الوصول إليه
    private val btnLike: ImageButton = binding.btnLike
    private val btnBookmark: ImageButton = binding.btnBookmark
    private val btnRepost: ImageButton = binding.btnRepost
    private val btnComment: ImageButton = binding.btnComment
    private val tvLikeCount: TextView = binding.tvLikeCount
    private val tvBookmarkCount: TextView = binding.tvBookmarkCount
    private val tvRepostCount: TextView = binding.tvRepostCount
    private val tvCommentCount: TextView = binding.tvCommentCount
    private val ivLikeReaction: ImageView = binding.ivLikeReaction
    private val btnMenu: ImageButton = binding.btnMenu

    private var currentPost: PostModel? = null

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
        btnLike.setOnClickListener {
            currentPost?.let { listener?.onLikeClicked(it) }
        }
        btnBookmark.setOnClickListener {
            currentPost?.let { listener?.onBookmarkClicked(it) }
        }
        btnRepost.setOnClickListener {
            currentPost?.let { listener?.onRepostClicked(it) }
        }
        btnLike.setOnLongClickListener {
            currentPost?.let { post ->
                listener?.onLikeButtonLongClicked(post, btnLike)
            }
            true
        }
        btnComment.setOnClickListener {
            currentPost?.let { listener?.onCommentClicked(it) }
        }
        btnMenu.setOnClickListener {
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
        val color = ContextCompat.getColor(context, if (isActive) activeColorRes else R.color.md_theme_onSurfaceVariant)
        button.setColorFilter(color)
    }

    private fun updateLikeReactionIcon() {
        val post = currentPost
        val reactionIcon = ivLikeReaction // استخدام المتغير المحلي الذي تم تعريفه
        val userId = currentUserId

        if (post == null || userId == null) {
            reactionIcon.visibility = View.GONE
            return
        }

        val userReaction = post.reactions[userId]

        if (post.isLiked && userReaction != null && userReaction != "❤️") {
            val drawableId = getDrawableIdForEmoji(userReaction, true)
            if (drawableId != 0 && drawableId != R.drawable.ic_emoji) {
                reactionIcon.setImageResource(drawableId) // **[تم التصحيح]**
                reactionIcon.clearColorFilter() // **[تم التصحيح]**
                reactionIcon.visibility = View.VISIBLE // **[تم التصحيح]**
            } else {
                reactionIcon.visibility = View.GONE // **[تم التصحيح]**
            }
        } else {
            reactionIcon.visibility = View.GONE // **[تم التصحيح]**
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