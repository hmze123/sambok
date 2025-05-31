package com.spidroid.starry.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth; // ★ إضافة استيراد
import com.google.android.material.snackbar.Snackbar;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.PostDetailActivity;
import com.spidroid.starry.activities.ProfileActivity; // ★ إضافة استيراد ProfileActivity
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.adapters.PostInteractionListener;
import com.spidroid.starry.databinding.FragmentFeedBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.ui.common.ReactionPickerFragment; // ★ إضافة استيراد
import com.spidroid.starry.viewmodels.PostViewModel;

import java.util.ArrayList;
import java.util.List;

public class ForYouFragment extends Fragment implements PostInteractionListener, ReactionPickerFragment.ReactionListener { // ★ إضافة ReactionPickerFragment.ReactionListener

  private FragmentFeedBinding binding;
  private PostViewModel postViewModel;
  private PostAdapter postAdapter;
  // private boolean isLoading = false; // لم تعد مستخدمة مباشرة هنا

  private String currentAuthUserId; // ★ معرّف المستخدم الحالي المسجل
  private PostModel currentPostForReaction; // ★ لتخزين المنشور الذي يتم التفاعل معه
  private static final String TAG = "ForYouFragment"; // ★ إضافة TAG

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) { // ★ إضافة onCreate
    super.onCreate(savedInstanceState);
    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
      currentAuthUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    } else {
      Log.w(TAG, "Current user is not authenticated in ForYouFragment.");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentFeedBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    postViewModel = new ViewModelProvider(requireActivity()).get(PostViewModel.class); // ★ استخدام requireActivity() للـ ViewModel

    setupRecyclerView();
    setupSwipeRefresh();
    setupObservers();

    loadInitialData();
  }

  private void setupRecyclerView() {
    if (getContext() == null) return; // ★ تحقق من السياق
    postAdapter = new PostAdapter(requireContext(), this);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerView.setAdapter(postAdapter);
  }

  private void setupSwipeRefresh() {
    binding.swipeRefresh.setOnRefreshListener(() -> {
      // isLoading = false; // لم تعد مستخدمة
      postViewModel.fetchPosts(15); // جلب أحدث 15 منشورًا
      postViewModel.fetchUserSuggestions(); // جلب اقتراحات المستخدمين
    });
  }

  private void setupObservers() {
    postViewModel.getCombinedFeed().observe(getViewLifecycleOwner(), items -> {
      if (binding == null) return; // ★ تحقق من أن binding ليس null
      binding.progressContainer.setVisibility(View.GONE);
      binding.swipeRefresh.setRefreshing(false);

      if (items != null) { // ★ التأكد من أن items ليست null
        postAdapter.submitCombinedList(items);
      } else {
        postAdapter.submitCombinedList(new ArrayList<>()); // تمرير قائمة فارغة إذا كانت null
      }

      boolean isEmpty = items == null || items.isEmpty();
      binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
      binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    });

    postViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
      if (errorMessage != null && binding != null) {
        binding.progressContainer.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
      }
    });
  }

  private void loadInitialData() {
    if (postAdapter.getItemCount() == 0 && binding != null) {
      binding.progressContainer.setVisibility(View.VISIBLE);
      postViewModel.fetchPosts(15);
      postViewModel.fetchUserSuggestions();
    }
  }

  // --- تطبيقات واجهة PostInteractionListener ---

  @Override
  public void onLikeClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null && post.getAuthorId() != null) {
      postViewModel.toggleLike(post, !post.isLiked());
    } else {
      Log.e(TAG, "Cannot toggle like, data missing in ForYouFragment");
      if (getContext() != null) {
        Toast.makeText(getContext(), "Error processing like.", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public void onBookmarkClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null) {
      postViewModel.toggleBookmark(post.getPostId(), !post.isBookmarked());
    }
  }

  @Override
  public void onRepostClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null) {
      postViewModel.toggleRepost(post.getPostId(), !post.isReposted());
    }
  }

  @Override
  public void onCommentClicked(PostModel post) {
    if (post != null && getActivity() != null) {
      Intent intent = new Intent(getActivity(), PostDetailActivity.class);
      intent.putExtra(PostDetailActivity.EXTRA_POST, post);
      startActivity(intent);
    }
  }

  @Override
  public void onMenuClicked(PostModel post, View anchorView) {
    Log.d(TAG, "Menu clicked for post: " + post.getPostId() + " in ForYouFragment");
    // هنا يمكنك عرض قائمة خيارات (مثل الحذف إذا كان المستخدم هو المالك، الإبلاغ، إلخ)
    // أو فتح منتقي الريأكشنات
    showReactionPickerForPost(post); // كمثال
  }

  private void showReactionPickerForPost(PostModel post) {
    if (post == null || getParentFragmentManager() == null) return;
    this.currentPostForReaction = post;
    ReactionPickerFragment reactionPicker = new ReactionPickerFragment();
    reactionPicker.setReactionListener(this); // this Fragment implements ReactionPickerFragment.ReactionListener
    reactionPicker.show(getParentFragmentManager(), ReactionPickerFragment.TAG);
  }

  // تطبيق دالة الواجهة ReactionPickerFragment.ReactionListener
  @Override
  public void onReactionSelected(String emojiUnicode) {
    if (currentPostForReaction != null && postViewModel != null && currentAuthUserId != null) {
      Log.d(TAG, "Emoji selected by ReactionPickerFragment: " + emojiUnicode + " for post: " + currentPostForReaction.getPostId());
      postViewModel.handleEmojiSelection(currentPostForReaction, emojiUnicode);
    } else {
      Log.e(TAG, "Cannot handle emoji selection from ReactionPickerFragment, data missing in ForYouFragment.");
      if (getContext() != null) {
        Toast.makeText(getContext(), "Error reacting to post.", Toast.LENGTH_SHORT).show();
      }
    }
    currentPostForReaction = null;
  }

  // ★★★ تطبيق الدوال المفقودة من PostInteractionListener ★★★
  @Override
  public void onEmojiSelected(PostModel post, String emojiUnicode) {
    if (post != null && postViewModel != null && currentAuthUserId != null && emojiUnicode != null) {
      Log.d(TAG, "Emoji selected via PostInteractionListener: " + emojiUnicode + " for post: " + post.getPostId());
      postViewModel.handleEmojiSelection(post, emojiUnicode);
    } else {
      Log.e(TAG, "Cannot handle emoji selection from PostInteractionListener in ForYouFragment, data missing.");
    }
  }

  @Override
  public void onEmojiSummaryClicked(PostModel post) {
    Log.d(TAG, "Emoji summary clicked for post: " + (post != null ? post.getPostId() : "null"));
    if (post != null && getContext() != null) {
      Toast.makeText(getContext(), "Emoji summary clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show();
      // يمكنك فتح منتقي الريأكشنات هنا أيضًا:
      // showReactionPickerForPost(post);
    }
  }

  @Override
  public void onLikeButtonLongClicked(PostModel post, View anchorView) {
    Log.d(TAG, "Like button long clicked for post: " + (post != null ? post.getPostId() : "null"));
    if (post != null) {
      showReactionPickerForPost(post);
    }
  }


  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override public void onHashtagClicked(String hashtag) { /* لم يتم التنفيذ بعد */ }
  @Override public void onDeletePost(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null) {
      // يمكنك إضافة مربع حوار تأكيد هنا
      postViewModel.deletePost(post.getPostId());
    }
  }
  @Override public void onEditPost(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onModeratePost(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onPostLongClicked(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onMediaClicked(List<String> mediaUrls, int position) { /* لم يتم التنفيذ بعد */ }
  @Override public void onVideoPlayClicked(String videoUrl) { /* لم يتم التنفيذ بعد */ }
  @Override public void onSharePost(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onCopyLink(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onReportPost(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onToggleBookmark(PostModel post) { /* مكرر مع onBookmarkClicked */ }
  @Override public void onLayoutClicked(PostModel post) { onCommentClicked(post); }
  @Override public void onSeeMoreClicked(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onTranslateClicked(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onShowOriginalClicked(PostModel post) { /* لم يتم التنفيذ بعد */ }
  @Override public void onFollowClicked(UserModel user) { /* لم يتم التنفيذ بعد */ }
  @Override public void onUserClicked(UserModel user) {
    if (user != null && user.getUserId() != null && getActivity() != null) {
      Intent intent = new Intent(getActivity(), ProfileActivity.class);
      intent.putExtra("userId", user.getUserId());
      startActivity(intent);
    }
  }
}