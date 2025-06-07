package com.spidroid.starry.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.activities.ProfileActivity
import com.spidroid.starry.adapters.UserAdapter
import com.spidroid.starry.databinding.FragmentSearchResultListBinding
import com.spidroid.starry.models.UserModel
import com.spidroid.starry.viewmodels.SearchViewModel

class UsersSearchFragment : Fragment(), UserAdapter.OnUserClickListener {
    private var _binding: FragmentSearchResultListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchResultListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(this)
        binding.recyclerViewResults.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewResults.adapter = userAdapter
    }

    private fun observeViewModel() {
        viewModel.userResults.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
            binding.textEmptyResults.visibility = if (users.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarResults.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onUserClick(user: UserModel) {
        val intent = Intent(activity, ProfileActivity::class.java).apply {
            putExtra("userId", user.userId)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}