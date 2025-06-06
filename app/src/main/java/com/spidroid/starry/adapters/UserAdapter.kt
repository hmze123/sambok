package com.spidroid.starry.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.models.UserModel
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(
    private val listener: OnUserClickListener
) : ListAdapter<UserModel, UserAdapter.UserViewHolder>(DIFF_CALLBACK) {

    interface OnUserClickListener {
        fun onUserClick(user: UserModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, listener)
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: CircleImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)

        fun bind(user: UserModel, listener: OnUserClickListener) {
            tvName.text = user.displayName ?: user.username

            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAvatar)

            itemView.setOnClickListener {
                listener.onUserClick(user)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UserModel>() {
            override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                // Compare properties that affect the UI
                return oldItem.displayName == newItem.displayName &&
                        oldItem.username == newItem.username &&
                        oldItem.profileImageUrl == newItem.profileImageUrl
            }
        }
    }
}
