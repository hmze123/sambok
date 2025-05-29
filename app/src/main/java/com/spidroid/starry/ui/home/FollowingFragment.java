package com.spidroid.starry.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.spidroid.starry.R;

public class FollowingFragment extends Fragment {

    public FollowingFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Use a proper layout file instead of reusing fragment_feed
        View view = inflater.inflate(R.layout.fragment_following, container, false);
        setupComingSoonMessage(view);
        return view;
    }

    private void setupComingSoonMessage(View view) {
        TextView comingSoon = view.findViewById(R.id.tvComingSoon);
        comingSoon.setText(R.string.coming_soon);
        comingSoon.setTextSize(18);
        comingSoon.setTextColor(getResources().getColor(R.color.text_secondary));
    }
}