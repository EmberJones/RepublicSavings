package com.example.republicsavingsapp.ui.home

import org.junit.Test
import kotlin.test.assertEquals

class GoalProgressTest {
    @Test
    fun spendBetweenMinAndMax_isOnTrack() =
        assertEquals(
            GoalStatus.ON_TRACK,
            GoalProgress(spent = 800.0, min = 500.0, max = 2000.0).status
        )

    @Test
    fun spendAboveMax_isOverMax() =
        assertEquals(GoalStatus.OVER_MAX, GoalProgress(2500.0, 500.0, 2000.0).status)

    @Test
    fun spendBelowMin_isUnderMin() =
        assertEquals(GoalStatus.UNDER_MIN, GoalProgress(100.0, 500.0, 2000.0).status)

    @Test
    fun noMinimumGoal_isNeverUnderMin() =
        assertEquals(GoalStatus.ON_TRACK, GoalProgress(0.0, 0.0, 2000.0).status)

    @Test
    fun spendExactlyOnMax_isStillOnTrack() =
        assertEquals(GoalStatus.ON_TRACK, GoalProgress(2000.0, 0.0, 2000.0).status)

    @Test
    fun spendExactlyOnMin_isOnTrack() =
        assertEquals(GoalStatus.ON_TRACK, GoalProgress(500.0, 500.0, 2000.0).status)

    @Test
    fun fractionAndRemaining_areComputedFromMax() {
        val p = GoalProgress(500.0, 0.0, 2000.0)
        assertEquals(0.25, p.fraction, 1e-9)
        assertEquals(1500.0, p.remaining, 1e-9)
    }

    @Test
    fun zeroMax_hasZeroFractionAndDoesNotDivideByZero() =
        assertEquals(0.0, GoalProgress(100.0, 0.0, 0.0).fraction, 1e-9)
}