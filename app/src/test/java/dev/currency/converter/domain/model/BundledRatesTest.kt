package dev.currency.converter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class BundledRatesTest {

    @Test
    fun `covers every currency in the catalog`() {
        val missing = CurrencyCatalog.all
            .map { it.code }
            .filterNot { BundledRates.table.rates.containsKey(it) }

        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun `is flagged as bundled with EUR as its base`() {
        assertTrue(BundledRates.table.isBundled)
        assertEquals("EUR", BundledRates.table.baseCode)
        assertEquals(BundledRates.AS_OF_DATE, BundledRates.table.date)
        assertEquals(0, BundledRates.table.rates.getValue("EUR").compareTo(BigDecimal.ONE))
    }

    @Test
    fun `includes TWD and derives cross rates offline`() {
        val twd = BundledRates.table.rates["TWD"]
        assertEquals(0, twd?.compareTo(BigDecimal("36.285")))

        // EUR -> TWD uses the bundled quote, TWD -> EUR is its reciprocal.
        assertEquals(
            0,
            BundledRates.table.rateBetween("EUR", "TWD")?.compareTo(BigDecimal("36.285")),
        )
        val twdToEur = requireNotNull(BundledRates.table.rateBetween("TWD", "EUR"))
        assertTrue(twdToEur.toDouble() in 0.027551..0.027571)
    }
}
