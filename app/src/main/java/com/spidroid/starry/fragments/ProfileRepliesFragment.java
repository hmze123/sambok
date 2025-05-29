package com.spidroid.starry.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.spidroid.starry.R;

public class ProfileRepliesFragment extends Fragment {

    public ProfileRepliesFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_replies, container, false);
        TextView comingSoon = view.findViewById(R.id.tvComingSoon);
        comingSoon.setText("Replies feature coming soon!\n🚀");
        return view;
    }
}