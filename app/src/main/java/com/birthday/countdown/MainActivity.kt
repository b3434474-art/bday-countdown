package com.birthday.countdown

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val fileRequest = 1001
    private val notificationPermissionRequest = 1002
    private var pendingBirthday: Pair<Int, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(255, 224, 239)
        window.navigationBarColor = Color.rgb(255, 240, 247)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(255, 240, 247))
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        webView.addJavascriptInterface(BirthdayJavascriptBridge(), "BirthdayCountdownAndroid")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message.orEmpty())
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "audio/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                return try {
                    startActivityForResult(intent, fileRequest)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    false
                }
            }
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    private inner class BirthdayJavascriptBridge {
        @JavascriptInterface
        fun scheduleBirthdayAlert(isoDate: String) {
            runOnUiThread { requestBirthdayReminder(isoDate) }
        }
    }

    private fun requestBirthdayReminder(isoDate: String) {
        val dateParts = isoDate.split("-")
        if (dateParts.size != 3) {
            Toast.makeText(this, "Please choose a valid birthday first.", Toast.LENGTH_LONG).show()
            return
        }
        val month = dateParts[1].toIntOrNull()
        val day = dateParts[2].toIntOrNull()
        if (month == null || day == null || month !in 1..12 || day !in 1..31) {
            Toast.makeText(this, "Please choose a valid birthday first.", Toast.LENGTH_LONG).show()
            return
        }
        pendingBirthday = month to day

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                notificationPermissionRequest
            )
        } else {
            saveBirthdayReminder()
        }
    }

    private fun saveBirthdayReminder() {
        val birthday = pendingBirthday ?: return
        if (BirthdayReminderScheduler.schedule(this, birthday.first, birthday.second)) {
            webView.evaluateJavascript(
                "document.getElementById('notify').textContent='✓ Birthday reminder set';",
                null
            )
            Toast.makeText(this, "Birthday reminder set for about 9:00 AM.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Could not set the birthday reminder. Please try again.", Toast.LENGTH_LONG).show()
        }
        pendingBirthday = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequest) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveBirthdayReminder()
            } else {
                pendingBirthday = null
                Toast.makeText(
                    this,
                    "Notifications are off. Allow them for Birthday Countdown to send your reminder.",
                    Toast.LENGTH_LONG
                ).show()
                webView.evaluateJavascript(
                    "document.getElementById('notify').textContent='🔔 Set birthday reminder';",
                    null
                )
            }
        }
    }

    @Deprecated("Deprecated for compatibility with WebView file selection")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileRequest) {
            val result = if (resultCode == Activity.RESULT_OK && data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            fileCallback?.onReceiveValue(result)
            fileCallback = null
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
