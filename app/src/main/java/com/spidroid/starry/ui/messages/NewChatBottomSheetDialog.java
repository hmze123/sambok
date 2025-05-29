package com.spidroid.starry.ui.messages;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.CreateGroupActivity;
import com.spidroid.starry.models.UserModel;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Date;
import com.spidroid.starry.activities.ChatActivity;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class NewChatBottomSheetDialog extends BottomSheetDialogFragment {

  private UsersAdapter adapter;
  private FirebaseFirestore db;
  private FirebaseAuth auth;

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_chat, null);
    dialog.setContentView(view);

    db = FirebaseFirestore.getInstance();
    auth = FirebaseAuth.getInstance();

    setupViews(view);
    loadUsers();
    return dialog;
  }

  private void setupViews(View view) {
    // Search functionality
    TextInputEditText etSearch = view.findViewById(R.id.et_search);
    etSearch.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            filterUsers(s.toString());
          }
        });

    // Button click listeners
    view.findViewById(R.id.btn_create_group)
        .setOnClickListener(
            v -> {
              // Handle group creation
              startActivity(new Intent(requireContext(), CreateGroupActivity.class));
              dismiss();
            });
  }

  private void loadUsers() {
    FirebaseUser currentUser = auth.getCurrentUser();
    if (currentUser == null) return;

    // First get current user's following and followers list
    db.collection("users")
        .document(currentUser.getUid())
        .get()
        .addOnSuccessListener(
            documentSnapshot -> {
              if (documentSnapshot.exists()) {
                UserModel currentUserModel = documentSnapshot.toObject(UserModel.class);
                if (currentUserModel != null) {
                  // Combine followers and following user IDs
                  Set<String> userIds = new HashSet<>();
                  userIds.addAll(currentUserModel.getFollowers().keySet());
                  userIds.addAll(currentUserModel.getFollowing().keySet());

                  if (userIds.isEmpty()) {
                    // Show empty state if needed
                    return;
                  }

                  // Split into chunks of 10 due to Firestore query limitations
                  List<List<String>> chunks = chunkList(new ArrayList<>(userIds), 10);
                  List<UserModel> allUsers = new ArrayList<>();

                  for (List<String> chunk : chunks) {
                    db.collection("users")
                        .whereIn("userId", chunk)
                        .get()
                        .addOnSuccessListener(
                            querySnapshot -> {
                              List<UserModel> chunkUsers = querySnapshot.toObjects(UserModel.class);
                              allUsers.addAll(chunkUsers);

                              // Only update when all chunks are processed
                              if (allUsers.size() >= userIds.size()) {
                                // Remove duplicates and sort
                                Set<UserModel> uniqueUsers = new HashSet<>(allUsers);
                                List<UserModel> sortedUsers = new ArrayList<>(uniqueUsers);
                                Collections.sort(
                                    sortedUsers,
                                    (u1, u2) ->
                                        u1.getDisplayName()
                                            .compareToIgnoreCase(u2.getDisplayName()));

                                updateUserList(sortedUsers);
                              }
                            });
                  }
                }
              }
            });
  }

  private void updateUserList(List<UserModel> users) {
    requireActivity()
        .runOnUiThread(
            () -> {
              adapter = new UsersAdapter(users);
              RecyclerView rvUsers = getDialog().findViewById(R.id.rv_users);
              if (rvUsers != null) {
                rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvUsers.setAdapter(adapter);
              }
            });
  }

  // Helper to split list into chunks
  private List<List<String>> chunkList(List<String> list, int chunkSize) {
    List<List<String>> chunks = new ArrayList<>();
    for (int i = 0; i < list.size(); i += chunkSize) {
      chunks.add(list.subList(i, Math.min(list.size(), i + chunkSize)));
    }
    return chunks;
  }

  private void filterUsers(String query) {
    if (adapter != null) {
      adapter.getFilter().filter(query);
    }
  }

  private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    private List<UserModel> users;
    private List<UserModel> filteredUsers;

    public UsersAdapter(List<UserModel> users) {
      this.users = users;
      this.filteredUsers = new ArrayList<>(users);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      UserModel user = filteredUsers.get(position);
      holder.bind(user);
    }

    @Override
    public int getItemCount() {
      return filteredUsers.size();
    }

    public Filter getFilter() {
      return new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
          List<UserModel> filtered = new ArrayList<>();
          String query = constraint.toString().toLowerCase();

          for (UserModel user : users) {
            if (user.getDisplayName().toLowerCase().contains(query)
                || user.getUsername().toLowerCase().contains(query)) {
              filtered.add(user);
            }
          }

          FilterResults results = new FilterResults();
          results.values = filtered;
          return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
          filteredUsers.clear();
          filteredUsers.addAll((List<UserModel>) results.values);
          notifyDataSetChanged();
        }
      };
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      private final CircleImageView ivAvatar;
      private final TextView tvName;

      ViewHolder(View itemView) {
        super(itemView);
        ivAvatar = itemView.findViewById(R.id.iv_avatar);
        tvName = itemView.findViewById(R.id.tv_name);

        itemView.setOnClickListener(
            v -> {
              UserModel user = filteredUsers.get(getAdapterPosition());
              startChatWithUser(user);
            });
      }

      void bind(UserModel user) {
        tvName.setText(user.getDisplayName());
        Glide.with(itemView)
            .load(user.getProfileImageUrl())
            .placeholder(R.drawable.ic_default_avatar)
            .into(ivAvatar);
      }
    }
  }

  private void startChatWithUser(UserModel user) {
    FirebaseUser currentUser = auth.getCurrentUser();
    if (currentUser == null) return;

    String currentUserId = currentUser.getUid();
    String selectedUserId = user.getUserId();

    // Generate unique chat ID (sorted combination of user IDs)
    String chatId = generateChatId(currentUserId, selectedUserId);

    // Check if chat already exists
    db.collection("chats")
        .document(chatId)
        .get()
        .addOnCompleteListener(
            task -> {
              if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                  // Create new chat document
                  Map<String, Object> chat = new HashMap<>();
                  chat.put("participants", Arrays.asList(currentUserId, selectedUserId));
                  chat.put("isGroup", false);
                  chat.put("createdAt", new Date());
                  chat.put("lastMessage", "");
                  chat.put("lastMessageTime", new Date());

                  db.collection("chats")
                      .document(chatId)
                      .set(chat)
                      .addOnSuccessListener(aVoid -> openChatActivity(chatId))
                      .addOnFailureListener(e -> showError());
                } else {
                  openChatActivity(chatId);
                }
              }
            });
  }

  private String generateChatId(String uid1, String uid2) {
    // Ensure consistent order for chat ID
    return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
  }

  private void openChatActivity(String chatId) {
    Intent intent = new Intent(requireContext(), ChatActivity.class);
    intent.putExtra("chatId", chatId);
    intent.putExtra("isGroup", false);
    startActivity(intent);
    dismiss();
  }

  private void showError() {
    Toast.makeText(requireContext(), "Failed to start chat", Toast.LENGTH_SHORT).show();
    dismiss();
  }
}
