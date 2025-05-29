package com.spidroid.starry.adapters;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.models.Chat;
import com.spidroid.starry.models.ChatMessage;
import com.spidroid.starry.models.UserModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Objects;

public class ChatsAdapter extends ListAdapter<Chat, ChatsAdapter.ChatViewHolder> {

  private final FirebaseFirestore db = FirebaseFirestore.getInstance();
  private final String currentUserId;
  private final ChatClickListener listener;

  public interface ChatClickListener {
    void onChatClick(Chat chat);
  }

  public ChatsAdapter(ChatClickListener listener) {
    super(DIFF_CALLBACK);
    this.listener = listener;
    currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
  }

  private static final DiffUtil.ItemCallback<Chat> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<Chat>() {
        @Override
        public boolean areItemsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
          return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
          return oldItem.getLastMessageTimestamp() == newItem.getLastMessageTimestamp()
              && oldItem.getUnreadCounts().equals(newItem.getUnreadCounts())
              && Objects.equals(oldItem.getLastMessage(), newItem.getLastMessage());
        }
      };

  @NonNull
  @Override
  public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
    return new ChatViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
    Chat chat = getItem(position);
    holder.bind(chat, listener);
  }

  class ChatViewHolder extends RecyclerView.ViewHolder {
    private final CircleImageView ivAvatar;
    private final TextView tvUserName;
    private final ImageView ivVerified;
    private final TextView tvLastMessage;
    private final TextView tvTime;
    private final TextView tvUnreadCount;
    private String currentChatId;

    public ChatViewHolder(@NonNull View itemView) {
      super(itemView);
      ivAvatar = itemView.findViewById(R.id.ivAvatar);
      tvUserName = itemView.findViewById(R.id.tvUserName);
      ivVerified = itemView.findViewById(R.id.ivVerified);
      tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
      tvTime = itemView.findViewById(R.id.tvTime);
      tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
    }

    void bind(Chat chat, ChatClickListener listener) {
      currentChatId = chat.getId();
      itemView.setOnClickListener(v -> listener.onChatClick(chat));

      // Handle time display
      if (chat.getLastMessageTime() != null) {
        tvTime.setText(
            DateUtils.formatDateTime(
                itemView.getContext(),
                chat.getLastMessageTime().getTime(),
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_TIME));
      } else {
        tvTime.setText("");
      }

      // Bind common elements
      bindUnreadCount(chat);
      bindLastMessage(chat);

      // Bind chat-specific elements
      if (chat.isGroup()) {
        bindGroupChat(chat);
      } else {
        bindDirectChat(chat);
      }
    }

    private void bindDirectChat(Chat chat) {
      for (String participant : chat.getParticipants()) {
        if (!participant.equals(currentUserId)) {
          fetchUserInfo(participant);
          break;
        }
      }
    }

    private void fetchUserInfo(String userId) {
      db.collection("users")
          .document(userId)
          .get()
          .addOnSuccessListener(
              documentSnapshot -> {
                UserModel user = documentSnapshot.toObject(UserModel.class);
                if (user != null && currentChatId.equals(getItem(getAdapterPosition()).getId())) {
                  updateUserInfo(user);
                }
              })
          .addOnFailureListener(e -> Log.e("ChatsAdapter", "Error fetching user info", e));
    }

    private void updateUserInfo(UserModel user) {
      tvUserName.setText(user.getDisplayName());
      ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
      Glide.with(itemView)
          .load(user.getProfileImageUrl())
          .placeholder(R.drawable.ic_default_avatar)
          .error(R.drawable.ic_default_avatar)
          .into(ivAvatar);
    }

    private void bindLastMessage(Chat chat) {
      String message = chat.getLastMessage();
      String messageType = chat.getLastMessageType();

      if (message == null) {
        tvLastMessage.setText(R.string.no_messages);
        return;
      }

      if (messageType != null) {
        switch (messageType) {
          case ChatMessage.TYPE_IMAGE:
            tvLastMessage.setText(R.string.photo_message);
            break;
          case ChatMessage.TYPE_VIDEO:
            tvLastMessage.setText(R.string.video_message);
            break;
          case ChatMessage.TYPE_FILE:
            tvLastMessage.setText(R.string.file_message);
            break;
          default:
            tvLastMessage.setText(message);
        }
      } else {
        tvLastMessage.setText(message);
      }
    }

    private void bindUnreadCount(Chat chat) {
      int unread = chat.getUnreadCounts().getOrDefault(currentUserId, 0);
      if (unread > 0) {
        tvUnreadCount.setVisibility(View.VISIBLE);
        tvUnreadCount.setText(String.valueOf(unread));
      } else {
        tvUnreadCount.setVisibility(View.GONE);
      }
    }

    private void bindGroupChat(Chat chat) {
      tvUserName.setText(chat.getGroupName());
      ivVerified.setVisibility(View.GONE);
      Glide.with(itemView)
          .load(chat.getGroupImage())
          .placeholder(R.drawable.ic_default_group)
          .error(R.drawable.ic_default_group)
          .into(ivAvatar);
    }
  }
}
