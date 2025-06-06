package com.spidroid.starry.ui.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.databinding.FragmentSearchBinding
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.utils.SearchHistoryManager
import com.spidroid.starry.viewmodels.SearchUiState
import com.spidroid.starry.viewmodels.SearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchFragment : Fragment(), UserResultAdapter.OnUserInteractionListener, RecentSearchAdapter.OnHistoryInteractionListener {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var userResultAdapter: UserResultAdapter
    private lateinit var recentSearchAdapter: RecentSearchAdapter
    private lateinit var searchHistoryManager: SearchHistoryManager
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchHistoryManager = SearchHistoryManager(requireContext())

        setupRecyclerViews()
        setupSearchInput()
        observeViewModel()

        showInitialState()
    }

    private fun setupRecyclerViews() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        userResultAdapter = UserResultAdapter(currentUserId, this)
        binding.rvResults.layoutManager = LinearLayoutManager(context)
        binding.rvResults.adapter = userResultAdapter

        recentSearchAdapter = RecentSearchAdapter(mutableListOf(), this)
        binding.rvRecentSearches.layoutManager = LinearLayoutManager(context)
        binding.rvRecentSearches.adapter = recentSearchAdapter
    }

    private fun setupSearchInput() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            searchJob?.cancel() // إلغاء عملية البحث السابقة
            val query = text.toString().trim()
            if (query.isNotEmpty()) {
                binding.ivClear.visibility = View.VISIBLE
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(400) // الانتظار قبل إرسال الطلب لتحسين الأداء
                    viewModel.searchUsers(query)
                }
            } else {
                binding.ivClear.visibility = View.GONE
                viewModel.clearSearch()
            }
        }

        binding.ivClear.setOnClickListener {
            binding.etSearch.text?.clear()
            searchHistoryManager.addSearchTerm(binding.etSearch.text.toString())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // إخفاء جميع الحالات مبدئيًا
                binding.layoutRecentSearches.visibility = View.GONE
                binding.rvResults.visibility = View.GONE
                binding.tvEmptyState.visibility = View.GONE
                // يمكنك إضافة مؤشر تحميل هنا
                // binding.progressBar.visibility = View.GONE

                when (state) {
                    is SearchUiState.Idle -> showInitialState()
                    is SearchUiState.Loading -> {
                        // binding.progressBar.visibility = View.VISIBLE
                    }
                    is SearchUiState.Success -> {
                        binding.rvResults.visibility = View.VISIBLE
                        userResultAdapter.submitList(state.users)
                    }
                    is SearchUiState.Empty -> {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.tvEmptyState.text = "No results found"
                    }
                    is SearchUiState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                            .setAction("Retry") { viewModel.retryLastSearch() }
                            .show()
                    }
                }
            }
        }
    }

    private fun showInitialState() {
        val history = searchHistoryManager.searchHistory
        if (history.isEmpty()) {
            binding.layoutRecentSearches.visibility = View.GONE
        } else {
            recentSearchAdapter.updateData(history)
            binding.layoutRecentSearches.visibility = View.VISIBLE
        }
        binding.rvResults.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE
    }

    // --- OnHistoryInteractionListener Callbacks ---
    override fun onTermClicked(term: String) {
        binding.etSearch.setText(term)
        binding.etSearch.setSelection(term.length)
    }

    override fun onRemoveClicked(term: String) {
        searchHistoryManager.removeSearchTerm(term)
        showInitialState() // تحديث قائمة السجل
    }

    // --- OnUserInteractionListener Callbacks ---
    override fun onFollowClicked(user: UserModel, position: Int) {
        viewModel.toggleFollowStatus(user)
        // تحديث الواجهة بشكل متفائل لتحسين التجربة
        val isCurrentlyFollowing = user.followers.containsKey(FirebaseAuth.getInstance().currentUser?.uid)
        // يجب أن تتأكد من أن هذا التغيير سينعكس بشكل صحيح
        // الأفضل هو أن الـ ViewModel يعيد قائمة محدثة
        userResultAdapter.notifyItemChanged(position)
    }

    override fun onUserClicked(user: UserModel) {
        val intent = Intent(activity, ProfileActivity::class.java).apply {
            putExtra("userId", user.userId)
        }
        startActivity(intent)
    }

    override fun onMoreClicked(user: UserModel, anchor: View) {
        // يمكن تنفيذ قائمة خيارات هنا (مثل الإبلاغ عن مستخدم)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
