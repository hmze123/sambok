package com.spidroid.starry.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.StoryPreview
import de.hdodenhof.circleimageview.CircleImageView

interface OnStoryClickListener {
    fun onAddStoryClicked()
    fun onViewMyStoryClicked()
    fun onStoryPreviewClicked(user: UserModel)
}

class StoryAdapter(
    private val context: Context,
    private val currentUserId: String?,
    private val listener: OnStoryClickListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val storyPreviews = mutableListOf<StoryPreview>()
    private var currentUser: UserModel? = null
    private var hasMyActiveStory = false

    companion object {
        private const val VIEW_TYPE_MY_STORY = 0
        private const val VIEW_TYPE_OTHER_STORY = 1
    }

    fun setStories(previews: List<StoryPreview>, currentUserStoryExists: Boolean) {
        storyPreviews.clear()
        storyPreviews.addAll(previews)
        hasMyActiveStory = currentUserStoryExists
        notifyDataSetChanged()
    }

    fun setViewedStories(viewedIds: Set<String>?) {
        // This logic is now handled by the 'hasUnseenStories' flag in StoryPreview
        // but we might need a refresh if a story is viewed while the app is open.
        // For simplicity, we can trigger a full refresh.
        notifyDataSetChanged()
    }

    fun setCurrentUser(user: UserModel) {
        this.currentUser = user
        if (itemCount > 0) {
            notifyItemChanged(0) // Update the "My Story" view
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_MY_STORY else VIEW_TYPE_OTHER_STORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_story_preview, parent, false)
        return if (viewType == VIEW_TYPE_MY_STORY) {
            MyStoryViewHolder(view)
        } else {
            OtherStoryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyStoryViewHolder) {
            holder.bind()
        } else if (holder is OtherStoryViewHolder) {
            val preview = storyPreviews[position - 1] // Adjust index for "My Story" item
            holder.bind(preview)
        }
    }

    override fun getItemCount(): Int = storyPreviews.size + 1 // +1 for "My Story"

    inner class MyStoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val storyRing: FrameLayout = itemView.findViewById(R.id.story_ring_frame)
        private val ivAvatar: CircleImageView = itemView.findViewById(R.id.ivStoryAuthorAvatar)
        private val ivAddStory: ImageView = itemView.findViewById(R.id.ivAddStory)
        private val tvAuthorName: TextView = itemView.findViewById(R.id.tvStoryAuthorName)

        fun bind() {
            tvAuthorName.text = context.getString(R.string.your_story)
            Glide.with(context)
                .load(currentUser?.profileImageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAvatar)

            ivAddStory.visibility = if (hasMyActiveStory) View.GONE else View.VISIBLE
            storyRing.setBackgroundResource(
                if (hasMyActiveStory) R.drawable.bg_story_ring_unseen else 0
            )

            itemView.setOnClickListener {
                if (hasMyActiveStory) {
                    listener?.onViewMyStoryClicked()
                } else {
                    listener?.onAddStoryClicked()
                }
            }
        }
    }

    inner class OtherStoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val storyRing: FrameLayout = itemView.findViewById(R.id.story_ring_frame)
        private val ivAvatar: CircleImageView = itemView.findViewById(R.id.ivStoryAuthorAvatar)
        private val tvAuthorName: TextView = itemView.findViewById(R.id.tvStoryAuthorName)

        fun bind(storyPreview: StoryPreview) {
            val user = storyPreview.user
            tvAuthorName.text = user.displayName ?: user.username
            Glide.with(context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(ivAvatar)

            storyRing.setBackgroundResource(
                if (storyPreview.hasUnseenStories) R.drawable.bg_story_ring_unseen else R.drawable.bg_story_ring_seen
            )

            itemView.setOnClickListener {
                listener?.onStoryPreviewClicked(user)
            }
        }
    }
}
