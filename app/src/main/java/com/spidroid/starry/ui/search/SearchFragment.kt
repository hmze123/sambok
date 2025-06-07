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

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupSearchView()
    }

    private fun setupViewPager() {
        val adapter = SearchStateAdapter(this)
        binding.viewPagerSearch.adapter = adapter

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
            // يتم استدعاؤه عند الضغط على زر البحث
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.performSearch(query)
                }
                binding.searchView.clearFocus() // إخفاء لوحة المفاتيح
                return true
            }

            // يتم استدعاؤه مع كل تغيير في النص (لن نستخدمه حاليًا لتجنب كثرة الطلبات)
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}