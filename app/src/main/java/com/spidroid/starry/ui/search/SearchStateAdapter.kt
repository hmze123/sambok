package com.spidroid.starry.ui.search

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SearchStateAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3 // عدد التبويبات: Users, Posts, Hashtags

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UsersSearchFragment()
            1 -> PostsSearchFragment()
            2 -> HashtagsSearchFragment() // سنقوم بإنشاء هذا الملف الآن
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}