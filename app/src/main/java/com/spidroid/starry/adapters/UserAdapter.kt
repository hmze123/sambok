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

class UserAdapter(private val listener: OnUserClickListener?) :
    ListAdapter<UserModel, UserAdapter.UserViewHolder?>(UserAdapter.Companion.DIFF_CALLBACK) {
    interface OnUserClickListener {
        fun onUserClick(user: UserModel?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // نستخدم item_user.xml الذي تم تقديمه سابقاً
        val view =
            LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, listener)
    }

    internal class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: CircleImageView
        private val tvName: TextView

        init {
            ivAvatar = itemView.findViewById<CircleImageView>(R.id.iv_avatar)
            tvName = itemView.findViewById<TextView>(R.id.tv_name)
        }

        fun bind(user: UserModel, listener: OnUserClickListener?) {
            tvName.setText(if (user.getDisplayName() != null) user.getDisplayName() else user.getUsername())
            Glide.with(itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAvatar)

            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    listener.onUserClick(user)
                }
            })
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<UserModel?> =
            object : DiffUtil.ItemCallback<UserModel?>() {
                override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                    return oldItem.getUserId() == newItem.getUserId()
                }

                override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
                    // يجب أن تتضمن هذه الدالة مقارنة لجميع الخصائص ذات الصلة التي قد تتغير
                    // للحفاظ على الأداء، قارن فقط الخصائص التي تؤثر على عرض العنصر
                    return oldItem.getDisplayName() == newItem.getDisplayName() &&
                            oldItem.getUsername() == newItem.getUsername() &&
                            oldItem.getProfileImageUrl() == newItem.getProfileImageUrl()
                    // يمكنك إضافة المزيد من المقارنات هنا إذا لزم الأمر
                }
            }
    }
}