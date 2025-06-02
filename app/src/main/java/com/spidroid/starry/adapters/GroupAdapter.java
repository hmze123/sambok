package com.spidroid.starry.adapters; // أو الحزمة المناسبة

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.Chat; // سنستخدم نموذج Chat للمجموعات أيضًا
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;

public class GroupAdapter extends ListAdapter<Chat, GroupAdapter.GroupViewHolder> {

    private final GroupClickListener listener;
    private final Context context; // ★ إضافة Context

    public interface GroupClickListener {
        void onGroupClick(Chat group);
    }

    public GroupAdapter(Context context, GroupClickListener listener) { // ★ تعديل المُنشئ
        super(DIFF_CALLBACK);
        this.context = context; // ★ تهيئة Context
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Chat> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Chat>() {
                @Override
                public boolean areItemsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
                    return oldItem.getLastMessageTimestamp() == newItem.getLastMessageTimestamp()
                            && Objects.equals(oldItem.getLastMessage(), newItem.getLastMessage())
                            && Objects.equals(oldItem.getGroupName(), newItem.getGroupName()) // مقارنة اسم المجموعة
                            && Objects.equals(oldItem.getGroupImage(), newItem.getGroupImage()); // مقارنة صورة المجموعة
                }
            };

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // يمكنك استخدام نفس item_chat أو إنشاء item_group مخصص إذا أردت عرض مختلف للمجموعات
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Chat group = getItem(position);
        holder.bind(group, listener);
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView ivAvatar;
        private final TextView tvUserName; // سنستخدمه لاسم المجموعة
        private final ImageView ivVerified; // قد لا نحتاجه للمجموعات
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final TextView tvUnreadCount; // قد لا يكون منطقيًا للمجموعات بنفس الطريقة

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            ivVerified = itemView.findViewById(R.id.ivVerified);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }

        void bind(Chat group, GroupClickListener listener) {
            itemView.setOnClickListener(v -> listener.onGroupClick(group));

            tvUserName.setText(group.getGroupName()); // عرض اسم المجموعة
            ivVerified.setVisibility(View.GONE); // المجموعات لا تحتاج لعلامة توثيق عادةً

            if (group.getGroupImage() != null && !group.getGroupImage().isEmpty() && context != null) {
                Glide.with(context)
                        .load(group.getGroupImage())
                        .placeholder(R.drawable.ic_default_group) // أيقونة افتراضية للمجموعات
                        .error(R.drawable.ic_default_group)
                        .into(ivAvatar);
            } else if (context != null) {
                ivAvatar.setImageResource(R.drawable.ic_default_group);
            }


            if (group.getLastMessageTime() != null) {
                tvTime.setText(
                        DateUtils.formatDateTime(
                                itemView.getContext(),
                                group.getLastMessageTime().getTime(),
                                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME));
            } else {
                tvTime.setText("");
            }

            if (group.getLastMessage() != null) {
                // يمكنك إضافة اسم المرسل هنا إذا أردت، مثلاً: "أحمد: أهلاً"
                tvLastMessage.setText(group.getLastMessage());
            } else {
                tvLastMessage.setText("No messages yet.");
            }

            // منطق عدد الرسائل غير المقروءة للمجموعات قد يكون مختلفًا أو غير مطلوب
            tvUnreadCount.setVisibility(View.GONE);
        }
    }
}