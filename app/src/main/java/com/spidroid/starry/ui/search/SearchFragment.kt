package com.spidroid.starry.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.spidroid.starry.databinding.FragmentSearchBinding
import com.spidroid.starry.viewmodels.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // استخدام by viewModels لربط الـ ViewModel بدورة حياة الـ Fragment
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchStateAdapter: SearchStateAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPagerAndTabs()
        setupSearchView()
    }

    private fun setupViewPagerAndTabs() {
        searchStateAdapter = SearchStateAdapter(this)
        binding.viewPagerSearch.adapter = searchStateAdapter

        // ربط الـ TabLayout مع الـ ViewPager2
        TabLayoutMediator(binding.tabLayoutSearch, binding.viewPagerSearch) { tab, position ->
            tab.text = when (position) {
                0 -> "Users"
                1 -> "Posts"
                2 -> "Hashtags"
                else -> null
            }
        }.attach()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            // يتم استدعاؤه عند الضغط على زر البحث في لوحة المفاتيح
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.performSearch(query)
                }
                binding.searchView.clearFocus() // إخفاء لوحة المفاتيح بعد البحث
                return true
            }

            // يتم استدعاؤه مع كل تغيير في النص
            override fun onQueryTextChange(newText: String?): Boolean {
                // إذا أردت بحثًا فوريًا، يمكنك تفعيل هذا الجزء
                // if (!newText.isNullOrBlank()) {
                //     viewModel.performSearch(newText)
                // }
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}