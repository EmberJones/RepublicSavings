package com.example.republicsavingsapp.fair

import java.time.LocalDate

/**
 * A ticket earned in The Fair. One ticket exists per completed week or month, identified by a
 * deterministic [ticketId] (see [TicketAward.documentId]) so the same period is never awarded twice.
 *
 * A ticket starts unspent. Spending it on the wheel fills in the prize fields and sets [spent].
 */
data class Ticket(
    val ticketId: String,
    val userId: Long,
    val period: TicketPeriod,
    val periodKey: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val savedUnderBudget: Double,
    val earnedAt: Long = System.currentTimeMillis(),
    val spent: Boolean = false,
    val prizeTier: PrizeTier? = null,
    val prizeName: String? = null,
    val spentAt: Long? = null
) {
    companion object {
        fun from(award: TicketAward, userId: Long): Ticket = Ticket(
            ticketId = award.documentId(userId),
            userId = userId,
            period = award.period,
            periodKey = award.periodKey,
            periodStart = award.periodStart,
            periodEnd = award.periodEnd,
            savedUnderBudget = award.savedUnderBudget
        )
    }
}

/** Storage for tickets. The real implementation is [FirestoreTicketDAO]; tests use an in-memory fake. */
interface TicketDAO {
    suspend fun getAllForUser(userId: Long): List<Ticket>

    suspend fun addIfAbsent(ticket: Ticket): Boolean

    suspend fun markSpent(ticketId: String, prize: Prize): Boolean
}