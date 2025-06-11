package com.spidroid.starry.ui.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.spidroid.starry.R
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.utils.PostInteractionHandler
import de.hdodenhof.circleimageview.CircleImageView
import java.io.Serializable
import kotlin.math.min
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReactionsListDialogFragment : BottomSheetDialogFragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReactionUserAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyReactionsText: TextView

    private var userIds: List<String> = emptyList()
    private var reactionsMap: Map<String, String> = emptyMap()
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userIds = it.getStringArrayList(ARG_USER_IDS) ?: emptyList()
            // طريقة آمنة لاسترداد الخريطة
            @Suppress("UNCHECKED_CAST")
            reactionsMap = (it.getSerializable(ARG_REACTIONS_MAP) as? HashMap<String, String>) ?: emptyMap()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_reactions_list, container, false)
        recyclerView = view.findViewById(R.id.rv_reaction_users)
        loadingProgressBar = view.findViewById(R.id.loading_reactions_progress)
        emptyReactionsText = view.findViewById(R.id.empty_reactions_text)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ReactionUserAdapter(requireContext(), reactionsMap) { userId ->
            if (userId.isNotEmpty()) {
                val intent = Intent(activity, ProfileActivity::class.java).apply {
                    putExtra("userId", userId)
                }
                startActivity(intent)
                dismiss()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        if (userIds.isEmpty()) {
            showEmptyState()
        } else {
            fetchUsersDetails()
        }
    }

    private fun fetchUsersDetails() {
        showLoading(true)
        val fetchedUsers = mutableListOf<UserModel>()

        // جلب المستخدمين على دفعات (chunks)
        userIds.chunked(10).forEach { chunk ->
            db.collection("users").whereIn("userId", chunk).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!isAdded) return@addOnSuccessListener
                    val usersFromChunk = querySnapshot.toObjects(UserModel::class.java)
                    fetchedUsers.addAll(usersFromChunk)
                    // تحديث الواجهة بعد جلب كل الدفعات
                    if (fetchedUsers.size >= userIds.size) {
                        adapter.updateUsers(fetchedUsers)
                        showLoading(false)
                    }
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    Log.e(TAG, "Error fetching user details chunk", e)
                    showLoading(false)
                    Toast.makeText(context, R.string.failed_to_load_some_user_details, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        emptyReactionsText.visibility = View.GONE
    }

    private fun showEmptyState() {
        loadingProgressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyReactionsText.visibility = View.VISIBLE
    }

    // Adapter Class
    private class ReactionUserAdapter(
        private val context: Context,
        private val reactionsMap: Map<String, String>,
        private val onUserClick: (String) -> Unit
    ) : RecyclerView.Adapter<ReactionUserAdapter.ViewHolder>() {

        private var users: List<UserModel> = emptyList()

        fun updateUsers(newUsers: List<UserModel>) {
            this.users = newUsers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_reaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.bind(user)
        }

        override fun getItemCount(): Int = users.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivUserAvatar: CircleImageView = itemView.findViewById(R.id.iv_user_avatar_reaction)
            private val tvUserName: TextView = itemView.findViewById(R.id.tv_user_name_reaction)
            private val ivReactionEmoji: ImageView = itemView.findViewById(R.id.iv_reaction_emoji_display)

            fun bind(user: UserModel) {
                tvUserName.text = user.displayName ?: user.username
                Glide.with(context)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(ivUserAvatar)

                val reactionEmoji = reactionsMap[user.userId]
                if (!reactionEmoji.isNullOrEmpty()) {
                    val drawableId = PostInteractionHandler.getDrawableIdForEmoji(reactionEmoji, true)
                    if (drawableId != 0 && drawableId != R.drawable.ic_emoji) {
                        ivReactionEmoji.setImageResource(drawableId)
                        ivReactionEmoji.clearColorFilter()
                        ivReactionEmoji.visibility = View.VISIBLE
                    } else {
                        ivReactionEmoji.visibility = View.GONE
                    }
                } else {
                    ivReactionEmoji.visibility = View.GONE
                }

                itemView.setOnClickListener { onUserClick(user.userId) }
            }
        }
    }


    companion object {
        private const val ARG_USER_IDS = "user_ids"
        private const val ARG_REACTIONS_MAP = "reactions_map"
        private const val TAG = "ReactionsListDialog"

        fun newInstance(reactions: Map<String, String>): ReactionsListDialogFragment {
            val fragment = ReactionsListDialogFragment()
            val args = Bundle().apply {
                putStringArrayList(ARG_USER_IDS, ArrayList(reactions.keys))
                putSerializable(ARG_REACTIONS_MAP, HashMap(reactions))
            }
            fragment.arguments = args
            return fragment
        }
    }
}