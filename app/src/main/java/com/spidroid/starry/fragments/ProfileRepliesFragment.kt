package com.spidroid.starry.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.activities.PostDetailActivity
import com.spidroid.starry.adapters.ProfileRepliesAdapter
import com.spidroid.starry.databinding.FragmentProfilePostsBinding
import com.spidroid.starry.models.CommentModel
import com.spidroid.starry.models.PostModel
import com.spidroid.starry.viewmodels.ProfileRepliesState
import com.spidroid.starry.viewmodels.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileRepliesFragment : Fragment(), ProfileRepliesAdapter.OnReplyClickListener {

    private var _binding: FragmentProfilePostsBinding? = null
    private val binding get() = _binding!!

    private var userId: String? = null
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private lateinit var repliesAdapter: ProfileRepliesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(USER_ID_ARG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        userId?.let {
            profileViewModel.fetchRepliesForUser(it)
        }
    }

    private fun setupRecyclerView() {
        repliesAdapter = ProfileRepliesAdapter(requireContext(), this)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = repliesAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            profileViewModel.repliesState.collectLatest { state ->
                binding.tvEmptyPosts.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE

                when (state) {
                    is ProfileRepliesState.Loading -> {
                        // يمكنك إضافة مؤشر تحميل هنا
                    }
                    is ProfileRepliesState.Success -> {
                        binding.recyclerView.visibility = View.VISIBLE
                        repliesAdapter.submitList(state.replies)
                    }
                    is ProfileRepliesState.Empty -> {
                        binding.tvEmptyPosts.visibility = View.VISIBLE
                        binding.tvEmptyPosts.text = "This user hasn't replied to anything yet."
                    }
                    is ProfileRepliesState.Error -> {
                        binding.tvEmptyPosts.visibility = View.VISIBLE
                        binding.tvEmptyPosts.text = state.message
                    }
                }
            }
        }
    }

    override fun onReplyClicked(comment: CommentModel) {
        // عند النقر على الرد، نفتح المنشور الأصلي
        val postId = comment.parentPostId
        if (postId.isNullOrEmpty()) {
            Toast.makeText(context, "Could not find original post.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val postDoc = Firebase.firestore.collection("posts").document(postId).get().await()
                val post = postDoc.toObject(PostModel::class.java)?.apply { this.postId = postDoc.id }
                if (post != null) {
                    val intent = Intent(activity, PostDetailActivity::class.java)
                    intent.putExtra(PostDetailActivity.EXTRA_POST, post)
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "Original post not found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileRepliesFragment", "Failed to fetch post for reply", e)
                Toast.makeText(context, "Error opening post.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val USER_ID_ARG = "USER_ID"
        @JvmStatic
        fun newInstance(userId: String) = ProfileRepliesFragment().apply {
            arguments = bundleOf(USER_ID_ARG to userId)
        }
    }
}