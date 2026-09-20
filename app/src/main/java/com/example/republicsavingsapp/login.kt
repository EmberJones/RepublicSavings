package com.example.republicsavingsapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
/**
 * A simple Fragment subclass.
 * Use the Login.newInstance factory method to
 * create an instance of this fragment.
 */
class Login : Fragment() {
    // TODO: Rename and change types of parameters
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

        // Views only exist after the layout is inflated - grab them here
        usernameField = view.findViewById(R.id.username)
        passwordField = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login)
        signupButton = view.findViewById(R.id.signUpPrompt)
        forgotPasswordButton = view.findViewById(R.id.forgotPassword)

        //Initialize RoomDB
        val database = AppDatabase.getDatabase(requireContext())
        val userDAO = database.userDAO()

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim().lowercase()
            val password = passwordField.text.toString().trim()

        //Basic validation check
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(requireContext(), "Fields cannot be empty",
                Toast.LENGTH_SHORT
            ).show()
            return@setOnClickListener
        }

        //Save to database on a background thread using Coroutines
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO){
                userDAO.getUserByCredentials(username, password)  //users will define this
            }

            if(user != null){
                CurrentUser.setUser(user)
                Toast.makeText(requireContext(), "Login Successful",
                    Toast.LENGTH_SHORT).show()
                (requireActivity() as MainActivity).showMainApp()
                // e.g. save userId to a shared ViewModel, SharedPreferences, or navigate on:
                // findNavController().navigate(R.id.action_login_to_home, bundleOf("userId" to userId))
                } else {
                Toast.makeText((requireContext()), "Invalid username or password",
                    Toast.LENGTH_SHORT).show()

                //Clear inputs after saving
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
    companion object{
        @JvmStatic
        fun newInstance() = Login()
    }
}