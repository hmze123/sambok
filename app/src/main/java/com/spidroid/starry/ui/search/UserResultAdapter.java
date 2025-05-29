package com.spidroid.starry.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.spidroid.starry.R;
import com.spidroid.starry.models.UserModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserResultAdapter extends RecyclerView.Adapter<UserResultAdapter.UserViewHolder> {
    private List<UserModel> users;
    private final String currentUserId;
    private OnUserInteractionListener listener;

    public interface OnUserInteractionListener {
        void onFollowClicked(UserModel user, int position);
        void onUserClicked(UserModel user);
        void onMoreClicked(UserModel user, View anchor);
    }

    public UserResultAdapter(String currentUserId, OnUserInteractionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.users = new ArrayList<>();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        final CircleImageView ivProfile;
        final TextView tvDisplayName;
        final TextView tvUsername;
        final TextView tvBio;
        final MaterialButton btnFollow;
        final ImageView ivVerified;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvDisplayName = itemView.findViewById(R.id.tv_display_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvBio = itemView.findViewById(R.id.tv_bio);
            btnFollow = itemView.findViewById(R.id.btn_follow);
            ivVerified = itemView.findViewById(R.id.iv_verified);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_result, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = users.get(position);
        
        // Basic Info
        holder.tvDisplayName.setText(user.getDisplayName() != null ? 
            user.getDisplayName() : user.getUsername());
        holder.tvUsername.setText("@" + user.getUsername());
        
        // Bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            holder.tvBio.setText(user.getBio());
            holder.tvBio.setVisibility(View.VISIBLE);
        } else {
            holder.tvBio.setVisibility(View.GONE);
        }

        // Verification Badge
        holder.ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);

        // Follow Button State
        updateFollowButton(holder.btnFollow, user);

        // Profile Image
        Glide.with(holder.itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .into(holder.ivProfile);

        // Click Listeners
        holder.btnFollow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFollowClicked(user, holder.getAdapterPosition());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClicked(user);
            }
        });

        holder.ivProfile.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClicked(user);
            }
        });
    }

    private void updateFollowButton(MaterialButton button, UserModel user) {
        boolean isFollowing = user.getFollowers().containsKey(currentUserId);
        boolean isPrivate = user.getPrivacySettings().isPrivateAccount();

        button.setVisibility(
            user.getUserId().equals(currentUserId) ? View.GONE : View.VISIBLE
        );

        if (isFollowing) {
            button.setText("Following");
            button.setIconResource(R.drawable.ic_check);
            button.setBackgroundColor(
                ContextCompat.getColor(button.getContext(), R.color.m3_surface_container_highest)
            );
        } else if (isPrivate) {
            button.setText("Request");
            button.setIconResource(R.drawable.ic_lock);
            button.setBackgroundColor(
                ContextCompat.getColor(button.getContext(), R.color.m3_primary)
            );
        } else {
            button.setText("Follow");
            button.setIconResource(R.drawable.ic_add);
            button.setBackgroundColor(
                ContextCompat.getColor(button.getContext(), R.color.m3_primary)
            );
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<UserModel> newUsers) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UserDiffCallback(users, newUsers));
        users.clear();
        users.addAll(newUsers);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class UserDiffCallback extends DiffUtil.Callback {
        private final List<UserModel> oldUsers;
        private final List<UserModel> newUsers;

        public UserDiffCallback(List<UserModel> oldUsers, List<UserModel> newUsers) {
            this.oldUsers = oldUsers;
            this.newUsers = newUsers;
        }

        @Override
        public int getOldListSize() {
            return oldUsers.size();
        }

        @Override
        public int getNewListSize() {
            return newUsers.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldUsers.get(oldItemPosition).getUserId()
                .equals(newUsers.get(newItemPosition).getUserId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UserModel oldUser = oldUsers.get(oldItemPosition);
            UserModel newUser = newUsers.get(newItemPosition);
            
            return oldUser.getFollowers().size() == newUser.getFollowers().size() &&
                   oldUser.isVerified() == newUser.isVerified() &&
                   oldUser.getPrivacySettings().isPrivateAccount() == 
                       newUser.getPrivacySettings().isPrivateAccount() &&
                   Objects.equals(oldUser.getDisplayName(), newUser.getDisplayName()) &&
                   Objects.equals(oldUser.getUsername(), newUser.getUsername()) &&
                   Objects.equals(oldUser.getBio(), newUser.getBio());
        }
    }
}