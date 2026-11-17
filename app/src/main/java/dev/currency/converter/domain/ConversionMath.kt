package dev.currency.converter.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Pure conversion helpers shared by the ViewModel and unit tests. */
object ConversionMath {
    private val CONTEXT = MathContext(16, RoundingMode.HALF_UP)

    fun convert(amount: BigDecimal, rate: BigDecimal): BigDecimal =
        amount.multiply(rate, CONTEXT)
}
