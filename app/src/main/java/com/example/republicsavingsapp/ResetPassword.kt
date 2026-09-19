package com.example.republicsavingsapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ResetPassword : Fragment() {

    private lateinit var newPasswordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var resetPasswordButton: Button
    private lateinit var backButton: ImageButton

    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        email = arguments?.getString("email")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_changepassword,
            container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newPasswordField = view.findViewById(R.id.newPasswordInput)
        confirmPasswordField = view.findViewById(R.id.confirmPasswordInput)
        resetPasswordButton = view.findViewById(R.id.confirmChangeButton)
        backButton = view.findViewById(R.id.backButton)


        val database = AppDatabase.getDatabase(requireContext())
        val userDAO = database.userDAO()

        resetPasswordButton.setOnClickListener {
            val newPassword = newPasswordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()
            val fragmentManager = requireActivity().supportFragmentManager

            if(newPassword.isEmpty() || confirmPassword.isEmpty()){
                Toast.makeText(requireContext(), "Please fill both fields",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(newPassword.length < 8){
                Toast.makeText(requireContext(), "Password Must have 8 characters and at least one number",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(newPassword != confirmPassword){
                Toast.makeText(requireContext(), "Passwords do not match",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userEmail = email
            if(userEmail == null){
                Toast.makeText(requireContext(), "No account reference found",
                    Toast.LENGTH_SHORT).show()
            }

            lifecycleScope.launch {
                withContext(Dispatchers.IO){
                    userDAO.updatePassword(userEmail, newPassword)
                }

                Toast.makeText(requireContext(), "Password successfully reset",
                    Toast.LENGTH_SHORT).show()

                while(fragmentManager.backStackEntryCount > 0){
                    fragmentManager.popBackStackImmediate()
                }

                fragmentManager.beginTransaction()
                    .replace(R.id.auth_container, Login())
                    .commit()
            }



        }

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, Login())
                .commit()
        }



    }
    companion object{
        @JvmStatic
        fun newInstance() = ResetPassword()
    }


}