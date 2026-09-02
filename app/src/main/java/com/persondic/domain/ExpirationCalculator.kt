package com.persondic.domain

import com.persondic.data.model.Volatility
import java.time.LocalDate

object ExpirationCalculator {

    fun calculateExpiresOn(volatility: Volatility, assertedOn: LocalDate): LocalDate? = when (volatility) {
        Volatility.PERMANENT -> null
        Volatility.SLOW -> assertedOn.plusYears(2)
        Volatility.SEASONAL -> assertedOn.plusMonths(6)
        Volatility.EVENT -> assertedOn.plusMonths(1)
    }
}
