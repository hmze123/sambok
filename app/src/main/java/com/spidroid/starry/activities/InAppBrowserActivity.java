package com.spidroid.starry.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.spidroid.starry.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class InAppBrowserActivity extends AppCompatActivity {

  private WebView webView;
  private ProgressBar progressBar;
  private TextView toolbarTitle;
  public static final String EXTRA_URL = "extra_url";

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_in_app_browser);

    // Initialize views
    webView = findViewById(R.id.webView);
    progressBar = findViewById(R.id.progressBar);
    toolbarTitle = findViewById(R.id.toolbar_title);

    // Set up toolbar
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayShowTitleEnabled(false);
    }
    toolbar.setNavigationOnClickListener(v -> onBackPressed());

    // Configure WebView
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.getSettings().setSupportZoom(true);

    // Handle page loading
    webView.setWebViewClient(
        new WebViewClient() {
          @Override
          public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            toolbarTitle.setText(view.getTitle());
          }

          @Override
          public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Handle external links here if needed
            view.loadUrl(url);
            return false;
          }
        });

    // Handle loading progress
    webView.setWebChromeClient(
        new WebChromeClient() {
          @Override
          public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            progressBar.setVisibility(newProgress == 100 ? ProgressBar.GONE : ProgressBar.VISIBLE);
          }
        });

    // Load initial URL from intent
    String initialUrl =
        getIntent().hasExtra(EXTRA_URL)
            ? getIntent().getStringExtra(EXTRA_URL)
            : "https://google.com";
    webView.loadUrl(initialUrl);
  }

  @Override
  public void onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack();
    } else {
      super.onBackPressed();
    }
  }
}
