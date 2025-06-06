package com.spidroid.starry.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemUserSelectableBinding
import com.spidroid.starry.models.UserModel
import java.util.Locale

class UserSelectionAdapter(
    private val context: Context,
    private val listener: OnUserSelectionChangedListener // ✨ تم تعديل هذا السطر
) : ListAdapter<UserModel, UserSelectionAdapter.UserSelectionViewHolder>(DIFF_CALLBACK), Filterable {

    private var fullList: List<UserModel> = listOf()
    val selectedUserIds = mutableSetOf<String>()

    interface OnUserSelectionChangedListener {
        fun onSelectionChanged(count: Int)
    }

    fun setData(users: List<UserModel>) {
        fullList = users
        submitList(users)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSelectionViewHolder {
        val binding = ItemUserSelectableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserSelectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserSelectionViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class UserSelectionViewHolder(private val binding: ItemUserSelectableBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel) {
            binding.tvNameSelectable.text = user.displayName ?: user.username

            Glide.with(context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(binding.ivAvatarSelectable)

            binding.cbSelectUser.setOnCheckedChangeListener(null)
            binding.cbSelectUser.isChecked = selectedUserIds.contains(user.userId)

            binding.cbSelectUser.setOnCheckedChangeListener { _, isChecked ->
                toggleSelection(user.userId)
            }

            itemView.setOnClickListener {
                binding.cbSelectUser.isChecked = !binding.cbSelectUser.isChecked
            }
        }

        private fun toggleSelection(userId: String) {
            if (selectedUserIds.contains(userId)) {
                selectedUserIds.remove(userId)
            } else {
                selectedUserIds.add(userId)
            }
            // الآن استدعاء المستمع صحيح
            listener.onSelectionChanged(selectedUserIds.size)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList = if (constraint.isNullOrBlank()) {
                    fullList
                } else {
                    val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()
                    fullList.filter { user ->
                        (user.displayName?.lowercase(Locale.getDefault())?.contains(filterPattern) == true) ||
                                (user.username.lowercase(Locale.getDefault()).contains(filterPattern))
                    }
                }
                return FilterResults().apply { values = filteredList }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                submitList(results?.values as? List<UserModel>)
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
                        oldItem.username == newItem.username &&
                        oldItem.profileImageUrl == newItem.profileImageUrl
            }
        }
    }
}