package com.example.republicsavingsapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Login : Fragment() {
    private lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var signupButton: TextView
    private lateinit var forgotPasswordButton: TextView
    private lateinit var biometricLoginButton: Button

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
        biometricLoginButton = view.findViewById(R.id.biometricLogin)

        loginButton.setOnClickListener {
            val identifier = usernameField.text.toString().trim().lowercase()
            val password = passwordField.text.toString().trim()

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Fields cannot be empty", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val app = requireActivity().application as RepublicSavingsApp
                val user = app.userRepository.getUserByEmailOrUsername(identifier)
                val appContext = requireContext().applicationContext

                if (user == null) {
                    Toast.makeText(
                        requireContext(),
                        "No account found with this email",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (user.userPassword != password) {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT)
                        .show()
                    passwordField.text.clear()
                    return@launch
                }

                if (user.email.isNotEmpty()) {
                    try {
                        Firebase.auth.signInWithEmailAndPassword(user.email, password).await()
                    } catch (e: Exception) {
                        Log.w("Login", "Firebase Auth sign-in: ${e.message}")
                    }
                }

                if (user != null) {
                    CurrentUser.setUser(user)
                    SessionManager.saveUserId(
                        requireContext(),
                        user.userID
                    ) //update for session manager
                    (requireActivity() as MainActivity).onLoginSuccess()
                }
                if (user.biometricEnabled) {
                    SessionManager.saveQuickUserId(appContext, user.userID)
                } else {
                    SessionManager.clearQuickLogin(appContext)
                }

                Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                (requireActivity() as MainActivity).onLoginSuccess()
            }
        }
        //for those who can use quick login on the phone
        val quickUserID = SessionManager.getQuickUserId(requireContext())
        val canQuickLogin = quickUserID != -1L && PasskeyAuth.isAvailable(requireActivity())
        biometricLoginButton.visibility = if (canQuickLogin) View.VISIBLE else View.VISIBLE


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

        biometricLoginButton.setOnClickListener {
            val appContext = requireContext().applicationContext
            //ask for biometrics or phone's password/pin/pattern
            PasskeyAuth.prompt(
                activity = requireActivity(),
                title = "Log in with biometrics or your phone's password",
                onSuccess = { quickLogin(quickUserID) }, //if passed, log user in
                onFailure = { message ->                //failed or canceled, show why
                    Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()

                }
            )
        }
    }

    //logs in saved user when phone check is passed
    fun quickLogin(userID: Long) {
        val appContext = requireContext().applicationContext
        val app = requireActivity().application as RepublicSavingsApp
        val mainActivity = requireActivity() as MainActivity

        //Ties to screen's view, work is canceled if screen goes away
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                //Load user account from Firestore
                val user = app.userRepository.getUserById(userID)

                if (user != null) {
                    //Same steps as normal login
                    CurrentUser.setUser(user)
                    SessionManager.saveUserId(
                        requireContext(),
                        user.userID
                    ) //update for session manager
                    (requireActivity() as MainActivity).onLoginSuccess()

                } else {
                    //account doesn't exist, quick login switches off
                    SessionManager.clearQuickLogin(appContext)
                    biometricLoginButton.visibility = View.GONE
                    Toast.makeText(
                        appContext, "Account not found, please log in with your email",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e //never swallow cancellation, otherwise coroutine doesn't stop properly
            } catch (e: Exception) {
                //in the event of no internet connection
                Toast.makeText(
                    appContext, "Error loading account, Please check connection: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = Login()
    }
}
