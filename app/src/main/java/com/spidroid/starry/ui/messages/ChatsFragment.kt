// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/messages/ChatsFragment.kt
package com.spidroid.starry.ui.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.spidroid.starry.R
import com.spidroid.starry.activities.ChatActivity
import com.spidroid.starry.adapters.ChatClickListener
import com.spidroid.starry.adapters.ChatsAdapter
import com.spidroid.starry.databinding.FragmentChatsBinding
import com.spidroid.starry.models.Chat
import com.spidroid.starry.models.UserModel
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.HashSet


class ChatsFragment : Fragment(), ChatClickListener {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var currentUser: FirebaseUser? = null

    private var chatsAdapter: ChatsAdapter? = null
    private var chatsListener: ListenerRegistration? = null
    private var usersMap: HashMap<String, UserModel> = HashMap() // لتخزين بيانات المستخدمين الآخرين

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth?.currentUser
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadChats()
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter(requireContext(), this)
        chatsAdapter?.setOtherUsers(usersMap)
        binding.chatsRecyclerView.layoutManager = LinearLayoutManager(context) // ✨ تم تصحيح ID View
        binding.chatsRecyclerView.adapter = chatsAdapter // ✨ تم تصحيح ID View

        binding.chatsRecyclerView.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() { // ✨ تم تصحيح ID View
            override fun onChanged() {
                super.onChanged()
                updateEmptyState()
            }
        })
    }

    private fun updateEmptyState() {
        val isEmpty = chatsAdapter?.itemCount == 0
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE // ✨ تم تصحيح ID View
        binding.chatsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE // ✨ تم تصحيح ID View
    }

    private fun loadChats() {
        val currentUserId = currentUser?.uid
        if (currentUserId == null) {
            binding.tvEmpty.text = "Please log in to see your chats." // ✨ تم تصحيح ID View
            binding.tvEmpty.visibility = View.VISIBLE // ✨ تم تصحيح ID View
            binding.pbLoadingChats.visibility = View.GONE // ✨ تم تصحيح ID View
            return
        }

        binding.chatsRecyclerView.visibility = View.GONE // ✨ تم تصحيح ID View
        binding.tvEmpty.visibility = View.GONE // ✨ تم تصحيح ID View
        binding.pbLoadingChats.visibility = View.VISIBLE // ✨ تم تصحيح ID View

        chatsListener?.remove()

        chatsListener = db?.collection("chats")
            ?.whereArrayContains("participants", currentUserId)
            ?.whereEqualTo("isGroup", true)
            ?.orderBy("lastMessageTime", Query.Direction.DESCENDING)
            ?.addSnapshotListener { queryDocumentSnapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for chats: " + e.message, e)
                    binding.tvEmpty.text = "Failed to load chats: " + e.message // ✨ تم تصحيح ID View
                    binding.tvEmpty.visibility = View.VISIBLE // ✨ تم تصحيح ID View
                    binding.pbLoadingChats.visibility = View.GONE // ✨ تم تصحيح ID View
                    return@addSnapshotListener
                }

                val chats: MutableList<Chat> = ArrayList()
                val participantIdsToFetch = HashSet<String>()

                if (queryDocumentSnapshots != null) {
                    for (doc in queryDocumentSnapshots.documents) {
                        val chat: Chat? = doc.toObject(Chat::class.java)
                        if (chat != null) {
                            chat.id = doc.id
                            chats.add(chat)
                            if (!chat.isGroup) {
                                chat.participants.forEach { participantId ->
                                    if (participantId != currentUserId) {
                                        participantIdsToFetch.add(participantId)
                                    }
                                }
                            }
                        }
                    }
                }

                if (participantIdsToFetch.isNotEmpty()) {
                    fetchUsersDetailsForChats(ArrayList(participantIdsToFetch), chats)
                } else {
                    chatsAdapter?.submitList(chats)
                    binding.pbLoadingChats.visibility = View.GONE // ✨ تم تصحيح ID View
                    updateEmptyState()
                }
            }
    }

    private fun fetchUsersDetailsForChats(userIds: ArrayList<String>, chats: List<Chat>) {
        if (userIds.isEmpty()) {
            chatsAdapter?.submitList(chats)
            updateEmptyState()
            return
        }

        val fetchedUsers: MutableList<UserModel> = ArrayList()
        val chunks = userIds.chunked(10)
        val chunksProcessed = intArrayOf(0)

        for (chunk in chunks) {
            db?.collection("users")
                ?.whereIn("userId", chunk)
                ?.get()
                ?.addOnSuccessListener { queryDocumentSnapshots ->
                    if (queryDocumentSnapshots != null) {
                        for (doc in queryDocumentSnapshots) {
                            val user: UserModel? = doc.toObject(UserModel::class.java)
                            user?.userId = doc.id
                            if (user != null) {
                                fetchedUsers.add(user)
                                usersMap[user.userId] = user
                            }
                        }
                    }
                    chunksProcessed[0]++
                    if (chunksProcessed[0] == chunks.size) {
                        chatsAdapter?.setOtherUsers(usersMap)
                        chatsAdapter?.submitList(chats)
                        binding.pbLoadingChats.visibility = View.GONE // ✨ تم تصحيح ID View
                        updateEmptyState()
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user details chunk for chats", e)
                    chunksProcessed[0]++
                    if (chunksProcessed[0] == chunks.size) {
                        chatsAdapter?.setOtherUsers(usersMap)
                        chatsAdapter?.submitList(chats)
                        binding.pbLoadingChats.visibility = View.GONE // ✨ تم تصحيح ID View
                        updateEmptyState()
                        Toast.makeText(context, "Failed to load some chat user details.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }


    override fun onChatClick(chat: Chat) {
        val intent = Intent(activity, ChatActivity::class.java)
        intent.putExtra("chatId", chat.id)
        intent.putExtra("isGroup", chat.isGroup)
        if (chat.isGroup) {
            intent.putExtra("groupName", chat.groupName)
            intent.putExtra("groupImage", chat.groupImage)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatsListener?.remove()
        chatsListener = null
        _binding = null
    }

    companion object {
        private const val TAG = "ChatsFragment"
    }
}