package com.spidroid.starry.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.google.firebase.firestore.Query;
import com.spidroid.starry.adapters.PostAdapter;
import com.spidroid.starry.models.PostModel;
import java.util.ArrayList;
import java.util.List;

public class ProfilePostsFragment extends Fragment {
  private String userId;
  private RecyclerView recyclerView;
  private PostAdapter adapter;

  public ProfilePostsFragment() {}

  public ProfilePostsFragment(String userId) {
    this.userId = userId;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_profile_posts, container, false);
    recyclerView = view.findViewById(R.id.recyclerView);
    return view;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupRecyclerView();
    loadPosts();
  }

  private void setupRecyclerView() {
//    adapter = new PostAdapter(requireContext(), new PostInteractionListenerImpl());
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
            });
  }

//  private class PostInteractionListenerImpl implements PostAdapter.PostInteractionListener {
//    // Implement all required methods with empty bodies
//    @Override
//    public void onHashtagClicked(String hashtag) {}
//
//    @Override
//    public void onLikeClicked(PostModel post) {}
//    // ... implement all other required interface methods ...
//  }
}
