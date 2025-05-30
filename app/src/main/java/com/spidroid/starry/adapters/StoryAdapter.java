package com.spidroid.starry.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.models.StoryModel;
import com.spidroid.starry.models.UserModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class StoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MY_STORY = 0;
    private static final int VIEW_TYPE_OTHER_STORY = 1;

    private final Context context;
    private final OnStoryClickListener listener;
    private final String currentUserId;

    private List<StoryModel> otherStories = new ArrayList<>();
    private UserModel currentUser;
    private boolean hasMyActiveStory = false;
    private Set<String> viewedStoryIds = new HashSet<>();

    public interface OnStoryClickListener {
        void onAddStoryClicked();
        void onViewMyStoryClicked();
        void onStoryPreviewClicked(StoryModel story);
    }

    public StoryAdapter(Context context, String currentUserId, OnStoryClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    public void setStories(List<StoryModel> stories, boolean hasCurrentUserStory) {
        this.otherStories.clear();
        this.hasMyActiveStory = hasCurrentUserStory;
        for (StoryModel story : stories) {
            if (!story.getUserId().equals(currentUserId)) {
                this.otherStories.add(story);
            }
        }
        notifyDataSetChanged();
    }

    public void setCurrentUser(UserModel user) {
        this.currentUser = user;
        if (getItemCount() > 0) {
            notifyItemChanged(0);
        }
    }

    // *** هذه هي الدالة التي تمت إضافتها لحل المشكلة ***
    public void setViewedStories(Set<String> viewedIds) {
        this.viewedStoryIds = viewedIds != null ? viewedIds : new HashSet<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_MY_STORY : VIEW_TYPE_OTHER_STORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_preview, parent, false);
        if (viewType == VIEW_TYPE_MY_STORY) {
            return new MyStoryViewHolder(view);
        } else {
            return new OtherStoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_MY_STORY) {
            ((MyStoryViewHolder) holder).bind();
        } else {
            StoryModel story = otherStories.get(position - 1);
            ((OtherStoryViewHolder) holder).bind(story);
        }
    }

    @Override
    public int getItemCount() {
        return otherStories.size() + 1;
    }

    class MyStoryViewHolder extends RecyclerView.ViewHolder {
        FrameLayout storyRing;
        CircleImageView ivAvatar;
        ImageView ivAddStory;
        TextView tvAuthorName;

        MyStoryViewHolder(@NonNull View itemView) {
            super(itemView);
            storyRing = itemView.findViewById(R.id.story_ring_frame);
            ivAvatar = itemView.findViewById(R.id.ivStoryAuthorAvatar);
            ivAddStory = itemView.findViewById(R.id.ivAddStory);
            tvAuthorName = itemView.findViewById(R.id.tvStoryAuthorName);
        }

        void bind() {
            tvAuthorName.setText("Your Story");
            if (currentUser != null && currentUser.getProfileImageUrl() != null) {
                Glide.with(context).load(currentUser.getProfileImageUrl()).placeholder(R.drawable.ic_default_avatar).into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_default_avatar);
            }

            ivAddStory.setVisibility(hasMyActiveStory ? View.GONE : View.VISIBLE);
            storyRing.setBackgroundResource(hasMyActiveStory ? R.drawable.bg_story_ring_unseen : 0);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if (hasMyActiveStory) {
                        listener.onViewMyStoryClicked();
                    } else {
                        listener.onAddStoryClicked();
                    }
                }
            });
        }
    }

    class OtherStoryViewHolder extends RecyclerView.ViewHolder {
        FrameLayout storyRing;
        CircleImageView ivAvatar;
        TextView tvAuthorName;

        OtherStoryViewHolder(@NonNull View itemView) {
            super(itemView);
            storyRing = itemView.findViewById(R.id.story_ring_frame);
            ivAvatar = itemView.findViewById(R.id.ivStoryAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvStoryAuthorName);
        }

        void bind(StoryModel story) {
            tvAuthorName.setText(story.getUserId()); // سيتم تحسينها لاحقاً لجلب الاسم الحقيقي
            Glide.with(context).load(story.getAuthorAvatarUrl()).placeholder(R.drawable.ic_default_avatar).into(ivAvatar);

            boolean isViewed = viewedStoryIds.contains(story.getStoryId());
            storyRing.setBackgroundResource(isViewed ? R.drawable.bg_story_ring_seen : R.drawable.bg_story_ring_unseen);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStoryPreviewClicked(story);
                }
            });
        }
    }
}