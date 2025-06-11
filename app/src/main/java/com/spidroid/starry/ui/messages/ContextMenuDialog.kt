// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/messages/ContextMenuDialog.kt
package com.spidroid.starry.ui.messages

import android.content.ClipData // ✨ تم إضافة هذا الاستيراد
import android.content.ClipboardManager // ✨ تم إضافة هذا الاستيراد
import android.content.Context // ✨ تم إضافة هذا الاستيراد
import android.os.Bundle // ✨ تم إضافة هذا الاستيراد
import android.view.LayoutInflater // ✨ تم إضافة هذا الاستيراد
import android.view.View
import android.view.ViewGroup // ✨ تم إضافة هذا الاستيراد
import android.widget.TextView // ✨ تم إضافة هذا الاستيراد
import android.widget.Toast // ✨ تم إضافة هذا الاستيراد
import com.google.android.material.bottomsheet.BottomSheetDialogFragment // ✨ تم إضافة هذا الاستيراد
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R // ✨ تم إضافة هذا الاستيراد
import com.spidroid.starry.models.ChatMessage

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContextMenuDialog : BottomSheetDialogFragment() {
    private var message: ChatMessage? = null
    private var listener: MessageContextMenuListener? = null
    private var currentUserId: String? = null

    // Method to set the listener
    fun setListener(listener: MessageContextMenuListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        message = arguments?.getParcelable(ARG_MESSAGE) // Use arguments property
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Use uid property
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.message_context_menu, container, false) // ✨ تم تصحيح R.layout

        // Find menu items
        val tvCopy = view.findViewById<TextView>(R.id.menu_copy) // ✨ تم تصحيح R.id
        val tvDelete = view.findViewById<TextView>(R.id.menu_delete) // ✨ تم تصحيح R.id
        val tvEdit = view.findViewById<TextView>(R.id.menu_edit) // ✨ تم تصحيح R.id
        val tvReport = view.findViewById<TextView>(R.id.menu_report) // ✨ تم تصحيح R.id
        val tvReply = view.findViewById<TextView>(R.id.menu_reply) // ✨ تم تصحيح R.id
        val tvTranslate = view.findViewById<TextView>(R.id.menu_translate) // ✨ تم تصحيح R.id و R.id

        // Set visibility based on conditions
        // Only sender can delete or edit
        val isSender = message?.senderId == currentUserId
        tvDelete.visibility = if (isSender) View.VISIBLE else View.GONE
        tvEdit.visibility = if (isSender) View.VISIBLE else View.GONE
        tvReport.visibility = if (isSender) View.GONE else View.VISIBLE // Can't report your own message

        // Set click listeners
        tvCopy.setOnClickListener {
            message?.content?.let { content ->
                val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("Message", content)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }

        tvDelete.setOnClickListener {
            message?.let { msg -> listener?.onDeleteMessage(msg) }
            dismiss()
        }

        tvEdit.setOnClickListener {
            message?.let { msg -> listener?.onEditMessage(msg) }
            dismiss()
        }

        tvReport.setOnClickListener {
            message?.let { msg -> listener?.onReportMessage(msg) }
            dismiss()
        }

        tvReply.setOnClickListener {
            message?.let { msg -> listener?.onReplyToMessage(msg) }
            dismiss()
        }

        tvTranslate.setOnClickListener {
            message?.let { msg -> listener?.onTranslateMessage(msg) }
            dismiss()
        }

        return view
    }

    companion object {
        private const val ARG_MESSAGE = "message"

        fun newInstance(message: ChatMessage): ContextMenuDialog {
            val fragment = ContextMenuDialog()
            val args = Bundle()
            args.putParcelable(ARG_MESSAGE, message)
            fragment.arguments = args
            return fragment
        }
    }
}