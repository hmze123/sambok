package com.spidroid.starry.ui.messages;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupMenu;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.spidroid.starry.R;
import com.spidroid.starry.models.ChatMessage;

public class ContextMenuDialog extends PopupMenu {
  private final ChatMessage message;
  private final MessageContextMenuListener listener;

  public ContextMenuDialog(
      Context context, View anchor, ChatMessage message, MessageContextMenuListener listener) {
    super(context, anchor);
    this.message = message;
    this.listener = listener;
    initialize();
  }

  private void initialize() {
    // Inflate your menu resource
    getMenuInflater().inflate(R.menu.message_context_menu, getMenu());

    // Set visibility based on ownership
    Menu menu = getMenu();
    boolean isCurrentUser = message.getSenderId().equals(FirebaseAuth.getInstance().getUid());
    menu.findItem(R.id.menu_edit).setVisible(isCurrentUser);

    // Set click listener
    setOnMenuItemClickListener(
        item -> {
          handleMenuItemClick(item.getItemId());
          return true;
        });
  }

  private void handleMenuItemClick(int itemId) {
    if (itemId == R.id.menu_reply) {
      listener.onReplySelected();
    } else if (itemId == R.id.menu_edit) {
      listener.onEditSelected();
    } else if (itemId == R.id.menu_delete) {
      listener.onDeleteSelected();
    } else if (itemId == R.id.menu_reaction) {
      listener.onReactionSelected();
    } else if (itemId == R.id.menu_report) {
      listener.onReportSelected();
    }
  }
}
