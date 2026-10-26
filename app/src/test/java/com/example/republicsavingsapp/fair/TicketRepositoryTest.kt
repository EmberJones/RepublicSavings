package com.example.republicsavingsapp.fair

import com.example.republicsavingsapp.CategoryDAO
import com.example.republicsavingsapp.CategoryTotal
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.Expenses
import com.example.republicsavingsapp.ExpensesDAO
import com.example.republicsavingsapp.User
import com.example.republicsavingsapp.ui.categories.Category
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import com.example.republicsavingsapp.TransactionType

/**
 * Tests the glue between the Fair rules and stored data, using in-memory fakes instead of Firestore
 * and SharedPreferences. Budget used: one category with a R3000 maximum, so the daily allowance in
 * September 2026 is R100 and the weekly allowance is R700. 7 September 2026 is a Monday.
 */
class TicketRepositoryTest {

    // ---- in-memory fakes ----

    private class FakeFairStateDAO : FairStateDAO {
        val tickets = mutableMapOf<Long, Int>()
        val awarded = mutableMapOf<Long, MutableSet<String>>()
        var refuseToUseTicket = false                 // simulates a ticket that was just used elsewhere
        var recordedByAnotherCheck: Set<String> = emptySet()   // simulates a second check winning the race

        override suspend fun getState(userId: Long) =
            FairState(tickets[userId] ?: 0, awarded[userId]?.toSet() ?: emptySet())

        override suspend fun addAwards(userId: Long, recordKeys: List<String>): List<String> {
            val known = awarded.getOrPut(userId) { mutableSetOf() }
            for (key in recordedByAnotherCheck) {
                if (known.add(key)) tickets[userId] = (tickets[userId] ?: 0) + 1
            }
            val fresh = recordKeys.distinct().filter { it !in known }
            known.addAll(fresh)
            tickets[userId] = (tickets[userId] ?: 0) + fresh.size
            return fresh
        }

        override suspend fun useTicket(userId: Long): Boolean {
            val have = tickets[userId] ?: 0
            if (refuseToUseTicket || have <= 0) return false
            tickets[userId] = have - 1
            return true
        }
    }

    private class FakeLocalStore : FairLocalStore {
        val savings = mutableMapOf<Long, List<Double>>()
        val prizes = mutableMapOf<Long, List<PrizeRecord>>()

        override fun getSavings(userId: Long) = savings[userId] ?: emptyList()
        override fun setSavings(userId: Long, savings: List<Double>) { this.savings[userId] = savings }
        override fun getPrizeHistory(userId: Long) = prizes[userId] ?: emptyList()
        override fun addPrize(userId: Long, prize: PrizeRecord) { prizes[userId] = listOf(prize) + getPrizeHistory(userId) }
    }

    private class FakeExpensesDAO(private val expenses: List<Expenses>) : ExpensesDAO {
        override suspend fun getAllFromUser(activeUserID: Long) = expenses.filter { it.userId == activeUserID }
        override suspend fun AddExpense(expenses: Expenses): Long = throw UnsupportedOperationException()
        override suspend fun AddUser(users: User): Long = throw UnsupportedOperationException()
        override suspend fun getUserById(userID: Long): User? = throw UnsupportedOperationException()
        override suspend fun updateCurrency(userID: Long, currency: String) = throw UnsupportedOperationException()
        override suspend fun getAllFromUserInCategory(activeUserID: Long, category: String): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun getAllExpensesSince(date: Long): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun getAllExpensesUpTo(date: Long): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun getAllExpensesBetween(firstDate: Long, lastDate: Long): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun updateExpense(docId: String, expense: Expenses) = throw UnsupportedOperationException()
        override suspend fun deleteExpense(docId: String) = throw UnsupportedOperationException()
        override suspend fun getTotalsByCategory(activeUserID: Long, startDate: Long, endDate: Long, budgetedOnly: Boolean): List<CategoryTotal> = throw UnsupportedOperationException()
        override suspend fun getAllUsers(): List<User> = throw UnsupportedOperationException()
    }

    private class FakeCategoryDAO(private val categories: List<Category>) : CategoryDAO {
        override suspend fun addCategory(category: Category): Long = throw UnsupportedOperationException()
        override suspend fun getAllForUser(activeUserID: Long) = categories.filter { it.userId == activeUserID }
        override suspend fun updateCategory(docId: String, category: Category) = throw UnsupportedOperationException()
        override suspend fun deleteCategory(docId: String) = throw UnsupportedOperationException()
    }

    // ---- helpers ----

