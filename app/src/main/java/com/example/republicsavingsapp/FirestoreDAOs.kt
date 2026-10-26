package com.example.republicsavingsapp

import com.example.republicsavingsapp.ui.categories.Category
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions
class FirestoreCategoryDAO(private val firestore: FirebaseFirestore) : CategoryDAO {
    private val categories get() = firestore.collection("categories")

    override suspend fun addCategory(category: Category): Long {
        val docRef = categories.document()
        val id = if (category.categoryID != 0) category.categoryID else (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        docRef.set(categoryData(category, id)).await()
        AppLog.d("Categories", "Added category \"${category.categoryName}\" (max ${category.monthlyMax}, min ${category.monthlyMin})")
        return id.toLong()
    }

    override suspend fun getAllForUser(activeUserID: Long): List<Category> {
        val snapshot = categories
            .whereEqualTo("userId", activeUserID)
            .get()
            .await()
        val result = snapshot.documents.map { docToCategory(it, activeUserID) }
        AppLog.d("Categories", "Loaded ${result.size} categories")
        return result
    }

    // merge keeps any fields this app doesn't know about; known fields are overwritten
    override suspend fun updateCategory(docId: String, category: Category) {
        categories.document(docId)
            .set(categoryData(category, category.categoryID), SetOptions.merge())
            .await()
            AppLog.d("Categories", "Updated category doc $docId")
    }

    // Expenses that used this category are kept (they reference it by name)
    override suspend fun deleteCategory(docId: String) {
        categories.document(docId).delete().await()
        AppLog.d("Categories", "Deleted category doc $docId")
    }

    private fun categoryData(category: Category, id: Int): Map<String, Any?> = mapOf(
        "categoryID" to id,
        "userId" to category.userId,
        "categoryName" to category.categoryName,
        "categoryIcon" to category.categoryIcon,
        "monthlyMax" to category.monthlyMax,
        "monthlyMin" to category.monthlyMin
    )

    private fun docToCategory(doc: DocumentSnapshot, defaultUserId: Long): Category {
        return Category(
            categoryID = doc.getLong("categoryID")?.toInt() ?: 0,
            userId = doc.getLong("userId") ?: defaultUserId,
            categoryName = doc.getString("categoryName") ?: "",
            categoryIcon = doc.getString("categoryIcon") ?: "",
            monthlyMax = doc.getDouble("monthlyMax") ?: 0.0,
            monthlyMin = doc.getDouble("monthlyMin") ?: 0.0, // old documents have no minimum
            docId = doc.id
        )
    }
}

class FirestoreExpensesDAO(private val firestore: FirebaseFirestore) : ExpensesDAO {
    override suspend fun AddExpense(expenses: Expenses): Long {
        val docRef = firestore.collection("expenses").document()
        val id = if (expenses.expenseID != 0) expenses.expenseID else (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        docRef.set(expenseData(expenses, id)).await()
        AppLog.d("Expenses", "Added ${expenses.type} \"${expenses.expenseName}\" in ${expenses.expenseCategory} (id=$id)")
        return id.toLong()
    }

    // merge keeps any fields this app does not know about; known fields (including a removed photo) are overwritten
    override suspend fun updateExpense(docId: String, expense: Expenses) {
        firestore.collection("expenses").document(docId)
            .set(expenseData(expense, expense.expenseID), SetOptions.merge())
            .await()
        AppLog.d("Expenses", "Updated expense doc $docId")
    }

    override suspend fun deleteExpense(docId: String) {
        firestore.collection("expenses").document(docId).delete().await()
        AppLog.d("Expenses", "Deleted expense doc $docId")
    }

    private fun expenseData(expenses: Expenses, id: Int): Map<String, Any?> = mapOf(
        "expenseID" to id,
        "userId" to expenses.userId,
        "expenseName" to expenses.expenseName,
        "expenseDescription" to expenses.expenseDescription,
        "expenseAmount" to expenses.expenseAmount,
        "expenseCategory" to expenses.expenseCategory,
        "includeInBudget" to expenses.includeInBudget,
        "photoFilePath" to expenses.photoFilePath,
        "expenseDate" to expenses.expenseDate,
        "uploaded" to expenses.uploaded,
        "type" to expenses.type.name
    )

    override suspend fun AddUser(users: User): Long {
        val docRef = firestore.collection("users").document()
        val id = if (users.userID != 0L) users.userID else System.currentTimeMillis()
        val data = mapOf(
            "userID" to id,
            "userName" to users.userName,
            "userSurname" to users.userSurname,
            "email" to users.email,
            "userPassword" to users.userPassword,
            "currency" to users.currency,
            "biometricEnabled" to users.biometricEnabled
        )
        docRef.set(data).await()
        return id
    }

    override suspend fun getUserById(userID: Long): User? {
        val snapshot = firestore.collection("users")
            .whereEqualTo("userID", userID)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return docToUser(doc)
    }

    override suspend fun updateCurrency(userID: Long, currency: String) {
        val snapshot = firestore.collection("users")
            .whereEqualTo("userID", userID)
            .get()
            .await()
        for (doc in snapshot.documents) {
            doc.reference.update("currency", currency).await()
        }
    }

    override suspend fun getAllFromUserInCategory(activeUserID: Long, category: String): List<Expenses> {
        val snapshot = firestore.collection("expenses")
            .whereEqualTo("userId", activeUserID)
            .whereEqualTo("expenseCategory", category)
            .get()
            .await()
        return snapshot.documents.mapNotNull { docToExpense(it) }
            .sortedByDescending { it.uploaded }
    }

    override suspend fun getAllExpensesSince(date: Long): List<Expenses> {
        val snapshot = firestore.collection("expenses")
            .whereEqualTo("userId", CurrentUser.userID)
            .whereGreaterThan("expenseDate", date)
            .get()
            .await()
        return snapshot.documents.mapNotNull { docToExpense(it) }
            .sortedBy { it.expenseDate }
    }

    override suspend fun getAllExpensesUpTo(date: Long): List<Expenses> {
        val snapshot = firestore.collection("expenses")
            .whereEqualTo("userId", CurrentUser.userID)
            .whereLessThan("expenseDate", date)
            .get()
            .await()
        return snapshot.documents.mapNotNull { docToExpense(it) }
            .sortedByDescending { it.expenseDate }
    }

    override suspend fun getAllExpensesBetween(firstDate: Long, lastDate: Long): List<Expenses> {
        val snapshot = firestore.collection("expenses")
            .whereEqualTo("userId", CurrentUser.userID)
            .get()
            .await()
        return snapshot.documents.mapNotNull { docToExpense(it) }
            .filter { it.expenseDate in firstDate..lastDate }
            .sortedBy { it.expenseDate }
    }

    override suspend fun getTotalsByCategory(activeUserID: Long, startDate: Long, endDate: Long, budgetedOnly: Boolean): List<CategoryTotal> {
        val snapshot = firestore.collection("expenses")
            .whereEqualTo("userId", activeUserID)
            .get()
            .await()
        val expenses = snapshot.documents.mapNotNull { docToExpense(it) }
            .filter { it.expenseDate in startDate..endDate }
            .filter { it.type == TransactionType.EXPENSE }
            .filter { !budgetedOnly || it.includeInBudget }
        AppLog.d("Expenses", "Totals: ${expenses.size} expenses in range (budgetedOnly=$budgetedOnly)")
        return expenses.groupBy { it.expenseCategory }
            .map { (cat, list) ->
                val total = list.sumOf { it.expenseAmount.toDoubleOrNull() ?: 0.0 }
                CategoryTotal(cat, total)
            }
    }

    override suspend fun getAllFromUser(activeUserID: Long): List<Expenses> {
        val snapshot = firestore.collection("expenses")
            .whereEqualTo("userId", activeUserID)
            .get()
            .await()
        return snapshot.documents.mapNotNull { docToExpense(it) }
            .sortedByDescending { it.uploaded }
    }

    override suspend fun getAllUsers(): List<User> {
        val snapshot = firestore.collection("users")
            .get()
            .await()
        return snapshot.documents.mapNotNull { docToUser(it) }
    }

    private fun docToUser(doc: DocumentSnapshot): User {
        return User(
            userID = doc.getLong("userID") ?: 0L,
            userName = doc.getString("userName") ?: "",
            userSurname = doc.getString("userSurname") ?: "",
            email = doc.getString("email") ?: "",
            userPassword = doc.getString("userPassword") ?: "",
            currency = doc.getString("currency") ?: "ZAR",
            biometricEnabled = doc.getBoolean("biometricEnabled") ?: false
        )
    }

    private fun docToExpense(doc: DocumentSnapshot): Expenses {
        return Expenses(
            expenseID = doc.getLong("expenseID")?.toInt() ?: 0,
            userId = doc.getLong("userId") ?: 0L,
            expenseName = doc.getString("expenseName") ?: "",
            expenseDescription = doc.getString("expenseDescription"),
            expenseAmount = doc.getString("expenseAmount") ?: "0.0",
            expenseCategory = doc.getString("expenseCategory") ?: "",
            includeInBudget = doc.getBoolean("includeInBudget") ?: true,
            photoFilePath = doc.getString("photoFilePath"),
            expenseDate = doc.getLong("expenseDate") ?: 0L,
            uploaded = doc.getLong("uploaded") ?: 0L,
            type = TransactionType.values().firstOrNull { it.name == doc.getString("type") } ?: TransactionType.EXPENSE,
            docId = doc.id)
    }
}

class FirestoreUserDAO(private val firestore: FirebaseFirestore) : UserDAO {
    override suspend fun getUserByCredentials(username: String, password: String): User? {
        val snapshot = firestore.collection("users")
            .whereEqualTo("userName", username)
            .whereEqualTo("userPassword", password)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return docToUser(doc)
    }

