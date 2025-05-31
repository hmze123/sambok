package com.spidroid.starry.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    loadPosts();
  }

  private void setupRecyclerView() {
    adapter = new PostAdapter(requireContext(), this);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    recyclerView.setAdapter(adapter);
  }

  private void loadPosts() {
    if (userId == null || userId.isEmpty()) {
      if (tvEmptyPosts != null) tvEmptyPosts.setVisibility(View.VISIBLE);
      if (recyclerView != null) recyclerView.setVisibility(View.GONE);
      return;
    }

    FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(
                    queryDocumentSnapshots -> {
                      if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        if (tvEmptyPosts != null) tvEmptyPosts.setVisibility(View.VISIBLE);
                        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                      } else {
                        List<PostModel> posts = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                          PostModel post = doc.toObject(PostModel.class);
                          if (post != null) {
                            post.setPostId(doc.getId());
                            if (currentAuthUserId != null) {
                              post.setLiked(post.getLikes() != null && post.getLikes().containsKey(currentAuthUserId));
                              post.setBookmarked(post.getBookmarks() != null && post.getBookmarks().containsKey(currentAuthUserId));
                              post.setReposted(post.getReposts() != null && post.getReposts().containsKey(currentAuthUserId));
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
              Log.e(TAG, "Failed to load posts", e);
              if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to load posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
              }
              if (tvEmptyPosts != null) tvEmptyPosts.setVisibility(View.VISIBLE);
              if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            });
  }

  // --- تطبيقات واجهة PostInteractionListener ---
  @Override
  public void onLikeClicked(PostModel post) {
    if (postViewModel != null && post != null && post.getPostId() != null && post.getAuthorId() != null) {
      postViewModel.toggleLike(post, !post.isLiked());
    } else {
      Log.e(TAG, "Cannot toggle like, data missing");
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
  public void onMenuClicked(PostModel post, View anchorView) { // تم تعديل اسم المعامل ليكون أوضح
    Log.d(TAG, "Menu clicked for post: " + post.getPostId());
    // يمكنك هنا عرض قائمة خيارات (مثل حذف، تعديل) أو فتح منتقي الريأكشنات
    showReactionPickerForPost(post); // كمثال، فتح منتقي الريأكشنات
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
      // هنا يتم استدعاء الدالة في ViewModel لمعالجة الريأكشن
      postViewModel.handleEmojiSelection(currentPostForReaction, emojiUnicode);
    } else {
      Log.e(TAG, "Cannot handle emoji selection from ReactionPickerFragment, data missing. currentPostForReaction: " +
              (currentPostForReaction != null ? currentPostForReaction.getPostId() : "null") +
              ", postViewModel: " + (postViewModel != null) +
              ", currentAuthUserId: " + currentAuthUserId);
      if (getContext() != null) {
        Toast.makeText(getContext(), "Error reacting to post.", Toast.LENGTH_SHORT).show();
      }
    }
    currentPostForReaction = null;
  }

  // ★★★ تطبيق الدالة onEmojiSelected من PostInteractionListener ★★★
  @Override
  public void onEmojiSelected(PostModel post, String emojiUnicode) {
    // هذه الدالة يتم استدعاؤها إذا كان لديك آلية أخرى لاختيار الـ emoji
    // مباشرة من الـ PostAdapter (مثلاً، إذا كان لديك أزرار emoji ثابتة في item_post.xml)
    // أو إذا كنت ستستخدم الضغط المطول على زر الإعجاب لفتح ReactionPicker ثم تستدعي هذه الدالة.
    // حاليًا، منطق اختيار الـ emoji الرئيسي يتم عبر ReactionPickerFragment.ReactionListener.onReactionSelected.
    // لكن يجب تطبيق هذه الدالة لأنها جزء من الواجهة.
    if (post != null && postViewModel != null && currentAuthUserId != null && emojiUnicode != null) {
      Log.d(TAG, "Emoji selected via PostInteractionListener: " + emojiUnicode + " for post: " + post.getPostId());
      // يمكنك أيضًا استدعاء showReactionPickerForPost(post) من هنا إذا أردت،
      // أو إذا كان هذا الـ listener مخصصًا لآلية اختيار مختلفة، قم باستدعاء ViewModel مباشرة.
      postViewModel.handleEmojiSelection(post, emojiUnicode);
    } else {
      Log.e(TAG, "Cannot handle emoji selection from PostInteractionListener in ProfilePostsFragment, data missing.");
    }
  }


  // ★★★ تطبيق الدالة onEmojiSummaryClicked من PostInteractionListener ★★★
  @Override
  public void onEmojiSummaryClicked(PostModel post) {
    Log.d(TAG, "Emoji summary clicked for post: " + (post != null ? post.getPostId() : "null"));
    if (post != null && getContext() != null) {
      Toast.makeText(getContext(), "Emoji summary clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show();
      // يمكنك أيضًا فتح منتقي الريأكشنات هنا إذا كان هذا هو السلوك المرغوب
      // showReactionPickerForPost(post);
      // أو يمكنك عرض قائمة بالمستخدمين الذين قاموا بالريأكشن (ميزة متقدمة)
    }
  }

  // --- دوال إضافية من PostInteractionListener يجب تطبيقها ---
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

  // ★★★ تطبيق الدالة المضافة حديثًا إلى الواجهة (على افتراض أنها onLikeButtonLongClicked) ★★★
  @Override
  public void onLikeButtonLongClicked(PostModel post, View anchorView) {
    Log.d(TAG, "Like button long clicked for post: " + (post != null ? post.getPostId() : "null"));
    if (post != null) {
      showReactionPickerForPost(post); // فتح منتقي الريأكشنات عند الضغط المطول على زر الإعجاب
    }
  }
}