package com.spidroid.starry.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.spidroid.starry.R
import com.spidroid.starry.adapters.PostInteractionListener
import com.spidroid.starry.models.PostModel

// أو الحزمة التي اخترتها

class ReactionPicker : BottomSheetDialogFragment() {
    private var post: PostModel? = null
    private var interactionListener: PostInteractionListener? = null

    // قائمة الإيموجيات المقترحة - يمكنك تخصيصها
    private val EMOJIS = arrayOf<String?>("❤️", "😂", "😮", "😢", "👍", "👎", "🔥", "🎉")

    // دالة لتعيين الـ listener من الـ Fragment/Activity المستدعي
    fun setPostInteractionListener(listener: PostInteractionListener?) {
        this.interactionListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getArguments() != null) {
            post = getArguments()!!.getParcelable<PostModel?>(ReactionPicker.Companion.ARG_POST)
        }
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

        if (post == null || interactionListener == null) {
            dismiss() // أغلق إذا لم يتم تمرير البيانات اللازمة
            return
        }

        val reactionGrid = view.findViewById<GridLayout>(R.id.reaction_grid)
        val title = view.findViewById<TextView?>(R.id.title)

        if (title != null) {
            // تأكد من إضافة هذا المورد إلى strings.xml
            // <string name="add_reaction_title">Add Reaction</string>
            title.setText(getString(R.string.add_reaction_title))
        }

        // إزالة أي إيموجيات قديمة إذا تم إعادة استخدام الـ view (على الرغم من أن BottomSheet عادةً ما يُنشئ view جديد)
        reactionGrid.removeAllViews()

        for (emoji in EMOJIS) {
            // نفخ (inflate) item_emoji_reaction.xml لكل إيموجي
            val emojiView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_emoji_reaction, reactionGrid, false) as TextView
            emojiView.setText(emoji)
            emojiView.setOnClickListener(View.OnClickListener { v: View? ->
                interactionListener!!.onEmojiSelected(post, emoji) // استدعاء دالة الـ listener
                dismiss() // إغلاق الـ BottomSheet بعد الاختيار
            })
            reactionGrid.addView(emojiView)
        }
    }

    companion object {
        private const val ARG_POST = "post_model_for_reaction"
        fun newInstance(post: PostModel?): ReactionPicker {
            val fragment = ReactionPicker()
            val args = Bundle()
            args.putParcelable(ReactionPicker.Companion.ARG_POST, post)
            fragment.setArguments(args)
            return fragment
        }
    }
}