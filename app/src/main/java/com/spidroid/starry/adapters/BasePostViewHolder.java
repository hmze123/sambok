package com.spidroid.starry.adapters;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.utils.PostInteractionHandler;

public abstract class BasePostViewHolder extends RecyclerView.ViewHolder {

    protected PostInteractionHandler interactionHandler;
    protected PostAdapter.PostInteractionListener listener;
    protected Context context;

    // المُنشئ الذي يقبل View, Listener, Context
    public BasePostViewHolder(@NonNull View itemView,
                              PostAdapter.PostInteractionListener listener,
                              Context context) {
        super(itemView);
        this.listener = listener;
        this.context = context;
        // تهيئة interactionHandler هنا لأنه مشترك
        this.interactionHandler = new PostInteractionHandler(itemView, listener, context);
    }

    // هذه الطريقة مجردة (abstract) لأنها تحتاج لتنفيذ في الفئات الأبناء
    public abstract void bindCommon(PostModel post);
}