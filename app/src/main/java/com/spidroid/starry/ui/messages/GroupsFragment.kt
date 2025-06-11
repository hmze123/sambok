// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/messages/GroupsFragment.kt
package com.spidroid.starry.ui.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
import com.spidroid.starry.adapters.GroupAdapter
import com.spidroid.starry.adapters.GroupClickListener
import com.spidroid.starry.databinding.FragmentGroupsBinding
import com.spidroid.starry.models.Chat
import java.util.ArrayList
import java.util.Date
import java.util.Objects

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupsFragment : Fragment(), GroupClickListener {

    private var binding: FragmentGroupsBinding? = null
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var currentUser: FirebaseUser? = null

    private var groupAdapter: GroupAdapter? = null
    private var groupsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth?.currentUser
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadGroups()
    }

    private fun setupRecyclerView() {
        binding!!.rvGroups.layoutManager = LinearLayoutManager(context) // ✨ تم تغيير recyclerViewGroups إلى rvGroups
        groupAdapter = GroupAdapter(requireContext(), this)
        binding!!.rvGroups.adapter = groupAdapter // ✨ تم تغيير recyclerViewGroups إلى rvGroups
    }


    private fun loadGroups() {
        val currentUserId = currentUser?.uid
        if (currentUserId == null) {
            binding!!.tvEmptyGroups.text = "Please log in to see your groups." // ✨ تم تغيير emptyGroupsText إلى tvEmptyGroups
            binding!!.tvEmptyGroups.visibility = View.VISIBLE // ✨ تم تغيير emptyGroupsText إلى tvEmptyGroups
            binding!!.pbLoadingGroups.visibility = View.GONE // ✨ تم تغيير loadingProgressBarGroups إلى pbLoadingGroups
            return
        }

        binding!!.rvGroups.visibility = View.GONE // ✨ تم تغيير recyclerViewGroups إلى rvGroups
        binding!!.tvEmptyGroups.visibility = View.GONE // ✨ تم تغيير emptyGroupsText إلى tvEmptyGroups
        binding!!.pbLoadingGroups.visibility = View.VISIBLE // ✨ تم تغيير loadingProgressBarGroups إلى pbLoadingGroups

        groupsListener?.remove()

        groupsListener = db?.collection("chats")
            ?.whereArrayContains("participants", currentUserId)
            ?.whereEqualTo("isGroup", true)
            ?.orderBy("lastMessageTime", Query.Direction.DESCENDING)
            ?.addSnapshotListener { queryDocumentSnapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for groups: " + e.message, e)
                    binding!!.tvEmptyGroups.text = "Failed to load groups: " + e.message // ✨ تم تغيير emptyGroupsText إلى tvEmptyGroups
                    binding!!.tvEmptyGroups.visibility = View.VISIBLE // ✨ تم تغيير emptyGroupsText إلى tvEmptyGroups
                    binding!!.pbLoadingGroups.visibility = View.GONE // ✨ تم تغيير loadingProgressBarGroups إلى pbLoadingGroups
                    return@addSnapshotListener
                }

                val groups: MutableList<Chat> = ArrayList()
                if (queryDocumentSnapshots != null) {
                    for (doc in queryDocumentSnapshots.documents) {
                        val chat: Chat? = doc.toObject(Chat::class.java)
                        if (chat != null) {
                            chat.id = doc.id
                            groups.add(chat)
                        }
                    }
                }
                groupAdapter?.submitList(groups)
                binding!!.pbLoadingGroups.visibility = View.GONE // ✨ تم تغيير loadingProgressBarGroups إلى pbLoadingGroups
                binding!!.rvGroups.visibility = if (groups.isNotEmpty()) View.VISIBLE else View.GONE // ✨ تم تغيير recyclerViewGroups إلى rvGroups
                binding!!.tvEmptyGroups.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE // ✨ تم تغيير emptyGroupsText إلى tvEmptyGroups
                binding!!.tvEmptyGroups.text = if (groups.isEmpty()) "You haven't joined any groups yet." else "" // ✨ تم تغيير emptyGroupsText إلى tvEmptyGroups
            }
    }

    override fun onGroupClick(group: Chat) {
        val intent = Intent(activity, ChatActivity::class.java)
        intent.putExtra("chatId", group.id)
        intent.putExtra("isGroup", true)
        intent.putExtra("groupName", group.groupName)
        intent.putExtra("groupImage", group.groupImage)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        groupsListener?.remove()
        groupsListener = null
        binding = null
    }

    companion object {
        private const val TAG = "GroupsFragment"
    }
}