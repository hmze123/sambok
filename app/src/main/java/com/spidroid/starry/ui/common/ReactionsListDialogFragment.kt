package com.spidroid.starry.ui.common

// ⭐ استيراد للوصول إلى getDrawableIdForEmoji ⭐
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.min

class ReactionsListDialogFragment : BottomSheetDialogFragment() {
    private var recyclerView: RecyclerView? = null
    private var adapter: ReactionUserAdapter? = null
    private var userIds: ArrayList<String?>? = null
    private var reactionsMap: HashMap<String?, String?>? = null
    private var db: FirebaseFirestore? = null
    private var loadingProgressBar: ProgressBar? = null
    private var emptyReactionsText: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        if (getArguments() != null) {
            userIds =
                getArguments().getStringArrayList(ReactionsListDialogFragment.Companion.ARG_USER_IDS)
            reactionsMap =
                getArguments().getSerializable(ReactionsListDialogFragment.Companion.ARG_REACTIONS_MAP) as HashMap<String?, String?>?
        }
        if (userIds == null) userIds = ArrayList<String?>()
        if (reactionsMap == null) reactionsMap = HashMap<String?, String?>()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.dialog_reactions_list, container, false)
        recyclerView = view.findViewById<RecyclerView?>(R.id.rv_reaction_users)
        loadingProgressBar = view.findViewById<ProgressBar?>(R.id.loading_reactions_progress)
        emptyReactionsText = view.findViewById<TextView?>(R.id.empty_reactions_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ReactionUserAdapter(
            ArrayList<UserModel?>(),
            reactionsMap,
            getContext(),
            ReactionUserAdapter.OnUserClickListener { userId: String? ->
                if (userId != null && !userId.isEmpty() && getActivity() != null) {
                    val intent: Intent = Intent(getActivity(), ProfileActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    dismiss()
                }
            })
        recyclerView.setLayoutManager(LinearLayoutManager(getContext()))
        recyclerView.setAdapter(adapter)

        if (userIds!!.isEmpty()) {
            showEmptyState()
        } else {
            fetchUsersDetails()
        }
    }

    private fun showLoading() {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.VISIBLE)
        if (recyclerView != null) recyclerView.setVisibility(View.GONE)
        if (emptyReactionsText != null) emptyReactionsText.setVisibility(View.GONE)
    }

    private fun showContent() {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.GONE)
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE)
        if (emptyReactionsText != null) emptyReactionsText.setVisibility(View.GONE)
    }

    private fun showEmptyState() {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.GONE)
        if (recyclerView != null) recyclerView.setVisibility(View.GONE)
        if (emptyReactionsText != null) {
            emptyReactionsText.setVisibility(View.VISIBLE)
            // تأكد من إضافة هذا المورد إلى strings.xml: <string name="no_reactions_yet">No reactions yet.</string>
            emptyReactionsText.setText(R.string.no_reactions_yet)
        }
    }


    private fun fetchUsersDetails() {
        showLoading()
        val fetchedUsers: MutableList<UserModel?> = ArrayList<UserModel?>()
        if (userIds == null || userIds!!.isEmpty()) {
            showEmptyState()
            return
        }

        val chunks = chunkList<String?>(userIds, 10)
        val chunksProcessed = intArrayOf(0)

        if (chunks.isEmpty()) {
            showEmptyState()
            return
        }

        for (chunk in chunks) {
            if (chunk.isEmpty()) {
                chunksProcessed[0]++
                if (chunksProcessed[0] == chunks.size) {
                    adapter!!.updateUsers(fetchedUsers)
                    if (fetchedUsers.isEmpty()) showEmptyState() else showContent()
                }
                continue
            }
            db.collection("users").whereIn("userId", chunk).get()
                .addOnSuccessListener({ queryDocumentSnapshots ->
                    if (queryDocumentSnapshots != null) {
                        for (doc in queryDocumentSnapshots) {
                            val user: UserModel? = doc.toObject(UserModel::class.java)
                            if (user != null) {
                                user.setUserId(doc.getId())
                                fetchedUsers.add(user)
                            }
                        }
                    }
                    chunksProcessed[0]++
                    if (chunksProcessed[0] == chunks.size) {
                        adapter!!.updateUsers(fetchedUsers)
                        if (fetchedUsers.isEmpty()) showEmptyState() else showContent()
                    }
                })
                .addOnFailureListener({ e ->
                    Log.e(
                        ReactionsListDialogFragment.Companion.TAG,
                        "Error fetching user details chunk",
                        e
                    )
                    chunksProcessed[0]++
                    if (chunksProcessed[0] == chunks.size) {
                        adapter!!.updateUsers(fetchedUsers)
                        if (fetchedUsers.isEmpty()) showEmptyState() else showContent()
                        if (getContext() != null) {
                            Toast.makeText(
                                getContext(),
                                R.string.failed_to_load_some_user_details,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        }
    }

    private fun <T> chunkList(
        list: MutableList<T?>?,
        chunkSize: Int
    ): MutableList<MutableList<T?>> {
        val chunks: MutableList<MutableList<T?>> = ArrayList<MutableList<T?>>()
        if (list == null || list.isEmpty() || chunkSize <= 0) return chunks
        var i = 0
        while (i < list.size) {
            chunks.add(
                ArrayList<T?>(
                    list.subList(
                        i,
                        min(list.size.toDouble(), (i + chunkSize).toDouble()).toInt()
                    )
                )
            )
            i += chunkSize
        }
        return chunks
    }

    private class ReactionUserAdapter(
        users: MutableList<UserModel?>?,
        reactionsMap: MutableMap<String?, String?>?,
        context: Context?,
        clickListener: OnUserClickListener?
    ) : RecyclerView.Adapter<ReactionUserAdapter.ViewHolder?>() {
        private val users: MutableList<UserModel?>?
        private val userReactionsMap: MutableMap<String?, String?>

        // ⭐ لم نعد بحاجة لـ dummyInteractionHandler كمثيل هنا ⭐
        private val userClickListener: OnUserClickListener?
        private val adapterContext: Context? // ⭐ إضافة Context للعثور على الموارد ⭐


        internal interface OnUserClickListener {
            fun onUserItemClicked(userId: String?)
        }

        init {
            this.users = users
            this.userReactionsMap =
                if (reactionsMap != null) reactionsMap else HashMap<String?, String?>()
            this.userClickListener = clickListener
            this.adapterContext = context // ⭐ حفظ الـ Context ⭐
        }

        fun updateUsers(newUsers: MutableList<UserModel?>?) {
            this.users!!.clear()
            if (newUsers != null) {
                this.users.addAll(newUsers)
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_reaction, parent, false)
            return ReactionUserAdapter.ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user: UserModel? = users!!.get(position)
            if (user == null) return

            var displayName: String? = user.getDisplayName()
            if (TextUtils.isEmpty(displayName)) {
                displayName = user.getUsername()
            }
            if (TextUtils.isEmpty(displayName) && adapterContext != null) { // ⭐ استخدام adapterContext ⭐
                displayName = adapterContext.getString(R.string.unknown_user_display_name)
            } else if (TextUtils.isEmpty(displayName)) {
                displayName = "User"
            }
            holder.tvUserName.setText(displayName)

            if (adapterContext != null) { // ⭐ استخدام adapterContext ⭐
                Glide.with(adapterContext)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(holder.ivUserAvatar)
            }


            val reactionEmoji = userReactionsMap.get(user.getUserId())
            if (reactionEmoji != null && !reactionEmoji.isEmpty()) {
                // ⭐ استدعاء الدالة بشكل ثابت ⭐
                val drawableId: Int = PostInteractionHandler.Companion.getDrawableIdForEmoji(
                    reactionEmoji,
                    true
                ) // true للأيقونة الصغيرة
                if (drawableId != 0) {
                    holder.ivReactionEmoji.setImageResource(drawableId)
                    holder.ivReactionEmoji.clearColorFilter()
                    holder.ivReactionEmoji.setVisibility(View.VISIBLE)
                } else {
                    holder.ivReactionEmoji.setVisibility(View.GONE)
                }
            } else {
                holder.ivReactionEmoji.setVisibility(View.GONE)
            }

            holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (userClickListener != null && user.getUserId() != null) {
                    userClickListener.onUserItemClicked(user.getUserId())
                }
            })
        }

        val itemCount: Int
            get() = if (users != null) users.size else 0

        internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var ivUserAvatar: CircleImageView
            var tvUserName: TextView
            var ivReactionEmoji: ImageView

            init {
                ivUserAvatar = itemView.findViewById<CircleImageView>(R.id.iv_user_avatar_reaction)
                tvUserName = itemView.findViewById<TextView>(R.id.tv_user_name_reaction)
                ivReactionEmoji = itemView.findViewById<ImageView>(R.id.iv_reaction_emoji_display)
            }
        }
    }

    companion object {
        private const val ARG_USER_IDS = "user_ids"
        private const val ARG_REACTIONS_MAP = "reactions_map"
        private const val TAG = "ReactionsListDialog"

        fun newInstance(
            userIds: ArrayList<String?>?,
            reactionsMap: MutableMap<String?, String?>?
        ): ReactionsListDialogFragment {
            val fragment = ReactionsListDialogFragment()
            val args: Bundle = Bundle()
            args.putStringArrayList(ReactionsListDialogFragment.Companion.ARG_USER_IDS, userIds)
            args.putSerializable(
                ReactionsListDialogFragment.Companion.ARG_REACTIONS_MAP,
                if (reactionsMap != null) HashMap<String?, String?>(reactionsMap) else HashMap<Any?, Any?>()
            )
            fragment.setArguments(args)
            return fragment
        }
    }
}