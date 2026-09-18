package com.example.republicsavingsapp

import java.util.Locale

object CurrencyFormatter {
    private val symbols = mapOf(
        "ZAR" to "R",
        "USD" to "$",
        "EUR" to "€"
    )

    fun symbolFor(currencyCode: String): String = symbols[currencyCode] ?: currencyCode

    // Two decimal places - for per-category maximums, precise amounts
    fun format(amount: Double, currencyCode: String = CurrentUser.currency): String {
        return String.format(Locale.getDefault(), "%s%,.2f", symbolFor(currencyCode), amount)
    }

    // Whole numbers - for totals/summaries where cents don't matter
    fun formatWhole(amount: Double, currencyCode: String = CurrentUser.currency): String {
        return String.format(Locale.getDefault(), "%s%,.0f", symbolFor(currencyCode), amount)
    }
}