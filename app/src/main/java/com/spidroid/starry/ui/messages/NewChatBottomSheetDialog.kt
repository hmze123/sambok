package com.spidroid.starry.ui.messages

import android.app.Dialog
import android.view.View
import android.widget.Filter
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Arrays
import java.util.Collections
import java.util.Date
import java.util.Locale
import kotlin.Any
import kotlin.CharSequence
import kotlin.Comparator
import kotlin.Int
import kotlin.String
import kotlin.math.min
import kotlin.toString

class NewChatBottomSheetDialog : BottomSheetDialogFragment() {
    private var adapter: UsersAdapter? = null
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: BottomSheetDialog =
            super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val view: View =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_chat, null)
        dialog.setContentView(view)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupViews(view)
        loadUsers()
        return dialog
    }

    private fun setupViews(view: View) {
        // Search functionality
        val etSearch: TextInputEditText = view.findViewById<TextInputEditText>(R.id.et_search)
        etSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable) {
                    filterUsers(s.toString())
                }
            })

        // Button click listeners
        view.findViewById<View?>(R.id.btn_create_group)
            .setOnClickListener(
                View.OnClickListener { v: View? ->
                    // Handle group creation
                    startActivity(Intent(requireContext(), CreateGroupActivity::class.java))
                    dismiss()
                })
    }

    private fun loadUsers() {
        val currentUser: FirebaseUser? = auth.getCurrentUser()
        if (currentUser == null) return

        // First get current user's following and followers list
        db.collection("users")
            .document(currentUser.getUid())
            .get()
            .addOnSuccessListener(
                { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val currentUserModel: UserModel? =
                            documentSnapshot.toObject(UserModel::class.java)
                        if (currentUserModel != null) {
                            // Combine followers and following user IDs
                            val userIds: MutableSet<String> = HashSet<String>()
                            userIds.addAll(currentUserModel.getFollowers().keys)
                            userIds.addAll(currentUserModel.getFollowing().keys)

                            if (userIds.isEmpty()) {
                                // Show empty state if needed
                                return@addOnSuccessListener
                            }

                            // Split into chunks of 10 due to Firestore query limitations
                            val chunks = chunkList(ArrayList<String?>(userIds), 10)
                            val allUsers: MutableList<UserModel?> = ArrayList<UserModel?>()

                            for (chunk in chunks) {
                                db.collection("users")
                                    .whereIn("userId", chunk)
                                    .get()
                                    .addOnSuccessListener(
                                        { querySnapshot ->
                                            val chunkUsers: MutableList<UserModel?> =
                                                querySnapshot.toObjects(UserModel::class.java)
                                            allUsers.addAll(chunkUsers)

                                            // Only update when all chunks are processed
                                            if (allUsers.size >= userIds.size) {
                                                // Remove duplicates and sort
                                                val uniqueUsers: MutableSet<UserModel> =
                                                    HashSet<UserModel>(allUsers)
                                                val sortedUsers: MutableList<UserModel> =
                                                    ArrayList<UserModel>(uniqueUsers)
                                                Collections.sort<UserModel?>(
                                                    sortedUsers,
                                                    Comparator { u1: UserModel?, u2: UserModel? ->
                                                        u1.getDisplayName()
                                                            .compareTo(
                                                                u2.getDisplayName(),
                                                                ignoreCase = true
                                                            )
                                                    })

                                                updateUserList(sortedUsers)
                                            }
                                        })
                            }
                        }
                    }
                })
    }

    private fun updateUserList(users: MutableList<UserModel>) {
        requireActivity()
            .runOnUiThread(
                Runnable {
                    adapter = UsersAdapter(users)
                    val rvUsers: RecyclerView? =
                        getDialog().findViewById<RecyclerView?>(R.id.rv_users)
                    if (rvUsers != null) {
                        rvUsers.setLayoutManager(LinearLayoutManager(requireContext()))
                        rvUsers.setAdapter(adapter)
                    }
                })
    }

    // Helper to split list into chunks
    private fun chunkList(
        list: MutableList<String?>,
        chunkSize: Int
    ): MutableList<MutableList<String?>?> {
        val chunks: MutableList<MutableList<String?>?> = ArrayList<MutableList<String?>?>()
        var i = 0
        while (i < list.size) {
            chunks.add(
                list.subList(
                    i,
                    min(list.size.toDouble(), (i + chunkSize).toDouble()).toInt()
                )
            )
            i += chunkSize
        }
        return chunks
    }

    private fun filterUsers(query: String?) {
        if (adapter != null) {
            adapter!!.filter.filter(query)
        }
    }

    private inner class UsersAdapter(users: MutableList<UserModel>) :
        RecyclerView.Adapter<UsersAdapter.ViewHolder?>() {
        private val users: MutableList<UserModel>
        private val filteredUsers: MutableList<UserModel>

        init {
            this.users = users
            this.filteredUsers = ArrayList<UserModel>(users)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false)
            return UsersAdapter.ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user: UserModel = filteredUsers.get(position)
            holder.bind(user)
        }

        val itemCount: Int
            get() = filteredUsers.size

        val filter: Filter
            get() = object : Filter() {
                override fun performFiltering(constraint: CharSequence): FilterResults {
                    val filtered: MutableList<UserModel?> =
                        ArrayList<UserModel?>()
                    val query =
                        constraint.toString().lowercase(Locale.getDefault())

                    for (user in users) {
                        if (user.getDisplayName().lowercase(Locale.getDefault())
                                .contains(query)
                            || user.getUsername().lowercase(Locale.getDefault())
                                .contains(query)
                        ) {
                            filtered.add(user)
                        }
                    }

                    val results = FilterResults()
                    results.values = filtered
                    return results
                }

                override fun publishResults(
                    constraint: CharSequence?,
                    results: FilterResults
                ) {
                    filteredUsers.clear()
                    filteredUsers.addAll(results.values as MutableList<UserModel?>?)
                    notifyDataSetChanged()
                }
            }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivAvatar: CircleImageView
            private val tvName: TextView

            init {
                ivAvatar = itemView.findViewById<CircleImageView>(R.id.iv_avatar)
                tvName = itemView.findViewById<TextView>(R.id.tv_name)

                itemView.setOnClickListener(
                    View.OnClickListener { v: View? ->
                        val user: UserModel = filteredUsers.get(getAdapterPosition())
                        startChatWithUser(user)
                    })
            }

            fun bind(user: UserModel) {
                tvName.setText(user.getDisplayName())
                Glide.with(itemView)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            }
        }
    }

    private fun startChatWithUser(user: UserModel) {
        val currentUser: FirebaseUser? = auth.getCurrentUser()
        if (currentUser == null) return

        val currentUserId: String = currentUser.getUid()
        val selectedUserId: String = user.getUserId()

        // Generate unique chat ID (sorted combination of user IDs)
        val chatId = generateChatId(currentUserId, selectedUserId)

        // Check if chat already exists
        db.collection("chats")
            .document(chatId)
            .get()
            .addOnCompleteListener(
                { task ->
                    if (task.isSuccessful()) {
                        if (!task.getResult().exists()) {
                            // Create new chat document
                            val chat: MutableMap<String?, Any?> = HashMap<String?, Any?>()
                            chat.put(
                                "participants",
                                Arrays.asList<String?>(currentUserId, selectedUserId)
                            )
                            chat.put("isGroup", false)
                            chat.put("createdAt", Date())
                            chat.put("lastMessage", "")
                            chat.put("lastMessageTime", Date())

                            db.collection("chats")
                                .document(chatId)
                                .set(chat)
                                .addOnSuccessListener({ aVoid -> openChatActivity(chatId) })
                                .addOnFailureListener({ e -> showError() })
                        } else {
                            openChatActivity(chatId)
                        }
                    }
                })
    }

    private fun generateChatId(uid1: String, uid2: String): String {
        // Ensure consistent order for chat ID
        return if (uid1.compareTo(uid2) < 0) uid1 + "_" + uid2 else uid2 + "_" + uid1
    }

    private fun openChatActivity(chatId: String?) {
        val intent: Intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("isGroup", false)
        startActivity(intent)
        dismiss()
    }

    private fun showError() {
        Toast.makeText(requireContext(), "Failed to start chat", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}
