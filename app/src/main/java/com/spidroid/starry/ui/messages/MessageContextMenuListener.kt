// app/src/main/java/com/spidroid/starry/ui/messages/MessageContextMenuListener.kt
package com.spidroid.starry.ui.messages

import com.spidroid.starry.models.ChatMessage

/**
 * Interface for handling context menu actions on messages.
 */
interface MessageContextMenuListener {
    fun onDeleteMessage(message: ChatMessage)
    fun onEditMessage(message: ChatMessage)
    fun onReportMessage(message: ChatMessage)
    fun onReplyToMessage(message: ChatMessage)
    fun onTranslateMessage(message: ChatMessage)
    // يمكنك إضافة المزيد من الدوال هنا إذا كنت بحاجة إليها
}