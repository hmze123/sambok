package com.spidroid.starry.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.spidroid.starry.databinding.FragmentSearchResultListBinding

// حاليًا، هذا الـ Fragment سيكون مجرد واجهة فارغة
// يمكننا تفعيله في خطوة مستقبلية
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HashtagsSearchFragment : Fragment() {
    private var _binding: FragmentSearchResultListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchResultListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textEmptyResults.text = "Hashtag search coming soon!"
        binding.textEmptyResults.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}