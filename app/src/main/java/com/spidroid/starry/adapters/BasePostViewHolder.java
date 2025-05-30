package com.spidroid.starry.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.databinding.ItemPostBinding;
import com.spidroid.starry.databinding.ItemUserSuggestionBinding;
import com.spidroid.starry.utils.PostInteractionHandler;

public abstract class BasePostViewHolder extends RecyclerView.ViewHolder {

    // ** تم التعديل هنا: لم نعد بحاجة لـ PostAdapter. قبل اسم الواجهة **
    protected PostInteractionListener listener;
    protected Context context;
    protected PostInteractionHandler interactionHandler;

    public BasePostViewHolder(@NonNull ViewBinding binding,
                              PostInteractionListener listener,
                              Context context) {
        super(binding.getRoot());
        this.listener = listener;
        this.context = context;
        this.interactionHandler = new PostInteractionHandler(binding.getRoot(), listener, context);
    }

    public abstract void bindCommon(PostModel post);
}