package com.spidroid.starry.ui.search

import com.google.firebase.auth.FirebaseAuth

class SearchFragment : androidx.fragment.app.Fragment(), OnUserInteractionListener,
    OnHistoryInteractionListener {
    private var viewModel: com.spidroid.starry.viewmodels.SearchViewModel? = null
    private var userResultAdapter: UserResultAdapter? = null
    private var recentSearchAdapter: RecentSearchAdapter? = null
    private var searchHistoryManager: SearchHistoryManager? = null
    private var searchInput: EditText? = null
    private var rvResults: RecyclerView? = null
    private var rvRecentSearches: RecyclerView? = null
    private var layoutRecentSearches: LinearLayout? = null
    private var tvEmptyState: TextView? = null
    private val searchHandler: android.os.Handler = android.os.Handler(Looper.getMainLooper())
    private var searchRunnable: java.lang.Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel =
            ViewModelProvider(this).get<com.spidroid.starry.viewmodels.SearchViewModel>(com.spidroid.starry.viewmodels.SearchViewModel::class.java)
        searchHistoryManager = SearchHistoryManager(requireContext())

        initializeViews(view)
        setupRecyclerViews()
        setupSearchInput()
        setupObservers()

        showInitialState()
    }

    private fun initializeViews(view: android.view.View) {
        searchInput = view.findViewById<EditText>(R.id.et_search)
        rvResults = view.findViewById<RecyclerView>(R.id.rv_results)
        rvRecentSearches = view.findViewById<RecyclerView>(R.id.rv_recent_searches)
        layoutRecentSearches = view.findViewById<LinearLayout>(R.id.layout_recent_searches)
        tvEmptyState = view.findViewById<TextView>(R.id.tv_empty_state)
        val clearButton = view.findViewById<android.widget.ImageView>(R.id.iv_clear)
        clearButton.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? ->
            searchInput.setText(
                ""
            )
        })
    }

    private fun setupRecyclerViews() {
        // Adapter لنتائج البحث
        val currentUserId: kotlin.String? =
            if (FirebaseAuth.getInstance().getCurrentUser() != null) FirebaseAuth.getInstance()
                .getCurrentUser().getUid() else ""
        userResultAdapter = UserResultAdapter(currentUserId, this)
        rvResults.setLayoutManager(LinearLayoutManager(getContext()))
        rvResults.setAdapter(userResultAdapter)

        // Adapter لسجل البحث
        rvRecentSearches.setLayoutManager(LinearLayoutManager(getContext()))
    }

    private fun setupSearchInput() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: kotlin.CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {
                searchHandler.removeCallbacks(searchRunnable!!)
                val query = s.toString().trim { it <= ' ' }

                if (query.isEmpty()) {
                    showInitialState()
                } else {
                    layoutRecentSearches.setVisibility(android.view.View.GONE)
                    rvResults.setVisibility(android.view.View.VISIBLE)

                    searchRunnable = java.lang.Runnable {
                        viewModel!!.searchUsers(query)
                        searchHistoryManager.addSearchTerm(query)
                    }
                    searchHandler.postDelayed(searchRunnable!!, 400) // Debounce
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel!!.getSearchResults().observe(
            getViewLifecycleOwner(),
            androidx.lifecycle.Observer { users: kotlin.collections.MutableList<UserModel?>? ->
                userResultAdapter!!.updateUsers(users)
                tvEmptyState.setVisibility(if (users!!.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
            })

        viewModel!!.getErrorMessage()
            .observe(getViewLifecycleOwner(), androidx.lifecycle.Observer { error: kotlin.String? ->
                if (error != null) {
                    Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG)
                        .setAction(
                            "Retry",
                            android.view.View.OnClickListener { v: android.view.View? -> viewModel!!.retryLastSearch() })
                        .show()
                }
            })
    }

    private fun showInitialState() {
        val history = searchHistoryManager.getSearchHistory()
        if (history.isEmpty()) {
            layoutRecentSearches.setVisibility(android.view.View.GONE)
        } else {
            recentSearchAdapter = RecentSearchAdapter(history, this)
            rvRecentSearches.setAdapter(recentSearchAdapter)
            layoutRecentSearches.setVisibility(android.view.View.VISIBLE)
        }
        rvResults.setVisibility(android.view.View.GONE)
        tvEmptyState.setVisibility(android.view.View.GONE)
        userResultAdapter!!.updateUsers(kotlin.collections.mutableListOf<UserModel?>())
    }

    // --- OnHistoryInteractionListener ---
    override fun onTermClicked(term: kotlin.String) {
        searchInput.setText(term)
        searchInput.setSelection(term.length)
    }

    override fun onRemoveClicked(term: kotlin.String?) {
        searchHistoryManager.removeSearchTerm(term)
        showInitialState() // Refresh history list
    }

    // --- OnUserInteractionListener ---
    override fun onFollowClicked(user: UserModel?, position: Int) { /* ... */
    }

    override fun onUserClicked(user: UserModel?) { /* ... */
    }

    override fun onMoreClicked(user: UserModel?, anchor: android.view.View?) { /* ... */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchHandler.removeCallbacks(searchRunnable!!)
    }
}