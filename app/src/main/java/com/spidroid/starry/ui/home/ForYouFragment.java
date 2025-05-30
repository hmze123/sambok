package com.spidroid.starry.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.PostDetailActivity;
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.adapters.PostInteractionListener; // ** استيراد مهم **
import com.spidroid.starry.databinding.FragmentFeedBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.viewmodels.PostViewModel;

import java.util.List;

public class ForYouFragment extends Fragment implements PostInteractionListener { // ** تم التعديل هنا **

  private FragmentFeedBinding binding;
  private PostViewModel postViewModel;
  private PostAdapter postAdapter;
  private boolean isLoading = false;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentFeedBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    postViewModel = new ViewModelProvider(this).get(PostViewModel.class);

    setupRecyclerView();
    setupSwipeRefresh();
    setupObservers();

    loadInitialData();
  }

  private void setupRecyclerView() {
    postAdapter = new PostAdapter(requireContext(), this);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.recyclerView.setAdapter(postAdapter);
  }

  private void setupSwipeRefresh() {
    binding.swipeRefresh.setOnRefreshListener(() -> {
      isLoading = false;
      postViewModel.fetchPosts(15);
      postViewModel.fetchUserSuggestions();
    });
  }

  private void setupObservers() {
    postViewModel.getCombinedFeed().observe(getViewLifecycleOwner(), items -> {
      binding.progressContainer.setVisibility(View.GONE);
      binding.swipeRefresh.setRefreshing(false);

      postAdapter.submitCombinedList(items);

      boolean isEmpty = items == null || items.isEmpty();
      binding.emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
      binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    });

    postViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
      if (errorMessage != null) {
        binding.progressContainer.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
      }
    });
  }

  private void loadInitialData() {
    if (postAdapter.getItemCount() == 0) {
      binding.progressContainer.setVisibility(View.VISIBLE);
      postViewModel.fetchPosts(15);
      postViewModel.fetchUserSuggestions();
    }
  }

  // --- تطبيق واجهة التفاعل ---

  @Override
  public void onLikeClicked(PostModel post) {
    postViewModel.toggleLike(post.getPostId(), !post.isLiked());
  }

  @Override
  public void onBookmarkClicked(PostModel post) {
    postViewModel.toggleBookmark(post.getPostId(), !post.isBookmarked());
  }

  @Override
  public void onRepostClicked(PostModel post) {
    postViewModel.toggleRepost(post.getPostId(), !post.isReposted());
  }

  @Override
  public void onCommentClicked(PostModel post) {
    Intent intent = new Intent(getActivity(), PostDetailActivity.class);
    intent.putExtra(PostDetailActivity.EXTRA_POST, post);
    startActivity(intent);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  // --- باقي دوال الـ listener، اتركها فارغة إذا لم تكن بحاجة إليها الآن ---
  @Override public void onHashtagClicked(String hashtag) {}
  @Override public void onMenuClicked(PostModel post, View anchor) {}
  @Override public void onDeletePost(PostModel post) {}
  @Override public void onEditPost(PostModel post) {}
  @Override public void onModeratePost(PostModel post) {}
  @Override public void onPostLongClicked(PostModel post) {}
  @Override public void onMediaClicked(List<String> mediaUrls, int position) {}
  @Override public void onVideoPlayClicked(String videoUrl) {}
  @Override public void onSharePost(PostModel post) {}
  @Override public void onCopyLink(PostModel post) {}
  @Override public void onReportPost(PostModel post) {}
  @Override public void onToggleBookmark(PostModel post) {}
  @Override public void onLayoutClicked(PostModel post) {
    // عند النقر على المنشور، انتقل إلى شاشة التفاصيل
    onCommentClicked(post);
  }
  @Override public void onSeeMoreClicked(PostModel post) {}
  @Override public void onTranslateClicked(PostModel post) {}
  @Override public void onShowOriginalClicked(PostModel post) {}
  @Override public void onFollowClicked(UserModel user) {}
  @Override public void onUserClicked(UserModel user) {}
}