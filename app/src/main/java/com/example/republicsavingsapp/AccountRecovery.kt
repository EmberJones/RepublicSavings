package com.example.republicsavingsapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AccountRecovery : Fragment(){

    private lateinit var recoveryEmailField: EditText
    private lateinit var sendResetLinkButton: Button
    private lateinit var verifyWithBiometricButton: Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_recovery, container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recoveryEmailField = view.findViewById(R.id.email)
        sendResetLinkButton = view.findViewById(R.id.sendResetLinkButton)
        verifyWithBiometricButton = view.findViewById(R.id.biometricVerifyButton)

        val database = AppDatabase.getDatabase(requireContext())
        val userDAO = database.userDAO()

        sendResetLinkButton.setOnClickListener {
            val email = recoveryEmailField.text.toString().trim()

            if(email.isEmpty()){
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO){
                    userDAO.getUserByEmail(email)
                }

                if(user == null){
                    Toast.makeText(requireContext(), "No Account found with that email", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                //No backend email service. This simulates the link being sent
                Toast.makeText(requireContext(), "Reset instructions have been sent to your email (SIMULATED)",
                    Toast.LENGTH_SHORT).show()

                goToResetPassword(email)


            }
        }

        verifyWithBiometricButton.setOnClickListener {
            val email = recoveryEmailField.text.toString().trim()

            if(email.isEmpty()){
                Toast.makeText(requireContext(), "Please enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO){
                    userDAO.getUserByEmail(email)
                }

                if (user == null){
                    Toast.makeText(requireContext(), "No account with this email", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                startBiometricVerification(email)
            }

        }

    }

    private fun startBiometricVerification(email: String){
        val biometricManager = BiometricManager.from(requireContext())

        when (biometricManager.canAuthenticate(BiometricManager.
        Authenticators.BIOMETRIC_STRONG)){
            BiometricManager.BIOMETRIC_SUCCESS -> {
                //proceed below
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(requireContext(),
                    "Biometric authentication is not available on this device",
                    Toast.LENGTH_SHORT).show()
                return
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(requireContext(), "No biometrics enrolled on this device",
                    Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                Toast.makeText(requireContext(), "Biometric Authentication Unavailable",
                    Toast.LENGTH_SHORT).show()
                return
            }

        }

        val executor = ContextCompat.getMainExecutor(requireContext())

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(requireContext(), "Identity Verified!",
                    Toast.LENGTH_SHORT).show()
                goToResetPassword(email)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(requireContext(), "Biometric error",
                    Toast.LENGTH_SHORT).show()
                return
            }

        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Your Identity")
            .setSubtitle("Use your fingerprint or face to reset your password")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }


    private fun goToResetPassword(email: String){
        val resetFragment = ResetPassword().apply {
            arguments = Bundle().apply {
                putString("email", email)
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, Login())
            .addToBackStack(null)
            .commit()

    }

    companion object{
        @JvmStatic
        fun newInstance() = AccountRecovery()
    }


}