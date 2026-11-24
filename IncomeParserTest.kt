package com.example.financereport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomeParserTest {
    private fun p(t: String, fb: String = "FALLBACK") = IncomeParser.parse(t, fb)

    @Test fun transfer_with_sender() =
        assertEquals(ParsedIncome("BUDI SANTOSO", 1_500_000), p("Anda menerima transfer Rp1.500.000 dari BUDI SANTOSO"))

    @Test fun sender_stops_at_sentence_end_and_balance_is_ignored() =
        assertEquals(ParsedIncome("ANI", 50_000), p("Dana masuk Rp 50.000 dari ANI. Saldo Rp 1.000.000"))

    @Test fun balance_before_amount_is_skipped() =
        assertEquals(ParsedIncome("Budi", 200_000), p("Saldo Rp 1.000.000. Kamu menerima Rp200.000 dari Budi via GoPay"))

    @Test fun title_and_text_joined() =
        assertEquals(ParsedIncome("SITI RAHAYU", 750_000), p("Transfer masuk Rp 750.000 dari SITI RAHAYU ke rekening 1234"))

    @Test fun decimals_are_dropped() =
        assertEquals(ParsedIncome("PT ABC", 1_234_567), p("Dana masuk Rp1.234.567,50 dari PT ABC (BCA)"))

    @Test fun english_us_format() =
        assertEquals(ParsedIncome("John", 500_000), p("You received Rp 500,000.00 from John"))

    @Test fun missing_sender_uses_fallback() =
        assertEquals(ParsedIncome("FALLBACK", 25_000), p("Anda menerima Rp25.000"))

    @Test fun trailing_period_after_amount() =
        assertEquals(ParsedIncome("FALLBACK", 50_000), p("Dana masuk Rp50.000."))

    @Test fun outgoing_is_rejected() = assertNull(p("Anda mengirim Rp100.000 ke ANI"))
    @Test fun otp_is_rejected() = assertNull(p("Kode OTP Anda 123456. Dana masuk Rp100.000 dari ANI"))
    @Test fun promo_without_income_hint_is_rejected() = assertNull(p("Promo cashback Rp 10.000 menanti"))
    @Test fun payment_is_rejected() = assertNull(p("Pembayaran Rp 85.000 ke Warung berhasil"))
    // Found by comparing against the JavaScript port: the regex shorthand for whitespace does not match non-breaking spaces.
    @Test fun nbsp_after_rp() =
        assertEquals(ParsedIncome("ANI", 1_500_000), p("Dana masuk Rp\u00A01.500.000 dari ANI"))

    @Test fun narrow_nbsp_after_rp() =
        assertEquals(ParsedIncome("BUDI", 750_000), p("Anda menerima transfer Rp\u202F750.000 dari BUDI"))

    @Test fun sender_stops_before_status_word() =
        assertEquals(ParsedIncome("Anto", 5_000), p("Rp 5.000 dari Anto diterima"))

    @Test fun idr_prefix() =
        assertEquals(ParsedIncome("PT MAJU JAYA TBK", 2_000_000), p("Uang masuk IDR 2.000.000 dari PT MAJU JAYA TBK"))

    @Test fun parseAmount_variants() {
        assertEquals(50_000L, IncomeParser.parseAmount("50.000"))
        assertEquals(1_234_567L, IncomeParser.parseAmount("1.234.567"))
        assertEquals(1_500_000L, IncomeParser.parseAmount("1.500.000,00"))
        assertEquals(1_500_000L, IncomeParser.parseAmount("1,500,000.00"))
        assertEquals(50L, IncomeParser.parseAmount("50,5"))
    }
}
