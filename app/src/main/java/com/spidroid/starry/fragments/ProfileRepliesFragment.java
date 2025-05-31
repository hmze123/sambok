package com.spidroid.starry.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull; // استيراد
import androidx.annotation.Nullable; // استيراد
import androidx.fragment.app.Fragment;
import com.spidroid.starry.R;

public class ProfileRepliesFragment extends Fragment {

    private String userId; // لتخزين المعرّف إذا احتجت إليه لاحقًا

    public ProfileRepliesFragment() {
        // Required empty public constructor
    }

    // ⭐ بداية إضافة دالة newInstance ⭐
    public static ProfileRepliesFragment newInstance(String userId) {
        ProfileRepliesFragment fragment = new ProfileRepliesFragment();
        Bundle args = new Bundle();
        args.putString("USER_ID", userId); // استخدم نفس المفتاح "USER_ID" للاتساق
        fragment.setArguments(args);
        return fragment;
    }
    // ⭐ نهاية إضافة دالة newInstance ⭐

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("USER_ID");
            // يمكنك استخدام userId هنا إذا كنت بحاجة لجلب بيانات خاصة بهذا المستخدم
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_replies, container, false);
        TextView comingSoon = view.findViewById(R.id.tvComingSoon);
        // يمكنك تخصيص الرسالة إذا أردت، أو استخدام userId إذا كان له تأثير
        comingSoon.setText("Replies feature for user " + (userId != null ? userId : "") + " coming soon!\n🚀");
        return view;
    }
}