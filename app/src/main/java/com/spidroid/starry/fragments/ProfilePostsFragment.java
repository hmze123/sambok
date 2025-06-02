package com.spidroid.starry.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.PostDetailActivity;
import com.spidroid.starry.activities.ProfileActivity;
// import com.spidroid.starry.activities.ComposePostActivity;
// import com.spidroid.starry.activities.ReportActivity;
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.adapters.PostInteractionListener;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.ui.common.ReactionPickerFragment;
import com.spidroid.starry.viewmodels.PostViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProfilePostsFragment extends Fragment implements PostInteractionListener, ReactionPickerFragment.ReactionListener {
  private String userId;
  private RecyclerView recyclerView;
  private PostAdapter adapter;
  private TextView tvEmptyPosts;
  private PostViewModel postViewModel;
  private String currentAuthUserId;
  private PostModel currentPostForReaction;

  private static final String TAG = "ProfilePostsFragment";

  public ProfilePostsFragment() {}

  public static ProfilePostsFragment newInstance(String userId) {
    ProfilePostsFragment fragment = new ProfilePostsFragment();
    Bundle args = new Bundle();
    args.putString("USER_ID", userId);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      userId = getArguments().getString("USER_ID");
    }
    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
      currentAuthUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    } else {
      Log.w(TAG, "Current user is not authenticated.");
    }
  }

  @Override
  public View onCreateView(
          LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_profile_posts, container, false);
    recyclerView = view.findViewById(R.id.recyclerView);
    tvEmptyPosts = view.findViewById(R.id.tv_empty_posts);
    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    postViewModel = new ViewModelProvider(requireActivity()).get(PostViewModel.class);
    setupRecyclerView();
    loadPosts(); // تحميل أولي

    // مراقبة حدث تحديث المنشور (مفيد لتحديث القائمة بعد تثبيت/إلغاء تثبيت منشور)
    postViewModel.getPostUpdatedEvent().observe(getViewLifecycleOwner(), updatedPostId -> {
      if (updatedPostId != null && userId != null) {
        // إذا كان التحديث متعلقًا بمنشور في هذا الملف الشخصي، قم بإعادة التحميل
        Log.d(TAG, "Received post update event for " + updatedPostId + ", reloading posts for profile: " + userId);
        loadPosts();
      }
    });

    postViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
      if (errorMessage != null && getContext() != null) {
        Log.e(TAG, "Error LiveData observed in ProfilePostsFragment: " + errorMessage);
        // يمكنك عرض رسالة خطأ عامة هنا إذا أردت
      }
    });
  }

  private void setupRecyclerView() {
    if (getContext() == null) return;
    adapter = new PostAdapter(requireContext(), this);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    recyclerView.setAdapter(adapter);
  }

  private void loadPosts() {
    if (userId == null || userId.isEmpty()) {
      if (tvEmptyPosts != null) tvEmptyPosts.setVisibility(View.VISIBLE);
      if (recyclerView != null) recyclerView.setVisibility(View.GONE);
      if (adapter!= null) adapter.submitCombinedList(new ArrayList<>());
      return;
    }
    Log.d(TAG, "Loading posts for user: " + userId);

    FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("isPinned", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(
                    queryDocumentSnapshots -> {
                      if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No posts found for user: " + userId);
                        if (tvEmptyPosts != null) tvEmptyPosts.setVisibility(View.VISIBLE);
                        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                        if (adapter != null) adapter.submitCombinedList(new ArrayList<>());
                      } else {
                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " posts for user: " + userId);
                        List<PostModel> posts = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                          PostModel post = doc.toObject(PostModel.class);
                          if (post != null) {
                            post.setPostId(doc.getId());
                            if (currentAuthUserId != null) {
                              post.setLiked(post.getLikes() != null && post.getLikes().containsKey(currentAuthUserId));
                              post.setBookmarked(post.getBookmarks() != null && post.getBookmarks().containsKey(currentAuthUserId));
                              post.setReposted(post.getReposts() != null && post.getReposts().containsKey(currentAuthUserId));
                              // isPinned يتم جلبها مباشرة من Firestore
                            }
                            posts.add(post);
                          }
                        }
                        if (adapter != null) adapter.submitCombinedList(new ArrayList<>(posts));
                        if (tvEmptyPosts != null) tvEmptyPosts.setVisibility(View.GONE);
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                      }
                    })
            .addOnFailureListener(e -> {
              Log.e(TAG, "Failed to load posts for user " + userId, e);
              if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to load posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
              }
              if (tvEmptyPosts != null) tvEmptyPosts.setVisibility(View.VISIBLE);
              if (recyclerView != null) recyclerView.setVisibility(View.GONE);
              if (adapter != null) adapter.submitCombinedList(new ArrayList<>());
            });
  }

  // --- تطبيقات واجهة PostInteractionListener ---
  @Override
  public void onLikeClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null && post.getAuthorId() != null) {
      postViewModel.toggleLike(post, post.isLiked());
    } else {
      Log.e(TAG, "Cannot toggle like, data missing in ProfilePostsFragment");
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
  public void onBookmarkClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null) {
      postViewModel.toggleBookmark(post.getPostId(), post.isBookmarked());
    }
  }

  @Override
  public void onRepostClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null) {
      postViewModel.toggleRepost(post.getPostId(), post.isReposted());
    }
  }

  @Override
  public void onMenuClicked(PostModel post, View anchorView) {
    if (getContext() == null || post == null || anchorView == null || currentAuthUserId == null) {
      Log.e(TAG, "Cannot show post options menu, data missing in ProfilePostsFragment.");
      return;
    }
    Log.d(TAG, "Menu clicked for post: " + post.getPostId());

    PopupMenu popup = new PopupMenu(requireContext(), anchorView);
    popup.getMenuInflater().inflate(R.menu.post_options_menu, popup.getMenu());
    boolean isAuthor = currentAuthUserId.equals(post.getAuthorId());

    MenuItem pinItem = popup.getMenu().findItem(R.id.action_pin_post);
    MenuItem editItem = popup.getMenu().findItem(R.id.action_edit_post);
    MenuItem deleteItem = popup.getMenu().findItem(R.id.action_delete_post);
    MenuItem privacyItem = popup.getMenu().findItem(R.id.action_edit_privacy);
    MenuItem reportItem = popup.getMenu().findItem(R.id.action_report_post);
    MenuItem saveItem = popup.getMenu().findItem(R.id.action_save_post);
    MenuItem copyLinkItem = popup.getMenu().findItem(R.id.action_copy_link);
    MenuItem shareItem = popup.getMenu().findItem(R.id.action_share_post);

    if (pinItem != null) {
      pinItem.setVisible(isAuthor);
      if (isAuthor) pinItem.setTitle(post.isPinned() ? "إلغاء تثبيت المنشور" : "تثبيت المنشور");
    }
    if (editItem != null) editItem.setVisible(isAuthor);
    if (deleteItem != null) deleteItem.setVisible(isAuthor);
    if (privacyItem != null) privacyItem.setVisible(isAuthor);
    if (reportItem != null) reportItem.setVisible(!isAuthor);
    if (saveItem != null) saveItem.setTitle(post.isBookmarked() ? "إلغاء حفظ المنشور" : "حفظ المنشور");
    if (copyLinkItem != null) copyLinkItem.setVisible(true);
    if (shareItem != null) shareItem.setVisible(true);

    popup.setOnMenuItemClickListener(item -> {
      int itemId = item.getItemId();
      if (itemId == R.id.action_pin_post) onTogglePinPostClicked(post);
      else if (itemId == R.id.action_edit_post) onEditPost(post);
      else if (itemId == R.id.action_delete_post) onDeletePost(post);
      else if (itemId == R.id.action_copy_link) onCopyLink(post);
      else if (itemId == R.id.action_share_post) onSharePost(post);
      else if (itemId == R.id.action_save_post) onBookmarkClicked(post);
      else if (itemId == R.id.action_edit_privacy) onEditPostPrivacy(post);
      else if (itemId == R.id.action_report_post) onReportPost(post);
      else return false;
      return true;
    });
    popup.show();
  }

  private void showReactionPickerForPost(PostModel post) {
    if (post == null || getParentFragmentManager() == null) {
      Log.e(TAG, "Cannot show reaction picker, post or FragmentManager is null in ProfilePostsFragment");
      return;
    }
    this.currentPostForReaction = post;
    ReactionPickerFragment reactionPicker = new ReactionPickerFragment();
    reactionPicker.setReactionListener(this);
    reactionPicker.show(getParentFragmentManager(), ReactionPickerFragment.TAG);
  }

  // تطبيق دالة الواجهة ReactionPickerFragment.ReactionListener
  @Override
  public void onReactionSelected(String emojiUnicode) {
    if (currentPostForReaction != null && postViewModel != null && currentAuthUserId != null) {
      Log.d(TAG, "Emoji selected by ReactionPickerFragment: " + emojiUnicode + " for post: " + currentPostForReaction.getPostId());
      postViewModel.handleEmojiSelection(currentPostForReaction, emojiUnicode);
    } else {
      Log.e(TAG, "Cannot handle emoji selection from ReactionPickerFragment, data missing.");
      if (getContext() != null) Toast.makeText(getContext(), "Error reacting to post.", Toast.LENGTH_SHORT).show();
    }
    currentPostForReaction = null;
  }

  // --- تطبيق الدوال المتبقية من PostInteractionListener ---
  @Override
  public void onTogglePinPostClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null && post.getAuthorId() != null) {
      Log.d(TAG, "Toggle pin clicked for post: " + post.getPostId() + " in ProfilePostsFragment");
      postViewModel.togglePostPinStatus(post);
      // لا حاجة لإعادة تحميل المنشورات هنا مباشرة، الـ LiveData _postUpdatedEvent سيتولى ذلك
    } else {
      Log.e(TAG, "Cannot toggle pin status, data missing in ProfilePostsFragment.");
      if (getContext() != null) Toast.makeText(getContext(), "Error processing pin/unpin action.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onEditPost(PostModel post) {
    Log.d(TAG, "Edit post clicked: " + post.getPostId());
    if (getContext()!= null && currentAuthUserId != null && currentAuthUserId.equals(post.getAuthorId())) {
      // Intent intent = new Intent(getActivity(), ComposePostActivity.class);
      // intent.putExtra("EDIT_POST_ID", post.getPostId());
      // startActivity(intent);
      Toast.makeText(getContext(), "TODO: Open ComposePostActivity in edit mode for: " + post.getPostId(), Toast.LENGTH_SHORT).show();
    } else if (getContext() != null) {
      Toast.makeText(getContext(), "You can only edit your own posts.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onDeletePost(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null &&
            currentAuthUserId != null && currentAuthUserId.equals(post.getAuthorId())) {
      new AlertDialog.Builder(requireContext())
              .setTitle("Delete Post")
              .setMessage("Are you sure you want to delete this post?")
              .setPositiveButton("Delete", (dialog, which) -> postViewModel.deletePost(post.getPostId()))
              .setNegativeButton("Cancel", null)
              .show();
    } else if (getContext() != null && post != null && currentAuthUserId != null && !currentAuthUserId.equals(post.getAuthorId())) {
      Toast.makeText(getContext(), "You can only delete your own posts.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onCopyLink(PostModel post) {
    if (post == null || post.getPostId() == null || getContext() == null) return;
    String postUrl = "https://starry.app/post/" + post.getPostId(); // ★ استبدل برابط تطبيقك الفعلي
    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    android.content.ClipData clip = android.content.ClipData.newPlainText("Post URL", postUrl);
    if (clipboard != null) {
      clipboard.setPrimaryClip(clip);
      Toast.makeText(getContext(), "Link copied!", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onSharePost(PostModel post) {
    if (post == null || getActivity() == null || post.getPostId() == null) return;
    String postUrl = "https://starry.app/post/" + post.getPostId(); // ★ استبدل برابط تطبيقك الفعلي
    String shareText = "Check out this post on Starry: " +
            (post.getContent() != null && post.getContent().length() > 70 ?
                    post.getContent().substring(0, 70) + "..." :
                    post.getContent()) +
            "\n" + postUrl;
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Post from Starry");
    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
    startActivity(Intent.createChooser(shareIntent, "Share post via"));
  }

  @Override
  public void onEditPostPrivacy(PostModel post) {
    Log.d(TAG, "Edit post privacy for: " + (post != null ? post.getPostId() : "null"));
    if (getContext() != null && post != null && currentAuthUserId != null && currentAuthUserId.equals(post.getAuthorId())) {
      Toast.makeText(getContext(), "TODO: Implement Edit Post Privacy UI for post: " + post.getPostId(), Toast.LENGTH_SHORT).show();
      // مثال: postViewModel.setPostPrivacy(post.getPostId(), "followers_only");
    } else if (getContext() != null) {
      Toast.makeText(getContext(), "You can only edit privacy for your own posts.", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onReportPost(PostModel post) {
    Log.d(TAG, "Report post: " + (post != null ? post.getPostId() : "null"));
    if (getContext() != null && post != null) {
      // Intent intent = new Intent(getActivity(), ReportActivity.class);
      // intent.putExtra("REPORTED_POST_ID", post.getPostId());
      // intent.putExtra("REPORTED_AUTHOR_ID", post.getAuthorId()); // ★ تمرير معرّف صاحب المنشور
      // startActivity(intent);
      Toast.makeText(getContext(), "TODO: Open ReportActivity for: " + post.getPostId(), Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onLikeButtonLongClicked(PostModel post, View anchorView) {
    Log.d(TAG, "Like button long clicked for post: " + (post != null ? post.getPostId() : "null"));
    if (post != null) showReactionPickerForPost(post);
  }

  @Override
  public void onEmojiSelected(PostModel post, String emojiUnicode) {
    if (post != null && postViewModel != null && currentAuthUserId != null && emojiUnicode != null) {
      Log.d(TAG, "Emoji selected via PostInteractionListener in ProfilePosts: " + emojiUnicode + " for post: " + post.getPostId());
      postViewModel.handleEmojiSelection(post, emojiUnicode);
    } else {
      Log.e(TAG, "Cannot handle emoji selection from PostInteractionListener in ProfilePostsFragment, data missing.");
    }
  }

  @Override
  public void onEmojiSummaryClicked(PostModel post) {
    Log.d(TAG, "Emoji summary clicked for post: " + (post != null ? post.getPostId() : "null"));
    if (post != null && getContext() != null) Toast.makeText(getContext(), "Emoji summary: " + post.getPostId(), Toast.LENGTH_SHORT).show();
  }

  // --- باقي دوال الواجهة ---
  @Override public void onHashtagClicked(String hashtag) { Log.d(TAG, "Hashtag clicked: " + hashtag); }
  @Override public void onPostLongClicked(PostModel post) { Log.d(TAG, "Post long clicked: " + post.getPostId()); }
  @Override public void onMediaClicked(List<String> mediaUrls, int position) { Log.d(TAG, "Media clicked"); }
  @Override public void onVideoPlayClicked(String videoUrl) { Log.d(TAG, "Video play clicked: " + videoUrl); }
  @Override public void onLayoutClicked(PostModel post) { onCommentClicked(post); }
  @Override public void onSeeMoreClicked(PostModel post) { Log.d(TAG, "See more clicked for post: " + post.getPostId()); }
  @Override public void onTranslateClicked(PostModel post) { Log.d(TAG, "Translate clicked for post: " + post.getPostId()); }
  @Override public void onShowOriginalClicked(PostModel post) { Log.d(TAG, "Show original clicked for post: " + post.getPostId()); }
  @Override public void onFollowClicked(UserModel user) { Log.d(TAG, "Follow clicked for user: " + (user != null ? user.getUserId() : "null")); }
  @Override public void onUserClicked(UserModel user) {
    if (user != null && user.getUserId() != null && getActivity() != null) {
      Intent intent = new Intent(getActivity(), ProfileActivity.class);
      intent.putExtra("userId", user.getUserId());
      startActivity(intent);
    }
  }
  @Override public void onModeratePost(PostModel post) { Log.d(TAG, "Moderate post clicked: " + (post != null ? post.getPostId() : "null")); }
}