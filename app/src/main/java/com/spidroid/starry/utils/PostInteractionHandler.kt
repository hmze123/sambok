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
    listener: PostInteractionListener,
    context: Context,
    currentUserId: String
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
    private val context: Context?
    private var currentPost: PostModel? = null
    private val listener: PostInteractionListener?
    private val currentUserId: String?
    private val btnMenu: ImageButton? // ★ إضافة المتغير لزر القائمة

    init {
        this.context = context
        this.listener = listener
        this.currentUserId = currentUserId

        btnLike = rootView.findViewById<ImageButton>(R.id.btnLike)
        btnBookmark = rootView.findViewById<ImageButton>(R.id.btnBookmark)
        btnRepost = rootView.findViewById<ImageButton>(R.id.btnRepost)
        btnComment = rootView.findViewById<ImageButton>(R.id.btnComment)
        tvLikeCount = rootView.findViewById<TextView>(R.id.tvLikeCount)
        tvBookmarkCount = rootView.findViewById<TextView>(R.id.tvBookmarkCount)
        tvRepostCount = rootView.findViewById<TextView>(R.id.tvRepostCount)
        tvCommentCount = rootView.findViewById<TextView>(R.id.tvCommentCount)
        ivLikeReaction = rootView.findViewById<ImageView?>(R.id.ivLikeReaction)
        btnMenu = rootView.findViewById<ImageButton?>(R.id.btnMenu) // ★ تهيئة زر القائمة

        setupClickListeners()
    }

    fun bind(post: PostModel?) {
        currentPost = post
        updateAllCounters()
        updateButtonStates()
        updateLikeReactionIcon()
    }

    fun handlePayload(payload: Bundle) {
        if (currentPost == null) return

        for (key in payload.keySet()) {
            when (key) {
                "liked" -> {
                    currentPost!!.setLiked(payload.getBoolean(key))
                    updateLikeButton()
                    updateLikeReactionIcon()
                }

                "likeCount" -> {
                    currentPost!!.setLikeCount(payload.getLong(key))
                    tvLikeCount.setText(formatCount(currentPost!!.getLikeCount()))
                }

                "reposted" -> {
                    currentPost!!.setReposted(payload.getBoolean(key))
                    updateRepostButton()
                }

                "repostCount" -> {
                    currentPost!!.setRepostCount(payload.getLong(key))
                    tvRepostCount.setText(formatCount(currentPost!!.getRepostCount()))
                }

                "bookmarked" -> {
                    currentPost!!.setBookmarked(payload.getBoolean(key))
                    updateBookmarkButton()
                }

                "bookmarkCount" -> {
                    currentPost!!.setBookmarkCount(payload.getLong(key))
                    tvBookmarkCount.setText(formatCount(currentPost!!.getBookmarkCount()))
                }

                "replyCount" -> {
                    currentPost!!.setReplyCount(payload.getLong(key))
                    tvCommentCount.setText(formatCount(currentPost!!.getReplyCount()))
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnLike.setOnClickListener(View.OnClickListener { v: View? ->
            if (currentPost != null && listener != null) toggleLike()
        })
        // ★ إضافة مستمع الضغط المطول لزر الإعجاب (إذا كنت قد أضفت الدالة للواجهة)
        btnLike.setOnLongClickListener(OnLongClickListener { v: View? ->
            if (currentPost != null && listener != null) {
                listener.onLikeButtonLongClicked(currentPost, btnLike)
                return@setOnLongClickListener true
            }
            false
        })
        btnBookmark.setOnClickListener(View.OnClickListener { v: View? ->
            if (currentPost != null && listener != null) toggleBookmark()
        })
        btnRepost.setOnClickListener(View.OnClickListener { v: View? ->
            if (currentPost != null && listener != null) toggleRepost()
        })
        btnComment.setOnClickListener(View.OnClickListener { v: View? ->
            if (listener != null && currentPost != null) listener.onCommentClicked(currentPost)
        })
        // ★★★ إضافة مستمع النقر لزر القائمة ★★★
        if (btnMenu != null) {
            btnMenu.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null && currentPost != null) {
                    listener.onMenuClicked(currentPost, btnMenu)
                }
            })
        }
    }

    private fun toggleLike() {
        currentPost!!.toggleLike()
        updateLikeButton()
        if (listener != null) {
            listener.onLikeClicked(currentPost)
        }
        updateLikeReactionIcon()
    }

    private fun toggleBookmark() {
        currentPost!!.toggleBookmark()
        updateBookmarkButton()
        if (listener != null) listener.onBookmarkClicked(currentPost)
    }

    private fun toggleRepost() {
        currentPost!!.toggleRepost()
        updateRepostButton()
        if (listener != null) listener.onRepostClicked(currentPost)
    }

    private fun updateAllCounters() {
        if (currentPost == null) return
        tvLikeCount.setText(formatCount(currentPost!!.getLikeCount()))
        tvBookmarkCount.setText(formatCount(currentPost!!.getBookmarkCount()))
        tvRepostCount.setText(formatCount(currentPost!!.getRepostCount()))
        tvCommentCount.setText(formatCount(currentPost!!.getReplyCount()))
    }

    private fun updateButtonStates() {
        if (currentPost == null) return
        updateLikeButton()
        updateButtonState(
            btnBookmark,
            currentPost!!.isBookmarked(),
            R.drawable.ic_bookmark_filled,
            R.color.yellow,
            R.drawable.ic_bookmark_outline
        )
        updateButtonState(
            btnRepost,
            currentPost!!.isReposted(),
            R.drawable.ic_repost_filled,
            R.color.green,
            R.drawable.ic_repost_outline
        )
    }

    private fun updateLikeButton() {
        if (currentPost == null) return
        updateButtonState(
            btnLike,
            currentPost!!.isLiked(),
            R.drawable.ic_like_filled,
            R.color.red,
            R.drawable.ic_like_outline
        )
        tvLikeCount.setText(formatCount(currentPost!!.getLikeCount()))
    }

    private fun updateBookmarkButton() {
        if (currentPost == null) return
        updateButtonState(
            btnBookmark,
            currentPost!!.isBookmarked(),
            R.drawable.ic_bookmark_filled,
            R.color.yellow,
            R.drawable.ic_bookmark_outline
        )
        tvBookmarkCount.setText(formatCount(currentPost!!.getBookmarkCount()))
    }

    private fun updateRepostButton() {
        if (currentPost == null) return
        updateButtonState(
            btnRepost,
            currentPost!!.isReposted(),
            R.drawable.ic_repost_filled,
            R.color.green,
            R.drawable.ic_repost_outline
        )
        tvRepostCount.setText(formatCount(currentPost!!.getRepostCount()))
    }

    private fun updateButtonState(
        button: ImageButton?,
        isActive: Boolean,
        filledRes: Int,
        activeColorRes: Int,
        outlineRes: Int
    ) {
        if (context == null || button == null) return
        button.setImageResource(if (isActive) filledRes else outlineRes)
        val colorRes = if (isActive) activeColorRes else R.color.text_secondary
        button.setColorFilter(ContextCompat.getColor(context, colorRes))
    }

    private fun updateLikeReactionIcon() {
        if (currentPost == null || ivLikeReaction == null || context == null || currentUserId == null) return

        val userReaction = currentPost!!.getUserReaction(currentUserId)

        if (currentPost!!.isLiked() && "❤️" == userReaction) {
            ivLikeReaction.setImageResource(R.drawable.ic_like_filled_red)
            ivLikeReaction.setColorFilter(ContextCompat.getColor(context, R.color.red))
            ivLikeReaction.setVisibility(View.VISIBLE)
        } else if (currentPost!!.isLiked()) {
            ivLikeReaction.setVisibility(View.GONE)
        } else {
            ivLikeReaction.setVisibility(View.GONE)
        }
    }

    private fun formatCount(count: Long): String {
        if (count >= 1000000000) return String.format(
            Locale.getDefault(),
            "%.1fB",
            count / 1000000000.0
        )
        if (count >= 1000000) return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0)
        if (count >= 1000) return String.format(Locale.getDefault(), "%.0fK", count / 1000.0)
        return count.toString()
    }

    companion object {
        fun getDrawableIdForEmoji(reactionEmoji: String?, isSmallIcon: Boolean): Int {
            if (reactionEmoji == null) {
                return R.drawable.ic_emoji
            }
            when (reactionEmoji) {
                "❤️" -> return R.drawable.ic_like_filled_red
                "😂" -> return R.drawable.ic_emoji_laugh_small // تأكد أن هذا الـ drawable موجود ومُعرَّف
                "😮" -> return R.drawable.ic_emoji // مؤقتًا
                "😢" -> return R.drawable.ic_emoji // مؤقتًا
                "👍" -> return R.drawable.ic_like_filled
                "👎" -> return R.drawable.ic_emoji // مؤقتًا
                else -> return R.drawable.ic_emoji
            }
        }
    }
}