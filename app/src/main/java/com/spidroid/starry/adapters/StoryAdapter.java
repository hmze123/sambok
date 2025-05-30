package com.spidroid.starry.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private List<StoryModel> stories = new ArrayList<>();
    private UserModel currentUserModel; // لتمثيل قصة المستخدم الحالي (إذا وجدت)
    private Context context;
    private OnStoryClickListener listener;

    public interface OnStoryClickListener {
        void onAddStoryClicked();
        void onStoryPreviewClicked(StoryModel story);
        void onMyStoryPreviewClicked(UserModel userModel); // للنقر على قصتي أنا
    }

    public StoryAdapter(Context context, OnStoryClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setStories(List<StoryModel> newStories) {
        this.stories.clear();
        this.stories.addAll(newStories);
        notifyDataSetChanged();
    }

    public void setCurrentUserStory(UserModel userModel) {
        this.currentUserModel = userModel;
        notifyItemChanged(0); // تحديث العنصر الأول (قصتي)
    }

    @Override
    public int getItemCount() {
        // +1 لعنصر "إضافة قصتي" الخاص بالمستخدم الحالي
        return stories.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0; // "قصتي"
        }
        return 1; // قصص المستخدمين الآخرين
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_preview, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        if (holder.getItemViewType() == 0) {
            // "قصتي"
            holder.ivAddStory.setVisibility(View.VISIBLE);
            holder.tvAuthorName.setText("Your Story"); // أو String resource

            // تحميل صورة الملف الشخصي للمستخدم الحالي
            if (currentUserModel != null && currentUserModel.getProfileImageUrl() != null && !currentUserModel.getProfileImageUrl().isEmpty()) {
                Glide.with(context).load(currentUserModel.getProfileImageUrl()).into(holder.ivStoryAuthorAvatar);
            } else {
                holder.ivStoryAuthorAvatar.setImageResource(R.drawable.ic_default_avatar);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if (currentUserModel != null) {
                        listener.onMyStoryPreviewClicked(currentUserModel);
                    } else {
                        listener.onAddStoryClicked(); // إذا لم يكن هناك نموذج مستخدم، يمكنه إضافة قصة جديدة
                    }
                }
            });

        } else {
            // قصص المستخدمين الآخرين
            StoryModel story = stories.get(position - 1); // -1 لأن العنصر الأول محجوز لقصتي
            holder.ivAddStory.setVisibility(View.GONE);
            holder.tvAuthorName.setText(story.getUserId()); // يجب استبدالها باسم المستخدم الفعلي أو اسم العرض
            // هنا يجب عليك جلب بيانات المستخدم الذي أنشأ القصة وعرض صورته
            // سنفترض مؤقتًا أن `getUserId()` يمكن استخدامه لجلب الصورة أو الاسم
            // الأفضل هو تخزين AuthorDisplayName و AuthorAvatarUrl في StoryModel مباشرة

            // مثال: تحميل صورة المستخدم من StoryModel (إذا أضفتها)
            // Glide.with(context).load(story.getAuthorAvatarUrl()).into(holder.ivStoryAuthorAvatar);
            // وإلا، ستحتاج إلى جلب بيانات المستخدم من Firestore

            // مؤقتًا، استخدم معرف المستخدم لتحميل الصورة الافتراضية
            Glide.with(context).load(R.drawable.ic_default_avatar).into(holder.ivStoryAuthorAvatar);


            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryPreviewClicked(story);
                }
            });
        }
    }

    static class StoryViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivStoryAuthorAvatar;
        ImageView ivAddStory;
        TextView tvAuthorName;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivStoryAuthorAvatar = itemView.findViewById(R.id.ivStoryAuthorAvatar);
            ivAddStory = itemView.findViewById(R.id.ivAddStory);
            tvAuthorName = itemView.findViewById(R.id.tvStoryAuthorName);
        }
    }
}