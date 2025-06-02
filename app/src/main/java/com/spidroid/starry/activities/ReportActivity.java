package com.spidroid.starry.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R; // تأكد من أن هذا الاستيراد صحيح

import java.util.HashMap;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private static final String TAG = "ReportActivity";

    public static final String EXTRA_REPORTED_ITEM_ID = "REPORTED_ITEM_ID";
    public static final String EXTRA_REPORT_TYPE = "REPORT_TYPE"; // "post", "comment", "user"
    public static final String EXTRA_REPORTED_AUTHOR_ID = "REPORTED_AUTHOR_ID"; // معرّف صاحب المحتوى

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private TextView tvReportingItemInfo;
    private RadioGroup rgReportReasons;
    private EditText etReportDetails;
    private Button btnSubmitReport;
    private ProgressBar pbLoading;

    private String reportedItemId;
    private String reportType;
    private String reportedAuthorId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to report content.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        reportedItemId = getIntent().getStringExtra(EXTRA_REPORTED_ITEM_ID);
        reportType = getIntent().getStringExtra(EXTRA_REPORT_TYPE);
        reportedAuthorId = getIntent().getStringExtra(EXTRA_REPORTED_AUTHOR_ID); // استلام المعرّف

        if (TextUtils.isEmpty(reportedItemId) || TextUtils.isEmpty(reportType)) {
            Toast.makeText(this, "Invalid report data.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_report);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Report " + capitalize(reportType));
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvReportingItemInfo = findViewById(R.id.tv_reporting_item_info);
        rgReportReasons = findViewById(R.id.rg_report_reasons);
        etReportDetails = findViewById(R.id.et_report_details);
        btnSubmitReport = findViewById(R.id.btn_submit_report);
        pbLoading = findViewById(R.id.pb_report_loading);

        tvReportingItemInfo.setText("You are reporting a " + reportType + " (ID: " + reportedItemId + ")");

        btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void submitReport() {
        int selectedReasonId = rgReportReasons.getCheckedRadioButtonId();
        if (selectedReasonId == -1) {
            Toast.makeText(this, "Please select a reason for reporting.", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRadioButton = findViewById(selectedReasonId);
        String reason = selectedRadioButton.getText().toString();
        String details = etReportDetails.getText().toString().trim();

        pbLoading.setVisibility(View.VISIBLE);
        btnSubmitReport.setEnabled(false);

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reportingUserId", currentUser.getUid());
        reportData.put("reportedItemId", reportedItemId);
        reportData.put("reportedItemType", reportType);
        if (reportedAuthorId != null && !reportedAuthorId.isEmpty()) { // إضافة reportedAuthorId إذا كان موجودًا
            reportData.put("reportedAuthorId", reportedAuthorId);
        }
        reportData.put("reason", reason);
        if (!details.isEmpty()) {
            reportData.put("details", details);
        }
        reportData.put("timestamp", FieldValue.serverTimestamp());
        reportData.put("status", "pending"); // "pending", "reviewed_accepted", "reviewed_rejected"

        db.collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference -> {
                    pbLoading.setVisibility(View.GONE);
                    btnSubmitReport.setEnabled(true);
                    Toast.makeText(ReportActivity.this, "Report submitted successfully. Thank you.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    btnSubmitReport.setEnabled(true);
                    Log.e(TAG, "Error submitting report", e);
                    Toast.makeText(ReportActivity.this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}