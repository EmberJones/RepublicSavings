package com.example.republicsavingsapp.fair

import android.util.Log
import com.example.republicsavingsapp.CategoryDAO
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.ExpensesDAO
import java.time.LocalDate
import kotlin.random.Random

/**
 * Connects The Fair's rules ([FairRules]) to the stored data: it reads the user's expenses and
 * category budgets, awards any newly earned tickets, and lets the user spend a ticket on the wheel.
 *
 * Tickets are worked out when the app asks ([refresh]), not in the background, so a week or
 * month is only turned into a ticket the next time the Fair or Home screen opens.
 */
class TicketRepository(
    private val ticketDAO: TicketDAO,
    private val expensesDAO: ExpensesDAO,
    private val categoryDAO: CategoryDAO,
    private val log: (String) -> Unit = { Log.d(TAG, it) }
) {

    /**
     * Awards every ticket the logged-in user has earned that is not stored yet.
     * Returns how many new tickets were added.
     *
     * The monthly budget is the sum of the user's category maximums. Only expenses counted toward
     * the budget are used.
     */
    suspend fun awardNewTickets(today: LocalDate = LocalDate.now()): Int {
        if (!CurrentUser.isLoggedIn) {
            log("awardNewTickets skipped: nobody is logged in")
            return 0
        }
        val userId = CurrentUser.userID

        val spends = expensesDAO.getAllFromUser(userId)
            .filter { it.includeInBudget && it.expenseDate > 0L }
            .mapNotNull { expense ->
                expense.expenseAmount.toDoubleOrNull()?.let { Spend.fromMillis(expense.expenseDate, it) }
            }
        val monthlyBudget = categoryDAO.getAllForUser(userId).sumOf { it.monthlyMax }

        val earned = FairRules.evaluateAwards(spends, monthlyBudget, today)
        val alreadyStored = ticketDAO.getAllForUser(userId).map { it.ticketId }.toSet()

        var added = 0
        for (award in earned) {
            val ticket = Ticket.from(award, userId)
            if (ticket.ticketId in alreadyStored) continue
            // addIfAbsent is the real guard: it still refuses a duplicate if two checks ran at once
            if (ticketDAO.addIfAbsent(ticket)) {
                added++
                log("Awarded ${award.period} ticket ${award.periodKey} (saved ${award.savedUnderBudget})")
            }
        }
        log("awardNewTickets finished: ${earned.size} earned in total, $added new")
        return added
    }

    suspend fun getTickets(): List<Ticket> =
        if (CurrentUser.isLoggedIn) ticketDAO.getAllForUser(CurrentUser.userID) else emptyList()

    /** Unspent tickets, oldest first. The first one is the ticket the next spin will use. */
    suspend fun getAvailableTickets(): List<Ticket> =
        getTickets().filter { !it.spent }.sortedBy { it.periodEnd.toEpochDay() }

    /** Tickets already spent, newest first, for the prize history. */
    suspend fun getPrizeHistory(): List<Ticket> =
        getTickets().filter { it.spent }.sortedByDescending { it.spentAt ?: 0L }

    /** Awards new tickets, then returns how many are available to spend. */
    suspend fun refresh(today: LocalDate = LocalDate.now()): Int {
        awardNewTickets(today)
        return getAvailableTickets().size
    }

    /**
     * Spends the oldest available ticket on the wheel and returns the prize, or null if the user
     * has no tickets. The prize is decided and saved here, so the wheel animation only has to
     * land on the segment for the returned prize. Draw the wheel from
     * [FairRules.eligibleTiers] using the first ticket's savedUnderBudget.
     */
    suspend fun spinNextTicket(random: Random = Random.Default): Prize? {
        for (ticket in getAvailableTickets()) {
            val prize = FairRules.pickPrize(ticket.savedUnderBudget, random) ?: continue
            // false means this ticket was spent in the meantime (e.g. a double tap): try the next one
            if (ticketDAO.markSpent(ticket.ticketId, prize)) {
                log("Spent ticket ${ticket.ticketId}: won ${prize.name} (${prize.tier})")
                return prize
            }
        }
        log("spinNextTicket: no ticket available")
        return null
    }

    private companion object {
        const val TAG = "TicketRepository"
    }
}