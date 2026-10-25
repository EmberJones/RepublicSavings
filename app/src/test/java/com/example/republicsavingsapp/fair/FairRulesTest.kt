package com.example.republicsavingsapp.fair

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random

/**
 * Tests for the Fair rules. Test data uses a budget of R3000 in September 2026 (30 days), so the
 * daily allowance is R100 and the weekly allowance is R700. 7 September 2026 is a Monday.
 */
class FairRulesTest {

    private val budget = 3000.0

    private fun d(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)
    private fun spend(year: Int, month: Int, day: Int, amount: Double) = Spend(d(year, month, day), amount)

    /** Spends [amount] on each of the seven days of the week starting Monday 7 September 2026. */
    private fun everyDayOfWeek37(amount: Double) = (7..13).map { spend(2026, 9, it, amount) }

    // ---- allowances and keys ----

    @Test
    fun dailyAllowance_spreadsBudgetOverDaysInThatMonth() {
        assertEquals(100.0, FairRules.dailyAllowance(budget, d(2026, 9, 10)), 0.0001)   // 30 days
        assertEquals(3100.0 / 31, FairRules.dailyAllowance(3100.0, d(2026, 10, 10)), 0.0001) // 31 days
    }

    @Test
    fun weekStart_isAlwaysTheMonday() {
        assertEquals(d(2026, 9, 7), FairRules.weekStart(d(2026, 9, 7)))   // Monday
        assertEquals(d(2026, 9, 7), FairRules.weekStart(d(2026, 9, 9)))   // Wednesday
        assertEquals(d(2026, 9, 7), FairRules.weekStart(d(2026, 9, 13)))  // Sunday
    }

    @Test
    fun weekKey_usesIsoWeekAndWeekBasedYear() {
        assertEquals("2026-W37", FairRules.weekKey(d(2026, 9, 7)))
        // 1 January 2027 is a Friday and belongs to the last ISO week of 2026
        assertEquals("2026-W53", FairRules.weekKey(FairRules.weekStart(d(2027, 1, 1))))
    }

    @Test
    fun documentId_isDeterministicPerUserAndPeriod() {
        val week = TicketAward(TicketPeriod.WEEK, "2026-W37", d(2026, 9, 7), d(2026, 9, 13), 500.0)
        val month = TicketAward(TicketPeriod.MONTH, "2026-09", d(2026, 9, 1), d(2026, 9, 30), 500.0)
        assertEquals("42_week_2026-W37", week.documentId(42))
        assertEquals("42_month_2026-09", month.documentId(42))
    }

    // ---- week tickets ----

    @Test
    fun week_withinDailyAllowanceEveryDay_earnsTicket() {
        val spends = listOf(spend(2026, 9, 7, 50.0), spend(2026, 9, 8, 60.0), spend(2026, 9, 10, 90.0))
        val awards = FairRules.evaluateAwards(spends, budget, d(2026, 9, 14))

        assertEquals(1, awards.size)
        assertEquals(TicketPeriod.WEEK, awards[0].period)
        assertEquals("2026-W37", awards[0].periodKey)
        assertEquals(d(2026, 9, 7), awards[0].periodStart)
        assertEquals(d(2026, 9, 13), awards[0].periodEnd)
        assertEquals(500.0, awards[0].savedUnderBudget, 0.001)   // 700 allowance - 200 spent
    }

    @Test
    fun week_oneDayOverAllowance_noTicket() {
        val spends = listOf(spend(2026, 9, 7, 50.0), spend(2026, 9, 9, 150.0))
        assertTrue(FairRules.evaluateAwards(spends, budget, d(2026, 9, 14)).isEmpty())
    }

    @Test
    fun week_spendingExactlyOnTheAllowance_stillCounts() {
        val spends = listOf(spend(2026, 9, 9, 100.0))
        assertEquals(1, FairRules.evaluateAwards(spends, budget, d(2026, 9, 14)).size)
    }

