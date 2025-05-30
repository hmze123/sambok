package com.spidroid.starry.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // استيراد TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfilePostsFragment extends Fragment {
  private String userId;
  private RecyclerView recyclerView;
  private PostAdapter adapter;
  private TextView tvEmptyPosts; // إضافة TextView لحالة الفراغ

  public ProfilePostsFragment() {}

  public ProfilePostsFragment(String userId) {
    this.userId = userId;
  }

  @Override
  public View onCreateView(
          LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_profile_posts, container, false);
    recyclerView = view.findViewById(R.id.recyclerView);
    tvEmptyPosts = view.findViewById(R.id.tv_empty_posts); // تهيئة tvEmptyPosts
    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupRecyclerView();
    loadPosts();
  }

  private void setupRecyclerView() {
    adapter = new PostAdapter(requireContext(), new PostAdapter.PostInteractionListener() {
      @Override public void onHashtagClicked(String hashtag) { Toast.makeText(getContext(), "Hashtag clicked: " + hashtag, Toast.LENGTH_SHORT).show(); }
      @Override public void onLikeClicked(PostModel post) { Toast.makeText(getContext(), "Like clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onCommentClicked(PostModel post) { Toast.makeText(getContext(), "Comment clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onRepostClicked(PostModel post) { Toast.makeText(getContext(), "Repost clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onBookmarkClicked(PostModel post) { Toast.makeText(getContext(), "Bookmark clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onMenuClicked(PostModel post, View anchor) { Toast.makeText(getContext(), "Menu clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onDeletePost(PostModel post) { Toast.makeText(getContext(), "Delete post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onEditPost(PostModel post) { Toast.makeText(getContext(), "Edit post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onModeratePost(PostModel post) { Toast.makeText(getContext(), "Moderate post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onPostLongClicked(PostModel post) { Toast.makeText(getContext(), "Post long clicked: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onMediaClicked(List<String> mediaUrls, int position) { Toast.makeText(getContext(), "Media clicked: " + mediaUrls.get(position), Toast.LENGTH_SHORT).show(); }
      @Override public void onVideoPlayClicked(String videoUrl) { Toast.makeText(getContext(), "Video play clicked: " + videoUrl, Toast.LENGTH_SHORT).show(); }
      @Override public void onSharePost(PostModel post) { Toast.makeText(getContext(), "Share post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onCopyLink(PostModel post) { Toast.makeText(getContext(), "Copy link: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onReportPost(PostModel post) { Toast.makeText(getContext(), "Report post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onToggleBookmark(PostModel post) { Toast.makeText(getContext(), "Toggle bookmark for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onLayoutClicked(PostModel post) { Toast.makeText(getContext(), "Layout clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onSeeMoreClicked(PostModel post) { Toast.makeText(getContext(), "See more clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onTranslateClicked(PostModel post) { Toast.makeText(getContext(), "Translate clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onShowOriginalClicked(PostModel post) { Toast.makeText(getContext(), "Show original clicked for post: " + post.getPostId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onFollowClicked(UserModel user) { Toast.makeText(getContext(), "Follow clicked for user: " + user.getUserId(), Toast.LENGTH_SHORT).show(); }
      @Override public void onUserClicked(UserModel user) { Toast.makeText(getContext(), "User clicked: " + user.getUserId(), Toast.LENGTH_SHORT).show(); }
    });

    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    recyclerView.setAdapter(adapter);
  }

  private void loadPosts() {
    FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(
                    queryDocumentSnapshots -> {
                      List<PostModel> posts = queryDocumentSnapshots.toObjects(PostModel.class);
                      adapter.submitCombinedList(new ArrayList<>(posts));
                      // إظهار/إخفاء رسالة "لا توجد منشورات"
                      if (posts.isEmpty()) {
                        tvEmptyPosts.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                      } else {
                        tvEmptyPosts.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                      }
                    })
            .addOnFailureListener(e -> {
              Toast.makeText(getContext(), "Failed to load posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
              tvEmptyPosts.setVisibility(View.VISIBLE); // إظهار الرسالة في حالة الفشل
              recyclerView.setVisibility(View.GONE);
            });
  }
}