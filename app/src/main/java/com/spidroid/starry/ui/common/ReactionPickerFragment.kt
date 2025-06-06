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

class ReactionPickerFragment : BottomSheetDialogFragment() {

    private var interactionListener: PostInteractionListener? = null
    private var post: PostModel? = null

    // دالة لتعيين المستمع والمنشور
    fun setListener(listener: PostInteractionListener, post: PostModel) {
        this.interactionListener = listener
        this.post = post
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.reaction_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reactionGrid = view.findViewById<GridLayout>(R.id.reaction_grid)
        val emojis = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")

        reactionGrid.removeAllViews()
        emojis.forEach { emoji ->
            val emojiView = layoutInflater.inflate(R.layout.item_emoji_reaction, reactionGrid, false) as TextView
            emojiView.text = emoji
            emojiView.setOnClickListener {
                // استدعاء الدالة الصحيحة من الواجهة مع تمرير المنشور
                interactionListener?.onReactionSelected(post, emoji)
                dismiss()
            }
            reactionGrid.addView(emojiView)
        }
    }

    companion object {
        const val TAG: String = "ReactionPickerFragment"
        // يمكنك إبقاء newInstance إذا أردت تمرير البيانات عبر Arguments
        // ولكن الطريقة الحالية setListener أبسط في هذه الحالة
        fun newInstance(): ReactionPickerFragment {
            return ReactionPickerFragment()
        }
    }
}