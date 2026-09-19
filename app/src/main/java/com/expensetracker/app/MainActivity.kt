package com.expensetracker.app

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import java.io.File
import java.io.OutputStream

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        // Required so the app's data (trips, people, expenses) persists between launches.
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.webViewClient = WebViewClient()

        // Bridge that lets the web page hand a base64 PDF straight to native
        // Android code, which saves it to the real Downloads folder. This
        // replaces the unreliable blob:/data: URI download tricks that
        // generic WebView-to-APK wrapper tools depend on.
        webView.addJavascriptInterface(DownloadBridge(this), "AndroidDownloader")

        webView.loadUrl("file:///android_asset/expense-tracker.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}

/**
 * Exposed to JavaScript as `window.AndroidDownloader`. The web page checks
 * for this object and, when present, calls saveBase64Pdf() instead of trying
 * a browser-style anchor/blob download — see exportPDF()'s downloadPreviewedPDF()
 * in expense-tracker.html.
 */
class DownloadBridge(private val context: Context) {

    @JavascriptInterface
    fun saveBase64Pdf(dataUriOrBase64: String, fileName: String) {
        try {
            // Accepts either a raw base64 string or a full "data:application/pdf;...;base64,XXXX" URI.
            val base64 = if (dataUriOrBase64.contains(",")) {
                dataUriOrBase64.substringAfter(",")
            } else {
                dataUriOrBase64
            }
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val safeName = if (fileName.isNotBlank()) fileName else "Expense-Report.pdf"

            val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(safeName, bytes)
            } else {
                saveViaLegacyFile(safeName, bytes)
            }

            showToast(if (saved) "Saved to Downloads: $safeName" else "Could not save the PDF")
        } catch (e: Exception) {
            showToast("Download failed: ${e.message}")
        }
    }

    private fun saveViaMediaStore(fileName: String, bytes: ByteArray): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out: OutputStream -> out.write(bytes) } ?: return false
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }

    private fun saveViaLegacyFile(fileName: String, bytes: ByteArray): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeBytes(bytes)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
