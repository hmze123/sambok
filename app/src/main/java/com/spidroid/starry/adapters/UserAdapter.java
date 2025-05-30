package com.spidroid.starry.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.UserModel;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends ListAdapter<UserModel, UserAdapter.UserViewHolder> {

    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(UserModel user);
    }

    public UserAdapter(OnUserClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<UserModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<UserModel>() {
                @Override
                public boolean areItemsTheSame(@NonNull UserModel oldItem, @NonNull UserModel newItem) {
                    return oldItem.getUserId().equals(newItem.getUserId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull UserModel oldItem, @NonNull UserModel newItem) {
                    // يجب أن تتضمن هذه الدالة مقارنة لجميع الخصائص ذات الصلة التي قد تتغير
                    // للحفاظ على الأداء، قارن فقط الخصائص التي تؤثر على عرض العنصر
                    return oldItem.getDisplayName().equals(newItem.getDisplayName()) &&
                            oldItem.getUsername().equals(newItem.getUsername()) &&
                            oldItem.getProfileImageUrl().equals(newItem.getProfileImageUrl());
                    // يمكنك إضافة المزيد من المقارنات هنا إذا لزم الأمر
                }
            };

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // نستخدم item_user.xml الذي تم تقديمه سابقاً
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = getItem(position);
        holder.bind(user, listener);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvName;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
        }

        void bind(UserModel user, OnUserClickListener listener) {
            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            Glide.with(itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivAvatar);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}