    private val userId = 1L
    private val log: (String) -> Unit = {}
    private val state = FakeFairStateDAO()
    private val local = FakeLocalStore()

    @Before
    fun logIn() {
        CurrentUser.setUser(User(userID = userId, userName = "tester", userSurname = "t", email = "t@t.com",
            userPassword = "x", currency = "ZAR", biometricEnabled = false))
    }

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun expense(day: Int, amount: String, forUser: Long = userId, inBudget: Boolean = true, date: Long = millis(2026, 9, day),
                        type: TransactionType = TransactionType.EXPENSE) =
        Expenses(userId = forUser, expenseName = "e", expenseAmount = amount, expenseCategory = "Food",
            includeInBudget = inBudget, photoFilePath = null, expenseDate = date, type = type)

    private val categories = listOf(Category(userId = userId, categoryName = "Food", categoryIcon = "food", monthlyMax = 3000.0))

    private fun repo(expenses: List<Expenses> = emptyList(), cats: List<Category> = categories) =
        TicketRepository(state, local, FakeExpensesDAO(expenses), FakeCategoryDAO(cats), log)

    private val afterWeek37 = LocalDate.of(2026, 9, 14)


    @Test
    fun awardNewTickets_recordsThePeriodAndAddsATicket() = runBlocking {
        val added = repo(listOf(expense(7, "50"))).awardNewTickets(afterWeek37)

        assertEquals(1, added)
        assertEquals(1, state.tickets[userId])
        assertTrue(state.awarded[userId]!!.contains("week_2026-W37"))
        assertEquals(listOf(650.0), local.getSavings(userId))   // 700 allowance - 50 spent
    }

    @Test
    fun awardNewTickets_ignoresIncome() = runBlocking {
        // If the R500 income were counted as spending, that day would break the R100 allowance
        val expenses = listOf(expense(7, "50"), expense(7, "500", type = TransactionType.INCOME))

        assertEquals(1, repo(expenses).awardNewTickets(afterWeek37))
    }

    @Test
    fun awardNewTickets_runningTwiceNeverDuplicates() = runBlocking {
        val repository = repo(listOf(expense(7, "50")))

        assertEquals(1, repository.awardNewTickets(afterWeek37))
        assertEquals(0, repository.awardNewTickets(afterWeek37))
        assertEquals(1, state.tickets[userId])
        assertEquals(listOf(650.0), local.getSavings(userId))
    }

    @Test
    fun awardNewTickets_ifAnotherCheckWonTheRace_addsNothingItself() = runBlocking {
        state.recordedByAnotherCheck = setOf("week_2026-W37")   // e.g. Home and the Fair screen refreshed together

        assertEquals(0, repo(listOf(expense(7, "50"))).awardNewTickets(afterWeek37))
        assertEquals(1, state.tickets[userId])                  // the other check's ticket, not a second one
        assertTrue(local.getSavings(userId).isEmpty())
    }

    @Test
    fun awardNewTickets_afterAReinstall_doesNotAwardPastWeeksAgain() = runBlocking {
        // Online record says week 37 was already awarded, but the phone has no local data
        state.tickets[userId] = 1
        state.awarded[userId] = mutableSetOf("week_2026-W37")

        assertEquals(0, repo(listOf(expense(7, "50"))).awardNewTickets(afterWeek37))
        assertEquals(1, state.tickets[userId])
    }

    @Test
    fun awardNewTickets_ignoresExpensesExcludedFromTheBudget() = runBlocking {
        // If the R500 excluded expense were counted, that day would break the R100 allowance
        val expenses = listOf(expense(7, "50"), expense(7, "500", inBudget = false))

        assertEquals(1, repo(expenses).awardNewTickets(afterWeek37))
    }

    @Test
    fun awardNewTickets_ignoresOtherUsersExpenses() = runBlocking {
        val expenses = listOf(expense(7, "50"), expense(7, "500", forUser = 2L))

        assertEquals(1, repo(expenses).awardNewTickets(afterWeek37))
        assertNull(state.tickets[2L])
    }

    @Test
    fun awardNewTickets_skipsBrokenAmountsAndDatesWithoutCrashing() = runBlocking {
        val expenses = listOf(expense(7, "not a number"), expense(8, "50", date = 0L))

        assertEquals(0, repo(expenses).awardNewTickets(afterWeek37))
    }

    @Test
    fun awardNewTickets_doesNothingWhenLoggedOut() = runBlocking {
        CurrentUser.clear()

        assertEquals(0, repo(listOf(expense(7, "50"))).awardNewTickets(afterWeek37))
        assertTrue(state.tickets.isEmpty())
    }

