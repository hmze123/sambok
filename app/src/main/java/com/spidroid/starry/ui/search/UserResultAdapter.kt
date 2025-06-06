package com.spidroid.starry.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemUserResultBinding
import com.spidroid.starry.models.UserModel

class UserResultAdapter(
    private val currentUserId: String?,
    private val listener: OnUserInteractionListener
) : ListAdapter<UserModel, UserResultAdapter.UserViewHolder>(DIFF_CALLBACK) {

    // واجهة للتفاعل مع الأحداث من الـ Fragment
    interface OnUserInteractionListener {
        fun onFollowClicked(user: UserModel, position: Int)
        fun onUserClicked(user: UserModel)
        fun onMoreClicked(user: UserModel, anchor: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        if (user != null) {
            holder.bind(user)
        }
    }

    // تم نقل الدالة القديمة updateUsers إلى submitList المدمجة في ListAdapter

    inner class UserViewHolder(private val binding: ItemUserResultBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // إعداد مستمعي النقرات مرة واحدة هنا
            binding.btnFollow.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onFollowClicked(getItem(bindingAdapterPosition), bindingAdapterPosition)
                }
            }

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onUserClicked(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(user: UserModel) {
            // ربط بيانات المستخدم بالواجهة
            binding.tvDisplayName.text = user.displayName ?: user.username
            binding.tvUsername.text = "@${user.username}"
            binding.tvBio.text = user.bio
            binding.tvBio.visibility = if (user.bio.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE

            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(binding.ivProfile)

            updateFollowButton(user)
        }

        private fun updateFollowButton(user: UserModel) {
            if (user.userId == currentUserId) {
                binding.btnFollow.visibility = View.GONE
                return
            }

            binding.btnFollow.visibility = View.VISIBLE
            val isFollowing = user.followers.containsKey(currentUserId)

            if (isFollowing) {
                binding.btnFollow.text = "Following"
                binding.btnFollow.icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_check)
            } else {
                binding.btnFollow.text = "Follow"
                binding.btnFollow.icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_add)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UserModel>() {
            override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                // قارن فقط الحقول التي تؤثر على العرض
                return oldItem.displayName == newItem.displayName &&
                        oldItem.username == newItem.username &&
                        oldItem.profileImageUrl == newItem.profileImageUrl &&
                        oldItem.followers.size == newItem.followers.size && // طريقة سريعة لمعرفة تغيير حالة المتابعة
                        oldItem.isVerified == newItem.isVerified
            }
        }
    }
}
