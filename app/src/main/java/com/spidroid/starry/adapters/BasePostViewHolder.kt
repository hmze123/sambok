// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/adapters/BasePostViewHolder.kt
package com.spidroid.starry.adapters

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ItemPostBinding
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.models.UserModel // ✨ تم إضافة هذا الاستيراد
import com.spidroid.starry.utils.PostInteractionHandler
import java.util.Date

/**
 * ViewHolder أساسي ومجرد يحتوي على المنطق المشترك لعرض المنشورات.
 * هذا يمنع تكرار الكود في الـ Adapters المختلفة التي قد تعرض منشورات.
 */
abstract class BasePostViewHolder(
    private val binding: ViewBinding,
    private val listener: PostInteractionListener?,
    private val context: Context,
    private val currentUserId: String?
) : RecyclerView.ViewHolder(binding.root) {

    // Handler متخصص للتعامل مع نقرات الإعجاب، المشاركة، الخ.
    protected val interactionHandler: PostInteractionHandler =
        PostInteractionHandler(binding.root, listener, context, currentUserId)

    // دالة عامة لربط البيانات المشتركة
    open fun bindCommon(post: PostModel) {
        // تأكد من أن الـ binding من النوع الصحيح قبل المتابعة
        val itemBinding = binding as? ItemPostBinding ?: return

        // التحقق من صحة البيانات الأساسية لمنع توقف التطبيق
        if (post.authorId.isNullOrEmpty()) {
            Log.e("BasePostViewHolder", "Post with null or empty authorId. Hiding view. Post ID: ${post.postId}")
            itemView.visibility = View.GONE
            itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            return
        }

        // إعادة إظهار الـ View وتعيين أبعاده في حال كان مخفياً
        itemView.visibility = View.VISIBLE
        itemView.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // ربط بيانات المؤلف
        itemBinding.tvAuthorName.text = post.authorDisplayName ?: post.authorUsername ?: "Unknown User"
        itemBinding.tvUsername.text = "@${post.authorUsername ?: "unknown"}"
        itemBinding.ivVerified.visibility = if (post.isAuthorVerified) View.VISIBLE else View.GONE

        // عرض الوقت النسبي للمنشور
        itemBinding.tvTimestamp.text = formatTimestamp(post.createdAt)

        // تحميل صورة المؤلف
        Glide.with(context)
            .load(post.authorAvatarUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .into(itemBinding.ivAuthorAvatar)

        // ربط المنشور مع معالج التفاعلات
        interactionHandler.bind(post)

        // تعيين مستمعي النقرات على معلومات المؤلف
        itemBinding.ivAuthorAvatar.setOnClickListener {
            // ✨ تم إنشاء كائن UserModel من بيانات المؤلف وتمريره
            val userModel = UserModel(
                userId = post.authorId!!, // تم التحقق من عدم كونه null في بداية الدالة
                username = post.authorUsername ?: "unknown", // توفير قيمة افتراضية
                email = "", // توفير قيمة افتراضية (يمكن تعديلها لتناسب نموذجك)
            ).apply {
                displayName = post.authorDisplayName
                profileImageUrl = post.authorAvatarUrl
                isVerified = post.isAuthorVerified
            }
            listener?.onUserClicked(userModel)
        }
        itemBinding.authorInfoLayout.setOnClickListener {
            // ✨ تم إنشاء كائن UserModel من بيانات المؤلف وتمريره
            val userModel = UserModel(
                userId = post.authorId!!, // تم التحقق من عدم كونه null في بداية الدالة
                username = post.authorUsername ?: "unknown", // توفير قيمة افتراضية
                email = "", // توفير قيمة افتراضية
            ).apply {
                displayName = post.authorDisplayName
                profileImageUrl = post.authorAvatarUrl
                isVerified = post.isAuthorVerified
            }
            listener?.onUserClicked(userModel)
        }
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return ""
        return DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}