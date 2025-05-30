package com.spidroid.starry.ui.messages;

import com.spidroid.starry.models.ChatMessage; // تأكد من وجود هذا الاستيراد إذا لم يكن موجودًا

public interface MessageContextMenuListener {
  void onReplySelected();

  void onEditSelected();

  void onDeleteSelected();

  void onReactionSelected();

  void onReportSelected(ChatMessage message); // تم إضافة بارامتر ChatMessage هنا
}