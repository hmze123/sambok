package com.spidroid.starry.adapters

// ★ تأكد من هذا الاستيراد
// ★ تأكد من هذا الاستيراد
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class ChatsAdapter(private val listener: ChatClickListener) :
    ListAdapter<Chat, ChatViewHolder?>(ChatsAdapter.Companion.DIFF_CALLBACK) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val currentUserId: String? = null

    interface ChatClickListener {
        fun onChatClick(chat: Chat?)
    }

    init {
        // تأكد من أن المستخدم مسجل دخوله قبل محاولة الحصول على UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid()
        } else {
            // يمكنك معالجة هذه الحالة هنا، مثلاً، رمي استثناء أو تسجيل خطأ
            // أو تعيين قيمة افتراضية إذا كان ذلك مناسبًا لسياق تطبيقك
            this.currentUserId = "" // أو null، لكن تأكد من معالجة NullPointerException لاحقًا
            Log.e("ChatsAdapter", "Current user is null, cannot get UID.")
            // قد ترغب في منع إنشاء الـ adapter إذا لم يكن هناك مستخدم حالي
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view: View =
            LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat: Chat = getItem(position)
        holder.bind(chat, listener)
    }

    internal inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: CircleImageView
        private val tvUserName: TextView
        private val ivVerified: ImageView
        private val tvLastMessage: TextView
        private val tvTime: TextView
        private val tvUnreadCount: TextView
        private var currentChatId: String? =
            null // لتتبع الدردشة الحالية التي يعرضها الـ ViewHolder

        init {
            ivAvatar = itemView.findViewById<CircleImageView>(R.id.ivAvatar)
            tvUserName = itemView.findViewById<TextView>(R.id.tvUserName)
            ivVerified = itemView.findViewById<ImageView>(R.id.ivVerified)
            tvLastMessage = itemView.findViewById<TextView>(R.id.tvLastMessage)
            tvTime = itemView.findViewById<TextView>(R.id.tvTime)
            tvUnreadCount = itemView.findViewById<TextView>(R.id.tvUnreadCount)
        }

        fun bind(chat: Chat, listener: ChatClickListener) {
            currentChatId = chat.getId() // حفظ معرّف الدردشة الحالي
            itemView.setOnClickListener(View.OnClickListener { v: View? -> listener.onChatClick(chat) })

            // Handle time display
            if (chat.getLastMessageTime() != null) {
                tvTime.setText(
                    DateUtils.formatDateTime(
                        itemView.getContext(),
                        chat.getLastMessageTime().getTime(),
                        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_TIME
                    )
                )
            } else {
                tvTime.setText("")
            }

            // Bind common elements
            bindUnreadCount(chat)
            bindLastMessage(chat)

            // Bind chat-specific elements
            if (chat.isGroup()) {
                bindGroupChat(chat)
            } else {
                bindDirectChat(chat)
            }
        }

        private fun bindDirectChat(chat: Chat) {
            for (participant in chat.getParticipants()) {
                // تأكد من أن currentUserId ليس فارغًا قبل المقارنة
                if (currentUserId != null && participant != currentUserId) {
                    fetchUserInfo(participant)
                    break
                }
            }
        }

        private fun fetchUserInfo(userId: String?) {
            db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(
                    { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val user: UserModel? = documentSnapshot.toObject(UserModel::class.java)
                            // تحقق من أن الـ ViewHolder لا يزال يعرض نفس الدردشة قبل تحديث الواجهة
                            if (user != null && currentChatId != null && getItem(getAdapterPosition()) != null && currentChatId == getItem(
                                    getAdapterPosition()
                                ).getId()
                            ) {
                                updateUserInfo(user)
                            }
                        } else {
                            Log.w("ChatsAdapter", "User document not found for ID: " + userId)
                            // يمكنك هنا تعيين قيم افتراضية أو إخفاء العنصر إذا لم يتم العثور على المستخدم
                            tvUserName.setText("Unknown User")
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                            ivVerified.setVisibility(View.GONE)
                        }
                    })
                .addOnFailureListener({ e ->
                    Log.e(
                        "ChatsAdapter",
                        "Error fetching user info for ID: " + userId,
                        e
                    )
                })
        }

        private fun updateUserInfo(user: UserModel) {
            tvUserName.setText(if (user.getDisplayName() != null) user.getDisplayName() else user.getUsername())
            ivVerified.setVisibility(if (user.isVerified()) View.VISIBLE else View.GONE)
            if (itemView.getContext() != null) { // تحقق من أن السياق متاح
                Glide.with(itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            }
        }

        private fun bindLastMessage(chat: Chat) {
            val message: String? = chat.getLastMessage()
            val messageType: String? = chat.getLastMessageType()

            if (message == null) {
                tvLastMessage.setText(R.string.no_messages) // استخدم string resource
                return
            }

            if (messageType != null) {
                when (messageType) {
                    ChatMessage.Companion.TYPE_IMAGE -> tvLastMessage.setText(R.string.photo_message) // استخدم string resource
                    ChatMessage.Companion.TYPE_VIDEO -> tvLastMessage.setText(R.string.video_message) // استخدم string resource
                    ChatMessage.Companion.TYPE_FILE -> tvLastMessage.setText(R.string.file_message) // استخدم string resource
                    else -> tvLastMessage.setText(message)
                }
            } else {
                tvLastMessage.setText(message)
            }
        }

        private fun bindUnreadCount(chat: Chat) {
            // تأكد من أن currentUserId ليس فارغًا
            val unread =
                if (chat.getUnreadCounts() != null && currentUserId != null) chat.getUnreadCounts()
                    .getOrDefault(currentUserId, 0) else 0
            if (unread > 0) {
                tvUnreadCount.setVisibility(View.VISIBLE)
                tvUnreadCount.setText(unread.toString())
            } else {
                tvUnreadCount.setVisibility(View.GONE)
            }
        }

        private fun bindGroupChat(chat: Chat) {
            tvUserName.setText(chat.getGroupName())
            ivVerified.setVisibility(View.GONE) // المجموعات عادة لا تكون "موثقة" بنفس طريقة المستخدمين
            if (itemView.getContext() != null) { // تحقق من أن السياق متاح
                Glide.with(itemView.getContext())
                    .load(chat.getGroupImage())
                    .placeholder(R.drawable.ic_default_group) // أيقونة افتراضية للمجموعات
                    .error(R.drawable.ic_default_group)
                    .into(ivAvatar)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<Chat?> =
            object : DiffUtil.ItemCallback<Chat?>() {
                override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                    return oldItem.getId() == newItem.getId()
                }

                override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                    // --- ★★ السطر الذي تم تعديله هنا ★★ ---
                    // تأكد أن دالة getLastMessageTimestamp() موجودة في كلاس Chat وتُرجع long
                    return oldItem.getLastMessageTimestamp() == newItem.getLastMessageTimestamp() && oldItem.getUnreadCounts() == newItem.getUnreadCounts()
                            && oldItem.getLastMessage() == newItem.getLastMessage()
                }
            }
    }
}