package com.spidroid.starry.activities

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityCreatePageBinding
import com.spidroid.starry.viewmodels.CreatePageViewModel
import com.spidroid.starry.viewmodels.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePageBinding
    private val viewModel: CreatePageViewModel by viewModels()
    private var selectedAvatarUri: Uri? = null

    private val pickImageLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedAvatarUri = it
                Glide.with(this).load(it).into(binding.ivPageAvatar)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCategoryDropdown()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarCreatePage.setNavigationOnClickListener { finish() }
    }

    private fun setupCategoryDropdown() {
        val categories = resources.getStringArray(R.array.page_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.actvPageCategory.setAdapter(adapter)
    }

    private fun setupListeners() {
        binding.ivPageAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnCreatePage.setOnClickListener {
            validateAndCreatePage()
        }
    }

    private fun validateAndCreatePage() {
        val pageName = binding.etPageName.text.toString().trim()
        val pageUsername = binding.etPageUsername.text.toString().trim()
        val category = binding.actvPageCategory.text.toString().trim()
        val description = binding.etPageDescription.text.toString().trim()

        if (pageName.isEmpty()) {
            binding.layoutPageName.error = getString(R.string.error_name_required)
            return
        }
        if (pageUsername.isEmpty()) {
            binding.layoutPageUsername.error = getString(R.string.error_username_required)
            return
        }
        if (category.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_category_required), Toast.LENGTH_SHORT).show()
            return
        }

        binding.layoutPageName.error = null
        binding.layoutPageUsername.error = null

        viewModel.createPage(pageName, pageUsername, category, description.ifEmpty { null }, selectedAvatarUri)
    }

    private fun observeViewModel() {
        viewModel.pageCreationState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    setLoading(true)
                }
                is UiState.SuccessWithData -> {
                    setLoading(false)
                    // --[ تم التعديل هنا ]--
                    Toast.makeText(this, getString(R.string.create_page_success, state.data.pageName), Toast.LENGTH_LONG).show()
                    finish()
                }
                is UiState.Error -> {
                    setLoading(false)
                    // --[ تم التعديل هنا ]--
                    Toast.makeText(this, getString(R.string.create_page_failed, state.message), Toast.LENGTH_LONG).show()
                }
                else -> setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBarCreatePage.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnCreatePage.isEnabled = !isLoading
    }
}