package com.spidroid.starry.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.spidroid.starry.R
import com.spidroid.starry.models.UserModel
import de.hdodenhof.circleimageview.CircleImageView

class UserResultAdapter(
    private val currentUserId: String?,
    private val listener: OnUserInteractionListener?
) : RecyclerView.Adapter<UserResultAdapter.UserViewHolder?>() {
    private val users: MutableList<UserModel>

    interface OnUserInteractionListener {
        fun onFollowClicked(user: UserModel?, position: Int)
        fun onUserClicked(user: UserModel?)
        fun onMoreClicked(user: UserModel?, anchor: View?)
    }

    init {
        this.users = ArrayList<UserModel>()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: CircleImageView
        val tvDisplayName: TextView
        val tvUsername: TextView
        val tvBio: TextView
        val btnFollow: MaterialButton
        val ivVerified: ImageView

        init {
            ivProfile = itemView.findViewById<CircleImageView>(R.id.iv_profile)
            tvDisplayName = itemView.findViewById<TextView>(R.id.tv_display_name)
            tvUsername = itemView.findViewById<TextView>(R.id.tv_username)
            tvBio = itemView.findViewById<TextView>(R.id.tv_bio)
            btnFollow = itemView.findViewById<MaterialButton>(R.id.btn_follow)
            ivVerified = itemView.findViewById<ImageView>(R.id.iv_verified)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user_result, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users.get(position)


        // Basic Info
        holder.tvDisplayName.setText(if (user.getDisplayName() != null) user.getDisplayName() else user.getUsername())
        holder.tvUsername.setText("@" + user.getUsername())


        // Bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            holder.tvBio.setText(user.getBio())
            holder.tvBio.setVisibility(View.VISIBLE)
        } else {
            holder.tvBio.setVisibility(View.GONE)
        }

        // Verification Badge
        holder.ivVerified.setVisibility(if (user.isVerified()) View.VISIBLE else View.GONE)

        // Follow Button State
        updateFollowButton(holder.btnFollow, user)

        // Profile Image
        Glide.with(holder.itemView.getContext())
            .load(user.getProfileImageUrl())
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .into(holder.ivProfile)

        // Click Listeners
        holder.btnFollow.setOnClickListener(View.OnClickListener { v: View? ->
            if (listener != null) {
                listener.onFollowClicked(user, holder.getAdapterPosition())
            }
        })

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            if (listener != null) {
                listener.onUserClicked(user)
            }
        })

        holder.ivProfile.setOnClickListener(View.OnClickListener { v: View? ->
            if (listener != null) {
                listener.onUserClicked(user)
            }
        })
    }

    private fun updateFollowButton(button: MaterialButton, user: UserModel) {
        val isFollowing = user.getFollowers().containsKey(currentUserId)
        val isPrivate = user.getPrivacySettings().isPrivateAccount()

        button.setVisibility(
            if (user.getUserId() == currentUserId) View.GONE else View.VISIBLE
        )

        if (isFollowing) {
            button.setText("Following")
            button.setIconResource(R.drawable.ic_check)
            button.setBackgroundColor(
                ContextCompat.getColor(button.getContext(), R.color.m3_surface_container_highest)
            )
        } else if (isPrivate) {
            button.setText("Request")
            button.setIconResource(R.drawable.ic_lock)
            button.setBackgroundColor(
                ContextCompat.getColor(button.getContext(), R.color.m3_primary)
            )
        } else {
            button.setText("Follow")
            button.setIconResource(R.drawable.ic_add)
            button.setBackgroundColor(
                ContextCompat.getColor(button.getContext(), R.color.m3_primary)
            )
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    fun updateUsers(newUsers: MutableList<UserModel>) {
        val diffResult = DiffUtil.calculateDiff(UserDiffCallback(users, newUsers))
        users.clear()
        users.addAll(newUsers)
        diffResult.dispatchUpdatesTo(this)
    }

    private class UserDiffCallback(
        private val oldUsers: MutableList<UserModel>,
        private val newUsers: MutableList<UserModel>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldUsers.size
        }

        override fun getNewListSize(): Int {
            return newUsers.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (oldUsers.get(oldItemPosition).getUserId()
                    == newUsers.get(newItemPosition).getUserId())
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldUser = oldUsers.get(oldItemPosition)
            val newUser = newUsers.get(newItemPosition)

            return oldUser.getFollowers().size == newUser.getFollowers().size && oldUser.isVerified() == newUser.isVerified() && oldUser.getPrivacySettings()
                .isPrivateAccount() ==
                    newUser.getPrivacySettings().isPrivateAccount() &&
                    oldUser.getDisplayName() == newUser.getDisplayName() &&
                    oldUser.getUsername() == newUser.getUsername() &&
                    oldUser.getBio() == newUser.getBio()
        }
    }
}