    override suspend fun getUserByEmail(email: String): User? {
        val snapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return docToUser(doc)
    }

    override suspend fun getUserByEmailOrUsername(identifier: String): User? {
        val userByEmail = getUserByEmail(identifier)
        if (userByEmail != null) return userByEmail

        val snapshot = firestore.collection("users")
            .whereEqualTo("userName", identifier)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return docToUser(doc)
    }

    override suspend fun getUserIdByUsername(username: String): Int? {
        val snapshot = firestore.collection("users")
            .whereEqualTo("userName", username)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return doc.getLong("userID")?.toInt()
    }

    override suspend fun updatePassword(email: String?, newPassword: String) {
        if (email.isNullOrEmpty()) return
        val snapshot = firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .await()
        for (doc in snapshot.documents) {
            doc.reference.update("userPassword", newPassword).await()
        }
    }

    private fun docToUser(doc: DocumentSnapshot): User {
        return User(
            userID = doc.getLong("userID") ?: 0L,
            userName = doc.getString("userName") ?: "",
            userSurname = doc.getString("userSurname") ?: "",
            email = doc.getString("email") ?: "",
            userPassword = doc.getString("userPassword") ?: "",
            currency = doc.getString("currency") ?: "ZAR",
            biometricEnabled = doc.getBoolean("biometricEnabled") ?: false
        )
    }
}
