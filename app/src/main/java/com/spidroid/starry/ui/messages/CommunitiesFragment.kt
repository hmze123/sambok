package com.spidroid.starry.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.spidroid.starry.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CommunitiesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_placeholder, container, false)
        val textView = view.findViewById<TextView>(R.id.tv_placeholder)
        textView.setText("Communities coming soon! 🚀")
        return view
    }
}