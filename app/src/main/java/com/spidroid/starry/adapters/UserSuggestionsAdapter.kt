package com.spidroid.starry.adapters

import android.view.View
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class UserSuggestionsAdapter
    (activity: Activity) :
    RecyclerView.Adapter<UserSuggestionsAdapter.UserSuggestionViewHolder?>() {
    private val users: MutableList<UserModel> = ArrayList<UserModel>()
    private val activity: Activity
    private val db: FirebaseFirestore
    private val auth: FirebaseAuth
    private val currentUserId: String?

    init {
        this.activity = activity
        this.db = FirebaseFirestore.getInstance()
        this.auth = FirebaseAuth.getInstance()
        this.currentUserId =
            if (auth.getCurrentUser() != null) auth.getCurrentUser().getUid() else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSuggestionViewHolder {
        val view: View =
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_suggestion, parent, false)
        return UserSuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserSuggestionViewHolder, position: Int) {
        val user: UserModel = users.get(position)

        holder.username.setText("@" + user.getUsername())
        holder.displayName.setText(
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                user.getDisplayName()
            else
                user.getUsername()
        )

        if (user.getBio() != null && !user.getBio().isEmpty()) {
            holder.bio.setText(user.getBio())
            holder.bio.setVisibility(View.VISIBLE)
        } else {
            holder.bio.setVisibility(View.GONE)
        }

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(holder.profileImage)

            holder.profileImage.setOnClickListener(
                View.OnClickListener { v: View? ->
                    val urls = ArrayList<String?>()
                    urls.add(user.getProfileImageUrl())
                    MediaViewerActivity.Companion.launch(activity, urls, 0, holder.profileImage)
                })
        }

        updateFollowButton(holder.followButton, user.getUserId())

        holder.followButton.setOnClickListener(
            View.OnClickListener { v: View? ->
                toggleFollowStatus(
                    user.getUserId(),
                    holder.followButton
                )
            })

        holder.itemView.setOnClickListener(
            View.OnClickListener { v: View? ->
                val intent: Intent = Intent(activity, ProfileActivity::class.java)
                intent.putExtra("userId", user.getUserId())
                activity.startActivity(intent)
            })
    }

    private fun updateFollowButton(followButton: ImageButton, userId: String?) {
        if (currentUserId == null || currentUserId == userId) {
            followButton.setVisibility(View.GONE)
            return
        }

        db.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener(
                { documentSnapshot ->
                    val following =
                        documentSnapshot.get("following")
                    if (following != null && following.containsKey(userId)) {
                        followButton.setImageResource(R.drawable.ic_check)
                        followButton.setContentDescription(activity.getString(R.string.following))
                    } else {
                        followButton.setImageResource(R.drawable.ic_add)
                        followButton.setContentDescription(activity.getString(R.string.follow))
                    }
                    followButton.setVisibility(View.VISIBLE)
                })
    }

    private fun toggleFollowStatus(userId: String?, followButton: ImageButton) {
        if (currentUserId == null) return

        val currentUserRef: DocumentReference = db.collection("users").document(currentUserId)
        val targetUserRef: DocumentReference = db.collection("users").document(userId)

        currentUserRef
            .get()
            .addOnSuccessListener(
                { documentSnapshot ->
                    val following =
                        documentSnapshot.get("following")
                    val isFollowing = following != null && following.containsKey(userId)
                    if (isFollowing) {
                        currentUserRef.update("following." + userId, null)
                        targetUserRef.update("followers." + currentUserId, null)
                        followButton.setImageResource(R.drawable.ic_add)
                        followButton.setContentDescription(activity.getString(R.string.follow))
                    } else {
                        currentUserRef.update("following." + userId, true)
                        targetUserRef.update("followers." + currentUserId, true)
                        followButton.setImageResource(R.drawable.ic_check)
                        followButton.setContentDescription(activity.getString(R.string.following))
                    }
                })
    }

    val itemCount: Int
        get() = users.size

    fun setUsers(newUsers: MutableList<UserModel?>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    internal class UserSuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var profileImage: CircleImageView
        var username: TextView
        var displayName: TextView
        var bio: TextView
        var followButton: ImageButton

        init {
            profileImage = itemView.findViewById<CircleImageView>(R.id.profileImage)
            username = itemView.findViewById<TextView>(R.id.username)
            displayName = itemView.findViewById<TextView>(R.id.displayName)
            bio = itemView.findViewById<TextView>(R.id.tvBio)
            followButton = itemView.findViewById<ImageButton>(R.id.followButton)

            val screenWidth = itemView.getResources().getDisplayMetrics().widthPixels
            val padding = (16 * itemView.getResources().getDisplayMetrics().density).toInt()
            bio.setMaxWidth(screenWidth - padding * 2)
        }
    }
}
