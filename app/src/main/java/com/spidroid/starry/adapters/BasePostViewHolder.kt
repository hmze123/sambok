package com.spidroid.starry.adapters

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemPostBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.utils.PostInteractionHandler

abstract class BasePostViewHolder(
    private val binding: ViewBinding,
    private val listener: PostInteractionListener?,
    private val context: Context,
    private val currentUserId: String?
) : RecyclerView.ViewHolder(binding.root) {

    protected val interactionHandler: PostInteractionHandler =
        PostInteractionHandler(binding.root, listener, context, currentUserId)

    open fun bindCommon(post: PostModel) {
        if (binding is ItemPostBinding) {
            // Check for valid data to prevent crashes from malformed posts
            if (post.authorId.isNullOrEmpty()) {
                Log.e("BasePostViewHolder", "Post with null or empty authorId. Hiding view. Post content: ${post.content}")
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
            binding.tvPostContent.text = post.content ?: ""
            binding.ivVerified.visibility = if (post.isAuthorVerified) View.VISIBLE else View.GONE

            Glide.with(context)
                .load(post.authorAvatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(binding.ivAuthorAvatar)

            // Bind the post to the interaction handler
            interactionHandler.bind(post)
        }
    }
}