    @Test
    fun week_twoExpensesOnOneDay_areAddedTogether() {
        val spends = listOf(spend(2026, 9, 9, 60.0), spend(2026, 9, 9, 60.0))   // 120 > 100
        assertTrue(FairRules.evaluateAwards(spends, budget, d(2026, 9, 14)).isEmpty())
    }

    @Test
    fun week_stillRunning_isNotAwardedUntilItHasFinished() {
        val spends = listOf(spend(2026, 9, 8, 50.0))
        // Sunday 13 September is the last day of the week, so the week has not finished yet
        assertTrue(FairRules.evaluateAwards(spends, budget, d(2026, 9, 13)).isEmpty())
        assertEquals(1, FairRules.evaluateAwards(spends, budget, d(2026, 9, 14)).size)
    }

    @Test
    fun week_nothingLogged_noTicket() {
        // The only expense is in an earlier week, so week 37 has nothing logged in it
        val spends = listOf(spend(2026, 9, 1, 50.0))
        val keys = FairRules.evaluateAwards(spends, budget, d(2026, 9, 14)).map { it.periodKey }
        assertFalse(keys.contains("2026-W37"))
    }

    @Test
    fun noExpensesOrNoBudget_noTickets() {
        assertTrue(FairRules.evaluateAwards(emptyList(), budget, d(2026, 9, 14)).isEmpty())
        assertTrue(FairRules.evaluateAwards(everyDayOfWeek37(10.0), 0.0, d(2026, 9, 14)).isEmpty())
    }

    @Test
    fun week_mustFinishAtLeastTheSmallestPrizeUnderBudget() {
        // 100 a day is within the allowance but saves nothing
        assertTrue(FairRules.evaluateAwards(everyDayOfWeek37(100.0), budget, d(2026, 9, 14)).isEmpty())
        // 94 a day saves 42, still below R100
        assertTrue(FairRules.evaluateAwards(everyDayOfWeek37(94.0), budget, d(2026, 9, 14)).isEmpty())
        // 80 a day saves 140
        val awards = FairRules.evaluateAwards(everyDayOfWeek37(80.0), budget, d(2026, 9, 14))
        assertEquals(1, awards.size)
        assertEquals(140.0, awards[0].savedUnderBudget, 0.001)
    }

    @Test
    fun week_acrossTwoMonths_usesEachDaysOwnMonthAllowance() {
        // Week 28 Sep - 4 Oct 2026. September allowance is 100 a day, October is 3000/31 = 96.77.
        val tooMuchForOctober = listOf(spend(2026, 10, 1, 98.0))
        assertTrue(FairRules.evaluateAwards(tooMuchForOctober, budget, d(2026, 10, 5)).none { it.periodKey == "2026-W40" })

        val fine = listOf(spend(2026, 10, 1, 96.0))
        val award = FairRules.evaluateAwards(fine, budget, d(2026, 10, 5)).first { it.periodKey == "2026-W40" }
        // 3 September days at 100 + 4 October days at 96.77 = 687.10 allowance, minus 96 spent
        assertEquals(300.0 + 4 * (3000.0 / 31) - 96.0, award.savedUnderBudget, 0.001)
    }

    // ---- month tickets ----

    @Test
    fun month_everyWeekWithinWeeklyAllowance_earnsTicket() {
        val spends = listOf(
            spend(2026, 9, 3, 300.0),    // 1-6 Sep slice (allowance 600)
            spend(2026, 9, 9, 400.0),    // 7-13 Sep
            spend(2026, 9, 16, 400.0),   // 14-20 Sep
            spend(2026, 9, 23, 400.0),   // 21-27 Sep
            spend(2026, 9, 30, 100.0)    // 28-30 Sep slice (allowance 300)
        )
        val month = FairRules.evaluateAwards(spends, budget, d(2026, 10, 1)).first { it.period == TicketPeriod.MONTH }

        assertEquals("2026-09", month.periodKey)
        assertEquals(d(2026, 9, 1), month.periodStart)
        assertEquals(d(2026, 9, 30), month.periodEnd)
        assertEquals(3000.0 - 1600.0, month.savedUnderBudget, 0.001)
    }

