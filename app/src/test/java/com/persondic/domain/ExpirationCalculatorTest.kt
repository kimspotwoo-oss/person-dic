package com.persondic.domain

import com.persondic.data.model.Volatility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ExpirationCalculatorTest {

    private val assertedOn: LocalDate = LocalDate.of(2026, 3, 15)

    @Test
    fun permanentNeverExpires() {
        val result = ExpirationCalculator.calculateExpiresOn(Volatility.PERMANENT, assertedOn)

        assertNull(result)
    }

    @Test
    fun slowExpiresTwoYearsLater() {
        val result = ExpirationCalculator.calculateExpiresOn(Volatility.SLOW, assertedOn)

        assertEquals(LocalDate.of(2028, 3, 15), result)
    }

    @Test
    fun seasonalExpiresSixMonthsLater() {
        val result = ExpirationCalculator.calculateExpiresOn(Volatility.SEASONAL, assertedOn)

        assertEquals(LocalDate.of(2026, 9, 15), result)
    }

    @Test
    fun eventExpiresOneMonthLater() {
        val result = ExpirationCalculator.calculateExpiresOn(Volatility.EVENT, assertedOn)

        assertEquals(LocalDate.of(2026, 4, 15), result)
    }
}
