package com.example.republicsavingsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecuritySettings : Fragment(){
    private lateinit var biometricSwitch: Switch
    private lateinit var createAccountButton: Button
    private lateinit var skipButton: Button

    private  val viewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step3, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        biometricSwitch = view.findViewById(R.id.biometricSwitch)
        createAccountButton = view.findViewById(R.id.createAccountButton)
        skipButton = view.findViewById(R.id.skipButton)

        //Account creates regardless of switch on or off
        createAccountButton.setOnClickListener {
            viewModel.biometricEnabled = biometricSwitch.isChecked
            createAccount()
        }

        //Skip bypasses switch and disables biometric login
        skipButton.setOnClickListener {
            viewModel.biometricEnabled = false
            createAccount()
        }
    }

    private fun createAccount(){
        val database = AppDatabase.getDatabase(requireContext())
        val userDAO = database.userDAO()

        val newUser = User(
            userName = viewModel.firstName,
            userSurname = viewModel.surname,
            email = viewModel.email,
            userID = viewModel.username,
            userPassword = viewModel.password,
            currency = viewModel.currency,
            biometricEnabled = viewModel.biometricEnabled
        )

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    userDAO.insertUser(newUser)
                }
                Toast.makeText(requireContext(), "Account Created Successfully", Toast.LENGTH_SHORT)
                    .show()
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.main, Login())
                    .commit()
                requireActivity().supportFragmentManager.popBackStack(
                    null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
            } catch (e: android.database.sqlite.SQLiteConstraintException){
                Toast.makeText(requireContext(), "The username is already being used", Toast.LENGTH_SHORT).show()
            }
        }

    }
    companion object{
        @JvmStatic
        fun newInstance() = SecuritySettings()
    }
}