    @Test
    fun month_canBeEarnedEvenIfADailyLimitWasBroken() {
        // 250 in one day breaks the daily rule (no week ticket) but the week total is within 700
        val spends = listOf(spend(2026, 9, 8, 250.0))
        val awards = FairRules.evaluateAwards(spends, budget, d(2026, 10, 1))

        assertTrue(awards.none { it.period == TicketPeriod.WEEK })
        assertEquals(1, awards.count { it.period == TicketPeriod.MONTH })
    }

    @Test
    fun month_oneWeekOverWeeklyAllowance_noMonthTicket() {
        val spends = listOf(spend(2026, 9, 16, 400.0), spend(2026, 9, 18, 400.0))  // 800 > 700 in week 38
        assertTrue(FairRules.evaluateAwards(spends, budget, d(2026, 10, 1)).none { it.period == TicketPeriod.MONTH })
    }

    @Test
    fun month_partialFirstWeekOnlyGetsItsOwnDaysAllowance() {
        // 1-6 September is six days, so its allowance is 600, not a full 700
        val spends = listOf(spend(2026, 9, 2, 650.0))
        assertTrue(FairRules.evaluateAwards(spends, budget, d(2026, 10, 1)).none { it.period == TicketPeriod.MONTH })
    }

    @Test
    fun month_stillRunning_isNotAwarded() {
        val spends = listOf(spend(2026, 9, 8, 50.0))
        assertTrue(FairRules.evaluateAwards(spends, budget, d(2026, 9, 30)).none { it.period == TicketPeriod.MONTH })
    }

    @Test
    fun month_keyFormat() {
        assertEquals("2026-09", FairRules.monthKey(YearMonth.of(2026, 9)))
    }

    // ---- prizes ----

    @Test
    fun eligibleTiers_onlyIncludeWhatTheSavingCovers() {
        assertTrue(FairRules.eligibleTiers(99.0).isEmpty())
        assertEquals(listOf(PrizeTier.SMALL), FairRules.eligibleTiers(100.0))
        assertEquals(listOf(PrizeTier.SMALL, PrizeTier.MEDIUM), FairRules.eligibleTiers(350.0))
        assertEquals(PrizeTier.entries, FairRules.eligibleTiers(700.0))
    }

    @Test
    fun pickPrize_returnsNullWhenNothingIsAffordable() {
        assertNull(FairRules.pickPrize(50.0, Random(1)))
    }

    @Test
    fun pickPrize_neverExceedsWhatTheUserSaved() {
        val random = Random(7)
        repeat(2000) {
            val prize = FairRules.pickPrize(350.0, random)
            assertNotNull(prize)
            assertTrue(prize!!.tier != PrizeTier.LARGE)
            assertTrue(prize.tier.examples.contains(prize.name))
        }
    }

    @Test
    fun pickPrize_followsTheTierWeights() {
        val random = Random(42)
        val picks = (1..20000).map { FairRules.pickPrize(700.0, random)!!.tier }
        val small = picks.count { it == PrizeTier.SMALL } / 20000.0
        val medium = picks.count { it == PrizeTier.MEDIUM } / 20000.0
        val large = picks.count { it == PrizeTier.LARGE } / 20000.0

        assertEquals(0.70, small, 0.02)
        assertEquals(0.25, medium, 0.02)
        assertEquals(0.05, large, 0.01)
    }

    // ---- conversion ----

    @Test
    fun spendFromMillis_usesTheUsersTimeZone() {
        // 22:30 UTC on 7 September is already 8 September in South Africa (UTC+2)
        val millis = Instant.parse("2026-09-07T22:30:00Z").toEpochMilli()
        assertEquals(d(2026, 9, 7), Spend.fromMillis(millis, 10.0, ZoneId.of("UTC")).date)
        assertEquals(d(2026, 9, 8), Spend.fromMillis(millis, 10.0, ZoneId.of("Africa/Johannesburg")).date)
    }
}
