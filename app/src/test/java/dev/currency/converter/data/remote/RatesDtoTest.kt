package dev.currency.converter.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RatesDtoTest {

    private val allowed = setOf("EUR", "USD", "DKK", "TWD")

    private val v2Rows = listOf(
        RateEntryDto(date = "2026-11-17", base = "EUR", quote = "EUR", rate = 1.0),
        RateEntryDto(date = "2026-11-17", base = "EUR", quote = "USD", rate = 1.1014),
        RateEntryDto(date = "2026-11-17", base = "EUR", quote = "TWD", rate = 36.285),
        RateEntryDto(date = "2026-11-16", base = "EUR", quote = "BTC", rate = 0.000017),
        RateEntryDto(date = "2026-11-17", base = "EUR", quote = "DKK", rate = 7.4796),
    )

    @Test
    fun `parses TWD from the v2 row format`() {
        val table = v2Rows.toRateTable(fetchedAtMillis = 42L, allowedCodes = allowed)

        requireNotNull(table)
        assertEquals("EUR", table.baseCode)
        assertEquals("2026-11-17", table.date)
        assertEquals(42L, table.fetchedAtMillis)
        assertEquals(0, table.rates.getValue("TWD").compareTo(BigDecimal("36.285")))
    }

    @Test
    fun `drops quotes the catalog cannot select`() {
        val table = requireNotNull(v2Rows.toRateTable(fetchedAtMillis = 1L, allowedCodes = allowed))

        assertFalse(table.rates.containsKey("BTC"))
        assertTrue(table.rates.containsKey("USD"))
        assertTrue(table.rates.containsKey("DKK"))
        assertEquals(0, table.rates.getValue("EUR").compareTo(BigDecimal.ONE))
    }

    @Test
    fun `drops malformed rates and reports unusable payloads`() {
        val broken = listOf(
            RateEntryDto("2026-11-17", "EUR", "USD", 0.0),
            RateEntryDto("2026-11-17", "EUR", "TWD", -3.0),
            RateEntryDto("2026-11-17", "EUR", "DKK", Double.NaN),
            RateEntryDto("2026-11-17", "EUR", "GBP", 0.83),
        )

        assertNull(broken.toRateTable(fetchedAtMillis = 1L, allowedCodes = allowed))
        assertTrue(broken.supportedEntries(allowed).isEmpty())
    }

    @Test
    fun `derives DKK to EUR from the TWD-containing table`() {
        val table = requireNotNull(v2Rows.toRateTable(fetchedAtMillis = 1L, allowedCodes = allowed))
        val rate = requireNotNull(table.rateBetween("DKK", "EUR"))

        // 1 DKK = 1 / 7.4796 EUR ≈ 0.133697... (the UI shows up to six decimals).
        assertTrue(rate.toDouble() in 0.13369..0.13371)
    }
}
