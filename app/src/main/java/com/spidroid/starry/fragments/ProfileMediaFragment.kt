package com.spidroid.starry.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.spidroid.starry.R

class ProfileMediaFragment : Fragment() {

    // A private nullable property to store the user ID.
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the user ID from the arguments when the fragment is created.
        arguments?.let {
            userId = it.getString(USER_ID_ARG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val view = inflater.inflate(R.layout.fragment_profile_media, container, false)
        val comingSoonTextView = view.findViewById<TextView>(R.id.tvComingSoon)

        // Set the text, using the user ID if it exists.
        comingSoonTextView.text = "Media tab coming soon!\n🖼️📹"

        return view
    }

    companion object {
        private const val USER_ID_ARG = "USER_ID"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided user ID.
         *
         * @param userId The ID of the user whose media is to be displayed.
         * @return A new instance of fragment ProfileMediaFragment.
         */
        @JvmStatic
        fun newInstance(userId: String) = ProfileMediaFragment().apply {
            // Use the 'bundleOf' KTX helper function for cleaner argument creation.
            arguments = bundleOf(USER_ID_ARG to userId)
        }
    }
}
