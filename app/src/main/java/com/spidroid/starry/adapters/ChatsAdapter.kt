// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/adapters/ChatsAdapter.kt
package com.spidroid.starry.adapters

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemChatBinding
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.ChatMessage
import com.spidroid.starry.models.UserModel // Added import for UserModel
import de.hdodenhof.circleimageview.CircleImageView

interface ChatClickListener {
    fun onChatClick(chat: Chat)
}

class ChatsAdapter(
    private val context: Context,
    private val listener: ChatClickListener
) : ListAdapter<Chat, ChatsAdapter.ChatViewHolder>(DIFF_CALLBACK) {

    private val currentUserId: String? = Firebase.auth.currentUser?.uid
    // This map should be populated by the Fragment/ViewModel with other users' data
    private var otherUsersMap: Map<String, UserModel> = emptyMap()

    // Function to update the map of other users
    fun setOtherUsers(users: Map<String, UserModel>) {
        otherUsersMap = users
        // It might be necessary to re-submit the list or notify adapter if new user data comes in
        // and affects currently displayed chats. For simplicity, we assume this is handled
        // when chats themselves are re-submitted.
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = getItem(position)
        holder.bind(chat)
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            binding.root.setOnClickListener { listener.onChatClick(chat) }

            // Bind Time
            binding.tvTime.text = chat.lastMessageTime?.let {
                DateUtils.formatDateTime(itemView.context, it.time, DateUtils.FORMAT_SHOW_TIME)
            } ?: ""

            // Bind Last Message
            binding.tvLastMessage.text = when (chat.lastMessageType) {
                ChatMessage.TYPE_IMAGE -> context.getString(R.string.photo_message)
                ChatMessage.TYPE_VIDEO -> context.getString(R.string.video_message)
                else -> chat.lastMessage ?: ""
            }

            // Bind Unread Count
            val unreadCount = chat.unreadCounts[currentUserId] ?: 0
            if (unreadCount > 0) {
                binding.tvUnreadCount.visibility = View.VISIBLE
                binding.tvUnreadCount.text = unreadCount.toString()
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }

            // Bind Chat Info (Group vs Direct)
            if (chat.isGroup) {
                bindGroupInfo(chat)
            } else {
                bindDirectChatInfo(chat)
            }
        }

        private fun bindGroupInfo(chat: Chat) {
            binding.tvUserName.text = chat.groupName ?: "Group Chat"
            binding.ivVerified.visibility = View.GONE
            Glide.with(itemView.context)
                .load(chat.groupImage)
                .placeholder(R.drawable.ic_default_group)
                .error(R.drawable.ic_default_group)
                .into(binding.ivAvatar)
        }

        private fun bindDirectChatInfo(chat: Chat) {
            val otherUserId = chat.participants.firstOrNull { it != currentUserId }
            val otherUser = otherUserId?.let { otherUsersMap[it] } // Get user from the map

            binding.tvUserName.text = otherUser?.displayName ?: otherUser?.username ?: "Direct Chat"
            binding.ivVerified.visibility = if (otherUser?.isVerified == true) View.VISIBLE else View.GONE
            Glide.with(itemView.context)
                .load(otherUser?.profileImageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(binding.ivAvatar)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                // Compare fields that affect the UI
                return oldItem.lastMessage == newItem.lastMessage &&
                        oldItem.lastMessageTime == newItem.lastMessageTime &&
                        oldItem.unreadCounts == newItem.unreadCounts &&
                        oldItem.groupName == newItem.groupName && // for groups
                        oldItem.groupImage == newItem.groupImage && // for groups
                        oldItem.participants == newItem.participants // For direct chats, participant list might change if user data is enriched
            }
        }
    }
}