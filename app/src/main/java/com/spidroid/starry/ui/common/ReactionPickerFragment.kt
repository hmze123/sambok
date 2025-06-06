package com.spidroid.starry.ui.common

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.spidroid.starry.R

// أو الحزمة المناسبة لمشروعك

// تأكد من أن R مستوردة بشكل صحيح
class ReactionPickerFragment : BottomSheetDialogFragment() {
    // واجهة للاستماع إلى اختيار الريأكشن
    interface ReactionListener {
        fun onReactionSelected(emojiUnicode: String?)
    }

    private var reactionListener: ReactionListener? = null

    // دالة لتعيين الـ Listener من الـ Fragment أو الـ Activity المستدعي
    fun setReactionListener(listener: ReactionListener?) {
        this.reactionListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // استخدام ملف التخطيط reaction_picker.xml
        return inflater.inflate(R.layout.reaction_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reactionGrid = view.findViewById<GridLayout>(R.id.reaction_grid)

        // يمكنك الحصول على قائمة الرموز التعبيرية من strings.xml أو تعريفها هنا
        // الترتيب مهم إذا كنت ستعتمد على الـ tags في reaction_picker.xml
        val emojis = arrayOf<String?>(
            "❤️",
            "😂",
            "😮",
            "😢",
            "👍",
            "👎"
        ) // نفس الرموز الموجودة في reaction_picker.xml
        val emojiTags =
            arrayOf<String?>("heart", "laugh", "wow", "sad", "like", "dislike") // الـ tags المقابلة

        if (reactionGrid.getChildCount() == emojis.size) { // تأكد من أن عدد الـ TextViews يطابق عدد الـ emojis
            for (i in 0..<reactionGrid.getChildCount()) {
                val child = reactionGrid.getChildAt(i)
                if (child is TextView) {
                    val emojiView = child
                    val selectedEmoji =
                        emojiView.getText().toString() // الحصول على الـ emoji مباشرة من النص

                    // يمكنك التحقق من الـ tag إذا أردت التأكد أكثر
                    // String tag = emojiView.getTag() != null ? emojiView.getTag().toString() : "";
                    // if (tag.equals(emojiTags[i])) { ... }
                    emojiView.setOnClickListener(View.OnClickListener { v: View? ->
                        if (reactionListener != null) {
                            reactionListener!!.onReactionSelected(selectedEmoji)
                        }
                        dismiss() // إغلاق الـ BottomSheet بعد الاختيار
                    })
                }
            }
        } else {
            // يمكنك إضافة Log هنا إذا كان عدد العناصر غير متطابق كتحذير
            Log.w(TAG, "Mismatch between defined emojis and TextViews in reaction_picker.xml")
        }
    }

    // (اختياري) يمكنك إضافة المزيد من التخصيصات هنا، مثل تغيير ارتفاع الـ BottomSheet
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // يمكنك إضافة المزيد من الإعدادات للـ dialog هنا إذا أردت
        return dialog
    }

    companion object {
        const val TAG: String = "ReactionPickerFragment"
    }
}