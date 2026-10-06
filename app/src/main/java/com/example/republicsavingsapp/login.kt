package com.example.republicsavingsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Login : Fragment() {
    private lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: TextView
    private lateinit var forgotPasswordButton: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usernameField = view.findViewById(R.id.username)
        passwordField = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login)
        signupButton = view.findViewById(R.id.signUpPrompt)
        forgotPasswordButton = view.findViewById(R.id.forgotPassword)

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim().lowercase()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val app = requireActivity().application as RepublicSavingsApp
                val user = app.userRepository.getUserByCredentials(username, password)

                if (user != null) {
                    CurrentUser.setUser(user)
                    Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                    (requireActivity() as MainActivity).onLoginSuccess()
                } else {
                    Toast.makeText(requireContext(), "Invalid username or password", Toast.LENGTH_SHORT).show()
                    usernameField.text.clear()
                    passwordField.text.clear()
                }
            }
        }

        signupButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, Register())
                .commit()
        }

        forgotPasswordButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, AccountRecovery())
                .commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = Login()
    }
}
