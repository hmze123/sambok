// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/messages/NewChatBottomSheetDialog.kt
package com.spidroid.starry.ui.messages

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.spidroid.starry.R
import com.spidroid.starry.activities.ChatActivity
import com.spidroid.starry.activities.CreateGroupActivity
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.UserModel
import de.hdodenhof.circleimageview.CircleImageView
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.HashSet
import java.util.Locale
import kotlin.math.min

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewChatBottomSheetDialog : BottomSheetDialogFragment() { // ✨ تم تصحيح التهجئة

    private var adapter: UsersAdapter? = null
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog { // ✨ تم تصحيح توقيع Override
        val dialog: BottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog // ✨ تم تصحيح التهجئة Cast
        val view: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_chat, null)
        dialog.setContentView(view)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupViews(view)
        loadUsers()
        return dialog
    }

    private fun setupViews(view: View) {
        // Search functionality
        val etSearch: TextInputEditText = view.findViewById(R.id.et_search)
        etSearch.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    filterUsers(s.toString())
                }
            })

        // Button click listeners
        view.findViewById<View>(R.id.btn_create_group)
            .setOnClickListener {
                // Handle group creation
                startActivity(Intent(requireContext(), CreateGroupActivity::class.java))
                dismiss()
            }
    }

    private fun loadUsers() {
        val currentUser: FirebaseUser? = auth?.currentUser // ✨ استخدام safe call
        if (currentUser == null) return

        // First get current user's following and followers list
        db?.collection("users")
            ?.document(currentUser.uid) // ✨ استخدام .uid
            ?.get()
            ?.addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val currentUserModel: UserModel? = documentSnapshot.toObject(UserModel::class.java)
                    if (currentUserModel != null) {
                        // Combine followers and following user IDs
                        val userIds: MutableSet<String> = HashSet()
                        userIds.addAll(currentUserModel.followers.keys) // ✨ استخدام .keys
                        userIds.addAll(currentUserModel.following.keys) // ✨ استخدام .keys

                        if (userIds.isEmpty()) {
                            // Show empty state if needed
                            return@addOnSuccessListener
                        }

                        // Split into chunks of 10 due to Firestore query limitations
                        val chunks = chunkList(ArrayList(userIds), 10)
                        val allUsers: MutableList<UserModel> = ArrayList()

                        for (chunk in chunks) {
                            db?.collection("users")
                                ?.whereIn("userId", chunk as List<String>) // ✨ التأكيد على النوع
                                ?.get()
                                ?.addOnSuccessListener { querySnapshot ->
                                    val chunkUsers: MutableList<UserModel> = querySnapshot.toObjects(UserModel::class.java)
                                    allUsers.addAll(chunkUsers)

                                    // Only update when all chunks are processed
                                    if (allUsers.size >= userIds.size) {
                                        // Remove duplicates and sort
                                        val uniqueUsers: MutableSet<UserModel> = HashSet(allUsers)
                                        val sortedUsers: MutableList<UserModel> = ArrayList(uniqueUsers)
                                        Collections.sort(sortedUsers,
                                            Comparator { u1: UserModel, u2: UserModel -> // ✨ تم تصحيح Comparator
                                                u1.displayName?.compareTo(u2.displayName ?: "", ignoreCase = true) ?: 0 // ✨ استخدام .displayName ومعالجة nullability
                                            })

                                        updateUserList(sortedUsers)
                                    }
                                }
                        }
                    }
                }
            }
    }

    private fun updateUserList(users: MutableList<UserModel>) {
        requireActivity()
            .runOnUiThread {
                adapter = UsersAdapter(users)
                val rvUsers: RecyclerView? = dialog?.findViewById(R.id.rv_users) // ✨ استخدام dialog?.findViewById
                if (rvUsers != null) {
                    rvUsers.layoutManager = LinearLayoutManager(requireContext()) // ✨ استخدام .layoutManager
                    rvUsers.adapter = adapter
                }
            }
    }

    // Helper to split list into chunks
    private fun chunkList(list: ArrayList<String>, chunkSize: Int): MutableList<MutableList<String>> { // ✨ تم تصحيح النوع
        val chunks: MutableList<MutableList<String>> = ArrayList()
        var i = 0
        while (i < list.size) {
            chunks.add(list.subList(i, min(list.size, (i + chunkSize))) as MutableList<String>)
            i += chunkSize
        }
        return chunks
    }

    private fun filterUsers(query: String?) {
        adapter?.filter?.filter(query) // ✨ استخدام safe call
    }

    private inner class UsersAdapter(private val users: MutableList<UserModel>) :
        RecyclerView.Adapter<UsersAdapter.ViewHolder>(), Filterable { // ✨ تم تصحيح التهيئة والوراثة

        private var filteredUsers: MutableList<UserModel> = ArrayList(users) // ✨ تم تصحيح التهيئة


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false) // ✨ استخدام parent.context
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user: UserModel = filteredUsers[position]
            holder.bind(user)
        }

        override fun getItemCount(): Int { // ✨ تم تصحيح Override
            return filteredUsers.size
        }

        override fun getFilter(): Filter { // ✨ تم تصحيح Override
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence): FilterResults {
                    val filtered: MutableList<UserModel> = ArrayList()
                    val query = constraint.toString().lowercase(Locale.getDefault())

                    for (user in users) {
                        if (user.displayName?.lowercase(Locale.getDefault())?.contains(query) == true // ✨ استخدام .displayName
                            || user.username.lowercase(Locale.getDefault()).contains(query) // ✨ استخدام .username
                        ) {
                            filtered.add(user)
                        }
                    }

                    val results = FilterResults()
                    results.values = filtered
                    return results
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                    filteredUsers.clear()
                    filteredUsers.addAll(results.values as Collection<UserModel>) // ✨ تم تصحيح Cast
                    notifyDataSetChanged()
                }
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivAvatar: CircleImageView
            private val tvName: TextView

            init {
                ivAvatar = itemView.findViewById(R.id.iv_avatar)
                tvName = itemView.findViewById(R.id.tv_name)

                itemView.setOnClickListener {
                    val user: UserModel = filteredUsers[bindingAdapterPosition] // ✨ استخدام operator[]
                    startChatWithUser(user)
                }
            }

            fun bind(user: UserModel) {
                tvName.text = user.displayName ?: user.username // ✨ استخدام .text
                Glide.with(itemView)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(ivAvatar)
            }
        }
    }

    private fun startChatWithUser(user: UserModel) {
        val currentUser: FirebaseUser? = auth?.currentUser
        if (currentUser == null) return

        val currentUserId: String = currentUser.uid
        val selectedUserId: String = user.userId

        // Generate unique chat ID (sorted combination of user IDs)
        val chatId = generateChatId(currentUserId, selectedUserId)

        // Check if chat already exists
        db?.collection("chats")
            ?.document(chatId)
            ?.get()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!task.result.exists()) {
                        // Create new chat document
                        val chat: MutableMap<String, Any?> = HashMap()
                        chat["participants"] = Arrays.asList(currentUserId, selectedUserId)
                        chat["isGroup"] = false
                        chat["createdAt"] = Date()
                        chat["lastMessage"] = ""
                        chat["lastMessageTime"] = Date()

                        db?.collection("chats")
                            ?.document(chatId)
                            ?.set(chat)
                            ?.addOnSuccessListener { openChatActivity(chatId) }
                            ?.addOnFailureListener { showError() }
                    } else {
                        openChatActivity(chatId)
                    }
                }
            }
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