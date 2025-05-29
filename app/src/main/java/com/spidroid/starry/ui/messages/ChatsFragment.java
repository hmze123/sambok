package com.spidroid.starry.ui.messages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ChatActivity;
import com.spidroid.starry.adapters.ChatsAdapter;
import com.spidroid.starry.models.Chat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatsFragment extends Fragment implements ChatsAdapter.ChatClickListener {

  private FirebaseFirestore db;
  private FirebaseAuth auth;
  private ChatsAdapter adapter;
  private ListenerRegistration chatsListener;
  private TextView tvEmpty;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    db = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_chats, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupRecyclerView(view);
    listenForChats();
  }

  private void setupRecyclerView(View view) {
    RecyclerView rvChats = view.findViewById(R.id.chatsRecyclerView);
    rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new ChatsAdapter(this);
    rvChats.setAdapter(adapter);

    tvEmpty = view.findViewById(R.id.tvEmpty);
    adapter.registerAdapterDataObserver(
        new RecyclerView.AdapterDataObserver() {
          @Override
          public void onChanged() {
            checkEmpty();
          }
        });
  }

  private void checkEmpty() {
    tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
  }

  private void listenForChats() {
    FirebaseUser user = auth.getCurrentUser();
    if (user == null) {
      Log.e("ChatsFragment", "User is not authenticated");
      return;
    }

    Query query =
        db.collection("chats")
            .whereArrayContains("participants", user.getUid())
            .whereGreaterThan("lastMessageTime", new Date(0))
            .orderBy("lastMessageTime", Query.Direction.DESCENDING);

    chatsListener =
        query.addSnapshotListener(
            (value, error) -> {
              if (error != null) {
                Log.e("ChatsFragment", "Listen failed", error);
                return;
              }

              Log.d("ChatsFragment", "Received " + value.size() + " chats");
              List<Chat> chats = new ArrayList<>();
              for (QueryDocumentSnapshot doc : value) {
                Chat chat = doc.toObject(Chat.class);
                chat.setId(doc.getId());
                chats.add(chat);
                Log.d("ChatsFragment", "Chat ID: " + chat.getId());
              }
              adapter.submitList(chats);
            });
  }

  @Override
  public void onChatClick(Chat chat) {
    Intent intent = new Intent(getActivity(), ChatActivity.class);
    intent.putExtra("chatId", chat.getId());
    intent.putExtra("isGroup", chat.isGroup());
    startActivity(intent);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (chatsListener != null) {
      chatsListener.remove();
    }
  }
}
