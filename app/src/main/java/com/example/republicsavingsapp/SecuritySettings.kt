package com.example.republicsavingsapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.log

class SecuritySettings : Fragment() {
    private lateinit var biometricSwitch: Switch
    private lateinit var createAccountButton: Button
    private lateinit var skipButton: Button

    private val viewModel: RegistrationViewModel by activityViewModels()

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

        createAccountButton.setOnClickListener {
            viewModel.biometricEnabled = biometricSwitch.isChecked
            createAccount()
        }

        skipButton.setOnClickListener {
            viewModel.biometricEnabled = false
            createAccount()
        }
    }

    private fun createAccount() {
        val app = requireActivity().application as RepublicSavingsApp
        val appContext = requireContext().applicationContext
        val mainActivity = requireActivity() as MainActivity

        val newUser = User(
            userName = viewModel.username,
            userSurname = viewModel.surname,
            email = viewModel.email,
            userPassword = viewModel.password,
            currency = viewModel.currency,
            biometricEnabled = viewModel.biometricEnabled
        )

        //stop double taps when run is called
        createAccountButton.isEnabled = false
        skipButton.isEnabled = false


        lifecycleScope.launch {
            try {
                //return new ID generated for new user
                val newId = app.userRepository.addUser(newUser)

                if (newUser.email.isNotEmpty() && newUser.userPassword.isNotEmpty()) {
                    try {
                        Firebase.auth.createUserWithEmailAndPassword(
                            newUser.email,
                            newUser.userPassword
                        ).await()
                    } catch (e: CancellationException) {
                        throw e //never swallow cancellation
                    } catch (e: Exception) {
                        Log.w("Register", "Firebase Auth user creation error: ${e.message}")
                    }
                }

                //log new user in from sign up straight away
                CurrentUser.setUser(newUser.copy(userID = newId))
                SessionManager.saveUserId(appContext, newId)

                //if biometrics are enabled, enable quick signup for user
                if(newUser.biometricEnabled) SessionManager.saveQuickUserId(appContext, newId)

                Toast.makeText(appContext, "Account Created Successfully", Toast.LENGTH_SHORT)
                    .show()

                val fragmentManager = mainActivity.supportFragmentManager

                while (fragmentManager.backStackEntryCount > 0) {
                    fragmentManager.popBackStackImmediate()
                }

                mainActivity.showMainApp()


            } catch(e: CancellationException){
                throw e
            } catch (e: Exception) {
                Log.e("Register", "Account Creation Failed", e)
                Toast.makeText(appContext, "Error creating account: ${e.message}", Toast.LENGTH_SHORT).show()
                if(isAdded){
                    createAccountButton.isEnabled = true
                    skipButton.isEnabled = true
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SecuritySettings()
    }
}
