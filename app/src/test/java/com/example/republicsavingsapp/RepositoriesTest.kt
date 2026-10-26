package com.example.republicsavingsapp

import com.example.republicsavingsapp.ui.categories.Category
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the category and expense repositories (goal validation, edit, delete, income) using
 * in-memory fakes in place of Firestore.
 */
class RepositoriesTest {

    // ---- in-memory fakes that behave like the real storage ----

    private class InMemoryCategoryDAO : CategoryDAO {
        val stored = LinkedHashMap<String, Category>()
        private var counter = 0

        override suspend fun addCategory(category: Category): Long {
            counter++
            stored["cat$counter"] = category.copy(docId = "cat$counter")
            return counter.toLong()
        }
        override suspend fun getAllForUser(activeUserID: Long) = stored.values.filter { it.userId == activeUserID }
        override suspend fun updateCategory(docId: String, category: Category) { stored[docId] = category }
        override suspend fun deleteCategory(docId: String) { stored.remove(docId) }
    }

    private class InMemoryExpensesDAO : ExpensesDAO {
        val stored = LinkedHashMap<String, Expenses>()
        var lastBudgetedOnly: Boolean? = null
        private var counter = 0

        override suspend fun AddExpense(expenses: Expenses): Long {
            counter++
            stored["exp$counter"] = expenses.copy(docId = "exp$counter")
            return counter.toLong()
        }
        override suspend fun updateExpense(docId: String, expense: Expenses) { stored[docId] = expense }
        override suspend fun deleteExpense(docId: String) { stored.remove(docId) }
        override suspend fun getTotalsByCategory(activeUserID: Long, startDate: Long, endDate: Long, budgetedOnly: Boolean): List<CategoryTotal> {
            lastBudgetedOnly = budgetedOnly
            return emptyList()
        }
        override suspend fun getAllFromUser(activeUserID: Long) = stored.values.filter { it.userId == activeUserID }
        override suspend fun AddUser(users: User): Long = throw UnsupportedOperationException()
        override suspend fun getUserById(userID: Long): User? = throw UnsupportedOperationException()
        override suspend fun updateCurrency(userID: Long, currency: String) = throw UnsupportedOperationException()
        override suspend fun getAllFromUserInCategory(activeUserID: Long, category: String): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun getAllExpensesSince(date: Long): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun getAllExpensesUpTo(date: Long): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun getAllExpensesBetween(firstDate: Long, lastDate: Long): List<Expenses> = throw UnsupportedOperationException()
        override suspend fun getAllUsers(): List<User> = throw UnsupportedOperationException()
    }

    private val userId = 7L
    private lateinit var categoryDao: InMemoryCategoryDAO
    private lateinit var expensesDao: InMemoryExpensesDAO
    private lateinit var categories: CategoryRepository
    private lateinit var expenses: ExpenseRepository

    @Before
    fun setUp() {
        CurrentUser.setUser(User(userID = userId, userName = "tester", userSurname = "t", email = "t@t.com",
            userPassword = "x", currency = "ZAR", biometricEnabled = false))
        categoryDao = InMemoryCategoryDAO()
        expensesDao = InMemoryExpensesDAO()
        categories = CategoryRepository(categoryDao)
        expenses = ExpenseRepository(expensesDao)
    }

    /** True if [block] throws IllegalArgumentException (JUnit 4.12 has no assertThrows). */
    private fun rejects(block: suspend () -> Unit): Boolean = try {
        runBlocking { block() }
        false
    } catch (e: IllegalArgumentException) {
        true
    }

    // ---- categories: minimum and maximum goals ----

    @Test
    fun addCategory_savesBothGoals() = runBlocking {
        categories.addCategory(userId, "Food", "food", monthlyMax = 4000.0, monthlyMin = 1500.0)

        val saved = categories.getCategoriesForUser(userId).single()
        assertEquals(4000.0, saved.monthlyMax, 0.001)
        assertEquals(1500.0, saved.monthlyMin, 0.001)
    }

    @Test
    fun addCategory_minimumDefaultsToNone() = runBlocking {
        categories.addCategory(userId, "Food", "food", monthlyMax = 4000.0)

        assertEquals(0.0, categories.getCategoriesForUser(userId).single().monthlyMin, 0.001)
    }

    @Test
    fun addCategory_minimumMayEqualTheMaximum() = runBlocking {
        categories.addCategory(userId, "Rent", "home", monthlyMax = 6500.0, monthlyMin = 6500.0)

        assertEquals(1, categories.getCategoriesForUser(userId).size)
    }

    @Test
    fun addCategory_rejectsMinimumAboveMaximum() {
        assertTrue(rejects { categories.addCategory(userId, "Food", "food", monthlyMax = 1000.0, monthlyMin = 1500.0) })
        assertTrue(categoryDao.stored.isEmpty())
    }