    @Test
    fun awardNewTickets_needsAtLeastOneCategoryBudget() = runBlocking {
        assertEquals(0, repo(listOf(expense(7, "50")), cats = emptyList()).awardNewTickets(afterWeek37))
    }

    @Test
    fun refresh_awardsNewTicketsThenCountsAllUnusedOnes() = runBlocking {
        state.tickets[userId] = 1
        state.awarded[userId] = mutableSetOf("week_2026-W36")
        local.savings[userId] = listOf(300.0)

        assertEquals(2, repo(listOf(expense(7, "50"))).refresh(afterWeek37))
        assertEquals(listOf(300.0, 650.0), local.getSavings(userId))
    }

    // ---- local record that is missing or out of date ----

    @Test
    fun missingLocalRecord_treatsOldTicketsAsHavingSavedTheMinimum() = runBlocking {
        state.tickets[userId] = 2        // two tickets online, nothing saved locally (a reinstall)

        assertEquals(100.0, repo().nextTicketSaving()!!, 0.001)

        val random = Random(11)
        repeat(50) {
            state.tickets[userId] = 2
            assertEquals(PrizeTier.SMALL, repo().spinNextTicket(random)!!.tier)   // only the R100 tier is affordable
        }
    }

    @Test
    fun extraLocalRecords_areDroppedFromTheOldEnd() = runBlocking {
        state.tickets[userId] = 1
        local.savings[userId] = listOf(300.0, 400.0, 650.0)   // two of these tickets were used on another phone

        assertEquals(650.0, repo().nextTicketSaving()!!, 0.001)
    }

    @Test
    fun newTicketsAreAddedAfterAMissingLocalRecordIsFilledIn() = runBlocking {
        state.tickets[userId] = 1
        state.awarded[userId] = mutableSetOf("week_2026-W36")   // an old ticket with no local record

        repo(listOf(expense(7, "50"))).awardNewTickets(afterWeek37)

        assertEquals(listOf(100.0, 650.0), local.getSavings(userId))
    }

    // ---- spinning ----

    @Test
    fun spinNextTicket_withNoTickets_returnsNull() = runBlocking {
        assertNull(repo().spinNextTicket())
        assertNull(repo().nextTicketSaving())
    }

    @Test
    fun spinNextTicket_usesTheOldestTicketAndRecordsThePrize() = runBlocking {
        state.tickets[userId] = 2
        local.savings[userId] = listOf(300.0, 650.0)
        val repository = repo()

        val prize = repository.spinNextTicket(Random(5))

        assertNotNull(prize)
        assertTrue(prize!!.tier.value <= 300)
        assertEquals(1, state.tickets[userId])
        assertEquals(listOf(650.0), local.getSavings(userId))
        assertEquals(1, repository.getPrizeHistory().size)
        assertEquals(prize.name, repository.getPrizeHistory()[0].prizeName)
        assertEquals(prize.tier, repository.getPrizeHistory()[0].tier)
        assertEquals(650.0, repository.nextTicketSaving()!!, 0.001)
    }

    @Test
    fun spinNextTicket_aTicketCannotBeSpentTwice() = runBlocking {
        state.tickets[userId] = 1
        local.savings[userId] = listOf(300.0)
        val repository = repo()

        assertNotNull(repository.spinNextTicket(Random(1)))
        assertNull(repository.spinNextTicket(Random(1)))
        assertEquals(0, state.tickets[userId])
    }

    @Test
    fun spinNextTicket_ifTheTicketWasJustUsedElsewhere_winsNothing() = runBlocking {
        state.tickets[userId] = 1
        local.savings[userId] = listOf(300.0)
        state.refuseToUseTicket = true
        val repository = repo()

        assertNull(repository.spinNextTicket(Random(2)))
        assertEquals(listOf(300.0), local.getSavings(userId))
        assertTrue(repository.getPrizeHistory().isEmpty())
    }

    @Test
    fun getPrizeHistory_listsNewestFirst() = runBlocking {
        state.tickets[userId] = 2
        local.savings[userId] = listOf(650.0, 650.0)
        val repository = repo()

        val first = repository.spinNextTicket(Random(3))!!
        val second = repository.spinNextTicket(Random(4))!!

        assertEquals(listOf(second.name, first.name), repository.getPrizeHistory().map { it.prizeName })
    }

    @Test
    fun anotherUsersLocalDataIsNeverUsed() = runBlocking {
        state.tickets[userId] = 1
        local.savings[2L] = listOf(650.0)

        assertEquals(100.0, repo().nextTicketSaving()!!, 0.001)
    }
}