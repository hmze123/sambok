package com.spidroid.starry.activities

// استيراد Collectors
import com.google.firebase.firestore.FirebaseFirestore

class FollowersListActivity : AppCompatActivity(),
    com.spidroid.starry.adapters.UserAdapter.OnUserClickListener {
    private var db: FirebaseFirestore? = null
    private var userId: kotlin.String? = null
    private var listType: kotlin.String? = null // نوع القائمة التي سيتم عرضها
    private var recyclerView: RecyclerView? = null
    private var userAdapter: UserAdapter? = null
    private var progressBar: ProgressBar? = null
    private var tvEmptyState: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        db = FirebaseFirestore.getInstance()
        userId = getIntent().getStringExtra(FollowersListActivity.Companion.EXTRA_USER_ID)
        listType = getIntent().getStringExtra(FollowersListActivity.Companion.EXTRA_LIST_TYPE)

        if (userId == null || listType == null) {
            Toast.makeText(this, "Invalid user or list type.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        fetchUsers()
    }

    private fun initializeViews() {
        recyclerView = findViewById<RecyclerView>(R.id.user_list_recycler_view)
        progressBar = findViewById<ProgressBar>(R.id.user_list_progress_bar)
        tvEmptyState = findViewById<TextView>(R.id.user_list_empty_state)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        java.util.Objects.requireNonNull<androidx.appcompat.app.ActionBar?>(getSupportActionBar())
            .setDisplayHomeAsUpEnabled(true)
        getSupportActionBar().setDisplayShowHomeEnabled(true)

        if ("followers" == listType) {
            getSupportActionBar().setTitle("Followers")
        } else if ("following" == listType) {
            getSupportActionBar().setTitle("Following")
        }
        toolbar.setNavigationOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> finish() })
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(this) // تهيئة المحول
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        recyclerView.setAdapter(userAdapter)
    }

    private fun fetchUsers() {
        progressBar.setVisibility(android.view.View.VISIBLE)
        db.collection("users").document(userId).get()
            .addOnSuccessListener({ documentSnapshot ->
                val user: UserModel? = documentSnapshot.toObject(UserModel::class.java)
                if (user != null) {
                    val userIdsToFetch: kotlin.collections.MutableList<kotlin.String?> =
                        java.util.ArrayList<kotlin.String?>()
                    if ("followers" == listType) {
                        userIdsToFetch.addAll(user.getFollowers().keys)
                    } else if ("following" == listType) {
                        userIdsToFetch.addAll(user.getFollowing().keys)
                    }

                    if (userIdsToFetch.isEmpty()) {
                        userAdapter.submitList(java.util.ArrayList<UserModel?>())
                        tvEmptyState.setText("No " + listType + " found.")
                        tvEmptyState.setVisibility(android.view.View.VISIBLE)
                        progressBar.setVisibility(android.view.View.GONE)
                        return@addOnSuccessListener
                    }

                    // Firestore whereIn supports up to 10 values.
                    // For larger lists, you'd need to split into chunks and make multiple queries.
                    val limitedUserIds = userIdsToFetch.stream().limit(10)
                        .collect(java.util.stream.Collectors.toList())

                    db.collection("users").whereIn("userId", limitedUserIds).get()
                        .addOnSuccessListener({ querySnapshot ->
                            val fetchedUsers: kotlin.collections.MutableList<UserModel?> =
                                querySnapshot.toObjects(UserModel::class.java)
                            userAdapter.submitList(fetchedUsers)
                            if (fetchedUsers.isEmpty()) {
                                tvEmptyState.setText("No " + listType + " found.")
                                tvEmptyState.setVisibility(android.view.View.VISIBLE)
                            } else {
                                tvEmptyState.setVisibility(android.view.View.GONE)
                            }
                            progressBar.setVisibility(android.view.View.GONE)
                        })
                        .addOnFailureListener({ e ->
                            android.util.Log.e(
                                "FollowListActivity",
                                "Error fetching users: " + e.getMessage()
                            )
                            Toast.makeText(
                                this,
                                "Failed to load users: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                            ).show()
                            tvEmptyState.setText("Failed to load " + listType + ".")
                            tvEmptyState.setVisibility(android.view.View.VISIBLE)
                            progressBar.setVisibility(android.view.View.GONE)
                        })
                } else {
                    Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                    tvEmptyState.setText("User profile not found.")
                    tvEmptyState.setVisibility(android.view.View.VISIBLE)
                    progressBar.setVisibility(android.view.View.GONE)
                }
            })
            .addOnFailureListener({ e ->
                android.util.Log.e(
                    "FollowListActivity",
                    "Error fetching user profile: " + e.getMessage()
                )
                Toast.makeText(
                    this,
                    "Failed to load user profile: " + e.getMessage(),
                    Toast.LENGTH_SHORT
                ).show()
                tvEmptyState.setText("Failed to load user profile.")
                tvEmptyState.setVisibility(android.view.View.VISIBLE)
                progressBar.setVisibility(android.view.View.GONE)
            })
    }

    override fun onUserClick(user: UserModel) {
        val intent: Intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("userId", user.getUserId())
        startActivity(intent)
    }

    companion object {
        const val EXTRA_USER_ID: kotlin.String = "user_id"
        const val EXTRA_LIST_TYPE: kotlin.String = "list_type" // "followers" or "following"
    }
}