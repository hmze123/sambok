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
import com.spidroid.starry.models.StoryModel
import com.spidroid.starry.models.UserModel
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(
    private val context: Context,
    private val currentUserId: String?,
    private val listener: OnStoryClickListener?
) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    private val otherStories: MutableList<StoryModel> = ArrayList<StoryModel>()
    private var currentUser: UserModel? = null
    private var hasMyActiveStory = false
    private var viewedStoryIds: MutableSet<String?> = HashSet<String?>()

    interface OnStoryClickListener {
        fun onAddStoryClicked()
        fun onViewMyStoryClicked()
        fun onStoryPreviewClicked(story: StoryModel?)
    }

    fun setStories(stories: MutableList<StoryModel>, hasCurrentUserStory: Boolean) {
        this.otherStories.clear()
        this.hasMyActiveStory = hasCurrentUserStory
        for (story in stories) {
            if (story.getUserId() != currentUserId) {
                this.otherStories.add(story)
            }
        }
        notifyDataSetChanged()
    }

    fun setCurrentUser(user: UserModel?) {
        this.currentUser = user
        if (getItemCount() > 0) {
            notifyItemChanged(0)
        }
    }

    // *** هذه هي الدالة التي تمت إضافتها لحل المشكلة ***
    fun setViewedStories(viewedIds: MutableSet<String?>?) {
        this.viewedStoryIds = if (viewedIds != null) viewedIds else HashSet<String?>()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) StoryAdapter.Companion.VIEW_TYPE_MY_STORY else StoryAdapter.Companion.VIEW_TYPE_OTHER_STORY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_story_preview, parent, false)
        if (viewType == StoryAdapter.Companion.VIEW_TYPE_MY_STORY) {
            return MyStoryViewHolder(view)
        } else {
            return OtherStoryViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.getItemViewType() == StoryAdapter.Companion.VIEW_TYPE_MY_STORY) {
            (holder as MyStoryViewHolder).bind()
        } else {
            val story = otherStories.get(position - 1)
            (holder as OtherStoryViewHolder).bind(story)
        }
    }

    override fun getItemCount(): Int {
        return otherStories.size + 1
    }

    internal inner class MyStoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var storyRing: FrameLayout
        var ivAvatar: CircleImageView
        var ivAddStory: ImageView
        var tvAuthorName: TextView

        init {
            storyRing = itemView.findViewById<FrameLayout>(R.id.story_ring_frame)
            ivAvatar = itemView.findViewById<CircleImageView>(R.id.ivStoryAuthorAvatar)
            ivAddStory = itemView.findViewById<ImageView>(R.id.ivAddStory)
            tvAuthorName = itemView.findViewById<TextView>(R.id.tvStoryAuthorName)
        }

        fun bind() {
            tvAuthorName.setText("Your Story")
            if (currentUser != null && currentUser!!.getProfileImageUrl() != null) {
                Glide.with(context).load(currentUser!!.getProfileImageUrl())
                    .placeholder(R.drawable.ic_default_avatar).into(ivAvatar)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }

            ivAddStory.setVisibility(if (hasMyActiveStory) View.GONE else View.VISIBLE)
            storyRing.setBackgroundResource(if (hasMyActiveStory) R.drawable.bg_story_ring_unseen else 0)

            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    if (hasMyActiveStory) {
                        listener.onViewMyStoryClicked()
                    } else {
                        listener.onAddStoryClicked()
                    }
                }
            })
        }
    }

    internal inner class OtherStoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var storyRing: FrameLayout
        var ivAvatar: CircleImageView
        var tvAuthorName: TextView

        init {
            storyRing = itemView.findViewById<FrameLayout>(R.id.story_ring_frame)
            ivAvatar = itemView.findViewById<CircleImageView>(R.id.ivStoryAuthorAvatar)
            tvAuthorName = itemView.findViewById<TextView>(R.id.tvStoryAuthorName)
        }

        fun bind(story: StoryModel) {
            tvAuthorName.setText(story.getUserId()) // سيتم تحسينها لاحقاً لجلب الاسم الحقيقي
            Glide.with(context).load(story.getAuthorAvatarUrl())
                .placeholder(R.drawable.ic_default_avatar).into(ivAvatar)

            val isViewed = viewedStoryIds.contains(story.getStoryId())
            storyRing.setBackgroundResource(if (isViewed) R.drawable.bg_story_ring_seen else R.drawable.bg_story_ring_unseen)

            itemView.setOnClickListener(View.OnClickListener { v: View? ->
                if (listener != null) {
                    listener.onStoryPreviewClicked(story)
                }
            })
        }
    }

    companion object {
        private const val VIEW_TYPE_MY_STORY = 0
        private const val VIEW_TYPE_OTHER_STORY = 1
    }
}