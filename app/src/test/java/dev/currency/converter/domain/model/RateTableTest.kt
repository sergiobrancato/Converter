package dev.currency.converter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class RateTableTest {

    private val table = RateTable(
        baseCode = "EUR",
        date = "2026-11-16",
        fetchedAtMillis = 1L,
        rates = mapOf(
            "EUR" to BigDecimal.ONE,
            "USD" to BigDecimal("1.10"),
            "GBP" to BigDecimal("0.80"),
        ),
    )

    @Test
    fun `same currency has a rate of one`() {
        assertEquals(0, table.rateBetween("EUR", "EUR")?.compareTo(BigDecimal.ONE))
        assertEquals(0, table.rateBetween("USD", "USD")?.compareTo(BigDecimal.ONE))
    }

    @Test
    fun `derives cross rates from the cached base table`() {
        assertEquals(0, table.rateBetween("EUR", "USD")?.compareTo(BigDecimal("1.10")))
        assertEquals(0, table.rateBetween("USD", "GBP")?.compareTo(BigDecimal("0.7272727272727273")))
        assertEquals(0, table.rateBetween("GBP", "EUR")?.compareTo(BigDecimal("1.25")))
    }

    @Test
    fun `returns null for unsupported currencies`() {
        assertNull(table.rateBetween("EUR", "JPY"))
    }
}
