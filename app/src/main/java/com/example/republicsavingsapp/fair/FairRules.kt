package com.example.republicsavingsapp.fair

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.random.Random

/*
 * The Fair: gamification rules for Republic Savings.
 *
 * This file is deliberately plain Kotlin: no Android, Firebase or UI code. That keeps the rules
 * easy to unit test (see FairRulesTest) and lets the data layer and the UI both depend on them.
 *
 * rules come from Part 2 design document :
 *  - A week completed without exceeding the DAILY budget earns a ticket.
 *  - A month completed without exceeding the WEEKLY budget earns a ticket.
 *  - A ticket is spent on a wheel spin that can win a small prize the user can afford while
 *    staying within budget.
 *
 * Week numbering uses ISO-8601 weeks (Monday to Sunday) via java.time.temporal.IsoFields.
 */

/** One budgeted expense, reduced to the only two things the rules care about. */
data class Spend(val date: LocalDate, val amount: Double) {
    companion object {
        /** Converts an expense timestamp (epoch millis) into a calendar day in the user's time zone. */
        @RequiresApi(Build.VERSION_CODES.O)
        fun fromMillis(epochMillis: Long, amount: Double, zone: ZoneId = ZoneId.systemDefault()): Spend =
            Spend(Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate(), amount)
    }
}

enum class TicketPeriod { WEEK, MONTH }

data class TicketAward(
    val period: TicketPeriod,
    val periodKey: String,          // e.g. "2026-W37" or "2026-09"
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val savedUnderBudget: Double    // how far under the allowance the user finished; determines prize tiers
) {
    val recordKey: String get() = "${period.name.lowercase(Locale.ROOT)}_$periodKey"}

/** Wheel prize tiers. Weights set how likely each tier is when it is eligible. */
enum class PrizeTier(val value: Int, val weight: Int, val examples: List<String>) {
    SMALL(100, 70, listOf("Pizza", "Movie tickets")),
    MEDIUM(300, 25, listOf("Aquarium tickets", "A nice dinner")),
    LARGE(500, 5, listOf("Surfing gear rental", "Sandboarding", "Whale watching tour"))
}

data class Prize(val tier: PrizeTier, val name: String)

object FairRules {

    /** Absorbs floating point noise so spending exactly on the allowance still counts as within it. */
    const val TOLERANCE = 0.005
    val MIN_SAVING_FOR_TICKET: Double = PrizeTier.SMALL.value.toDouble()

