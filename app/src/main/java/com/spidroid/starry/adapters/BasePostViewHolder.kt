package com.spidroid.starry.adapters

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.format.DateUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemPostBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.utils.PostInteractionHandler
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

abstract class BasePostViewHolder(
    private val binding: ItemPostBinding, // تم التأكيد على أنه يستقبل ItemPostBinding
    private val listener: PostInteractionListener?,
    private val context: Context,
    private val currentUserId: String?
) : RecyclerView.ViewHolder(binding.root) {

    protected val interactionHandler: PostInteractionHandler =
        PostInteractionHandler(binding, listener, context, currentUserId) // يتم تمرير الـ binding مباشرة

    open fun bindCommon(post: PostModel) {
        if (post.authorId.isNullOrEmpty()) {
            Log.e("BasePostViewHolder", "Post with null or empty authorId. Hiding view. Post ID: ${post.postId}")
            itemView.visibility = View.GONE
            itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        }

        itemView.visibility = View.VISIBLE
        itemView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.tvAuthorName.text = post.authorDisplayName ?: post.authorUsername ?: "Unknown User"
        binding.tvUsername.text = "@${post.authorUsername ?: "unknown"}"
        binding.ivVerified.visibility = if (post.isAuthorVerified) View.VISIBLE else View.GONE
        binding.tvTimestamp.text = formatTimestamp(post.createdAt)

        Glide.with(context)
            .load(post.authorAvatarUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .into(binding.ivAuthorAvatar)

        interactionHandler.bind(post)

        val userModel = UserModel(
            userId = post.authorId!!,
            username = post.authorUsername ?: "unknown",
            email = "", // Email is not needed for this user model instance
        ).apply {
            displayName = post.authorDisplayName
            profileImageUrl = post.authorAvatarUrl
            isVerified = post.isAuthorVerified
        }

        binding.ivAuthorAvatar.setOnClickListener { listener?.onUserClicked(userModel) }
        binding.authorInfoLayout.setOnClickListener { listener?.onUserClicked(userModel) }

        // Bind different content types
        bindQuotedPost(post)
        setupTranslation(post)
    }

    private fun bindQuotedPost(post: PostModel) {
        val quotedPostContainer = binding.layoutQuotedPost.root
        val quotedPostData = post.quotedPost

        if (quotedPostData != null) {
            quotedPostContainer.visibility = View.VISIBLE

            val quotedAuthorAvatar: ImageView = quotedPostContainer.findViewById(R.id.iv_quoted_author_avatar)
            val quotedAuthorName: TextView = quotedPostContainer.findViewById(R.id.tv_quoted_author_name)
            val quotedVerifiedBadge: ImageView = quotedPostContainer.findViewById(R.id.iv_quoted_verified)
            val quotedContent: TextView = quotedPostContainer.findViewById(R.id.tv_quoted_content)

            quotedAuthorName.text = quotedPostData.authorDisplayName ?: quotedPostData.authorUsername
            quotedContent.text = quotedPostData.content
            quotedVerifiedBadge.visibility = if (quotedPostData.isAuthorVerified) View.VISIBLE else View.GONE

            Glide.with(context)
                .load(quotedPostData.authorAvatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .into(quotedAuthorAvatar)

            quotedPostContainer.setOnClickListener {
                Toast.makeText(context, "Navigate to quoted post: ${quotedPostData.postId}", Toast.LENGTH_SHORT).show()
                // TODO: Implement navigation to the quoted post detail
            }

        } else {
            quotedPostContainer.visibility = View.GONE
        }
    }

    private fun setupTranslation(post: PostModel) {
        val originalText = post.content ?: ""
        val translateButton = binding.tvTranslate

        if (post.language != null && post.language != Locale.getDefault().language && originalText.isNotBlank()) {
            translateButton.visibility = View.VISIBLE
            if (post.isTranslated) {
                setClickableSpannableText(binding.tvPostContent, post.translatedContent, listener)
                translateButton.text = context.getString(R.string.show_original)
                translateButton.setOnClickListener {
                    post.isTranslated = false
                    listener?.onShowOriginalClicked(post)
                }
            } else {
                setClickableSpannableText(binding.tvPostContent, originalText, listener)
                translateButton.text = context.getString(R.string.translate)
                translateButton.setOnClickListener {
                    listener?.onTranslateClicked(post)
                }
            }
        } else {
            translateButton.visibility = View.GONE
            setClickableSpannableText(binding.tvPostContent, originalText, listener)
        }
    }

    private fun setClickableSpannableText(textView: TextView, fullText: String?, listener: PostInteractionListener?) {
        if (fullText.isNullOrEmpty()) {
            textView.text = ""
            return
        }

        val spannableString = SpannableStringBuilder(fullText)
        val pattern = Pattern.compile("([@#])(\\w+)")
        val matcher = pattern.matcher(fullText)

        while (matcher.find()) {
            val type = matcher.group(1)
            val name = matcher.group(2)

            if (name != null) {
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        if (type == "@") {
                            Toast.makeText(context, "Mention: $name", Toast.LENGTH_SHORT).show()
                        } else if (type == "#") {
                            listener?.onHashtagClicked(name)
                        }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(context, R.color.link_color)
                        ds.isUnderlineText = false
                    }
                }
                spannableString.setSpan(clickableSpan, matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return ""
        return DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}