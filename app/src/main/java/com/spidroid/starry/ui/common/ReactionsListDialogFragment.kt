// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/common/ReactionsListDialogFragment.kt
package com.spidroid.starry.ui.common

// ⭐ استيراد للوصول إلى getDrawableIdForEmoji ⭐
import android.app.Dialog
import android.content.Context
import android.content.Intent // Added import
import android.os.Bundle
import android.text.TextUtils
import android.util.Log // Added import
import android.view.LayoutInflater // Added import
import android.view.View
import android.view.ViewGroup // Added import
import android.widget.ImageView
import android.widget.ProgressBar // Added import
import android.widget.TextView // Added import
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager // Added import
import androidx.recyclerview.widget.RecyclerView // Added import
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment // Added import
import com.google.firebase.firestore.FirebaseFirestore
import com.spidroid.starry.R
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.utils.PostInteractionHandler // Added import
import de.hdodenhof.circleimageview.CircleImageView
import java.util.ArrayList
import java.util.HashMap
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
        if (arguments != null) {
            userIds =
                arguments?.getStringArrayList(ARG_USER_IDS)
            reactionsMap =
                arguments?.getSerializable(ARG_REACTIONS_MAP) as HashMap<String?, String?>?
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
            context,
            object : ReactionUserAdapter.OnUserClickListener { // Corrected anonymous object syntax
                override fun onUserItemClicked(userId: String?) {
                    if (userId != null && userId.isNotEmpty() && activity != null) {
                        val intent: Intent = Intent(activity, ProfileActivity::class.java)
                        intent.putExtra("userId", userId)
                        startActivity(intent)
                        dismiss()
                    }
                }
            })
        recyclerView?.layoutManager = LinearLayoutManager(context) // Used safe call and assigned to layoutManager
        recyclerView?.adapter = adapter

        if (userIds!!.isEmpty()) {
            showEmptyState()
        } else {
            fetchUsersDetails()
        }
    }

    private fun showLoading() {
        if (loadingProgressBar != null) loadingProgressBar!!.visibility = View.VISIBLE
        if (recyclerView != null) recyclerView!!.visibility = View.GONE
        if (emptyReactionsText != null) emptyReactionsText!!.visibility = View.GONE
    }

    private fun showContent() {
        if (loadingProgressBar != null) loadingProgressBar!!.visibility = View.GONE
        if (recyclerView != null) recyclerView!!.visibility = View.VISIBLE
        if (emptyReactionsText != null) emptyReactionsText!!.visibility = View.GONE
    }

    private fun showEmptyState() {
        if (loadingProgressBar != null) loadingProgressBar!!.visibility = View.GONE
        if (recyclerView != null) recyclerView!!.visibility = View.GONE
        if (emptyReactionsText != null) {
            emptyReactionsText!!.visibility = View.VISIBLE
            // تأكد من إضافة هذا المورد إلى strings.xml: <string name="no_reactions_yet">No reactions yet.</string>
            emptyReactionsText!!.setText(R.string.no_reactions_yet)
        }
    }


    private fun fetchUsersDetails() {
        showLoading()
        val fetchedUsers: MutableList<UserModel?> = ArrayList<UserModel?>()
        if (userIds == null || userIds!!.isEmpty()) {
            showEmptyState()
            return
        }

        val chunks = chunkList(userIds, 10)
        val chunksProcessed = intArrayOf(0)

        if (chunks.isEmpty()) {
            showEmptyState()
            return
        }

        for (chunk in chunks) {
            if (chunk.isEmpty()) {
                chunksProcessed[0]++
                if (chunksProcessed[0] == chunks.size) {
                    adapter?.updateUsers(fetchedUsers)
                    if (fetchedUsers.isEmpty()) showEmptyState() else showContent()
                }
                continue
            }
            db?.collection("users")?.whereIn("userId", chunk)?.get()
                ?.addOnSuccessListener({ queryDocumentSnapshots ->
                    if (queryDocumentSnapshots != null) {
                        for (doc in queryDocumentSnapshots) {
                            val user: UserModel? = doc.toObject(UserModel::class.java)
                            user?.userId = doc.id // Correct way to set ID
                            if (user != null) {
                                fetchedUsers.add(user)
                            }
                        }
                    }
                    chunksProcessed[0]++
                    if (chunksProcessed[0] == chunks.size) {
                        adapter?.updateUsers(fetchedUsers)
                        if (fetchedUsers.isEmpty()) showEmptyState() else showContent()
                    }
                })
                ?.addOnFailureListener({ e ->
                    Log.e(
                        TAG,
                        "Error fetching user details chunk",
                        e
                    )
                    chunksProcessed[0]++
                    if (chunksProcessed[0] == chunks.size) {
                        adapter?.updateUsers(fetchedUsers)
                        if (fetchedUsers.isEmpty()) showEmptyState() else showContent()
                        if (context != null) {
                            Toast.makeText(
                                context,
                                R.string.failed_to_load_some_user_details,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        }
    }

    private fun <T> chunkList(
        list: MutableList<T>?,
        chunkSize: Int
    ): MutableList<MutableList<T>> {
        val chunks: MutableList<MutableList<T>> = ArrayList()
        if (list == null || list.isEmpty() || chunkSize <= 0) return chunks
        var i = 0
        while (i < list.size) {
            chunks.add(
                ArrayList(
                    list.subList(
                        i,
                        min(list.size, (i + chunkSize))
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
    ) : RecyclerView.Adapter<ReactionUserAdapter.ViewHolder>() { // Removed nullable ViewHolder
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
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_reaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user: UserModel? = users?.get(position)
            if (user == null) return

            var displayName: String? = user.displayName
            if (TextUtils.isEmpty(displayName)) {
                displayName = user.username
            }
            if (TextUtils.isEmpty(displayName) && adapterContext != null) { // ⭐ استخدام adapterContext ⭐
                displayName = adapterContext.getString(R.string.unknown_user_display_name)
            } else if (TextUtils.isEmpty(displayName)) {
                displayName = "User"
            }
            holder.tvUserName.text = displayName

            if (adapterContext != null) { // ⭐ استخدام adapterContext ⭐
                Glide.with(adapterContext)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(holder.ivUserAvatar)
            }


            val reactionEmoji = userReactionsMap[user.userId] // Used safe access for userId
            if (reactionEmoji != null && reactionEmoji.isNotEmpty()) {
                // ⭐ استدعاء الدالة بشكل ثابت ⭐
                val drawableId: Int = PostInteractionHandler.getDrawableIdForEmoji(
                    reactionEmoji,
                    true
                ) // true للأيقونة الصغيرة
                if (drawableId != 0) {
                    holder.ivReactionEmoji.setImageResource(drawableId)
                    holder.ivReactionEmoji.clearColorFilter()
                    holder.ivReactionEmoji.visibility = View.VISIBLE
                } else {
                    holder.ivReactionEmoji.visibility = View.GONE
                }
            } else {
                holder.ivReactionEmoji.visibility = View.GONE
            }

            holder.itemView.setOnClickListener(View.OnClickListener {
                if (userClickListener != null && user.userId != null) {
                    userClickListener.onUserItemClicked(user.userId)
                }
            })
        }

        override fun getItemCount(): Int { // Changed to override
            return if (users != null) users.size else 0
        }

        internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var ivUserAvatar: CircleImageView
            var tvUserName: TextView
            var ivReactionEmoji: ImageView

            init {
                ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar_reaction)
                tvUserName = itemView.findViewById(R.id.tv_user_name_reaction)
                ivReactionEmoji = itemView.findViewById(R.id.iv_reaction_emoji_display)
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
            args.putStringArrayList(ARG_USER_IDS, userIds)
            args.putSerializable(
                ARG_REACTIONS_MAP,
                if (reactionsMap != null) HashMap<String?, String?>(reactionsMap) else HashMap<String?, String?>()
            )
            fragment.arguments = args
            return fragment
        }
    }
}