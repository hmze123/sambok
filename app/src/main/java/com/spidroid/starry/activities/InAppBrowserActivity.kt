// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/InAppBrowserActivity.kt
package com.spidroid.starry.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient // ✨ تم إضافة هذا الاستيراد
import android.webkit.WebView // ✨ تم التأكد من هذا الاستيراد
import android.webkit.WebViewClient // ✨ تم إضافة هذا الاستيراد
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
        if (supportActionBar != null) { // ✨ استخدام supportActionBar
            supportActionBar!!.setDisplayShowTitleEnabled(false) // ✨ استخدام supportActionBar
        }
        toolbar.setNavigationOnClickListener(View.OnClickListener { v: View? -> onBackPressed() })

        // Configure WebView
        webView!!.settings.javaScriptEnabled = true // ✨ استخدام .settings
        webView!!.settings.domStorageEnabled = true // ✨ استخدام .settings
        webView!!.settings.setSupportZoom(true) // ✨ استخدام .settings

        // Handle page loading
        webView!!.webViewClient =
            object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    toolbarTitle!!.text = view.title // ✨ استخدام .text
                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    // Handle external links here if needed
                    view.loadUrl(url)
                    return false
                }
            }

        // Handle loading progress
        webView!!.webChromeClient =
            object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar!!.progress = newProgress // ✨ استخدام .progress
                    progressBar!!.visibility = if (newProgress == 100) ProgressBar.GONE else ProgressBar.VISIBLE
                }
            }

        // Load initial URL from intent
        val initialUrl =
            if (intent.hasExtra(EXTRA_URL)) // ✨ استخدام intent
                intent.getStringExtra(EXTRA_URL) // ✨ استخدام intent
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