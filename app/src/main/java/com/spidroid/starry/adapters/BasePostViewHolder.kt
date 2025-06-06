package com.spidroid.starry.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.utils.PostInteractionHandler

// ★ تأكد من استيراد ViewBinding الصحيح إذا كنت تستخدمه مباشرة
// لا حاجة لاستيراد ItemPostBinding أو ItemUserSuggestionBinding هنا مباشرة
abstract class BasePostViewHolder(
    binding: ViewBinding,
    listener: PostInteractionListener,
    context: Context,
    currentUserId: String
) : RecyclerView.ViewHolder(binding.getRoot()) {
    protected var listener: PostInteractionListener?
    protected var context: Context?
    protected var interactionHandler: PostInteractionHandler?
    protected var currentUserId: String? // ★ تم الاحتفاظ به هنا إذا احتجته لاحقًا في الكلاسات الفرعية

    // ★★★ تم تعديل المُنشئ ليقبل currentUserId ★★★
    init { // ★ إضافة currentUserId كمعامل
        this.listener = listener
        this.context = context
        this.currentUserId = currentUserId // ★ تخزين currentUserId

        // ★ تمرير currentUserId إلى مُنشئ PostInteractionHandler
        this.interactionHandler =
            PostInteractionHandler(binding.getRoot(), listener, context, currentUserId)
    }

    abstract fun bindCommon(post: PostModel?)
}