package com.example.financereport

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class IncomeEvent(
    val id: String,
    val time: Long,
    val sender: String,
    val amount: Long,
    val source: String,
    val delivered: Boolean = false
)

/**
 * Keeps detected income in app-private storage so nothing is lost while the
 * screen is closed. Only sender, amount, time and app name are stored, never the full notification text.
 */
object IncomeStore {
    private const val PREFS = "income_events"
    private const val KEY = "events"
    private const val MAX_EVENTS = 500

    /** Set by MainActivity while it is on screen; the service calls it for live updates. */
    @Volatile
    var liveListener: ((IncomeEvent) -> Unit)? = null

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readAll(ctx: Context): MutableList<IncomeEvent> {
        val raw = prefs(ctx).getString(KEY, null) ?: return mutableListOf()
        val out = mutableListOf<IncomeEvent>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    IncomeEvent(
                        id = o.getString("id"),
                        time = o.getLong("time"),
                        sender = o.getString("sender"),
                        amount = o.getLong("amount"),
                        source = o.optString("source", ""),
                        delivered = o.optBoolean("delivered", false)
                    )
                )
            }
        } catch (e: Exception) {
            // Corrupted data: start fresh instead of crashing the service.
        }
        return out
    }

    private fun writeAll(ctx: Context, list: List<IncomeEvent>) {
        val arr = JSONArray()
        list.takeLast(MAX_EVENTS).forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id).put("time", e.time).put("sender", e.sender)
                    .put("amount", e.amount).put("source", e.source).put("delivered", e.delivered)
            )
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    @Synchronized
    fun add(ctx: Context, event: IncomeEvent) {
        val list = readAll(ctx)
        list.add(event)
        writeAll(ctx, list)
    }

    @Synchronized
    fun undelivered(ctx: Context): List<IncomeEvent> = readAll(ctx).filter { !it.delivered }

    @Synchronized
    fun markDelivered(ctx: Context, ids: Set<String>) {
        writeAll(ctx, readAll(ctx).map { if (it.id in ids) it.copy(delivered = true) else it })
    }
}
