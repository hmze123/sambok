package com.spidroid.starry.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.MediaViewerActivity;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.models.UserModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserSuggestionsAdapter
    extends RecyclerView.Adapter<UserSuggestionsAdapter.UserSuggestionViewHolder> {

  private List<UserModel> users = new ArrayList<>();
  private final Activity activity;
  private final FirebaseFirestore db;
  private final FirebaseAuth auth;
  private final String currentUserId;

  public UserSuggestionsAdapter(Activity activity) {
    this.activity = activity;
    this.db = FirebaseFirestore.getInstance();
    this.auth = FirebaseAuth.getInstance();
    this.currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
  }

  @NonNull
  @Override
  public UserSuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user_suggestion, parent, false);
    return new UserSuggestionViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull UserSuggestionViewHolder holder, int position) {
    UserModel user = users.get(position);

    holder.username.setText("@" + user.getUsername());
    holder.displayName.setText(
        user.getDisplayName() != null && !user.getDisplayName().isEmpty()
            ? user.getDisplayName()
            : user.getUsername());

    if (user.getBio() != null && !user.getBio().isEmpty()) {
      holder.bio.setText(user.getBio());
      holder.bio.setVisibility(View.VISIBLE);
    } else {
      holder.bio.setVisibility(View.GONE);
    }

    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
      Glide.with(holder.itemView.getContext())
          .load(user.getProfileImageUrl())
          .placeholder(R.drawable.ic_default_avatar)
          .error(R.drawable.ic_default_avatar)
          .into(holder.profileImage);

      holder.profileImage.setOnClickListener(
          v -> {
            ArrayList<String> urls = new ArrayList<>();
            urls.add(user.getProfileImageUrl());
            MediaViewerActivity.launch(activity, urls, 0, holder.profileImage);
          });
    }

    updateFollowButton(holder.followButton, user.getUserId());

    holder.followButton.setOnClickListener(
        v -> toggleFollowStatus(user.getUserId(), holder.followButton));

    holder.itemView.setOnClickListener(
        v -> {
          Intent intent = new Intent(activity, ProfileActivity.class);
          intent.putExtra("userId", user.getUserId());
          activity.startActivity(intent);
        });
  }

  private void updateFollowButton(ImageButton followButton, String userId) {
    if (currentUserId == null || currentUserId.equals(userId)) {
      followButton.setVisibility(View.GONE);
      return;
    }

    db.collection("users")
        .document(currentUserId)
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              Map<String, Boolean> following =
                  (Map<String, Boolean>) documentSnapshot.get("following");
              if (following != null && following.containsKey(userId)) {
                followButton.setImageResource(R.drawable.ic_check);
                followButton.setContentDescription(activity.getString(R.string.following));
              } else {
                followButton.setImageResource(R.drawable.ic_add);
                followButton.setContentDescription(activity.getString(R.string.follow));
              }
              followButton.setVisibility(View.VISIBLE);
            });
  }

  private void toggleFollowStatus(String userId, ImageButton followButton) {
    if (currentUserId == null) return;

    DocumentReference currentUserRef = db.collection("users").document(currentUserId);
    DocumentReference targetUserRef = db.collection("users").document(userId);

    currentUserRef
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              Map<String, Boolean> following =
                  (Map<String, Boolean>) documentSnapshot.get("following");
              boolean isFollowing = following != null && following.containsKey(userId);

              if (isFollowing) {
                currentUserRef.update("following." + userId, null);
                targetUserRef.update("followers." + currentUserId, null);
                followButton.setImageResource(R.drawable.ic_add);
                followButton.setContentDescription(activity.getString(R.string.follow));
              } else {
                currentUserRef.update("following." + userId, true);
                targetUserRef.update("followers." + currentUserId, true);
                followButton.setImageResource(R.drawable.ic_check);
                followButton.setContentDescription(activity.getString(R.string.following));
              }
            });
  }

  @Override
  public int getItemCount() {
    return users.size();
  }

  public void setUsers(List<UserModel> newUsers) {
    users.clear();
    users.addAll(newUsers);
    notifyDataSetChanged();
  }

  static class UserSuggestionViewHolder extends RecyclerView.ViewHolder {
    CircleImageView profileImage;
    TextView username;
    TextView displayName;
    TextView bio;
    ImageButton followButton;

    public UserSuggestionViewHolder(@NonNull View itemView) {
      super(itemView);
      profileImage = itemView.findViewById(R.id.profileImage);
      username = itemView.findViewById(R.id.username);
      displayName = itemView.findViewById(R.id.displayName);
      bio = itemView.findViewById(R.id.tvBio);
      followButton = itemView.findViewById(R.id.followButton);

      int screenWidth = itemView.getResources().getDisplayMetrics().widthPixels;
      int padding = (int) (16 * itemView.getResources().getDisplayMetrics().density);
      bio.setMaxWidth(screenWidth - padding * 2);
    }
  }
}
