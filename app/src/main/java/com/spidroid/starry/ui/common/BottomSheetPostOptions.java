package com.spidroid.starry.ui.common; // أو أي حزمة مناسبة

import android.os.Bundle;
import android.text.TextUtils; // استيراد
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.spidroid.starry.R; // استيراد R
import com.spidroid.starry.databinding.BottomSheetPostOptionsBinding;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.adapters.PostInteractionListener;

public class BottomSheetPostOptions extends BottomSheetDialogFragment {

    private static final String ARG_POST = "post_model";
    private BottomSheetPostOptionsBinding binding;
    private PostModel post;
    private PostInteractionListener interactionListener;

    public static BottomSheetPostOptions newInstance(PostModel post) {
        BottomSheetPostOptions fragment = new BottomSheetPostOptions();
        Bundle args = new Bundle();
        args.putParcelable(ARG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    public void setPostInteractionListener(PostInteractionListener listener) {
        this.interactionListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            post = getArguments().getParcelable(ARG_POST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPostOptionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (post == null || interactionListener == null) {
            dismiss();
            return;
        }

        boolean isCurrentUserPost = post.getAuthorId() != null &&
                FirebaseAuth.getInstance().getCurrentUser() != null &&
                post.getAuthorId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid());

        binding.optionEdit.setVisibility(isCurrentUserPost ? View.VISIBLE : View.GONE);
        binding.optionDelete.setVisibility(isCurrentUserPost ? View.VISIBLE : View.GONE);
        binding.optionReport.setVisibility(!isCurrentUserPost ? View.VISIBLE : View.GONE); // إخفاء الإبلاغ عن منشورات المستخدم نفسه

        // يمكنك إضافة منطق لإخفاء/إظهار "Moderate Post" بناءً على دور المستخدم
        binding.optionModerate.setVisibility(View.GONE); // مثال: إخفاؤه مبدئيًا


        binding.optionReport.setOnClickListener(v -> {
            interactionListener.onReportPost(post);
            dismiss();
        });
        binding.optionShare.setOnClickListener(v -> {
            interactionListener.onSharePost(post);
            dismiss();
        });
        binding.optionCopyLink.setOnClickListener(v -> {
            interactionListener.onCopyLink(post);
            dismiss();
        });
        binding.optionSave.setOnClickListener(v -> {
            interactionListener.onToggleBookmark(post);
            dismiss();
        });
        binding.optionEdit.setOnClickListener(v -> {
            interactionListener.onEditPost(post);
            dismiss();
        });
        binding.optionDelete.setOnClickListener(v -> {
            interactionListener.onDeletePost(post);
            dismiss();
        });
        // binding.optionModerate.setOnClickListener...
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}