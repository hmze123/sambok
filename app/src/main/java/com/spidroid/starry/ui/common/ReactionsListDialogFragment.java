package com.spidroid.starry.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.utils.PostInteractionHandler; // ⭐ استيراد للوصول إلى getDrawableIdForEmoji ⭐

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ReactionsListDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_USER_IDS = "user_ids";
    private static final String ARG_REACTIONS_MAP = "reactions_map";
    private static final String TAG = "ReactionsListDialog";

    private RecyclerView recyclerView;
    private ReactionUserAdapter adapter;
    private ArrayList<String> userIds;
    private HashMap<String, String> reactionsMap;
    private FirebaseFirestore db;
    private ProgressBar loadingProgressBar;
    private TextView emptyReactionsText;


    public static ReactionsListDialogFragment newInstance(ArrayList<String> userIds, Map<String, String> reactionsMap) {
        ReactionsListDialogFragment fragment = new ReactionsListDialogFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_USER_IDS, userIds);
        args.putSerializable(ARG_REACTIONS_MAP, reactionsMap != null ? new HashMap<>(reactionsMap) : new HashMap<>());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            userIds = getArguments().getStringArrayList(ARG_USER_IDS);
            reactionsMap = (HashMap<String, String>) getArguments().getSerializable(ARG_REACTIONS_MAP);
        }
        if (userIds == null) userIds = new ArrayList<>();
        if (reactionsMap == null) reactionsMap = new HashMap<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_reactions_list, container, false);
        recyclerView = view.findViewById(R.id.rv_reaction_users);
        loadingProgressBar = view.findViewById(R.id.loading_reactions_progress);
        emptyReactionsText = view.findViewById(R.id.empty_reactions_text);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new ReactionUserAdapter(new ArrayList<>(), reactionsMap, getContext(), userId -> {
            if (userId != null && !userId.isEmpty() && getActivity() != null) {
                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                dismiss();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (userIds.isEmpty()) {
            showEmptyState();
        } else {
            fetchUsersDetails();
        }
    }

    private void showLoading() {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyReactionsText != null) emptyReactionsText.setVisibility(View.GONE);
    }

    private void showContent() {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (emptyReactionsText != null) emptyReactionsText.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyReactionsText != null) {
            emptyReactionsText.setVisibility(View.VISIBLE);
            // تأكد من إضافة هذا المورد إلى strings.xml: <string name="no_reactions_yet">No reactions yet.</string>
            emptyReactionsText.setText(R.string.no_reactions_yet);
        }
    }


    private void fetchUsersDetails() {
        showLoading();
        final List<UserModel> fetchedUsers = new ArrayList<>();
        if (userIds == null || userIds.isEmpty()) {
            showEmptyState();
            return;
        }

        List<List<String>> chunks = chunkList(userIds, 10);
        final int[] chunksProcessed = {0};

        if (chunks.isEmpty()){
            showEmptyState();
            return;
        }

        for (List<String> chunk : chunks) {
            if (chunk.isEmpty()) {
                chunksProcessed[0]++;
                if (chunksProcessed[0] == chunks.size()) {
                    adapter.updateUsers(fetchedUsers);
                    if (fetchedUsers.isEmpty()) showEmptyState(); else showContent();
                }
                continue;
            }
            db.collection("users").whereIn("userId", chunk).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null) {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                UserModel user = doc.toObject(UserModel.class);
                                if (user != null) {
                                    user.setUserId(doc.getId());
                                    fetchedUsers.add(user);
                                }
                            }
                        }
                        chunksProcessed[0]++;
                        if (chunksProcessed[0] == chunks.size()) {
                            adapter.updateUsers(fetchedUsers);
                            if (fetchedUsers.isEmpty()) showEmptyState(); else showContent();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user details chunk", e);
                        chunksProcessed[0]++;
                        if (chunksProcessed[0] == chunks.size()) {
                            adapter.updateUsers(fetchedUsers);
                            if (fetchedUsers.isEmpty()) showEmptyState(); else showContent();
                            if (getContext() != null) {
                                Toast.makeText(getContext(), R.string.failed_to_load_some_user_details, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        if (list == null || list.isEmpty() || chunkSize <= 0) return chunks;
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + chunkSize))));
        }
        return chunks;
    }

    private static class ReactionUserAdapter extends RecyclerView.Adapter<ReactionUserAdapter.ViewHolder> {
        private List<UserModel> users;
        private Map<String, String> userReactionsMap;
        // ⭐ لم نعد بحاجة لـ dummyInteractionHandler كمثيل هنا ⭐
        private OnUserClickListener userClickListener;
        private Context adapterContext; // ⭐ إضافة Context للعثور على الموارد ⭐


        interface OnUserClickListener {
            void onUserItemClicked(String userId);
        }

        ReactionUserAdapter(List<UserModel> users, Map<String, String> reactionsMap, Context context, OnUserClickListener clickListener) {
            this.users = users;
            this.userReactionsMap = reactionsMap != null ? reactionsMap : new HashMap<>();
            this.userClickListener = clickListener;
            this.adapterContext = context; // ⭐ حفظ الـ Context ⭐
        }

        public void updateUsers(List<UserModel> newUsers) {
            this.users.clear();
            if (newUsers != null) {
                this.users.addAll(newUsers);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_reaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserModel user = users.get(position);
            if (user == null) return;

            String displayName = user.getDisplayName();
            if (TextUtils.isEmpty(displayName)) {
                displayName = user.getUsername();
            }
            if (TextUtils.isEmpty(displayName) && adapterContext != null) { // ⭐ استخدام adapterContext ⭐
                displayName = adapterContext.getString(R.string.unknown_user_display_name);
            } else if (TextUtils.isEmpty(displayName)) {
                displayName = "User";
            }
            holder.tvUserName.setText(displayName);

            if (adapterContext != null) { // ⭐ استخدام adapterContext ⭐
                Glide.with(adapterContext)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .into(holder.ivUserAvatar);
            }


            String reactionEmoji = userReactionsMap.get(user.getUserId());
            if (reactionEmoji != null && !reactionEmoji.isEmpty()) {
                // ⭐ استدعاء الدالة بشكل ثابت ⭐
                int drawableId = PostInteractionHandler.getDrawableIdForEmoji(reactionEmoji, true); // true للأيقونة الصغيرة
                if (drawableId != 0) {
                    holder.ivReactionEmoji.setImageResource(drawableId);
                    holder.ivReactionEmoji.clearColorFilter();
                    holder.ivReactionEmoji.setVisibility(View.VISIBLE);
                } else {
                    holder.ivReactionEmoji.setVisibility(View.GONE);
                }
            } else {
                holder.ivReactionEmoji.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (userClickListener != null && user.getUserId() != null) {
                    userClickListener.onUserItemClicked(user.getUserId());
                }
            });
        }

        @Override
        public int getItemCount() {
            return users != null ? users.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            CircleImageView ivUserAvatar;
            TextView tvUserName;
            ImageView ivReactionEmoji;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar_reaction);
                tvUserName = itemView.findViewById(R.id.tv_user_name_reaction);
                ivReactionEmoji = itemView.findViewById(R.id.iv_reaction_emoji_display);
            }
        }
    }
}