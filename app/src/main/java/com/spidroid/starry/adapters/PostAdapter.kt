package com.spidroid.starry.adapters

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemPostBinding
import com.spidroid.starry.databinding.ItemUserSuggestionBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.utils.PostInteractionHandler
import java.util.Collections.emptyList
import java.util.Comparator
import java.util.LinkedHashMap
import java.util.stream.Collectors

class PostAdapter(
    private val context: Context,
    private val listener: PostInteractionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Any> = emptyList()
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    fun submitCombinedList(newItems: List<Any>) {
        this.items = newItems
        // For better performance, DiffUtil should be used here.
        // But for simplicity of correction, we'll use notifyDataSetChanged.
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PostModel -> TYPE_POST
            is UserModel -> TYPE_SUGGESTION
            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_POST -> {
                val binding = ItemPostBinding.inflate(inflater, parent, false)
                PostViewHolder(binding, listener, context, currentUserId)
            }
            TYPE_SUGGESTION -> {
                val binding = ItemUserSuggestionBinding.inflate(inflater, parent, false)
                UserSuggestionViewHolder(binding, listener)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is PostViewHolder -> holder.bind(item as PostModel)
            is UserSuggestionViewHolder -> holder.bind(item as UserModel)
        }
    }

    override fun getItemCount(): Int = items.size

    class PostViewHolder(
        private val binding: ItemPostBinding,
        listener: PostInteractionListener,
        private val context: Context,
        currentUserId: String?
    ) : BasePostViewHolder(binding, listener, context, currentUserId ?: "") {

        private val reactionsDisplayContainer: LinearLayout = binding.reactionsDisplayContainer

        fun bind(post: PostModel) {
            super.bindCommon(post) // Call the common binding logic from base class

            // This method now only handles logic specific to PostViewHolder
            updateReactionsDisplay(post.reactions)
        }

        private fun updateReactionsDisplay(reactionsMap: Map<String, String>?) {
            reactionsDisplayContainer.removeAllViews()
            if (reactionsMap.isNullOrEmpty()) {
                reactionsDisplayContainer.visibility = View.GONE
                return
            }

            val emojiCounts = reactionsMap.values.groupingBy { it }.eachCount()

            val sortedEmoji = emojiCounts.entries.sortedByDescending { it.value }

            var reactionsToShow = 0
            for ((emoji, _) in sortedEmoji) {
                if (reactionsToShow >= 3) break
                val emojiView = TextView(context).apply {
                    text = emoji
                    textSize = 16f
                    setPadding(0, 0, 4, 0)
                }
                reactionsDisplayContainer.addView(emojiView)
                reactionsToShow++
            }

            if (reactionsMap.isNotEmpty()) {
                val totalReactionsView = TextView(context).apply {
                    text = reactionsMap.size.toString()
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = 8 }
                }
                reactionsDisplayContainer.addView(totalReactionsView)
            }

            reactionsDisplayContainer.visibility = if (reactionsDisplayContainer.childCount > 0) View.VISIBLE else View.GONE
        }
    }

    class UserSuggestionViewHolder(
        private val binding: ItemUserSuggestionBinding,
        private val listener: PostInteractionListener?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            binding.username.text = "@${user.username}"
            binding.displayName.text = user.displayName ?: user.username

            if (!user.bio.isNullOrEmpty()) {
                binding.tvBio.text = user.bio
                binding.tvBio.visibility = View.VISIBLE
            } else {
                binding.tvBio.visibility = View.GONE
            }

            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(binding.profileImage)

            binding.followButton.setOnClickListener { listener?.onFollowClicked(user) }
            itemView.setOnClickListener { listener?.onUserClicked(user) }
        }
    }

    companion object {
        private const val TYPE_POST = 0
        private const val TYPE_SUGGESTION = 1
        private const val TAG = "PostAdapter"
    }
}