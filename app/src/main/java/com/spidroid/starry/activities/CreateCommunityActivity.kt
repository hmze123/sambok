// In: app/src/main/java/com/spidroid/starry/activities/CreateCommunityActivity.kt
package com.spidroid.starry.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.databinding.ActivityCreateCommunityBinding // قم بتفعيل ViewBinding
import com.spidroid.starry.viewmodels.CommunityViewModel
import com.spidroid.starry.viewmodels.UiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateCommunityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateCommunityBinding
    private val viewModel: CommunityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCreateCommunity.setOnClickListener {
            val name = binding.etCommunityName.text.toString().trim()
            val description = binding.etCommunityDescription.text.toString().trim()
            val isPublic = binding.switchPublic.isChecked
            val ownerId = Firebase.auth.currentUser?.uid

            if (name.isNotEmpty() && ownerId != null) {
                viewModel.createCommunity(name, description, isPublic, ownerId).observe(this) { state ->
                    // <-- إصلاح: when الآن شاملة
                    when (state) {
                        is UiState.Loading -> {
                            // أظهر ProgressDialog أو أي مؤشر تحميل
                            Toast.makeText(this, "Creating community...", Toast.LENGTH_SHORT).show()
                        }
                        is UiState.SuccessWithData -> {
                            Toast.makeText(this, "Community created successfully!", Toast.LENGTH_LONG).show()
                            finish() // تم بنجاح، أغلق الشاشة
                        }
                        is UiState.Error -> {
                            // أظهر رسالة خطأ للمستخدم
                            Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            // للتعامل مع أي حالات أخرى مثل Empty أو Success بدون بيانات
                        }
                    }
                }
            } else if (name.isEmpty()) {
                Toast.makeText(this, "Community name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }
}