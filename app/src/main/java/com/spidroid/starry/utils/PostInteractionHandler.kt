package com.spidroid.starry.utils

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnLongClickListener
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
    private val btnLike: ImageButton
    private val btnBookmark: ImageButton
    private val btnRepost: ImageButton
    private val btnComment: ImageButton
    private val tvLikeCount: TextView
    private val tvBookmarkCount: TextView
    private val tvRepostCount: TextView
    private val tvCommentCount: TextView
    private val ivLikeReaction: ImageView?
    private var currentPost: PostModel? = null
    private val btnMenu: ImageButton?

    init {
        btnLike = rootView.findViewById(R.id.btnLike)
        btnBookmark = rootView.findViewById(R.id.btnBookmark)
        btnRepost = rootView.findViewById(R.id.btnRepost)
        btnComment = rootView.findViewById(R.id.btnComment)
        tvLikeCount = rootView.findViewById(R.id.tvLikeCount)
        tvBookmarkCount = rootView.findViewById(R.id.tvBookmarkCount)
        tvRepostCount = rootView.findViewById(R.id.tvRepostCount)
        tvCommentCount = rootView.findViewById(R.id.tvCommentCount)
        ivLikeReaction = rootView.findViewById(R.id.ivLikeReaction)
        btnMenu = rootView.findViewById(R.id.btnMenu)

        setupClickListeners()
    }

    fun bind(post: PostModel) {
        currentPost = post
        updateAllCounters()
        updateButtonStates()
        updateLikeReactionIcon()
    }

    fun handlePayload(payload: Bundle) {
        val post = currentPost ?: return

        for (key in payload.keySet()) {
            when (key) {
                "liked" -> {
                    post.isLiked = payload.getBoolean(key)
                    updateLikeButton()
                    updateLikeReactionIcon()
                }
                "likeCount" -> {
                    post.likeCount = payload.getLong(key)
                    tvLikeCount.text = formatCount(post.likeCount)
                }
                "reposted" -> {
                    post.isReposted = payload.getBoolean(key)
                    updateRepostButton()
                }
                "repostCount" -> {
                    post.repostCount = payload.getLong(key)
                    tvRepostCount.text = formatCount(post.repostCount)
                }
                "bookmarked" -> {
                    post.isBookmarked = payload.getBoolean(key)
                    updateBookmarkButton()
                }
                "bookmarkCount" -> {
                    post.bookmarkCount = payload.getLong(key)
                    tvBookmarkCount.text = formatCount(post.bookmarkCount)
                }
                "replyCount" -> {
                    post.replyCount = payload.getLong(key)
                    tvCommentCount.text = formatCount(post.replyCount)
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnLike.setOnClickListener {
            currentPost?.let { toggleLike(it) }
        }
        btnLike.setOnLongClickListener {
            currentPost?.let { post ->
                listener?.onLikeButtonLongClicked(post, btnLike)
            }
            true
        }
        btnBookmark.setOnClickListener {
            currentPost?.let { toggleBookmark(it) }
        }
        btnRepost.setOnClickListener {
            currentPost?.let { toggleRepost(it) }
        }
        btnComment.setOnClickListener {
            currentPost?.let { listener?.onCommentClicked(it) }
        }
        btnMenu?.setOnClickListener {
            currentPost?.let { post -> listener?.onMenuClicked(post, btnMenu) }
        }
    }

    private fun toggleLike(post: PostModel) {
        post.toggleLike()
        updateLikeButton()
        listener?.onLikeClicked(post)
        updateLikeReactionIcon()
    }

    private fun toggleBookmark(post: PostModel) {
        post.toggleBookmark()
        updateBookmarkButton()
        listener?.onBookmarkClicked(post)
    }

    private fun toggleRepost(post: PostModel) {
        post.toggleRepost()
        updateRepostButton()
        listener?.onRepostClicked(post)
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
        tvLikeCount.text = formatCount(post.likeCount)
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
        tvBookmarkCount.text = formatCount(post.bookmarkCount)
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
        tvRepostCount.text = formatCount(post.repostCount)
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
        if (post == null || reactionIcon == null || localContext == null || userId == null) return

        val userReaction = post.getUserReaction(userId)

        if (post.isLiked && userReaction != null && userReaction != "❤️") {
            val drawableId = getDrawableIdForEmoji(userReaction, true)
            if (drawableId != R.drawable.ic_emoji) {
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
            if (reactionEmoji == null) {
                return R.drawable.ic_emoji
            }
            return when (reactionEmoji) {
                "❤️" -> R.drawable.ic_like_filled_red
                "😂" -> R.drawable.ic_emoji_laugh_small
                "😮" -> R.drawable.ic_emoji // Placeholder
                "😢" -> R.drawable.ic_emoji // Placeholder
                "👍" -> R.drawable.ic_like_filled
                "👎" -> R.drawable.ic_emoji // Placeholder
                else -> R.drawable.ic_emoji
            }
        }
    }
}