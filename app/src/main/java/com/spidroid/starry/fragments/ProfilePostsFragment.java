package com.spidroid.starry.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.spidroid.starry.adapters.PostInteractionListener; // ** استيراد مهم **
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;

import java.util.ArrayList;
import java.util.List;

public class ProfilePostsFragment extends Fragment {
  private String userId;
  private RecyclerView recyclerView;
  private PostAdapter adapter;
  private TextView tvEmptyPosts;

  public ProfilePostsFragment() {}

  // استخدام newInstance لتمرير البيانات للـ Fragment هي الممارسة الأفضل
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
    setupRecyclerView();
    loadPosts();
  }

  private void setupRecyclerView() {
    // ** تم التعديل هنا: استخدام الواجهة مباشرة **
    adapter = new PostAdapter(requireContext(), new PostInteractionListener() {
      // يمكنك ترك هذه الدوال فارغة لأن التفاعلات يتم معالجتها
      // في الشاشات الرئيسية، أو يمكنك إضافة منطق خاص بالملف الشخصي هنا
      @Override public void onHashtagClicked(String hashtag) {}
      @Override public void onLikeClicked(PostModel post) {}
      @Override public void onCommentClicked(PostModel post) {}
      @Override public void onRepostClicked(PostModel post) {}
      @Override public void onBookmarkClicked(PostModel post) {}
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
      @Override public void onLayoutClicked(PostModel post) {}
      @Override public void onSeeMoreClicked(PostModel post) {}
      @Override public void onTranslateClicked(PostModel post) {}
      @Override public void onShowOriginalClicked(PostModel post) {}
      @Override public void onFollowClicked(UserModel user) {}
      @Override public void onUserClicked(UserModel user) {}
    });

    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    recyclerView.setAdapter(adapter);
  }

  private void loadPosts() {
    if (userId == null || userId.isEmpty()) {
      tvEmptyPosts.setVisibility(View.VISIBLE);
      recyclerView.setVisibility(View.GONE);
      return;
    }

    FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(
                    queryDocumentSnapshots -> {
                      if (queryDocumentSnapshots.isEmpty()) {
                        tvEmptyPosts.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                      } else {
                        List<PostModel> posts = queryDocumentSnapshots.toObjects(PostModel.class);
                        adapter.submitCombinedList(new ArrayList<>(posts));
                        tvEmptyPosts.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                      }
                    })
            .addOnFailureListener(e -> {
              Toast.makeText(getContext(), "Failed to load posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
              tvEmptyPosts.setVisibility(View.VISIBLE);
              recyclerView.setVisibility(View.GONE);
            });
  }
}