package com.spidroid.starry.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.spidroid.starry.R

class InAppBrowserActivity : AppCompatActivity() {
    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var toolbarTitle: TextView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_browser)

        // Initialize views
        webView = findViewById<WebView>(R.id.webView)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)
        toolbarTitle = findViewById<TextView>(R.id.toolbar_title)

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (getSupportActionBar() != null) {
            getSupportActionBar()!!.setDisplayShowTitleEnabled(false)
        }
        toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> onBackPressed() })

        // Configure WebView
        webView!!.getSettings().setJavaScriptEnabled(true)
        webView!!.getSettings().setDomStorageEnabled(true)
        webView!!.getSettings().setSupportZoom(true)

        // Handle page loading
        webView!!.setWebViewClient(
            object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    toolbarTitle!!.setText(view.getTitle())
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    // Handle external links here if needed
                    view.loadUrl(url)
                    return false
                }
            })

        // Handle loading progress
        webView!!.setWebChromeClient(
            object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar!!.setProgress(newProgress)
                    progressBar!!.setVisibility(if (newProgress == 100) ProgressBar.GONE else ProgressBar.VISIBLE)
                }
            })

        // Load initial URL from intent
        val initialUrl =
            if (getIntent().hasExtra(EXTRA_URL))
                getIntent().getStringExtra(EXTRA_URL)
            else
                "https://google.com"
        webView!!.loadUrl(initialUrl!!)
    }

    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_URL: String = "extra_url"
    }
}
