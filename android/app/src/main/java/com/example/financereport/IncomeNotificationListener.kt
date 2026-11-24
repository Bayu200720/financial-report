package com.example.financereport

import android.app.Notification
import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.UUID

/** Only notifications from these apps are read. Everything else is ignored immediately. */
object AppAllowlist {
    // Verify each package name on your own phone (see DEBUG_LOG_PACKAGES below).
    private val apps = mapOf(
        "id.co.bri.brimo" to "BRImo",
        "id.bmri.livin" to "Livin' by Mandiri",
        "com.bca" to "BCA mobile",
        "com.bca.mybca.omni.android" to "myBCA",
        "src.com.bni" to "BNI Mobile",
        "id.dana" to "DANA",
        "ovo.id" to "OVO",
        "com.gojek.app" to "GoPay"
    )

    fun labelFor(packageName: String): String? = apps[packageName]
}

class IncomeNotificationListener : NotificationListenerService() {

    private val recent = LinkedHashMap<String, Long>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return

        // Logs the app id only (never the content), so you can find the right package names in Logcat.
        if (DEBUG_LOG_PACKAGES) Log.d(TAG, "notification from package=$pkg")

        val label = AppAllowlist.labelFor(pkg) ?: return
        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        val extras = notification.extras ?: return

        val text = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
            .mapNotNull { extras.getCharSequence(it)?.toString() }
            .distinct()
            .joinToString(" ")
        if (text.isBlank()) return

        val parsed = IncomeParser.parse(text, label) ?: return
        if (isDuplicate("$pkg|${parsed.amount}|${parsed.sender}|${text.hashCode()}")) return

        val event = IncomeEvent(
            id = UUID.randomUUID().toString(),
            time = sbn.postTime,
            sender = parsed.sender,
            amount = parsed.amount,
            source = label
        )
        IncomeStore.add(applicationContext, event)
        mainHandler.post { IncomeStore.liveListener?.invoke(event) }
    }

    /** Banks often re-post or update the same notification; ignore repeats within two minutes. */
    private fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        val iter = recent.entries.iterator()
        while (iter.hasNext()) {
            if (now - iter.next().value > DEDUPE_WINDOW_MS) iter.remove()
        }
        if (recent.containsKey(key)) return true
        recent[key] = now
        return false
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Ask Android to reconnect the listener if the system dropped it.
        requestRebind(ComponentName(this, IncomeNotificationListener::class.java))
    }

    companion object {
        private const val TAG = "IncomeListener"
        private const val DEBUG_LOG_PACKAGES = true   // set to false once your package names are confirmed
        private const val DEDUPE_WINDOW_MS = 120_000L
    }
}
