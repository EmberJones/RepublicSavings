package com.example.republicsavingsapp.ui.home

enum class GoalStatus { ON_TRACK, UNDER_MIN, OVER_MAX }

data class GoalProgress(val spent: Double, val min: Double, val max: Double) {

    val fraction: Double get() = if (max > 0) spent / max else 0.0

    val remaining: Double get() = max - spent

    val status: GoalStatus
        get() = when {
            max > 0 && spent > max + TOLERANCE -> GoalStatus.OVER_MAX   // exactly on the max is still fine
            min > 0 && spent < min - TOLERANCE -> GoalStatus.UNDER_MIN  // a minimum of 0 means "no minimum"
            else -> GoalStatus.ON_TRACK
        }

    private companion object { const val TOLERANCE = 0.005 }
}