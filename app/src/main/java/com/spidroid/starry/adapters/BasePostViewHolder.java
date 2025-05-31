package com.spidroid.starry.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding; // ★ تأكد من استيراد ViewBinding الصحيح إذا كنت تستخدمه مباشرة
import com.spidroid.starry.models.PostModel;
// لا حاجة لاستيراد ItemPostBinding أو ItemUserSuggestionBinding هنا مباشرة
import com.spidroid.starry.utils.PostInteractionHandler;

public abstract class BasePostViewHolder extends RecyclerView.ViewHolder {

    protected PostInteractionListener listener;
    protected Context context;
    protected PostInteractionHandler interactionHandler;
    protected String currentUserId; // ★ تم الاحتفاظ به هنا إذا احتجته لاحقًا في الكلاسات الفرعية

    // ★★★ تم تعديل المُنشئ ليقبل currentUserId ★★★
    public BasePostViewHolder(@NonNull ViewBinding binding,
                              PostInteractionListener listener,
                              Context context,
                              String currentUserId) { // ★ إضافة currentUserId كمعامل
        super(binding.getRoot());
        this.listener = listener;
        this.context = context;
        this.currentUserId = currentUserId; // ★ تخزين currentUserId

        // ★ تمرير currentUserId إلى مُنشئ PostInteractionHandler
        this.interactionHandler = new PostInteractionHandler(binding.getRoot(), listener, context, currentUserId);
    }

    public abstract void bindCommon(PostModel post);
}