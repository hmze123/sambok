package com.spidroid.starry.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ItemPostBinding;
import com.spidroid.starry.databinding.ItemUserSuggestionBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private List<Object> items = new ArrayList<>();
  private final Context context;
  private final PostInteractionListener listener;

  private static final int TYPE_POST = 0;
  private static final int TYPE_SUGGESTION = 1;

  public PostAdapter(Context context, PostInteractionListener listener) {
    this.context = context;
    this.listener = listener;
  }

  public void submitCombinedList(List<Object> newItems) {
    this.items = newItems != null ? newItems : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    if (viewType == TYPE_POST) {
      ItemPostBinding binding = ItemPostBinding.inflate(inflater, parent, false);
      return new PostViewHolder(binding, listener, context);
    } else {
      ItemUserSuggestionBinding binding = ItemUserSuggestionBinding.inflate(inflater, parent, false);
      return new UserSuggestionViewHolder(binding, listener);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    Object item = items.get(position);
    if (holder.getItemViewType() == TYPE_POST) {
      ((PostViewHolder) holder).bindCommon((PostModel) item);
    } else if (holder.getItemViewType() == TYPE_SUGGESTION) {
      ((UserSuggestionViewHolder) holder).bind((UserModel) item);
    }
  }

  @Override
  public int getItemCount() {
    return items != null ? items.size() : 0;
  }

  @Override
  public int getItemViewType(int position) {
    return (items.get(position) instanceof PostModel) ? TYPE_POST : TYPE_SUGGESTION;
  }

  public static class PostViewHolder extends BasePostViewHolder {
    private final ItemPostBinding binding;

    PostViewHolder(@NonNull ItemPostBinding binding, PostInteractionListener listener, Context context) {
      super(binding, listener, context);
      this.binding = binding;
    }

    @Override
    public void bindCommon(PostModel post) {
      // التحقق من البيانات الأساسية قبل العرض
      if (post == null || TextUtils.isEmpty(post.getAuthorDisplayName()) || TextUtils.isEmpty(post.getContent())) {
        itemView.setVisibility(View.GONE);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
        return;
      }

      itemView.setVisibility(View.VISIBLE);
      itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      // ربط البيانات
      binding.tvAuthorName.setText(post.getAuthorDisplayName());
      binding.tvUsername.setText("@" + post.getAuthorUsername());
      binding.tvPostContent.setText(post.getContent());

      String avatarUrl = post.getAuthorAvatarUrl();
      if (avatarUrl != null && !avatarUrl.isEmpty()) {
        Glide.with(context).load(avatarUrl).placeholder(R.drawable.ic_default_avatar).into(binding.ivAuthorAvatar);
      } else {
        binding.ivAuthorAvatar.setImageResource(R.drawable.ic_default_avatar);
      }

      interactionHandler.bind(post);
    }
  }

  static class UserSuggestionViewHolder extends RecyclerView.ViewHolder {
    private final ItemUserSuggestionBinding binding;
    private final PostInteractionListener listener;

    UserSuggestionViewHolder(@NonNull ItemUserSuggestionBinding binding, PostInteractionListener listener) {
      super(binding.getRoot());
      this.binding = binding;
      this.listener = listener;
    }

    void bind(UserModel user) {
      binding.username.setText("@" + user.getUsername());
      binding.displayName.setText(user.getDisplayName());
      Glide.with(itemView.getContext()).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_default_avatar).into(binding.profileImage);
      binding.followButton.setOnClickListener(v -> listener.onFollowClicked(user));
      itemView.setOnClickListener(v -> listener.onUserClicked(user));
    }
  }
}