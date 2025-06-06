package com.spidroid.starry.ui.messages

// ★ إضافة هذا
// ★ تأكد من هذا الاستيراد
// ★ استخدام GroupAdapter
// ★ استخدام نموذج Chat
import com.google.firebase.auth.FirebaseAuth

class GroupsFragment : androidx.fragment.app.Fragment(), GroupClickListener {
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var currentUser: FirebaseUser? = null
    private var groupAdapter: GroupAdapter? = null // ★ تغيير نوع المحول
    private var groupsListener: ListenerRegistration? = null

    private var rvGroups: RecyclerView? = null
    private var tvEmptyGroups: TextView? = null
    private var pbLoadingGroups: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth.getCurrentUser()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        val view: android.view.View = inflater.inflate(R.layout.fragment_groups, container, false)
        rvGroups = view.findViewById<RecyclerView?>(R.id.rv_groups)
        tvEmptyGroups = view.findViewById<TextView?>(R.id.tv_empty_groups)
        pbLoadingGroups = view.findViewById<ProgressBar?>(R.id.pb_loading_groups)
        return view
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        if (currentUser != null) {
            listenForGroups()
        } else {
            tvEmptyGroups.setText("Please log in to see your groups.")
            tvEmptyGroups.setVisibility(android.view.View.VISIBLE)
            pbLoadingGroups.setVisibility(android.view.View.GONE)
        }
    }

    private fun setupRecyclerView() {
        if (getContext() == null) return  // تحقق من أن السياق متاح

        groupAdapter = GroupAdapter(getContext(), this) // ★ تمرير السياق
        rvGroups.setLayoutManager(LinearLayoutManager(getContext()))
        rvGroups.setAdapter(groupAdapter)
    }

    private fun listenForGroups() {
        if (currentUser == null) return

        pbLoadingGroups.setVisibility(android.view.View.VISIBLE)
        tvEmptyGroups.setVisibility(android.view.View.GONE)

        // استعلام لجلب الدردشات التي هي مجموعات والمستخدم الحالي مشارك فيها
        val query: Query = db.collection("chats")
            .whereEqualTo("isGroup", true)
            .whereArrayContains("participants", currentUser.getUid())
            .orderBy("lastMessageTime", Query.Direction.DESCENDING) // ترتيب حسب آخر رسالة

        if (groupsListener != null) {
            groupsListener.remove() // إزالة أي مستمع قديم
        }

        groupsListener = query.addSnapshotListener({ queryDocumentSnapshots, e ->
            pbLoadingGroups.setVisibility(android.view.View.GONE)
            if (e != null) {
                android.util.Log.e(GroupsFragment.Companion.TAG, "Listen failed for groups.", e)
                tvEmptyGroups.setText("Failed to load groups.")
                tvEmptyGroups.setVisibility(android.view.View.VISIBLE)
                if (getContext() != null) { // تحقق من السياق قبل عرض Toast
                    Toast.makeText(
                        getContext(),
                        "Error loading groups: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@addSnapshotListener
            }
            if (queryDocumentSnapshots != null) {
                val groups: kotlin.collections.MutableList<Chat?> = java.util.ArrayList<Chat?>()
                for (doc in queryDocumentSnapshots) {
                    val group: Chat = doc.toObject(Chat::class.java)
                    group.setId(doc.getId()) // تأكد من تعيين الـ ID
                    groups.add(group)
                }
                groupAdapter.submitList(groups)
                tvEmptyGroups.setVisibility(if (groups.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
                if (groups.isEmpty()) {
                    tvEmptyGroups.setText("You are not a member of any group yet, or no groups found.")
                }
            } else {
                tvEmptyGroups.setVisibility(android.view.View.VISIBLE)
                tvEmptyGroups.setText("No groups found.")
            }
        })
    }

    override fun onGroupClick(group: Chat) {
        if (getActivity() != null) {
            val intent: Intent = Intent(getActivity(), ChatActivity::class.java)
            intent.putExtra("chatId", group.getId())
            intent.putExtra("isGroup", true) // من المهم تمرير أن هذه دردشة جماعية
            // يمكنك تمرير اسم المجموعة وصورتها أيضًا إذا أردت عرضها مباشرة في ChatActivity
            intent.putExtra("groupName", group.getGroupName())
            intent.putExtra("groupImage", group.getGroupImage())
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (groupsListener != null) {
            groupsListener.remove()
        }
        // لا تقم بإلغاء تهيئة binding هنا إذا كنت لا تستخدم ViewBinding في هذا الـ Fragment
        rvGroups = null
        tvEmptyGroups = null
        pbLoadingGroups = null
    }

    companion object {
        private const val TAG = "GroupsFragment"
    }
}