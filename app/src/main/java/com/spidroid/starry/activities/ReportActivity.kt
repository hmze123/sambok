package com.spidroid.starry.activities

// تأكد من أن هذا الاستيراد صحيح
import com.google.firebase.auth.FirebaseAuth

class ReportActivity : AppCompatActivity() {
    private var db: FirebaseFirestore? = null
    private var auth: FirebaseAuth? = null
    private var currentUser: FirebaseUser? = null

    private var tvReportingItemInfo: TextView? = null
    private var rgReportReasons: RadioGroup? = null
    private var etReportDetails: EditText? = null
    private var btnSubmitReport: android.widget.Button? = null
    private var pbLoading: ProgressBar? = null

    private var reportedItemId: kotlin.String? = null
    private var reportType: kotlin.String? = null
    private var reportedAuthorId: kotlin.String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth.getCurrentUser()

        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to report content.", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        reportedItemId = getIntent().getStringExtra(ReportActivity.Companion.EXTRA_REPORTED_ITEM_ID)
        reportType = getIntent().getStringExtra(ReportActivity.Companion.EXTRA_REPORT_TYPE)
        reportedAuthorId =
            getIntent().getStringExtra(ReportActivity.Companion.EXTRA_REPORTED_AUTHOR_ID) // استلام المعرّف

        if (TextUtils.isEmpty(reportedItemId) || TextUtils.isEmpty(reportType)) {
            Toast.makeText(this, "Invalid report data.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_report)
        setSupportActionBar(toolbar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
            getSupportActionBar().setDisplayShowHomeEnabled(true)
            getSupportActionBar().setTitle("Report " + capitalize(reportType))
        }
        toolbar.setNavigationOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> finish() })

        tvReportingItemInfo = findViewById<TextView>(R.id.tv_reporting_item_info)
        rgReportReasons = findViewById<RadioGroup>(R.id.rg_report_reasons)
        etReportDetails = findViewById<EditText>(R.id.et_report_details)
        btnSubmitReport = findViewById<android.widget.Button>(R.id.btn_submit_report)
        pbLoading = findViewById<ProgressBar>(R.id.pb_report_loading)

        tvReportingItemInfo.setText("You are reporting a " + reportType + " (ID: " + reportedItemId + ")")

        btnSubmitReport!!.setOnClickListener(android.view.View.OnClickListener { v: android.view.View? -> submitReport() })
    }

    private fun capitalize(str: kotlin.String?): kotlin.String? {
        if (str == null || str.isEmpty()) {
            return str
        }
        return str.substring(0, 1).uppercase(java.util.Locale.getDefault()) + str.substring(1)
    }

    private fun submitReport() {
        val selectedReasonId = rgReportReasons.getCheckedRadioButtonId()
        if (selectedReasonId == -1) {
            Toast.makeText(this, "Please select a reason for reporting.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRadioButton: RadioButton = findViewById<RadioButton>(selectedReasonId)
        val reason = selectedRadioButton.getText().toString()
        val details = etReportDetails.getText().toString().trim { it <= ' ' }

        pbLoading.setVisibility(android.view.View.VISIBLE)
        btnSubmitReport!!.setEnabled(false)

        val reportData: kotlin.collections.MutableMap<kotlin.String?, kotlin.Any?> =
            java.util.HashMap<kotlin.String?, kotlin.Any?>()
        reportData.put("reportingUserId", currentUser.getUid())
        reportData.put("reportedItemId", reportedItemId)
        reportData.put("reportedItemType", reportType)
        if (reportedAuthorId != null && !reportedAuthorId!!.isEmpty()) { // إضافة reportedAuthorId إذا كان موجودًا
            reportData.put("reportedAuthorId", reportedAuthorId)
        }
        reportData.put("reason", reason)
        if (!details.isEmpty()) {
            reportData.put("details", details)
        }
        reportData.put("timestamp", FieldValue.serverTimestamp())
        reportData.put("status", "pending") // "pending", "reviewed_accepted", "reviewed_rejected"

        db.collection("reports")
            .add(reportData)
            .addOnSuccessListener({ documentReference ->
                pbLoading.setVisibility(android.view.View.GONE)
                btnSubmitReport!!.setEnabled(true)
                Toast.makeText(
                    this@ReportActivity,
                    "Report submitted successfully. Thank you.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            })
            .addOnFailureListener({ e ->
                pbLoading.setVisibility(android.view.View.GONE)
                btnSubmitReport!!.setEnabled(true)
                android.util.Log.e(ReportActivity.Companion.TAG, "Error submitting report", e)
                Toast.makeText(
                    this@ReportActivity,
                    "Failed to submit report: " + e.getMessage(),
                    Toast.LENGTH_LONG
                ).show()
            })
    }

    companion object {
        private const val TAG = "ReportActivity"

        const val EXTRA_REPORTED_ITEM_ID: kotlin.String = "REPORTED_ITEM_ID"
        const val EXTRA_REPORT_TYPE: kotlin.String = "REPORT_TYPE" // "post", "comment", "user"
        const val EXTRA_REPORTED_AUTHOR_ID: kotlin.String =
            "REPORTED_AUTHOR_ID" // معرّف صاحب المحتوى
    }
}