package com.example.republicsavingsapp.fair

import android.util.Log
import com.example.republicsavingsapp.CategoryDAO
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.ExpensesDAO
import java.time.LocalDate
import kotlin.random.Random
import com.example.republicsavingsapp.TransactionType

/**
 * Connects The Fair's rules ([FairRules]) to the stored data.
 *
 * Online (the user's Firestore document, via [FairStateDAO]): how many unused tickets the user has,
 * and which weeks/months have already been awarded. Local ([FairLocalStore]): how much each unused
 * ticket saved (which decides the prizes it can win) and the prize history.
 *
 * Tickets are worked out when the app asks (see [refresh]), not in the background, so a week or
 * month is only turned into a ticket the next time the Fair or Home screen opens.
 */
class TicketRepository(
    private val stateDAO: FairStateDAO,
    private val localStore: FairLocalStore,
    private val expensesDAO: ExpensesDAO,
    private val categoryDAO: CategoryDAO,
    private val log: (String) -> Unit = { Log.d(TAG, it) }
) {

    /**
     * Awards every ticket the logged-in user has earned that has not been awarded yet.
     * Returns how many new tickets were added.
     *
     * The monthly budget is the sum of the user's category maximums. Only expenses counted toward
     * the budget are used. Note: changing a category's maximum later changes how past periods are
     * judged, because the budget is not stored per period.
     */
    suspend fun awardNewTickets(today: LocalDate = LocalDate.now()): Int {
        if (!CurrentUser.isLoggedIn) {
            log("awardNewTickets skipped: nobody is logged in")
            return 0
        }
        val userId = CurrentUser.userID

        val spends = expensesDAO.getAllFromUser(userId)
            .filter { it.type == TransactionType.EXPENSE && it.includeInBudget && it.expenseDate > 0L }            .mapNotNull { expense ->
                expense.expenseAmount.toDoubleOrNull()?.let { Spend.fromMillis(expense.expenseDate, it) }
            }
        val monthlyBudget = categoryDAO.getAllForUser(userId).sumOf { it.monthlyMax }

        val earned = FairRules.evaluateAwards(spends, monthlyBudget, today)
        val state = stateDAO.getState(userId)
        val candidates = earned.filter { it.recordKey !in state.awardedKeys }
        if (candidates.isEmpty()) {
            log("awardNewTickets: nothing new (${earned.size} earned in total)")
            return 0
        }

        // The atomic write is the real guard: it only counts the keys that were still new when it ran
        val added = stateDAO.addAwards(userId, candidates.map { it.recordKey }).toSet()
        val newAwards = candidates.filter { it.recordKey in added }
        if (newAwards.isNotEmpty()) {
            localStore.setSavings(userId, alignedSavings(userId, state.tickets) + newAwards.map { it.savedUnderBudget })
            newAwards.forEach { log("Awarded ${it.recordKey} (saved ${it.savedUnderBudget})") }
        }
        return newAwards.size
    }

    /** How many unused tickets the logged-in user has. */
    suspend fun availableTicketCount(): Int =
        if (CurrentUser.isLoggedIn) stateDAO.getState(CurrentUser.userID).tickets else 0

    /** Awards any new tickets, then returns how many are available to spend. */
    suspend fun refresh(today: LocalDate = LocalDate.now()): Int {
        awardNewTickets(today)
        return availableTicketCount()
    }

    /**
     * How much the ticket used by the next spin saved, or null if there are no tickets. Draw the
     * wheel from FairRules.eligibleTiers(thisValue).
     */
    suspend fun nextTicketSaving(): Double? {
        if (!CurrentUser.isLoggedIn) return null
        val state = stateDAO.getState(CurrentUser.userID)
        if (state.tickets <= 0) return null
        return alignedSavings(CurrentUser.userID, state.tickets).first()
    }

    /**
     * Spends one ticket on the wheel and returns the prize, or null if the user has no tickets.
     * The prize is decided and saved here, so the wheel animation only has to land on the segment
     * for the returned prize.
     */
    suspend fun spinNextTicket(random: Random = Random.Default): Prize? {
        if (!CurrentUser.isLoggedIn) return null
        val userId = CurrentUser.userID

        val state = stateDAO.getState(userId)
        if (state.tickets <= 0) {
            log("spinNextTicket: no ticket available")
            return null
        }

        val savings = alignedSavings(userId, state.tickets)
        val prize = FairRules.pickPrize(savings.first(), random) ?: return null

        // Atomic: false means the ticket was already used (e.g. a double tap), so nothing is awarded
        if (!stateDAO.useTicket(userId)) {
            log("spinNextTicket: the ticket was already used")
            return null
        }

        localStore.setSavings(userId, savings.drop(1))
        localStore.addPrize(userId, PrizeRecord(System.currentTimeMillis(), prize.tier, prize.name))
        log("Spent a ticket: won ${prize.name} (${prize.tier})")
        return prize
    }

    /** Prizes won on this device, newest first. */
    fun getPrizeHistory(): List<PrizeRecord> =
        if (CurrentUser.isLoggedIn) localStore.getPrizeHistory(CurrentUser.userID) else emptyList()

    /**
     * The saved amount of each unused ticket, oldest first, made to match the online ticket count.
     * The local record can be missing or out of date (an Installation, install, or tickets used on another
     * phone), so tickets with no local record are treated as having saved the minimum, R100, and
     * extra local records for tickets that no longer exist are dropped from the old end.
     */
    private fun alignedSavings(userId: Long, ticketCount: Int): List<Double> {
        val local = localStore.getSavings(userId)
        return when {
            local.size > ticketCount -> local.takeLast(ticketCount)
            local.size < ticketCount -> List(ticketCount - local.size) { FairRules.MIN_SAVING_FOR_TICKET } + local
            else -> local
        }
    }

    private companion object {
        const val TAG = "TicketRepository"
    }
}