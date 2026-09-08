package com.example.republicsavingsapp

class UserRepository(private val expensesDAO: ExpensesDAO) {

    suspend fun addUser(name: String, pass: String) : Long
    {
        val newUser = Users(
            userName = name,
            userPassword = pass
        )

        return expensesDAO.AddUser(newUser)
    }

    suspend fun getUsers() : List<Users> {
        return expensesDAO.getAllUsers()
    }
}