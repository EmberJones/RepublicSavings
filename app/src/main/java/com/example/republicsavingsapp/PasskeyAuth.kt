package com.example.republicsavingsapp

import android.annotation.SuppressLint
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.play.integrity.internal.ac

object PasskeyAuth {
     //Accept Biometrics or phone pin/password
     private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

     //False if phone has no lock screen set up
     fun isAvailable(activity: FragmentActivity): Boolean {
      return BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) ==
              BiometricManager.BIOMETRIC_SUCCESS
     }

    //shows system prompt, onSuccess runs if passed, onFailure sends an error message
    //includes when prompt is canceled.
    @SuppressLint("SuspiciousIndentation")
    fun prompt(
         activity: FragmentActivity,
         title: String,
         onSuccess: () -> Unit,
         onFailure: (String) -> Unit
     ) {
         //callbacks run on main ui thread
         val executor = ContextCompat.getMainExecutor(activity)

         val biometricPrompt = BiometricPrompt(activity, executor,
             object : BiometricPrompt.AuthenticationCallback() {

                 //User passes check
                 override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                     onSuccess()
                 }

                 //Prompt ended without success (canceled/too many attempts)
                 override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                     onFailure(errString.toString()) //includes user cancelling
                 }

             })

        //Prompt and asks user to authenticate with available methods
         val promptInfo = BiometricPrompt.PromptInfo.Builder()
             .setTitle(title)
             .setAllowedAuthenticators(AUTHENTICATORS)
             .build() //no cancel button, can't be combined with DEVICE_CREDENTIAL

            biometricPrompt.authenticate(promptInfo)
     }


}