package com.example.republicsavingsapp

class UserRepository(private val expensesDAO: ExpensesDAO) {

    suspend fun addUser(name: String, surname: String, pass: String, curr : String, mail: String, biomet: Boolean) : Long
    {
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

    suspend fun getUsers() : List<User> {
        return expensesDAO.getAllUsers()
    }
    suspend fun getUserById(userID: Int): User? {
        return expensesDAO.getUserById(userID)
    }

    suspend fun updateCurrency(userID: Int, currency: String) {
        expensesDAO.updateCurrency(userID, currency)
    }

    suspend fun isCorrectUsernameAndPassword(name: String, pass: String) : Boolean {
        val allUsers = expensesDAO.getAllUsers()

        for (user in allUsers)
        {
            if (user.userName == name && user.userPassword == pass)
            {
                return true
            }
        }

        return false
    }

    // Does user exist?

    // used to determine if the username entered has used this app before? if false, prompt to make account
    suspend fun isPreExistingUsername(name: String): Boolean {
        val allUsers = expensesDAO.getAllUsers()

        for (user in allUsers)
        {
            if (user.userName == name)
                return true
        }

        return false
    }
}