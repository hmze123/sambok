package com.spidroid.starry.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemPostBinding
import com.spidroid.starry.databinding.LayoutUserSuggestionsBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel
import java.util.stream.Collectors

class PostAdapter(
    private val context: Context,
    private val listener: PostInteractionListener
) : ListAdapter<Any, RecyclerView.ViewHolder>(POST_DIFF_CALLBACK) {

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    // دالة خاصة لتحديث القائمة التي تحتوي على أنواع مختلفة
    fun submitCombinedList(newItems: List<Any>) {
        submitList(newItems)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PostModel -> VIEW_TYPE_POST
            is List<*> -> VIEW_TYPE_SUGGESTION // افترضنا أن الاقتراحات تأتي كقائمة
            else -> throw IllegalArgumentException("Unknown view type at position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_POST -> {
                val binding = ItemPostBinding.inflate(inflater, parent, false)
                PostViewHolder(binding, listener, context, currentUserId)
            }
            VIEW_TYPE_SUGGESTION -> {
                val binding = LayoutUserSuggestionsBinding.inflate(inflater, parent, false)
                UserSuggestionsViewHolder(binding, listener)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PostViewHolder -> holder.bind(item as PostModel)
            is UserSuggestionsViewHolder -> {
                // تأكد من أن العنصر هو قائمة من UserModel
                val userList = (item as? List<*>)?.filterIsInstance<UserModel>()
                if (userList != null) {
                    holder.bind(userList)
                }
            }
        }
    }

    /**
     * ViewHolder لعرض المنشورات. يرث من BasePostViewHolder
     */
    class PostViewHolder(
        private val binding: ItemPostBinding,
        listener: PostInteractionListener,
        private val context: Context,
        currentUserId: String?
    ) : BasePostViewHolder(binding, listener, context, currentUserId) {

        private val reactionsDisplayContainer: LinearLayout = binding.reactionsDisplayContainer

        fun bind(post: PostModel) {
            super.bindCommon(post) // استدعاء دالة الربط المشتركة من الكلاس الأب
            updateReactionsDisplay(post.reactions)
        }

        private fun updateReactionsDisplay(reactionsMap: Map<String, String>) {
            reactionsDisplayContainer.removeAllViews()
            if (reactionsMap.isEmpty()) {
                reactionsDisplayContainer.visibility = View.GONE
                return
            }

            // ... (بقية منطق عرض التفاعلات يبقى كما هو)
            reactionsDisplayContainer.visibility = View.VISIBLE
        }
    }

    /**
     * ViewHolder لعرض اقتراحات المستخدمين.
     */
    class UserSuggestionsViewHolder(
        private val binding: LayoutUserSuggestionsBinding,
        private val listener: PostInteractionListener?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(users: List<UserModel>) {
            // هنا يمكنك إعداد RecyclerView الداخلي الخاص بالاقتراحات
            val suggestionAdapter = UserSuggestionsAdapter(object : UserSuggestionsAdapter.OnUserSuggestionInteraction {
                override fun onFollowSuggestionClicked(user: UserModel, position: Int) {
                    listener?.onFollowClicked(user)
                }
                override fun onSuggestionUserClicked(user: UserModel) {
                    listener?.onUserClicked(user)
                }
            })
            binding.usersRecyclerView.adapter = suggestionAdapter
            binding.usersRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            suggestionAdapter.submitList(users)
        }
    }

    companion object {
        private const val VIEW_TYPE_POST = 0
        private const val VIEW_TYPE_SUGGESTION = 1

        private val POST_DIFF_CALLBACK = object : DiffUtil.ItemCallback<Any>() {
            override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
                return when {
                    oldItem is PostModel && newItem is PostModel -> oldItem.postId == newItem.postId
                    oldItem is List<*> && newItem is List<*> -> oldItem == newItem // مقارنة بسيطة للاقتراحات
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
                return oldItem == newItem // Data classes handle this well
            }
        }
    }
}