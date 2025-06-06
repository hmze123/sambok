package com.spidroid.starry.ui.messages

import com.spidroid.starry.models.ChatMessage

// تأكد من وجود هذا الاستيراد إذا لم يكن موجودًا
interface MessageContextMenuListener {
    fun onReplySelected()

    fun onEditSelected()

    fun onDeleteSelected()

    fun onReactionSelected()

    fun onReportSelected(message: ChatMessage?) // تم إضافة بارامتر ChatMessage هنا
}