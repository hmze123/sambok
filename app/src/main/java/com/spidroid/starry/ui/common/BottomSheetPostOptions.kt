package com.spidroid.starry.ui.common

// أو الحزمة الصحيحة لمشروعك

// كمثال، قد تكون تستخدم MaterialButton
// ستحتاجه للتحقق من المستخدم الحالي
// تأكد من استيراد R
import android.view.View
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class BottomSheetPostOptions : BottomSheetDialogFragment() {
    private var post: PostModel? = null
    private var interactionListener: PostInteractionListener? = null
    private var currentUserId: String? = null

    // لتعيين الـ listener من الـ Activity أو الـ Fragment المستدعي
    fun setPostInteractionListener(listener: PostInteractionListener?) {
        this.interactionListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getArguments() != null) {
            post =
                getArguments().getParcelable<PostModel?>(BottomSheetPostOptions.Companion.ARG_POST)
        }
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // استخدام ملف التخطيط bottom_sheet_post_options.xml
        val view: View? = inflater.inflate(R.layout.bottom_sheet_post_options, container, false)
        // يمكنك إضافة المزيد من تخصيصات التصميم هنا
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (post == null || interactionListener == null) {
            dismiss() // أغلق الـ BottomSheet إذا لم تكن البيانات أو الـ listener متاحة
            return
        }

        val isAuthor = currentUserId != null && currentUserId == post.getAuthorId()

        // الحصول على مراجع للأزرار من ملف التخطيط
        // افترض أن لديك هذه المعرفات في bottom_sheet_post_options.xml
        val optionPin =
            view.findViewById<View?>(R.id.option_pin_post_bs) // ستحتاج لإضافة هذا ID في XML
        val optionEdit = view.findViewById<View?>(R.id.option_edit_post_bs)
        val optionDelete = view.findViewById<View?>(R.id.option_delete_post_bs)
        val optionCopyLink = view.findViewById<View?>(R.id.option_copy_link_bs)
        val optionShare = view.findViewById<View?>(R.id.option_share_post_bs)
        val optionSave = view.findViewById<View?>(R.id.option_save_post_bs)
        val optionEditPrivacy = view.findViewById<View?>(R.id.option_edit_privacy_bs)
        val optionReport = view.findViewById<View?>(R.id.option_report_post_bs)
        // يمكنك استخدام MaterialButton إذا كانت لديك أيقونات ونصوص
        // مثال لزر الحفظ:
        val saveButton =
            view.findViewById<Button?>(R.id.option_save_post_bs) // افترض أن هذا هو ID زر الحفظ

        // التحكم في ظهور الخيارات
        if (optionPin != null) optionPin.setVisibility(if (isAuthor) View.VISIBLE else View.GONE)
        if (optionEdit != null) optionEdit.setVisibility(if (isAuthor) View.VISIBLE else View.GONE)
        if (optionDelete != null) optionDelete.setVisibility(if (isAuthor) View.VISIBLE else View.GONE)
        if (optionEditPrivacy != null) optionEditPrivacy.setVisibility(if (isAuthor) View.VISIBLE else View.GONE)
        if (optionReport != null) optionReport.setVisibility(if (isAuthor) View.GONE else View.VISIBLE)

        // تحديث نص زر الحفظ
        if (saveButton != null) {
            saveButton.setText(if (post.isBookmarked()) "إلغاء حفظ المنشور" else "حفظ المنشور")
        }


        // تعيين مستمعي النقر
        if (optionPin != null) {
            optionPin.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener.onTogglePinPostClicked(post)
                dismiss()
            })
        }
        if (optionEdit != null) {
            optionEdit.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener.onEditPost(post)
                dismiss()
            })
        }
        if (optionDelete != null) {
            optionDelete.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener.onDeletePost(post)
                dismiss()
            })
        }
        if (optionCopyLink != null) {
            optionCopyLink.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener.onCopyLink(post)
                dismiss()
            })
        }
        if (optionShare != null) {
            optionShare.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener.onSharePost(post)
                dismiss()
            })
        }
        if (optionSave != null) {
            optionSave.setOnClickListener(View.OnClickListener { v: View? ->
                // ★★★ هذا هو السطر الذي يسبب الخطأ، تم تعديله ★★★
                interactionListener.onBookmarkClicked(post)
                dismiss()
            })
        }
        if (optionEditPrivacy != null) {
            optionEditPrivacy.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener.onEditPostPrivacy(post)
                dismiss()
            })
        }
        if (optionReport != null) {
            optionReport.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener.onReportPost(post)
                dismiss()
            })
        }
    }

    companion object {
        const val TAG: String = "BottomSheetPostOptions"
        private const val ARG_POST = "post_model"

        // دالة لإنشاء نسخة جديدة من الـ BottomSheet مع تمرير بيانات المنشور
        fun newInstance(post: PostModel?): BottomSheetPostOptions {
            val fragment = BottomSheetPostOptions()
            val args: Bundle = Bundle()
            args.putParcelable(BottomSheetPostOptions.Companion.ARG_POST, post)
            fragment.setArguments(args)
            return fragment
        }
    }
}