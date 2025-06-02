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
import com.spidroid.starry.models.Chat; // ★ تأكد من هذا الاستيراد
import com.spidroid.starry.models.ChatMessage;
import com.spidroid.starry.models.UserModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Objects; // ★ تأكد من هذا الاستيراد

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
    // تأكد من أن المستخدم مسجل دخوله قبل محاولة الحصول على UID
    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
      this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    } else {
      // يمكنك معالجة هذه الحالة هنا، مثلاً، رمي استثناء أو تسجيل خطأ
      // أو تعيين قيمة افتراضية إذا كان ذلك مناسبًا لسياق تطبيقك
      this.currentUserId = ""; // أو null، لكن تأكد من معالجة NullPointerException لاحقًا
      Log.e("ChatsAdapter", "Current user is null, cannot get UID.");
      // قد ترغب في منع إنشاء الـ adapter إذا لم يكن هناك مستخدم حالي
    }
  }

  private static final DiffUtil.ItemCallback<Chat> DIFF_CALLBACK =
          new DiffUtil.ItemCallback<Chat>() {
            @Override
            public boolean areItemsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
              return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Chat oldItem, @NonNull Chat newItem) {
              // --- ★★ السطر الذي تم تعديله هنا ★★ ---
              // تأكد أن دالة getLastMessageTimestamp() موجودة في كلاس Chat وتُرجع long
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
    private String currentChatId; // لتتبع الدردشة الحالية التي يعرضها الـ ViewHolder

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
      currentChatId = chat.getId(); // حفظ معرّف الدردشة الحالي
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
        // تأكد من أن currentUserId ليس فارغًا قبل المقارنة
        if (currentUserId != null && !participant.equals(currentUserId)) {
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
                        if (documentSnapshot.exists()) {
                          UserModel user = documentSnapshot.toObject(UserModel.class);
                          // تحقق من أن الـ ViewHolder لا يزال يعرض نفس الدردشة قبل تحديث الواجهة
                          if (user != null && currentChatId != null && getItem(getAdapterPosition()) != null && currentChatId.equals(getItem(getAdapterPosition()).getId())) {
                            updateUserInfo(user);
                          }
                        } else {
                          Log.w("ChatsAdapter", "User document not found for ID: " + userId);
                          // يمكنك هنا تعيين قيم افتراضية أو إخفاء العنصر إذا لم يتم العثور على المستخدم
                          tvUserName.setText("Unknown User");
                          ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                          ivVerified.setVisibility(View.GONE);
                        }
                      })
              .addOnFailureListener(e -> Log.e("ChatsAdapter", "Error fetching user info for ID: " + userId, e));
    }

    private void updateUserInfo(UserModel user) {
      tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
      ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
      if (itemView.getContext() != null) { // تحقق من أن السياق متاح
        Glide.with(itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAvatar);
      }
    }

    private void bindLastMessage(Chat chat) {
      String message = chat.getLastMessage();
      String messageType = chat.getLastMessageType();

      if (message == null) {
        tvLastMessage.setText(R.string.no_messages); // استخدم string resource
        return;
      }

      if (messageType != null) {
        switch (messageType) {
          case ChatMessage.TYPE_IMAGE:
            tvLastMessage.setText(R.string.photo_message); // استخدم string resource
            break;
          case ChatMessage.TYPE_VIDEO:
            tvLastMessage.setText(R.string.video_message); // استخدم string resource
            break;
          case ChatMessage.TYPE_FILE:
            tvLastMessage.setText(R.string.file_message); // استخدم string resource
            break;
          default:
            tvLastMessage.setText(message);
        }
      } else {
        tvLastMessage.setText(message);
      }
    }

    private void bindUnreadCount(Chat chat) {
      // تأكد من أن currentUserId ليس فارغًا
      Integer unread = chat.getUnreadCounts() != null && currentUserId != null ? chat.getUnreadCounts().getOrDefault(currentUserId, 0) : 0;
      if (unread > 0) {
        tvUnreadCount.setVisibility(View.VISIBLE);
        tvUnreadCount.setText(String.valueOf(unread));
      } else {
        tvUnreadCount.setVisibility(View.GONE);
      }
    }

    private void bindGroupChat(Chat chat) {
      tvUserName.setText(chat.getGroupName());
      ivVerified.setVisibility(View.GONE); // المجموعات عادة لا تكون "موثقة" بنفس طريقة المستخدمين
      if (itemView.getContext() != null) { // تحقق من أن السياق متاح
        Glide.with(itemView.getContext())
                .load(chat.getGroupImage())
                .placeholder(R.drawable.ic_default_group) // أيقونة افتراضية للمجموعات
                .error(R.drawable.ic_default_group)
                .into(ivAvatar);
      }
    }
  }
}