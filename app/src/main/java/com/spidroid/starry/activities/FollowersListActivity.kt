// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/FollowersListActivity.kt
package com.spidroid.starry.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.adapters.UserAdapter
import com.spidroid.starry.databinding.ActivityUserListBinding
import com.spidroid.starry.models.UserModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FollowersListActivity : AppCompatActivity(), UserAdapter.OnUserClickListener {

    private lateinit var binding: ActivityUserListBinding
    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    private var userId: String? = null
    private var listType: String? = null // "followers" or "following"

    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra(EXTRA_USER_ID)
        listType = intent.getStringExtra(EXTRA_LIST_TYPE)

        if (userId.isNullOrEmpty() || listType.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid user or list type.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        fetchUserList()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = listType?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(this)
        binding.userListRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FollowersListActivity)
            adapter = userAdapter
        }
    }

    private fun fetchUserList() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val userDoc = db.collection("users").document(userId!!).get().await()
                val user = userDoc.toObject<UserModel>()
                if (user == null) {
                    showEmptyState("User profile not found.")
                    return@launch
                }

                val idMap = if (listType == "followers") user.followers else user.following
                val idsToFetch = idMap.keys.toList()

                if (idsToFetch.isEmpty()) {
                    showEmptyState("No $listType found.")
                    return@launch
                }

                val fetchedUsers = fetchUsersInChunks(idsToFetch)
                userAdapter.submitList(fetchedUsers)
                showContent()

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user list", e)
                showEmptyState("Failed to load list.")
            }
        }
    }

    private suspend fun fetchUsersInChunks(userIds: List<String>): List<UserModel> {
        val allUsers = mutableListOf<UserModel>()
        userIds.chunked(10).forEach { chunk ->
            try {
                val usersSnapshot = db.collection("users").whereIn("userId", chunk).get().await()
                allUsers.addAll(usersSnapshot.toObjects(UserModel::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user chunk", e)
            }
        }
        return allUsers
    }

    private fun showLoading(isLoading: Boolean) {
        binding.userListProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.userListRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.userListEmptyState.visibility = View.GONE
    }

    private fun showEmptyState(message: String) {
        binding.userListProgressBar.visibility = View.GONE
        binding.userListRecyclerView.visibility = View.GONE
        binding.userListEmptyState.visibility = View.VISIBLE
        binding.userListEmptyState.text = message
    }

    private fun showContent() {
        binding.userListProgressBar.visibility = View.GONE
        binding.userListRecyclerView.visibility = View.VISIBLE
        binding.userListEmptyState.visibility = View.GONE
    }


    override fun onUserClick(user: UserModel) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("userId", user.userId)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "FollowersListActivity"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_LIST_TYPE = "list_type"
    }
}