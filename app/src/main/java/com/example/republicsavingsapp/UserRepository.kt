package com.example.republicsavingsapp

class UserRepository(
    private val userDAO: UserDAO,
    private val expensesDAO: ExpensesDAO
) {

    suspend fun addUser(name: String, surname: String, pass: String, curr: String, mail: String, biomet: Boolean): Long {
        val newUser = User(
            userID = 0,
            userName = name,
            userSurname = surname,
            currency = curr,
            email = mail,
            biometricEnabled = biomet,
            userPassword = pass
        )
        return expensesDAO.AddUser(newUser)
    }

    suspend fun addUser(user: User): Long {
        return expensesDAO.AddUser(user)
    }

    suspend fun getUsers(): List<User> {
        return expensesDAO.getAllUsers()
    }

    suspend fun getUserById(userID: Long): User? {
        return expensesDAO.getUserById(userID)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDAO.getUserByEmail(email)
    }

    suspend fun getUserByEmailOrUsername(identifier: String): User? {
        return userDAO.getUserByEmailOrUsername(identifier)
    }

    suspend fun getUserByCredentials(username: String, pass: String): User? {
        return userDAO.getUserByCredentials(username, pass)
    }

    suspend fun updateCurrency(userID: Long, currency: String) {
        expensesDAO.updateCurrency(userID, currency)
    }

    suspend fun updatePassword(email: String, newPassword: String) {
        userDAO.updatePassword(email, newPassword)
    }

    suspend fun isCorrectUsernameAndPassword(name: String, pass: String): Boolean {
        val user = userDAO.getUserByCredentials(name, pass)
        return user != null
    }

    suspend fun isPreExistingUsername(name: String): Boolean {
        val userId = userDAO.getUserIdByUsername(name)
        return userId != null
    }
}