    @Test
    fun addCategory_rejectsNegativeMinimumAndZeroMaximum() {
        assertTrue(rejects { categories.addCategory(userId, "Food", "food", monthlyMax = 1000.0, monthlyMin = -1.0) })
        assertTrue(rejects { categories.addCategory(userId, "Food", "food", monthlyMax = 0.0) })
    }

    // ---- categories: edit and delete ----

    @Test
    fun updateCategory_savesNewGoalsOnTheStoredCategory() = runBlocking {
        categories.addCategory(userId, "Food", "food", monthlyMax = 4000.0)
        val original = categories.getCategoriesForUser(userId).single()

        categories.updateCategory(original.copy(monthlyMax = 5000.0, monthlyMin = 2000.0))

        val saved = categories.getCategoriesForUser(userId).single()
        assertEquals(5000.0, saved.monthlyMax, 0.001)
        assertEquals(2000.0, saved.monthlyMin, 0.001)
        assertEquals(1, categoryDao.stored.size)
    }

    @Test
    fun updateCategory_rejectsBadGoalsAndUnsavedCategories() = runBlocking {
        categories.addCategory(userId, "Food", "food", monthlyMax = 4000.0)
        val original = categories.getCategoriesForUser(userId).single()

        assertTrue(rejects { categories.updateCategory(original.copy(monthlyMin = 9000.0)) })
        assertTrue(rejects { categories.updateCategory(original.copy(docId = "")) })
    }

    @Test
    fun deleteCategory_removesItButKeepsItsExpenses() = runBlocking {
        categories.addCategory(userId, "Food", "food", monthlyMax = 4000.0)
        expenses.addExpense("Checkers", "120", "Food")

        categories.deleteCategory(categories.getCategoriesForUser(userId).single())

        assertTrue(categories.getCategoriesForUser(userId).isEmpty())
        assertEquals(1, expenses.getExpenses().size)
    }

    // ---- expenses: income, edit, delete ----

    @Test
    fun addExpense_defaultsToAnExpenseForTheLoggedInUser() = runBlocking {
        expenses.addExpense("Checkers", "120", "Food")

        val saved = expenses.getExpenses().single()
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(userId, saved.userId)
    }

    @Test
    fun addExpense_canRecordIncome() = runBlocking {
        expenses.addExpense("Salary", "15000", "Income", type = TransactionType.INCOME)

        assertEquals(TransactionType.INCOME, expenses.getExpenses().single().type)
    }

    @Test
    fun updateExpense_savesChangesIncludingTimeOfDay() = runBlocking {
        expenses.addExpense("Checkers", "120", "Food", date = 1_000L)
        val original = expenses.getExpenses().single()

        expenses.updateExpense(
            original.copy(
                expenseName = "Woolworths",
                expenseAmount = "180",
                expenseDate = 5_400_000L
            )
        )

        val saved = expenses.getExpenses().single()
        assertEquals("Woolworths", saved.expenseName)
        assertEquals("180", saved.expenseAmount)
        assertEquals(5_400_000L, saved.expenseDate)
        assertEquals(1, expensesDao.stored.size)
    }

    @Test
    fun updateExpense_canRemoveAPhotoAndSwitchTheType() = runBlocking {
        expenses.addExpense("Checkers", "120", "Food", photoFilePath = "abc")
        val original = expenses.getExpenses().single()

        expenses.updateExpense(original.copy(photoFilePath = null, type = TransactionType.INCOME))

        val saved = expenses.getExpenses().single()
        assertNull(saved.photoFilePath)
        assertEquals(TransactionType.INCOME, saved.type)
    }

    @Test
    fun updateAndDeleteExpense_refuseAnExpenseThatWasNeverSaved() {
        val unsaved = Expenses(userId = userId, expenseName = "x", expenseAmount = "1", expenseCategory = "Food",
            photoFilePath = null, expenseDate = 1L)

        assertTrue(rejects { expenses.updateExpense(unsaved) })
        assertTrue(rejects { expenses.deleteExpense(unsaved) })
    }

    @Test
    fun deleteExpense_removesOnlyThatExpense() = runBlocking {
        expenses.addExpense("Checkers", "120", "Food")
        expenses.addExpense("Shell", "450", "Transport")
        val toDelete = expenses.getExpenses().first { it.expenseName == "Checkers" }

        expenses.deleteExpense(toDelete)

        assertEquals(listOf("Shell"), expenses.getExpenses().map { it.expenseName })
    }

    @Test
    fun getTotalsByCategory_passesTheBudgetedOnlyFlagThrough() = runBlocking {
        expenses.getTotalsByCategory(0L, 10L)
        assertFalse(expensesDao.lastBudgetedOnly!!)

        expenses.getTotalsByCategory(0L, 10L, budgetedOnly = true)
        assertTrue(expensesDao.lastBudgetedOnly!!)
    }
}