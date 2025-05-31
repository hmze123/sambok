package com.spidroid.starry.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth; // ★ إضافة استيراد FirebaseAuth
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ItemPostBinding;
import com.spidroid.starry.databinding.ItemUserSuggestionBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private List<Object> items = new ArrayList<>();
  private final Context context;
  private final PostInteractionListener listener;
  private final String currentUserId; // ★ إضافة حقل لتخزين معرّف المستخدم الحالي

  private static final int TYPE_POST = 0;
  private static final int TYPE_SUGGESTION = 1;
  private static final String TAG = "PostAdapter";

  // ★★★ تم تعديل المُنشئ ليقوم بتهيئة currentUserId ★★★
  public PostAdapter(Context context, PostInteractionListener listener) {
    this.context = context;
    this.listener = listener;
    // تهيئة currentUserId عند إنشاء الـ Adapter
    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
      this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    } else {
      this.currentUserId = ""; // أو قيمة افتراضية مناسبة إذا لم يكن هناك مستخدم
      Log.w(TAG, "Current user is null when creating PostAdapter.");
    }
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
      // ★ تمرير currentUserId إلى مُنشئ PostViewHolder
      return new PostViewHolder(binding, listener, context, currentUserId);
    } else { // TYPE_SUGGESTION
      ItemUserSuggestionBinding binding = ItemUserSuggestionBinding.inflate(inflater, parent, false);
      return new UserSuggestionViewHolder(binding, listener);
      // ملاحظة: UserSuggestionViewHolder لا يستخدم PostInteractionHandler مباشرة بنفس الطريقة
      // لذا قد لا يحتاج إلى currentUserId في مُنشئه إلا إذا كنت ستضيف تفاعلات مشابهة له
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
    if (items.get(position) instanceof PostModel) {
      return TYPE_POST;
    } else if (items.get(position) instanceof UserModel) {
      return TYPE_SUGGESTION;
    }
    return -1;
  }

  public static class PostViewHolder extends BasePostViewHolder {
    private final ItemPostBinding binding;
    private final LinearLayout reactionsDisplayContainer;

    // ★★★ تم تعديل مُنشئ PostViewHolder ليقبل currentUserId ويمرره إلى super ★★★
    PostViewHolder(@NonNull ItemPostBinding binding, PostInteractionListener listener, Context context, String currentUserId) {
      super(binding, listener, context, currentUserId); // ★ تمرير currentUserId إلى مُنشئ BasePostViewHolder
      this.binding = binding;
      this.reactionsDisplayContainer = binding.reactionsDisplayContainer;
      // PostInteractionHandler يتم تهيئته الآن في BasePostViewHolder
    }

    @Override
    public void bindCommon(PostModel post) {
      if (post == null) {
        Log.e(TAG, "PostModel is null in bindCommon. Hiding item.");
        itemView.setVisibility(View.GONE);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
        return;
      }
      if (TextUtils.isEmpty(post.getAuthorDisplayName()) && TextUtils.isEmpty(post.getAuthorUsername())) {
        binding.tvAuthorName.setText("Unknown User");
      } else {
        binding.tvAuthorName.setText(post.getAuthorDisplayName() != null ? post.getAuthorDisplayName() : post.getAuthorUsername());
      }

      if (TextUtils.isEmpty(post.getContent()) && (post.getMediaUrls() == null || post.getMediaUrls().isEmpty()) && (post.getLinkPreviews() == null || post.getLinkPreviews().isEmpty())) {
        itemView.setVisibility(View.GONE);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
        return;
      }

      itemView.setVisibility(View.VISIBLE);
      itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      binding.tvUsername.setText("@" + (post.getAuthorUsername() != null ? post.getAuthorUsername() : "unknown"));
      binding.tvPostContent.setText(post.getContent() != null ? post.getContent() : "");

      String avatarUrl = post.getAuthorAvatarUrl();
      if (avatarUrl != null && !avatarUrl.isEmpty() && context != null) {
        Glide.with(context).load(avatarUrl).placeholder(R.drawable.ic_default_avatar).into(binding.ivAuthorAvatar);
      } else if (context != null) {
        binding.ivAuthorAvatar.setImageResource(R.drawable.ic_default_avatar);
      }

      if (interactionHandler != null) {
        interactionHandler.bind(post);
      }
      updateReactionsDisplay(post.getReactions());
    }

    private void updateReactionsDisplay(Map<String, String> reactionsMap) {
      if (reactionsDisplayContainer == null || context == null) {
        return;
      }
      reactionsDisplayContainer.removeAllViews();

      if (reactionsMap == null || reactionsMap.isEmpty()) {
        reactionsDisplayContainer.setVisibility(View.GONE);
        return;
      }

      Map<String, Integer> emojiCounts = new HashMap<>();
      for (String emoji : reactionsMap.values()) {
        emojiCounts.put(emoji, emojiCounts.getOrDefault(emoji, 0) + 1);
      }

      Map<String, Integer> sortedEmojiCounts = emojiCounts.entrySet().stream()
              .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
              .collect(Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (e1, e2) -> e1,
                      LinkedHashMap::new
              ));

      int reactionsToShow = 0;
      for (Map.Entry<String, Integer> entry : sortedEmojiCounts.entrySet()) {
        if (reactionsToShow >= 3) break;

        String emoji = entry.getKey();
        int count = entry.getValue();

        if (count > 0) {
          TextView emojiView = new TextView(context);
          emojiView.setText(emoji);
          emojiView.setTextSize(16);
          emojiView.setPadding(0, 0, 4, 0);
          reactionsDisplayContainer.addView(emojiView);
          reactionsToShow++;
        }
      }

      if (!reactionsMap.isEmpty()) {
        TextView totalReactionsView = new TextView(context);
        totalReactionsView.setText(String.valueOf(reactionsMap.size()));
        totalReactionsView.setTextSize(14);
        if (context != null) { // Check context before accessing resources
          totalReactionsView.setTextColor(context.getResources().getColor(R.color.text_secondary, null));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginStart(8);
        totalReactionsView.setLayoutParams(params);
        reactionsDisplayContainer.addView(totalReactionsView);
      }

      if (reactionsDisplayContainer.getChildCount() > 0) {
        reactionsDisplayContainer.setVisibility(View.VISIBLE);
      } else {
        reactionsDisplayContainer.setVisibility(View.GONE);
      }
    }
  }

  static class UserSuggestionViewHolder extends RecyclerView.ViewHolder {
    private final ItemUserSuggestionBinding binding;
    private final PostInteractionListener listener;
    private final Context context;

    UserSuggestionViewHolder(@NonNull ItemUserSuggestionBinding binding, PostInteractionListener listener) {
      super(binding.getRoot());
      this.binding = binding;
      this.listener = listener;
      this.context = itemView.getContext();
    }

    void bind(UserModel user) {
      if (user == null) return;

      binding.username.setText("@" + (user.getUsername() != null ? user.getUsername() : ""));
      binding.displayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "");

      if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty() && context != null) {
        Glide.with(context).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_default_avatar).into(binding.profileImage);
      } else if (context != null) {
        binding.profileImage.setImageResource(R.drawable.ic_default_avatar);
      }

      binding.followButton.setOnClickListener(v -> {
        if (listener != null) listener.onFollowClicked(user);
      });
      itemView.setOnClickListener(v -> {
        if (listener != null) listener.onUserClicked(user);
      });
    }
  }
}