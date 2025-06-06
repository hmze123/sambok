package com.spidroid.starry.adapters

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemChatBinding
import com.spidroid.starry.models.Chat

interface GroupClickListener {
    fun onGroupClick(group: Chat)
}

class GroupAdapter(
    private val context: Context,
    private val listener: GroupClickListener
) : ListAdapter<Chat, GroupAdapter.GroupViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        if (group != null) {
            holder.bind(group)
        }
    }

    inner class GroupViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            binding.root.setOnClickListener { listener.onGroupClick(chat) }

            // Set group name
            binding.tvUserName.text = chat.groupName ?: "Group Chat"

            // Groups don't have a verified badge
            binding.ivVerified.visibility = View.GONE

            // Load group image
            Glide.with(context)
                .load(chat.groupImage)
                .placeholder(R.drawable.ic_default_group) // Specific placeholder for groups
                .error(R.drawable.ic_default_group)
                .into(binding.ivAvatar)

            // Set last message time
            binding.tvTime.text = chat.lastMessageTime?.let {
                DateUtils.getRelativeTimeSpanString(it.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
            } ?: ""

            // Set last message content
            binding.tvLastMessage.text = chat.lastMessage ?: "No messages yet."

            // Unread count for groups can be complex, hiding it for now.
            binding.tvUnreadCount.visibility = View.GONE
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem == newItem // Data class automatically provides an equals implementation
            }
        }
    }
}
