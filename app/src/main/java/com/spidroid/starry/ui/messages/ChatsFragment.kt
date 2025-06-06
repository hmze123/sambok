package com.spidroid.starry.ui.messages

import com.google.firebase.auth.FirebaseAuth

class ChatsFragment : androidx.fragment.app.Fragment(), ChatClickListener {
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var adapter: ChatsAdapter? = null
    private var chatsListener: ListenerRegistration? = null
    private var tvEmpty: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        return inflater.inflate(R.layout.fragment_chats, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(view)
        listenForChats()
    }

    private fun setupRecyclerView(view: android.view.View) {
        val rvChats: RecyclerView = view.findViewById<RecyclerView>(R.id.chatsRecyclerView)
        rvChats.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = ChatsAdapter(this)
        rvChats.setAdapter(adapter)

        tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        adapter.registerAdapterDataObserver(
            object : AdapterDataObserver() {
                override fun onChanged() {
                    checkEmpty()
                }
            })
    }

    private fun checkEmpty() {
        tvEmpty.setVisibility(if (adapter.getItemCount() == 0) android.view.View.VISIBLE else android.view.View.GONE)
    }

    private fun listenForChats() {
        val user: FirebaseUser? = auth.getCurrentUser()
        if (user == null) {
            android.util.Log.e("ChatsFragment", "User is not authenticated")
            return
        }

        val query: Query =
            db.collection("chats")
                .whereArrayContains("participants", user.getUid())
                .whereGreaterThan("lastMessageTime", java.util.Date(0))
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        chatsListener =
            query.addSnapshotListener(
                { value, error ->
                    if (error != null) {
                        android.util.Log.e("ChatsFragment", "Listen failed", error)
                        return@addSnapshotListener
                    }
                    android.util.Log.d("ChatsFragment", "Received " + value.size() + " chats")
                    val chats: kotlin.collections.MutableList<Chat?> = java.util.ArrayList<Chat?>()
                    for (doc in value) {
                        val chat: Chat = doc.toObject(Chat::class.java)
                        chat.setId(doc.getId())
                        chats.add(chat)
                        android.util.Log.d("ChatsFragment", "Chat ID: " + chat.getId())
                    }
                    adapter.submitList(chats)
                })
    }

    override fun onChatClick(chat: Chat) {
        val intent: Intent = Intent(getActivity(), ChatActivity::class.java)
        intent.putExtra("chatId", chat.getId())
        intent.putExtra("isGroup", chat.isGroup())
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (chatsListener != null) {
            chatsListener.remove()
        }
    }
}
