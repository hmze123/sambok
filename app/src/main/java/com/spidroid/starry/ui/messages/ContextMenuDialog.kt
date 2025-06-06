package com.spidroid.starry.ui.messages

import com.google.firebase.auth.FirebaseAuth

class ContextMenuDialog(
    context: android.content.Context?,
    anchor: android.view.View?,
    message: ChatMessage,
    listener: MessageContextMenuListener
) : android.widget.PopupMenu(context, anchor) {
    private val message: ChatMessage
    private val listener: MessageContextMenuListener

    init {
        this.message = message
        this.listener = listener
        initialize()
    }

    private fun initialize() {
        // Inflate your menu resource
        getMenuInflater().inflate(R.menu.message_context_menu, getMenu())

        // Set visibility based on ownership
        val menu = getMenu()
        val isCurrentUser = message.getSenderId() == FirebaseAuth.getInstance().getUid()
        menu.findItem(R.id.menu_edit).setVisible(isCurrentUser)

        // Set click listener
        setOnMenuItemClickListener(
            android.widget.PopupMenu.OnMenuItemClickListener { item: android.view.MenuItem? ->
                handleMenuItemClick(item!!.getItemId())
                true
            })
    }

    private fun handleMenuItemClick(itemId: Int) {
        if (itemId == R.id.menu_reply) {
            listener.onReplySelected()
        } else if (itemId == R.id.menu_edit) {
            listener.onEditSelected()
        } else if (itemId == R.id.menu_delete) {
            listener.onDeleteSelected()
        } else if (itemId == R.id.menu_reaction) {
            listener.onReactionSelected()
        } else if (itemId == R.id.menu_report) {
            listener.onReportSelected(message) // تم التعديل هنا: تمرير كائن الرسالة
        }
    }
}