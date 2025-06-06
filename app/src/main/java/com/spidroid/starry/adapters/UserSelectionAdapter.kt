package com.spidroid.starry.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.adapters.UserSelectionAdapter.UserSelectionViewHolder
import com.spidroid.starry.models.UserModel
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale

// ★ إضافة هذا
class UserSelectionAdapter(
    private val context: Context,
    userList: MutableList<UserModel>,
    private val listener: OnUserSelectionChangedListener?
) : RecyclerView.Adapter<UserSelectionViewHolder?>(), Filterable {
    private var userListFull: MutableList<UserModel> // القائمة الأصلية الكاملة
    private var userListFiltered: MutableList<UserModel> // القائمة المعروضة بعد الفلترة
    val selectedUserIds: MutableSet<String?> = HashSet<String?>()

    interface OnUserSelectionChangedListener {
        fun onSelectionChanged(count: Int)
    }

    init {
        this.userListFull = ArrayList<UserModel>(userList) // احتفظ بنسخة من القائمة الأصلية
        this.userListFiltered = ArrayList<UserModel>(userList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSelectionViewHolder {
        // ستحتاج لإنشاء ملف layout جديد `item_user_selectable.xml`
        // والذي يحتوي على CircleImageView, TextView للاسم, و CheckBox
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_user_selectable, parent, false)
        return UserSelectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserSelectionViewHolder, position: Int) {
        val user = userListFiltered.get(position)
        holder.tvName.setText(if (user.getDisplayName() != null) user.getDisplayName() else user.getUsername())
        Glide.with(context)
            .load(user.getProfileImageUrl())
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .into(holder.ivAvatar)

        holder.cbSelectUser.setOnCheckedChangeListener(null) // مهم لإزالة المستمع القديم
        holder.cbSelectUser.setChecked(selectedUserIds.contains(user.getUserId()))
        holder.cbSelectUser.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            toggleSelection(user.getUserId())
        })

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            holder.cbSelectUser.setChecked(!holder.cbSelectUser.isChecked()) // تبديل حالة الـ CheckBox عند النقر على العنصر
        })
    }

    private fun toggleSelection(userId: String?) {
        if (selectedUserIds.contains(userId)) {
            selectedUserIds.remove(userId)
        } else {
            selectedUserIds.add(userId)
        }
        if (listener != null) {
            listener.onSelectionChanged(selectedUserIds.size)
        }
    }

    fun setData(newUserList: MutableList<UserModel>) {
        this.userListFull = ArrayList<UserModel>(newUserList)
        this.userListFiltered = ArrayList<UserModel>(newUserList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return userListFiltered.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val charString =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                if (charString.isEmpty()) {
                    userListFiltered = ArrayList<UserModel>(userListFull) // استخدام القائمة الكاملة
                } else {
                    val filteredList: MutableList<UserModel> = ArrayList<UserModel>()
                    for (user in userListFull) { // البحث في القائمة الكاملة
                        if ((user.getDisplayName() != null && user.getDisplayName()
                                .lowercase(Locale.getDefault()).contains(charString)) ||
                            (user.getUsername() != null && user.getUsername()
                                .lowercase(Locale.getDefault()).contains(charString))
                        ) {
                            filteredList.add(user)
                        }
                    }
                    userListFiltered = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = userListFiltered
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                userListFiltered = results.values as ArrayList<UserModel>
                notifyDataSetChanged()
            }
        }
    }

    internal class UserSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivAvatar: CircleImageView
        var tvName: TextView
        var cbSelectUser: CheckBox // ★ إضافة CheckBox

        init {
            ivAvatar =
                itemView.findViewById<CircleImageView>(R.id.iv_avatar_selectable) // ★ تعديل الـ ID
            tvName = itemView.findViewById<TextView>(R.id.tv_name_selectable) // ★ تعديل الـ ID
            cbSelectUser = itemView.findViewById<CheckBox>(R.id.cb_select_user) // ★ تهيئة الـ ID
        }
    }
}