package com.spidroid.starry.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView; // Import TextView
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ComposePostActivity;
import com.spidroid.starry.activities.MediaViewerActivity;
import com.spidroid.starry.activities.PostDetailActivity;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.databinding.FragmentFeedBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.viewmodels.PostViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForYouFragment extends Fragment implements PostAdapter.PostInteractionListener {

  private FragmentFeedBinding binding;
  private PostViewModel postViewModel;
  private PostAdapter postAdapter;
  private boolean isLastItemReached;
  private boolean isLoading = false;
  private static final String KEY_PRESERVE_STATE = "preserve_state";
  private boolean shouldPreserveState = false;

  // إضافة TextViews لحالة "لا توجد منشورات"
  private TextView tvNoPostsYet; //
  private TextView tvFollowUsersPrompt; //

  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentFeedBinding.inflate(inflater, container, false); //
    if (savedInstanceState != null) {
      shouldPreserveState = savedInstanceState.getBoolean(KEY_PRESERVE_STATE, false); //
    }
    return binding.getRoot(); //
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // تهيئة TextViews لحالة "لا توجد منشورات"
    tvNoPostsYet = view.findViewById(R.id.tvNoPostsYet);
    tvFollowUsersPrompt = view.findViewById(R.id.tvFollowUsersPrompt);

    setupViewModel(); //
    setupRecyclerView(); //
    setupSwipeRefresh(); //
    setupObservers(); //
    loadInitialPosts(); //
  }

  private void setupViewModel() {
    postViewModel = new ViewModelProvider(this).get(PostViewModel.class); //
  }

  private void setupRecyclerView() {
    postAdapter = new PostAdapter(requireContext(), this); //

    binding.recyclerView.setHasFixedSize(false); //
    binding.recyclerView.setItemViewCacheSize(20); //
    binding.recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW); //
    binding.recyclerView.setItemAnimator(null); //

    LinearLayoutManager layoutManager =
            new LinearLayoutManager(requireContext()) { //
              @Override
              public boolean supportsPredictiveItemAnimations() { //
                return false;
              }

              @Override
              public int getExtraLayoutSpace(RecyclerView.State state) { //
                return 500;
              }
            };

    layoutManager.setInitialPrefetchItemCount(15); //
    layoutManager.setRecycleChildrenOnDetach(true); //
    binding.recyclerView.setLayoutManager(layoutManager); //
    binding.recyclerView.setAdapter(postAdapter); //

    binding.recyclerView.addOnScrollListener(
            new RecyclerView.OnScrollListener() { //
              private final AtomicBoolean isScrolling = new AtomicBoolean(false); //
              private final Handler handler = new Handler(); //
              private final Runnable glideResumer =
                      () -> {
                        if (isScrolling.compareAndSet(true, false)) { //
                          Glide.with(requireContext()).resumeRequests(); //
                        }
                      };

              @Override
              public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState); //
                handler.removeCallbacks(glideResumer); //

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) { //
                  isScrolling.set(true); //
                  Glide.with(requireContext()).pauseRequests(); //
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) { //
                  handler.postDelayed(glideResumer, 300); //
                }
              }

              @Override
              public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy); //
                LinearLayoutManager layoutManager =
                        (LinearLayoutManager) recyclerView.getLayoutManager(); //
                if (layoutManager != null && !isLoading && !isLastItemReached) { //
                  int visibleItemCount = layoutManager.getChildCount(); //
                  int totalItemCount = layoutManager.getItemCount(); //
                  int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition(); //

                  if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                          && firstVisibleItemPosition >= 0
                          && totalItemCount >= 15) {
                    loadMorePosts(); //
                  }
                }
              }
            });
  }

  private void setupSwipeRefresh() {
    binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.primary),
            ContextCompat.getColor(requireContext(), R.color.secondary)); //
    binding.swipeRefresh.setOnRefreshListener(this::refreshPosts); //
  }

  private void setupObservers() {

    postViewModel
            .getPosts()
            .observe( // Changed to getPosts()
                    getViewLifecycleOwner(),
                    posts -> {
                      // Update your combined list logic here if needed
                    });

    postViewModel
            .getCombinedFeed()
            .observe(
                    getViewLifecycleOwner(),
                    items -> {
                      postAdapter.submitCombinedList(items); //
                      binding.progressContainer.setVisibility(View.GONE); //
                      binding.circularProgressBar.stopIndeterminateAnimation(); //
                      binding.swipeRefresh.setRefreshing(false); //
                      updateEmptyState(items.isEmpty()); // تحديث حالة "لا توجد منشورات"
                    });

    postViewModel
            .getErrorLiveData()
            .observe(
                    getViewLifecycleOwner(),
                    errorMessage -> {
                      if (errorMessage != null) {
                        showErrorSnackbar(errorMessage); //
                        binding.progressContainer.setVisibility(View.GONE); //
                        binding.circularProgressBar.stopIndeterminateAnimation(); //
                        binding.swipeRefresh.setRefreshing(false); //
                      }
                    });

    postViewModel
            .getCombinedFeed()
            .observe(
                    getViewLifecycleOwner(),
                    items -> {
                      postAdapter.submitCombinedList(items); //
                    });

    postViewModel.fetchUserSuggestions(); //
  }

  @Override
  public void onFollowClicked(UserModel user) {
    String currentUserId = FirebaseAuth.getInstance().getUid(); //
    if (currentUserId == null) return; //

    DocumentReference userRef =
            FirebaseFirestore.getInstance().collection("users").document(currentUserId); //

    userRef
            .get()
            .addOnSuccessListener(
                    snapshot -> {
                      Map<String, Boolean> following = (Map<String, Boolean>) snapshot.get("following"); //
                      boolean isFollowing = following != null && following.containsKey(user.getUserId()); //

                      if (isFollowing) {
                        userRef.update("following." + user.getUserId(), null); //
                      } else {
                        userRef.update("following." + user.getUserId(), true); //
                      }
                    });
  }

  @Override
  public void onUserClicked(UserModel user) {
    Intent intent = new Intent(requireContext(), ProfileActivity.class); //
    intent.putExtra("userId", user.getUserId()); //
    startActivity(intent); //
  }

  private void loadInitialPosts() {
    if (isLoading) return; //
    isLoading = true; //

    if (!shouldPreserveState) {
      binding.progressContainer.setVisibility(View.VISIBLE); //
      binding.circularProgressBar.startIndeterminateAnimation(); //
      postViewModel.fetchPosts(15); //
    }
  }

  private void refreshPosts() {
    isLastItemReached = false; //
    postViewModel.fetchPosts(15); //
  }

  private void loadMorePosts() {
    if (isLoading || isLastItemReached) return; //
    isLoading = true; //
    postViewModel.fetchPosts(postAdapter.getItems().size() + 15); // Use actual item count
  }

  @Override
  public void onHashtagClicked(String hashtag) {
    NavController navController =
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main); //
    Bundle args = new Bundle(); //
    args.putString("searchQuery", "#" + hashtag); //
    navController.navigate(
            R.id.navigation_search,
            args,
            new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()); //
  }

  @Override
  public void onCommentClicked(PostModel post) {
    if (post == null) {
      Toast.makeText(requireContext(), "Post not available", Toast.LENGTH_SHORT).show(); //
      return;
    }

    Intent intent = new Intent(requireContext(), PostDetailActivity.class); //
    intent.putExtra(PostDetailActivity.EXTRA_POST, post); //
    startActivity(intent); //
    getActivity().overridePendingTransition(R.anim.slide_up, R.anim.stay); //
  }

  @Override
  public void onMenuClicked(PostModel post, View anchor) {
    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext()); //
    View bottomSheetView =
            LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_post_options, null); //
    bottomSheetDialog.setContentView(bottomSheetView); //

    String currentUserId = FirebaseAuth.getInstance().getUid(); //
    boolean isAdmin = false;
    boolean isAuthor = post.getAuthorId().equals(currentUserId); //
    boolean showAdminOptions = isAdmin || isAuthor;

    MaterialButton btnDelete = bottomSheetView.findViewById(R.id.option_delete); //
    MaterialButton btnEdit = bottomSheetView.findViewById(R.id.option_edit); //
    MaterialButton btnModerate = bottomSheetView.findViewById(R.id.option_moderate); //

    btnDelete.setVisibility(showAdminOptions ? View.VISIBLE : View.GONE); //
    btnEdit.setVisibility((showAdminOptions && isAuthor) ? View.VISIBLE : View.GONE); //
    btnModerate.setVisibility((showAdminOptions && isAdmin) ? View.VISIBLE : View.GONE); //

    bottomSheetView
            .findViewById(R.id.option_report)
            .setOnClickListener(
                    v -> {
                      onReportPost(post); //
                      bottomSheetDialog.dismiss(); //
                    });

    bottomSheetView
            .findViewById(R.id.option_share)
            .setOnClickListener(
                    v -> {
                      onSharePost(post); //
                      bottomSheetDialog.dismiss(); //
                    });

    bottomSheetView
            .findViewById(R.id.option_copy_link)
            .setOnClickListener(
                    v -> {
                      onCopyLink(post); //
                      bottomSheetDialog.dismiss(); //
                    });

    bottomSheetView
            .findViewById(R.id.option_save)
            .setOnClickListener(
                    v -> {
                      onBookmarkClicked(post); //
                      bottomSheetDialog.dismiss(); //
                    });

    btnDelete.setOnClickListener(
            v -> {
              bottomSheetDialog.dismiss(); //
              onDeletePost(post); //
            });

    btnEdit.setOnClickListener(
            v -> {
              bottomSheetDialog.dismiss(); //
              onEditPost(post); //
            });

    btnModerate.setOnClickListener(
            v -> {
              bottomSheetDialog.dismiss(); //
              onModeratePost(post); //
            });

    bottomSheetDialog.show(); //
  }

  @Override
  public void onLayoutClicked(PostModel post) {
    if (post == null) {
      Toast.makeText(requireContext(), "Post not available", Toast.LENGTH_SHORT).show(); //
      return;
    }

    Intent intent = new Intent(requireContext(), PostDetailActivity.class); //
    intent.putExtra(PostDetailActivity.EXTRA_POST, post); //
    startActivity(intent); //
    getActivity().overridePendingTransition(R.anim.slide_up, R.anim.stay); //
  }

  @Override
  public void onLikeClicked(PostModel post) {
    postViewModel.toggleLike(post.getPostId(), post.isLiked()); //
  }

  @Override
  public void onBookmarkClicked(PostModel post) {
    postViewModel.toggleBookmark(post.getPostId(), post.isBookmarked()); //
  }

  @Override
  public void onRepostClicked(PostModel post) {
    postViewModel.toggleRepost(post.getPostId(), post.isReposted()); //
  }

  @Override
  public void onSharePost(PostModel post) {
    Intent shareIntent = new Intent(Intent.ACTION_SEND); //
    shareIntent.setType("text/plain"); //
    shareIntent.putExtra(Intent.EXTRA_TEXT, post.getContent() + "\n" + generatePostLink(post)); //
    startActivity(Intent.createChooser(shareIntent, "Share Post")); //
  }

  @Override
  public void onCopyLink(PostModel post) {
    ClipboardManager clipboard =
            (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE); //
    ClipData clip = ClipData.newPlainText("Post Link", generatePostLink(post)); //
    clipboard.setPrimaryClip(clip); //
    Toast.makeText(requireContext(), "Link copied", Toast.LENGTH_SHORT).show(); //
  }

  @Override
  public void onEditPost(PostModel post) {
    Intent editIntent = new Intent(requireContext(), ComposePostActivity.class); //
    editIntent.putExtra("EDIT_MODE", true); //
    editIntent.putExtra("POST_ID", post.getPostId()); //
    editIntent.putExtra("CONTENT", post.getContent()); //
    editIntent.putStringArrayListExtra("MEDIA_URLS", new ArrayList<>(post.getMediaUrls())); //
    startActivity(editIntent); //
  }

  @Override
  public void onModeratePost(PostModel post) {
    // Actual moderation implementation
  }

  @Override
  public void onReportPost(PostModel post) {
    new AlertDialog.Builder(requireContext())
            .setTitle("Report Post") //
            .setMessage("Are you sure you want to report this post?") //
            .setPositiveButton("Report", (d, w) -> postViewModel.reportPost(post.getPostId())) //
            .setNegativeButton("Cancel", null) //
            .show(); //
  }

  @Override
  public void onToggleBookmark(PostModel post) {
    postViewModel.toggleBookmark(post.getPostId(), post.isBookmarked()); //
  }

  @Override
  public void onPostLongClicked(PostModel post) {
    // Long press implementation
  }

  @Override
  public void onMediaClicked(List<String> mediaUrls, int position) {
    List<String> imageUrls = new ArrayList<>(); //
    for (String url : mediaUrls) { //
      if (!isVideoUrl(url)) { //
        imageUrls.add(url); //
      }
    }

    if (!imageUrls.isEmpty()) { //
      int imagePosition = imageUrls.indexOf(mediaUrls.get(position)); //
      if (imagePosition != -1) { //
        View mediaView = null; //

        if (mediaView != null
                && mediaView.isShown()
                && mediaView.getGlobalVisibleRect(new Rect())) { //
          MediaViewerActivity.launch(
                  requireActivity(), new ArrayList<>(imageUrls), imagePosition, mediaView); //
        } else {
          MediaViewerActivity.launchWithoutTransition(
                  requireActivity(), new ArrayList<>(imageUrls), imagePosition); //
        }
      }
    }
  }

  @Override
  public void onVideoPlayClicked(String videoUrl) {
    // Video player implementation
  }

  private String generatePostLink(PostModel post) {
    return "https://starry-app.com/posts/" + post.getPostId(); //
  }

  private boolean isVideoUrl(String url) {
    return url.matches(".*\\.(mp4|mov|avi|mkv|webm|3gp)$"); //
  }

  private void showErrorSnackbar(String message) {
    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG) //
            .setAction("Retry", v -> refreshPosts()) //
            .show(); //
  }

  @Override
  public void onDeletePost(PostModel post) {
    new AlertDialog.Builder(requireContext())
            .setTitle("Delete Post") //
            .setMessage("Are you sure you want to delete this post?") //
            .setPositiveButton("Delete", (d, w) -> postViewModel.deletePost(post.getPostId())) //
            .setNegativeButton("Cancel", null) //
            .show(); //
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState); //
    outState.putBoolean(KEY_PRESERVE_STATE, shouldPreserveState); //
  }

  @Override
  public void onSeeMoreClicked(PostModel post) {
    post.setExpanded(true); //
    int position = findPostPosition(post); //
    if (position != -1) postAdapter.notifyItemChanged(position); //
  }

  @Override
  public void onTranslateClicked(PostModel post) {
    String translatedText = "TRANSLATED: " + post.getContent(); //
    post.setTranslatedContent(translatedText); //
    post.setTranslated(true); //
    int position = findPostPosition(post); //
    if (position != -1) postAdapter.notifyItemChanged(position); //
  }

  @Override
  public void onShowOriginalClicked(PostModel post) {
    post.setTranslated(false); //
    int position = findPostPosition(post); //
    if (position != -1) postAdapter.notifyItemChanged(position); //
  }

  private int findPostPosition(PostModel post) {
    for (int i = 0; i < postAdapter.getItems().size(); i++) { //
      Object item = postAdapter.getItems().get(i); // Use getItems()
      if (item instanceof PostModel && ((PostModel) item).getPostId().equals(post.getPostId())) { //
        return i;
      }
    }
    return -1; //
  }

  @Override
  public void onResume() {
    super.onResume(); //
    if (!shouldPreserveState && postAdapter.getItemCount() == 0) {
      loadInitialPosts(); //
    }
    shouldPreserveState = false; //
  }

  @Override
  public void onPause() {
    super.onPause(); //
    shouldPreserveState = true; //
    binding.swipeRefresh.setRefreshing(false); //
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView(); //
    Glide.with(this).clear(binding.recyclerView); //
    binding.recyclerView.setAdapter(null); //
    binding = null; //
  }

  // إضافة الدالة الجديدة هنا
  private void updateEmptyState(boolean isEmpty) {
    if (isEmpty) {
      tvNoPostsYet.setVisibility(View.VISIBLE);
      tvFollowUsersPrompt.setVisibility(View.VISIBLE);
      binding.recyclerView.setVisibility(View.GONE);
    } else {
      tvNoPostsYet.setVisibility(View.GONE);
      tvFollowUsersPrompt.setVisibility(View.GONE);
      binding.recyclerView.setVisibility(View.VISIBLE);
    }
  }
}