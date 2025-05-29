package com.spidroid.starry.ui.messages;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.spidroid.starry.R;

public class CommunitiesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView textView = view.findViewById(R.id.tv_placeholder);
        textView.setText("Communities coming soon! 🚀");
        return view;
    }
}