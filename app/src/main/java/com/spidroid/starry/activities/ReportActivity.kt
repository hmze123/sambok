// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/ReportActivity.kt
package com.spidroid.starry.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityReportBinding
import java.util.Locale // ✨ تم إضافة هذا الاستيراد

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private var currentUser: FirebaseUser? = null

    private var reportedItemId: String? = null
    private var reportType: String? = null
    private var reportedAuthorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to report content.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        reportedItemId = intent.getStringExtra(EXTRA_REPORTED_ITEM_ID)
        reportType = intent.getStringExtra(EXTRA_REPORT_TYPE)
        reportedAuthorId = intent.getStringExtra(EXTRA_REPORTED_AUTHOR_ID)

        if (reportedItemId.isNullOrEmpty() || reportType.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid report data provided.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarReport)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = reportType?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        binding.toolbarReport.setNavigationOnClickListener { finish() }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI() {
        binding.tvReportingItemInfo.text = "You are reporting a $reportType (ID: $reportedItemId)"
        binding.btnSubmitReport.setOnClickListener { submitReport() }
    }

    private fun submitReport() {
        val selectedReasonId = binding.rgReportReasons.checkedRadioButtonId
        if (selectedReasonId == -1) {
            Toast.makeText(this, "Please select a reason for reporting.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton = findViewById<RadioButton>(selectedReasonId)
        val reason = selectedRadioButton.text.toString()
        val details = binding.etReportDetails.text.toString().trim()

        showLoading(true)

        val reportData = hashMapOf<String, Any?>(
            "reportingUserId" to (currentUser?.uid ?: "unknown"),
            "reportedItemId" to reportedItemId,
            "reportedItemType" to reportType,
            "reportedAuthorId" to reportedAuthorId, // Can be null if not provided
            "reason" to reason,
            "details" to if (details.isNotEmpty()) details else null,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "pending"
        )

        db.collection("reports").add(reportData)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Report submitted successfully. Thank you.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Error submitting report", e)
                Toast.makeText(this, "Failed to submit report: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.pbReportLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmitReport.isEnabled = !isLoading
    }

    companion object {
        private const val TAG = "ReportActivity"
        const val EXTRA_REPORTED_ITEM_ID = "REPORTED_ITEM_ID"
        const val EXTRA_REPORT_TYPE = "REPORT_TYPE"
        const val EXTRA_REPORTED_AUTHOR_ID = "REPORTED_AUTHOR_ID"
    }
}