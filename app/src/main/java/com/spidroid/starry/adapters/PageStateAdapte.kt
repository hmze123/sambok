package com.spidroid.starry.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.spidroid.starry.fragments.PagePostsFragment

class PageStateAdapter(activity: FragmentActivity, private val pageId: String) : FragmentStateAdapter(activity) {

    // --- تم حذف كلمة private من هنا ---
    val fragmentTitles = listOf("Posts", "About")

    override fun getItemCount(): Int = fragmentTitles.size

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PagePostsFragment.newInstance(pageId)
            // 1 -> PageAboutFragment.newInstance(pageId) // للمستقبل
            else -> PagePostsFragment.newInstance(pageId) // fallback
        }
    }
}
