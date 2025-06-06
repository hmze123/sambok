// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/common/BottomSheetPostOptions.kt
package com.spidroid.starry.ui.common

import android.os.Bundle // ✨ تم إضافة هذا الاستيراد
import android.view.LayoutInflater // ✨ تم إضافة هذا الاستيراد
import android.view.View
import android.view.ViewGroup // ✨ تم إضافة هذا الاستيراد
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment // ✨ تم إضافة هذا الاستيراد
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R // ✨ تم إضافة هذا الاستيراد
import com.spidroid.starry.adapters.PostInteractionListener // ✨ تم إضافة هذا الاستيراد
import com.spidroid.starry.models.PostModel // ✨ تم إضافة هذا الاستيراد


class BottomSheetPostOptions : BottomSheetDialogFragment() {
    private var post: PostModel? = null
    private var interactionListener: PostInteractionListener? = null
    private var currentUserId: String? = null

    // لتعيين الـ listener من الـ Activity أو الـ Fragment المستدعي
    fun setPostInteractionListener(listener: PostInteractionListener?) {
        this.interactionListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) { // ✨ تم تصحيح Override
        super.onCreate(savedInstanceState) // ✨ تم تصحيح استدعاء Super
        arguments?.let { // ✨ استخدام خاصية arguments
            post = it.getParcelable(ARG_POST) // ✨ استخدام it.getParcelable
        }
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid // ✨ معالجة Null safety
    }

    override fun onCreateView( // ✨ تم تصحيح Override
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // استخدام ملف التخطيط bottom_sheet_post_options.xml
        val view: View? = inflater.inflate(R.layout.bottom_sheet_post_options, container, false) // ✨ استخدام R
        // يمكنك إضافة المزيد من تخصيصات التصميم هنا
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { // ✨ تم تصحيح Override
        super.onViewCreated(view, savedInstanceState) // ✨ تم تصحيح استدعاء Super

        if (post == null || interactionListener == null) {
            dismiss() // أغلق الـ BottomSheet إذا لم تكن البيانات أو الـ listener متاحة
            return
        }

        val isAuthor = currentUserId != null && currentUserId == post?.authorId // ✨ معالجة Null safety

        // الحصول على مراجع للأزرار من ملف التخطيط
        // افترض أن لديك هذه المعرفات في bottom_sheet_post_options.xml
        val optionPin = view.findViewById<Button>(R.id.option_pin_post_bs) // ✨ استخدام R
        val optionEdit = view.findViewById<Button>(R.id.option_edit_post_bs) // ✨ استخدام R
        val optionDelete = view.findViewById<Button>(R.id.option_delete_post_bs) // ✨ استخدام R
        val optionCopyLink = view.findViewById<Button>(R.id.option_copy_link_bs) // ✨ استخدام R
        val optionShare = view.findViewById<Button>(R.id.option_share_post_bs) // ✨ استخدام R
        val optionSave = view.findViewById<Button>(R.id.option_save_post_bs) // ✨ استخدام R
        val optionEditPrivacy = view.findViewById<Button>(R.id.option_edit_privacy_bs) // ✨ استخدام R
        val optionReport = view.findViewById<Button>(R.id.option_report_post_bs) // ✨ استخدام R

        // يمكنك استخدام MaterialButton إذا كانت لديك أيقونات ونصوص
        // مثال لزر الحفظ:
        val saveButton = view.findViewById<Button>(R.id.option_save_post_bs) // افترض أن هذا هو ID زر الحفظ

        // التحكم في ظهور الخيارات
        optionPin?.visibility = if (isAuthor) View.VISIBLE else View.GONE
        optionEdit?.visibility = if (isAuthor) View.VISIBLE else View.GONE
        optionDelete?.visibility = if (isAuthor) View.VISIBLE else View.GONE
        optionEditPrivacy?.visibility = if (isAuthor) View.VISIBLE else View.GONE
        optionReport?.visibility = if (isAuthor) View.GONE else View.VISIBLE

        // تحديث نص زر الحفظ
        saveButton?.text = if (post?.isBookmarked == true) "إلغاء حفظ المنشور" else "حفظ المنشور" // ✨ استخدام post?.isBookmarked

        // تعيين مستمعي النقر
        optionPin?.setOnClickListener {
            interactionListener?.onTogglePinPostClicked(post) // ✨ استخدام interactionListener?.onTogglePinPostClicked
            dismiss()
        }
        optionEdit?.setOnClickListener {
            interactionListener?.onEditPost(post) // ✨ استخدام interactionListener?.onEditPost
            dismiss()
        }
        optionDelete?.setOnClickListener {
            interactionListener?.onDeletePost(post) // ✨ استخدام interactionListener?.onDeletePost
            dismiss()
        }
        optionCopyLink?.setOnClickListener {
            interactionListener?.onCopyLink(post) // ✨ استخدام interactionListener?.onCopyLink
            dismiss()
        }
        optionShare?.setOnClickListener {
            interactionListener?.onSharePost(post) // ✨ استخدام interactionListener?.onSharePost
            dismiss()
        }
        optionSave?.setOnClickListener {
            interactionListener?.onBookmarkClicked(post) // ✨ استخدام interactionListener?.onBookmarkClicked
            dismiss()
        }
        optionEditPrivacy?.setOnClickListener {
            interactionListener?.onEditPostPrivacy(post) // ✨ استخدام interactionListener?.onEditPostPrivacy
            dismiss()
        }
        optionReport?.setOnClickListener {
            interactionListener?.onReportPost(post) // ✨ استخدام interactionListener?.onReportPost
            dismiss()
        }
    }

    companion object {
        const val TAG: String = "BottomSheetPostOptions"
        private const val ARG_POST = "post_model"

        // دالة لإنشاء نسخة جديدة من الـ BottomSheet مع تمرير بيانات المنشور
        fun newInstance(post: PostModel?): BottomSheetPostOptions {
            val fragment = BottomSheetPostOptions()
            val args: Bundle = Bundle()
            args.putParcelable(ARG_POST, post)
            fragment.arguments = args // ✨ استخدام خاصية arguments
            return fragment
        }
    }
}