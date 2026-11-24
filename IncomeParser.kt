package com.example.financereport

data class ParsedIncome(val sender: String, val amount: Long)

/**
 * Pure Kotlin (no Android classes), so it can be unit-tested on a laptop.
 * Conservative on purpose: a missed notification is better than a wrong transaction.
 */
object IncomeParser {

    // Includes non-breaking spaces (U+00A0, U+2007, U+202F): banks often put one after "Rp", and plain \s misses them.
    private val WHITESPACE = Regex("""[\s\u00A0\u2007\u202F]+""")
    private val AMOUNT = Regex("""(?i)(?:rp\.?|idr)\s*([0-9][0-9.,]*)""")
    private val SENDER = Regex(
        """(?i)\b(?:dari|from)\s+(.+?)(?=\s*[.,;]\s|\s*[.,;]$|\s*\(|\s+(?:ke|pada|melalui|via|sebesar|senilai|rekening|rek|dengan|tanggal|tgl|jam|pukul|saldo|diterima|berhasil|to|on)\b|$)"""
    )

    private val INCOME_HINTS = listOf(
        "menerima", "diterima", "dana masuk", "uang masuk", "transfer masuk",
        "saldo masuk", "setoran", "received", "incoming"
    )
    private val REJECT_HINTS = listOf(
        "mengirim", "dikirim", "transfer ke", "pembayaran", "membayar", "debit",
        "penarikan", "tarik tunai", "otp", "kode verifikasi", "tagihan",
        "pembelian", "belanja", "top up", "topup"
    )
    private val SKIP_AMOUNT_AFTER = listOf("saldo", "biaya", "admin", "fee", "sisa")

    fun parse(rawText: String, fallbackSender: String): ParsedIncome? {
        val text = rawText.replace(WHITESPACE, " ").trim()
        val lower = text.lowercase()
        if (REJECT_HINTS.any { lower.contains(it) }) return null
        if (INCOME_HINTS.none { lower.contains(it) }) return null

        val amount = AMOUNT.findAll(text).firstNotNullOfOrNull { m ->
            val start = m.range.first
            val before = text.substring(maxOf(0, start - 12), start).lowercase()
            if (SKIP_AMOUNT_AFTER.any { before.contains(it) }) null
            else parseAmount(m.groupValues[1])
        } ?: return null
        if (amount <= 0L) return null

        val sender = SENDER.find(text)?.groupValues?.get(1)?.trim()
            ?.take(60)?.takeIf { it.isNotEmpty() } ?: fallbackSender
        return ParsedIncome(sender, amount)
    }

    /** "1.500.000", "1.500.000,00", "1,500,000.00", "50.000." -> whole rupiah. */
    internal fun parseAmount(raw: String): Long? {
        val t = raw.trimEnd('.', ',')
        if (t.isEmpty()) return null
        val dot = t.lastIndexOf('.')
        val comma = t.lastIndexOf(',')
        val intPart = when {
            dot >= 0 && comma >= 0 -> t.substring(0, maxOf(dot, comma))
            dot >= 0 || comma >= 0 -> {
                val sep = if (dot >= 0) '.' else ','
                val pos = t.lastIndexOf(sep)
                val after = t.length - pos - 1
                if (t.count { it == sep } == 1 && after != 3) t.substring(0, pos) else t
            }
            else -> t
        }
        val digits = intPart.filter { it.isDigit() }
        if (digits.isEmpty() || digits.length > 12) return null
        return digits.toLongOrNull()
    }
}
