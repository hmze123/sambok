package com.spidroid.starry.ui.messages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast; // ★ إضافة هذا

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
import com.spidroid.starry.activities.ChatActivity; // ★ تأكد من هذا الاستيراد
import com.spidroid.starry.adapters.GroupAdapter; // ★ استخدام GroupAdapter
import com.spidroid.starry.models.Chat; // ★ استخدام نموذج Chat

import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment implements GroupAdapter.GroupClickListener {

    private static final String TAG = "GroupsFragment";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private GroupAdapter groupAdapter; // ★ تغيير نوع المحول
    private ListenerRegistration groupsListener;

    private RecyclerView rvGroups;
    private TextView tvEmptyGroups;
    private ProgressBar pbLoadingGroups;

    public GroupsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        rvGroups = view.findViewById(R.id.rv_groups);
        tvEmptyGroups = view.findViewById(R.id.tv_empty_groups);
        pbLoadingGroups = view.findViewById(R.id.pb_loading_groups);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        if (currentUser != null) {
            listenForGroups();
        } else {
            tvEmptyGroups.setText("Please log in to see your groups.");
            tvEmptyGroups.setVisibility(View.VISIBLE);
            pbLoadingGroups.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        if (getContext() == null) return; // تحقق من أن السياق متاح
        groupAdapter = new GroupAdapter(getContext(), this); // ★ تمرير السياق
        rvGroups.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGroups.setAdapter(groupAdapter);
    }

    private void listenForGroups() {
        if (currentUser == null) return;

        pbLoadingGroups.setVisibility(View.VISIBLE);
        tvEmptyGroups.setVisibility(View.GONE);

        // استعلام لجلب الدردشات التي هي مجموعات والمستخدم الحالي مشارك فيها
        Query query = db.collection("chats")
                .whereEqualTo("isGroup", true)
                .whereArrayContains("participants", currentUser.getUid())
                .orderBy("lastMessageTime", Query.Direction.DESCENDING); // ترتيب حسب آخر رسالة

        if (groupsListener != null) {
            groupsListener.remove(); // إزالة أي مستمع قديم
        }

        groupsListener = query.addSnapshotListener((queryDocumentSnapshots, e) -> {
            pbLoadingGroups.setVisibility(View.GONE);
            if (e != null) {
                Log.e(TAG, "Listen failed for groups.", e);
                tvEmptyGroups.setText("Failed to load groups.");
                tvEmptyGroups.setVisibility(View.VISIBLE);
                if (getContext() != null) { // تحقق من السياق قبل عرض Toast
                    Toast.makeText(getContext(), "Error loading groups: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (queryDocumentSnapshots != null) {
                List<Chat> groups = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Chat group = doc.toObject(Chat.class);
                    group.setId(doc.getId()); // تأكد من تعيين الـ ID
                    groups.add(group);
                }
                groupAdapter.submitList(groups);
                tvEmptyGroups.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
                if(groups.isEmpty()){
                    tvEmptyGroups.setText("You are not a member of any group yet, or no groups found.");
                }
            } else {
                tvEmptyGroups.setVisibility(View.VISIBLE);
                tvEmptyGroups.setText("No groups found.");
            }
        });
    }

    @Override
    public void onGroupClick(Chat group) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("chatId", group.getId());
            intent.putExtra("isGroup", true); // من المهم تمرير أن هذه دردشة جماعية
            // يمكنك تمرير اسم المجموعة وصورتها أيضًا إذا أردت عرضها مباشرة في ChatActivity
            intent.putExtra("groupName", group.getGroupName());
            intent.putExtra("groupImage", group.getGroupImage());
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (groupsListener != null) {
            groupsListener.remove();
        }
        // لا تقم بإلغاء تهيئة binding هنا إذا كنت لا تستخدم ViewBinding في هذا الـ Fragment
        rvGroups = null;
        tvEmptyGroups = null;
        pbLoadingGroups = null;
    }
}