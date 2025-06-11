package com.spidroid.starry.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.spidroid.starry.R
import com.spidroid.starry.adapters.PageStateAdapter
import com.spidroid.starry.databinding.ActivityPageBinding
import com.spidroid.starry.models.PageModel
import com.spidroid.starry.viewmodels.PageViewModel
import com.spidroid.starry.viewmodels.UiState
import java.text.NumberFormat
import java.util.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPageBinding
    private val viewModel: PageViewModel by viewModels()
    private var pageId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pageId = intent.getStringExtra(EXTRA_PAGE_ID)
        if (pageId.isNullOrEmpty()) {
            Toast.makeText(this, "Page ID is missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        observeViewModel()
        viewModel.fetchPageData(pageId!!)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarPage)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarPage.setNavigationOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> { /* Show loading state */ }
                is UiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    finish()
                }
                is UiState.SuccessWithData -> {
                    bindPageDetails(state.data)
                    setupViewPager(state.data.pageId)
                }
                else -> {}
            }
        }
    }

    private fun bindPageDetails(page: PageModel) {
        binding.collapsingToolbar.title = page.pageName
        binding.tvPageName.text = page.pageName
        binding.tvPageUsername.text = "@${page.pageUsername}"
        binding.tvPageDescription.text = page.description
        binding.tvPageCategory.text = page.category

        val followersText = "${formatCount(page.followerCount)} Followers"
        binding.tvFollowerCount.text = followersText

        Glide.with(this)
            .load(page.pageAvatarUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .into(binding.ivPageAvatar)

        Glide.with(this)
            .load(page.pageCoverUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(binding.ivPageCover)
    }

    private fun setupViewPager(pageId: String) {
        val adapter = PageStateAdapter(this, pageId)
        binding.viewPagerPage.adapter = adapter

        TabLayoutMediator(binding.tabLayoutPage, binding.viewPagerPage) { tab, position ->
            tab.text = adapter.fragmentTitles[position]
        }.attach()
    }

    private fun formatCount(count: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(count)
    }

    companion object {
        const val EXTRA_PAGE_ID = "page_id"
    }
}