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

/**
 * Tests the glue between the Fair rules and stored data, using in-memory fakes instead of Firestore.
 * Budget used: one category with a R3000 maximum, so the daily allowance in September 2026 is R100
 * and the weekly allowance is R700. 7 September 2026 is a Monday.
 */
class TicketRepositoryTest {

    private class FakeTicketDAO : TicketDAO {
        val stored = LinkedHashMap<String, Ticket>()
        val refuseToSpend = mutableSetOf<String>()   // simulates a ticket someone else already spent

        override suspend fun getAllForUser(userId: Long) = stored.values.filter { it.userId == userId }

        override suspend fun addIfAbsent(ticket: Ticket): Boolean {
            if (stored.containsKey(ticket.ticketId)) return false
            stored[ticket.ticketId] = ticket
            return true
        }

        override suspend fun markSpent(ticketId: String, prize: Prize): Boolean {
            val ticket = stored[ticketId] ?: return false
            if (ticket.spent || ticketId in refuseToSpend) return false
            stored[ticketId] = ticket.copy(
                spent = true, prizeTier = prize.tier, prizeName = prize.name, spentAt = System.nanoTime()
            )
            return true
        }
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
        override suspend fun getTotalsByCategory(activeUserID: Long, startDate: Long, endDate: Long): List<CategoryTotal> = throw UnsupportedOperationException()
        override suspend fun getAllUsers(): List<User> = throw UnsupportedOperationException()
    }

    private class FakeCategoryDAO(private val categories: List<Category>) : CategoryDAO {
        override suspend fun addCategory(category: Category): Long = throw UnsupportedOperationException()
        override suspend fun getAllForUser(activeUserID: Long) = categories.filter { it.userId == activeUserID }
    }

    // ---- helpers ----

    private val userId = 1L
    private val log: (String) -> Unit = {}

    @Before
    fun logIn() {
        CurrentUser.setUser(
            User(
                userID = userId, userName = "tester", userSurname = "t", email = "t@t.com",
                userPassword = "x", currency = "ZAR", biometricEnabled = false
            )
        )
    }

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun expense(day: Int, amount: String, forUser: Long = userId, inBudget: Boolean = true, date: Long = millis(2026, 9, day)) =
        Expenses(
            userId = forUser, expenseName = "e", expenseAmount = amount, expenseCategory = "Food",
            includeInBudget = inBudget, photoFilePath = null, expenseDate = date
        )

    private val categories = listOf(
        Category(
            userId = userId,
            categoryName = "Food",
            categoryIcon = "food",
            monthlyMax = 3000.0
        )
    )

    private fun repo(dao: FakeTicketDAO, expenses: List<Expenses>, cats: List<Category> = categories) =
        TicketRepository(dao, FakeExpensesDAO(expenses), FakeCategoryDAO(cats), log)

    private fun ticket(week: Int, saved: Double, id: String = "1_week_2026-W$week") = Ticket(
        ticketId = id, userId = userId, period = TicketPeriod.WEEK, periodKey = "2026-W$week",
        periodStart = LocalDate.of(2026, 1, 1).plusWeeks(week.toLong()), periodEnd = LocalDate.of(2026, 1, 7).plusWeeks(week.toLong()),
        savedUnderBudget = saved
    )

    private val afterWeek37 = LocalDate.of(2026, 9, 14)

    //awarding tickets

    @Test
    fun awardNewTickets_storesATicketForACompletedWeek() = runBlocking {
        val dao = FakeTicketDAO()
        val added = repo(dao, listOf(expense(7, "50"))).awardNewTickets(afterWeek37)

        assertEquals(1, added)
        val ticket = dao.stored["1_week_2026-W37"]
        assertNotNull(ticket)
        assertEquals(650.0, ticket!!.savedUnderBudget, 0.001)   // 700 allowance,  50 spent
        assertFalse(ticket.spent)
    }

    @Test
    fun awardNewTickets_runningTwiceNeverDuplicates() = runBlocking {
        val dao = FakeTicketDAO()
        val repository = repo(dao, listOf(expense(7, "50")))

        assertEquals(1, repository.awardNewTickets(afterWeek37))
        assertEquals(0, repository.awardNewTickets(afterWeek37))
        assertEquals(1, dao.stored.size)
    }

    @Test
    fun awardNewTickets_ignoresExpensesExcludedFromTheBudget() = runBlocking {
        val dao = FakeTicketDAO()
        // If R500 excluded expense were counted, that day would break R100 allowance
        val expenses = listOf(expense(7, "50"), expense(7, "500", inBudget = false))

        assertEquals(1, repo(dao, expenses).awardNewTickets(afterWeek37))
    }