    /** The amount the user may spend on [day]: the monthly budget spread over that month's days. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun dailyAllowance(monthlyBudget: Double, day: LocalDate): Double =
        monthlyBudget / day.lengthOfMonth()

    /** The Monday of the week containing [day]. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun weekStart(day: LocalDate): LocalDate =
        day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @RequiresApi(Build.VERSION_CODES.O)
    fun weekKey(weekStart: LocalDate): String =
        String.format(
            Locale.ROOT, "%d-W%02d",
            weekStart.get(IsoFields.WEEK_BASED_YEAR),
            weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        )

    fun monthKey(month: YearMonth): String =
        String.format(Locale.ROOT, "%d-%02d", month.year, month.monthValue)

    /**
     * Works out every ticket the user has earned from [spends] (budgeted expenses only, income and
     * expenses excluded from the budget must be filtered out by the caller).
     *
     * Only periods that finished before [today] are considered, and a period must contain at least
     * one logged expense, so doing nothing never earns a ticket. The caller compares the result with
     * tickets already stored (by [TicketAward.documentId]) and saves only the new ones.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun evaluateAwards(spends: List<Spend>, monthlyBudget: Double, today: LocalDate): List<TicketAward> {
        if (spends.isEmpty() || monthlyBudget <= 0.0) return emptyList()

        val spendByDay: Map<LocalDate, Double> =
            spends.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.amount } }

        val firstDay = LocalDate.ofEpochDay(spends.minOf { it.date.toEpochDay() })
        val awards = mutableListOf<TicketAward>()

        var week = weekStart(firstDay)
        while (week.plusDays(6).isBefore(today)) {
            evaluateWeek(week, spendByDay, monthlyBudget)?.let { awards.add(it) }
            week = week.plusWeeks(1)
        }

        var month = YearMonth.from(firstDay)
        while (month.atEndOfMonth().isBefore(today)) {
            evaluateMonth(month, spendByDay, monthlyBudget)?.let { awards.add(it) }
            month = month.plusMonths(1)
        }

        return awards.sortedBy { it.periodEnd.toEpochDay() }
    }

    /** Week rule: every day of the week stayed within that day's allowance. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun evaluateWeek(
        weekStart: LocalDate,
        spendByDay: Map<LocalDate, Double>,
        monthlyBudget: Double
    ): TicketAward? {
        val days = (0L..6L).map { weekStart.plusDays(it) }
        if (days.none { spendByDay.containsKey(it) }) return null

        for (day in days) {
            val spent = spendByDay[day] ?: 0.0
            if (spent > dailyAllowance(monthlyBudget, day) + TOLERANCE) return null
        }

        val allowance = days.sumOf { dailyAllowance(monthlyBudget, it) }
        val saved = (allowance - days.sumOf { spendByDay[it] ?: 0.0 }).coerceAtLeast(0.0)
        if (saved + TOLERANCE < MIN_SAVING_FOR_TICKET) return null

        return TicketAward(TicketPeriod.WEEK, weekKey(weekStart), weekStart, weekStart.plusDays(6), saved)
    }

    /**
     * Month rule: every Monday-to-Sunday week inside the month stayed within the weekly allowance.
     * Weeks cut off by the start or end of the month are only allowed for the days that fall inside
     * the month (daily allowance x days in that slice).
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun evaluateMonth(
        month: YearMonth,
        spendByDay: Map<LocalDate, Double>,
        monthlyBudget: Double
    ): TicketAward? {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        if (spendByDay.keys.none { !it.isBefore(first) && !it.isAfter(last) }) return null

        val daily = monthlyBudget / month.lengthOfMonth()
        var total = 0.0
        var cursor = first
        while (!cursor.isAfter(last)) {
            val sunday = cursor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            val sliceEnd = if (sunday.isAfter(last)) last else sunday
            val sliceDays = ChronoUnit.DAYS.between(cursor, sliceEnd) + 1

            var sliceSpend = 0.0
            var day = cursor
            while (!day.isAfter(sliceEnd)) {
                sliceSpend += spendByDay[day] ?: 0.0
                day = day.plusDays(1)
            }

            if (sliceSpend > daily * sliceDays + TOLERANCE) return null
            total += sliceSpend
            cursor = sliceEnd.plusDays(1)
        }

        val saved = (monthlyBudget - total).coerceAtLeast(0.0)
        if (saved + TOLERANCE < MIN_SAVING_FOR_TICKET) return null

        return TicketAward(TicketPeriod.MONTH, monthKey(month), first, last, saved)
    }

    /** Prize tiers the user can afford, given how far under budget they finished. */
    fun eligibleTiers(savedUnderBudget: Double): List<PrizeTier> =
        PrizeTier.values().filter { it.value <= savedUnderBudget + TOLERANCE }

    /**
     * Spins the wheel: picks one eligible tier using the tier weights, then one of its example
     * prizes. Returns null if the saving is too small for any prize. [random] can be seeded in tests.
     */
    fun pickPrize(savedUnderBudget: Double, random: Random = Random.Default): Prize? {
        val tiers = eligibleTiers(savedUnderBudget)
        if (tiers.isEmpty()) return null

        var roll = random.nextInt(tiers.sumOf { it.weight })
        for (tier in tiers) {
            if (roll < tier.weight) return Prize(tier, tier.examples.random(random))
            roll -= tier.weight
        }
        return null // unreachable: the roll is always smaller than the total weight
    }
}