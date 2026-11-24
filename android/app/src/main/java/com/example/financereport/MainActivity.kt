package com.example.financereport

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject

/**
 * Hosts My Financial Report (put financial-ledger.html in app/src/main/assets/) and pushes every
 * detected income into it with importIncome(id, sender, amount, timeMillis, sourceApp).
 * The page records it as an Income transaction and ignores an id it has already seen.
 *
 * Also makes three things work that a bare WebView cannot do on its own:
 *  - choosing a file or photo (Import CSV, Scan receipt)  -> onShowFileChooser
 *  - saving the CSV export                                -> AndroidBridge.saveFile + system "Save as" dialog
 *  - confirm() dialogs (delete transaction, remove budget) -> a WebChromeClient must be set
 *
 * To use the plain log page from step 2 instead, set PAGE = "log.html"; it receives addTransaction(...).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private var pageReady = false
    private var askedPermission = false
    private val deliveredIds = HashSet<String>()

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingSave: String? = null

    private val fileChooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // parseResult returns null when the user cancelled, which is what the page expects.
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            filePathCallback?.onReceiveValue(uris)
            filePathCallback = null
        }

    // Needs androidx.activity 1.7 or newer for CreateDocument(mimeType).
    private val saveDialog =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            val text = pendingSave
            pendingSave = null
            if (uri == null || text == null) return@registerForActivityResult
            val saved = try {
                contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
            } catch (e: Exception) {
                false
            }
            Toast.makeText(this, if (saved) "File tersimpan" else "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
        }

    /** The only door from the page into the app: it can ask to save text, and the user must confirm in the system dialog. */
    inner class FileBridge {
        @JavascriptInterface
        fun saveFile(name: String, text: String) {
            if (text.length > MAX_SAVE_CHARS) return
            val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifEmpty { "export.csv" }
            runOnUiThread {
                pendingSave = text
                saveDialog.launch(safeName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(FileBridge(), "AndroidBridge")

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = params?.createIntent()
                if (intent == null) {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    return true
                }
                return try {
                    fileChooser.launch(intent)
                    true
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    true
                }
            }
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageReady = true
                drainPending()
            }

            // Never navigate away from the app's own page (links in imported text, redirects, and so on).
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                request?.url?.toString()?.startsWith("file:///android_asset/") != true
        }
        web.loadUrl("file:///android_asset/$PAGE")
    }

    override fun onStart() {
        super.onStart()
        IncomeStore.liveListener = { deliver(it) }
        drainPending()
    }

    override fun onStop() {
        IncomeStore.liveListener = null
        super.onStop()
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (!enabled && !askedPermission) {
            askedPermission = true
            AlertDialog.Builder(this)
                .setTitle("Izinkan akses notifikasi")
                .setMessage("Agar uang masuk dari aplikasi bank tercatat otomatis, aktifkan akses notifikasi untuk aplikasi ini. Data hanya diproses di perangkat ini.")
                .setPositiveButton("Buka pengaturan") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("Nanti", null)
                .show()
        }
    }

    private fun deliver(e: IncomeEvent) {
        if (!pageReady) return                       // stays stored; drained when the page finishes loading
        if (!deliveredIds.add(e.id)) return          // never send the same event twice
        web.evaluateJavascript(jsCall(e)) { result ->
            // The page answers true (added) or false (invalid or already imported): either way it is handled.
            // "null" means the call failed (for example the page is not ready), so keep it for the next try.
            if (result == "true" || result == "false") {
                IncomeStore.markDelivered(this, setOf(e.id))
            } else {
                deliveredIds.remove(e.id)
            }
        }
    }

    private fun jsCall(e: IncomeEvent): String =
        if (PAGE == LEDGER_PAGE) {
            "importIncome(${JSONObject.quote(e.id)}, ${JSONObject.quote(e.sender)}, ${e.amount}, ${e.time}, ${JSONObject.quote(e.source)})"
        } else {
            "addTransaction(${JSONObject.quote(e.sender)}, ${e.amount}, ${e.time})"
        }

    private fun drainPending() {
        if (!pageReady) return
        IncomeStore.undelivered(this).sortedBy { it.time }.forEach { deliver(it) }
    }

    companion object {
        private const val LEDGER_PAGE = "financial-ledger.html"
        private const val PAGE = LEDGER_PAGE
        private const val MAX_SAVE_CHARS = 5_000_000
    }
}
