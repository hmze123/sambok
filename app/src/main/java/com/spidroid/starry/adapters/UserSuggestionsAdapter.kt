package com.spidroid.starry.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemUserSuggestionBinding
import com.spidroid.starry.models.UserModel

class UserSuggestionsAdapter(
    private val listener: OnUserSuggestionInteraction
) : ListAdapter<UserModel, UserSuggestionsAdapter.UserSuggestionViewHolder>(DIFF_CALLBACK) {

    // واجهة للتفاعل مع النقرات داخل هذا المحول
    interface OnUserSuggestionInteraction {
        fun onFollowSuggestionClicked(user: UserModel, position: Int)
        fun onSuggestionUserClicked(user: UserModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSuggestionViewHolder {
        val binding = ItemUserSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserSuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserSuggestionViewHolder, position: Int) {
        val user = getItem(position)
        if (user != null) {
            holder.bind(user)
        }
    }

    inner class UserSuggestionViewHolder(private val binding: ItemUserSuggestionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            binding.displayName.text = user.displayName ?: user.username
            binding.username.text = "@${user.username}"

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

            // إخفاء زر المتابعة إذا كان المستخدم المقترح هو المستخدم الحالي
            if (currentUserId == user.userId) {
                binding.followButton.visibility = View.GONE
            } else {
                binding.followButton.visibility = View.VISIBLE

                // تحديث شكل الزر بناءً على ما إذا كان المستخدم الحالي يتابع هذا الشخص أم لا
                // نفترض أن هذه المعلومة تأتي جاهزة مع كائن UserModel
                val isFollowing = user.followers.containsKey(currentUserId)
                if(isFollowing) {
                    binding.followButton.setImageResource(R.drawable.ic_check)
                } else {
                    binding.followButton.setImageResource(R.drawable.ic_add)
                }
            }

            binding.followButton.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onFollowSuggestionClicked(getItem(bindingAdapterPosition), bindingAdapterPosition)
                }
            }

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    listener.onSuggestionUserClicked(getItem(bindingAdapterPosition))
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UserModel>() {
            override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                return oldItem.userId == newItem.userId
            }

            override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                return oldItem.displayName == newItem.displayName &&
                        oldItem.followers.size == newItem.followers.size && // التحقق من عدد المتابعين كمؤشر للتغيير
                        oldItem.profileImageUrl == newItem.profileImageUrl
            }
        }
    }
}