    @Test
    fun awardNewTickets_ignoresOtherUsersExpenses() = runBlocking {
        val dao = FakeTicketDAO()
        val expenses = listOf(expense(7, "50"), expense(7, "500", forUser = 2L))

        assertEquals(1, repo(dao, expenses).awardNewTickets(afterWeek37))
        assertTrue(dao.stored.values.all { it.userId == userId })
    }

    @Test
    fun awardNewTickets_skipsBrokenAmountsAndDatesWithoutCrashing() = runBlocking {
        val dao = FakeTicketDAO()
        val expenses = listOf(expense(7, "not a number"), expense(8, "50", date = 0L))

        assertEquals(0, repo(dao, expenses).awardNewTickets(afterWeek37))
    }

    @Test
    fun awardNewTickets_doesNothingWhenLoggedOut() = runBlocking {
        CurrentUser.clear()
        val dao = FakeTicketDAO()

        assertEquals(0, repo(dao, listOf(expense(7, "50"))).awardNewTickets(afterWeek37))
        assertTrue(dao.stored.isEmpty())
    }

    @Test
    fun awardNewTickets_needsAtLeastOneCategoryBudget() = runBlocking {
        val dao = FakeTicketDAO()
        assertEquals(
            0,
            repo(dao, listOf(expense(7, "50")), cats = emptyList()).awardNewTickets(afterWeek37)
        )
    }

    @Test
    fun refresh_awardsNewTicketsThenCountsOnlyUnspentOnes() = runBlocking {
        val dao = FakeTicketDAO()
        dao.stored["1_week_2026-W36"] = ticket(36, 300.0).copy(spent = true)   // already used
        val repository = repo(dao, listOf(expense(7, "50")))

        // week 37 earns new ticket; the spent week 36 ticket isn't counted
        assertEquals(1, repository.refresh(afterWeek37))
        assertEquals(2, dao.stored.size)
    }

    // spinning

    @Test
    fun spinNextTicket_withNoTickets_returnsNull() = runBlocking {
        assertNull(repo(FakeTicketDAO(), emptyList()).spinNextTicket())
    }

    @Test
    fun spinNextTicket_spendsTheOldestTicketAndRecordsThePrize() = runBlocking {
        val dao = FakeTicketDAO()
        dao.stored["1_week_2026-W37"] = ticket(37, 650.0)
        dao.stored["1_week_2026-W36"] = ticket(36, 300.0)
        val repository = repo(dao, emptyList())

        val prize = repository.spinNextTicket(Random(5))

        assertNotNull(prize)
        assertTrue(prize!!.tier.value <= 300)                 //  older ticket only saved 300
        val spentTicket = dao.stored["1_week_2026-W36"]!!
        assertTrue(spentTicket.spent)
        assertEquals(prize.name, spentTicket.prizeName)
        assertEquals(prize.tier, spentTicket.prizeTier)
        assertFalse(dao.stored["1_week_2026-W37"]!!.spent)    //  newer ticket is untouched
        assertEquals(1, repository.getAvailableTickets().size)
    }

    @Test
    fun spinNextTicket_aTicketCannotBeSpentTwice() = runBlocking {
        val dao = FakeTicketDAO()
        dao.stored["1_week_2026-W36"] = ticket(36, 300.0)
        val repository = repo(dao, emptyList())

        assertNotNull(repository.spinNextTicket(Random(1)))
        assertNull(repository.spinNextTicket(Random(1)))
    }

    @Test
    fun spinNextTicket_movesOnIfTheFirstTicketWasSpentElsewhere() = runBlocking {
        val dao = FakeTicketDAO()
        dao.stored["1_week_2026-W36"] = ticket(36, 300.0)
        dao.stored["1_week_2026-W37"] = ticket(37, 650.0)
        dao.refuseToSpend.add("1_week_2026-W36")   // e.g. double tap spent it first
        val repository = repo(dao, emptyList())

        assertNotNull(repository.spinNextTicket(Random(2)))
        assertFalse(dao.stored["1_week_2026-W36"]!!.spent)
        assertTrue(dao.stored["1_week_2026-W37"]!!.spent)
    }

    @Test
    fun getPrizeHistory_listsSpentTicketsNewestFirst() = runBlocking {
        val dao = FakeTicketDAO()
        dao.stored["1_week_2026-W36"] = ticket(36, 300.0)
        dao.stored["1_week_2026-W37"] = ticket(37, 650.0)
        val repository = repo(dao, emptyList())
        repository.spinNextTicket(Random(3))   // spends W36
        repository.spinNextTicket(Random(4))   // spends W37

        assertEquals(
            listOf("2026-W37", "2026-W36"),
            repository.getPrizeHistory().map { it.periodKey })
    }
}