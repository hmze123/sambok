package com.spidroid.starry.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.spidroid.starry.R

class FollowingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Use a proper layout file instead of reusing fragment_feed
        val view = inflater.inflate(R.layout.fragment_following, container, false)
        setupComingSoonMessage(view)
        return view
    }

    private fun setupComingSoonMessage(view: View) {
        val comingSoon = view.findViewById<TextView>(R.id.tvComingSoon)
        comingSoon.setText(R.string.coming_soon)
        comingSoon.setTextSize(18f)
        comingSoon.setTextColor(getResources().getColor(R.color.text_secondary))
    }
}