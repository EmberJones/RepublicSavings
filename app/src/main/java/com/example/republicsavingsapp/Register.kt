package com.example.republicsavingsapp

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class Register : Fragment() {

    private lateinit var firstNameField: EditText
    private lateinit var surnameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var registerButton: Button

    private val viewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firstNameField = view.findViewById(R.id.firstName)
        surnameField = view.findViewById(R.id.lastName)
        emailField = view.findViewById(R.id.email)
        passwordField = view.findViewById(R.id.password)
        registerButton = view.findViewById(R.id.continueButton)

        val database = AppDatabase.getDatabase(requireContext())
        val userDAO = database.userDAO()

        registerButton.setOnClickListener {
            val firstName = firstNameField.text.toString().trim()
            val lastName = surnameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()


            //standard validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()){
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                Toast.makeText(requireContext(), "Enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8){
                Toast.makeText(requireContext(), "Password must be at least 8 characters with one number",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = withContext(Dispatchers.IO){
                    userDAO.getUserByEmail(email)
                }

                if (existingUser != null){
                    Toast.makeText(requireContext(), "An account with this email already exists",
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }
                    viewModel.firstName = firstName
                    viewModel.surname = lastName
                    viewModel.email = email
                    viewModel.username = firstName //username same as email
                    viewModel.password = password

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main, CurrencySelection())
                    .addToBackStack(null)
                    .commit()
            }


        }



    }

    companion object{
        @JvmStatic
        fun newInstance() = Register()
    }

}