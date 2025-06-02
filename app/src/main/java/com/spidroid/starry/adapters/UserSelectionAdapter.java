package com.spidroid.starry.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox; // ★ إضافة هذا
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.UserModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserSelectionAdapter extends RecyclerView.Adapter<UserSelectionAdapter.UserSelectionViewHolder> implements Filterable {

    private Context context;
    private List<UserModel> userListFull; // القائمة الأصلية الكاملة
    private List<UserModel> userListFiltered; // القائمة المعروضة بعد الفلترة
    private Set<String> selectedUserIds = new HashSet<>();
    private OnUserSelectionChangedListener listener;

    public interface OnUserSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public UserSelectionAdapter(Context context, List<UserModel> userList, OnUserSelectionChangedListener listener) {
        this.context = context;
        this.userListFull = new ArrayList<>(userList); // احتفظ بنسخة من القائمة الأصلية
        this.userListFiltered = new ArrayList<>(userList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ستحتاج لإنشاء ملف layout جديد `item_user_selectable.xml`
        // والذي يحتوي على CircleImageView, TextView للاسم, و CheckBox
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_selectable, parent, false);
        return new UserSelectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserSelectionViewHolder holder, int position) {
        UserModel user = userListFiltered.get(position);
        holder.tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(holder.ivAvatar);

        holder.cbSelectUser.setOnCheckedChangeListener(null); // مهم لإزالة المستمع القديم
        holder.cbSelectUser.setChecked(selectedUserIds.contains(user.getUserId()));
        holder.cbSelectUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleSelection(user.getUserId());
        });

        holder.itemView.setOnClickListener(v -> {
            holder.cbSelectUser.setChecked(!holder.cbSelectUser.isChecked()); // تبديل حالة الـ CheckBox عند النقر على العنصر
            // المستمع الخاص بالـ CheckBox سيتولى الباقي
        });
    }

    private void toggleSelection(String userId) {
        if (selectedUserIds.contains(userId)) {
            selectedUserIds.remove(userId);
        } else {
            selectedUserIds.add(userId);
        }
        if (listener != null) {
            listener.onSelectionChanged(selectedUserIds.size());
        }
    }

    public Set<String> getSelectedUserIds() {
        return selectedUserIds;
    }

    public void setData(List<UserModel> newUserList) {
        this.userListFull = new ArrayList<>(newUserList);
        this.userListFiltered = new ArrayList<>(newUserList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return userListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString().toLowerCase().trim();
                if (charString.isEmpty()) {
                    userListFiltered = new ArrayList<>(userListFull); // استخدام القائمة الكاملة
                } else {
                    List<UserModel> filteredList = new ArrayList<>();
                    for (UserModel user : userListFull) { // البحث في القائمة الكاملة
                        if ((user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(charString)) ||
                                (user.getUsername() != null && user.getUsername().toLowerCase().contains(charString))) {
                            filteredList.add(user);
                        }
                    }
                    userListFiltered = filteredList;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = userListFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                userListFiltered = (ArrayList<UserModel>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class UserSelectionViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName;
        CheckBox cbSelectUser; // ★ إضافة CheckBox

        UserSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar_selectable); // ★ تعديل الـ ID
            tvName = itemView.findViewById(R.id.tv_name_selectable); // ★ تعديل الـ ID
            cbSelectUser = itemView.findViewById(R.id.cb_select_user); // ★ تهيئة الـ ID
        }
    }
}