package com.example.republicsavingsapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController

class CurrencySelection : Fragment() {

    private lateinit var currencyGroup: RadioGroup
    private lateinit var continueButton: Button

    private val viewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_step2, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currencyGroup = view.findViewById(R.id.currencyOptionGroup)
        continueButton = view.findViewById(R.id.continueButton)

        continueButton.setOnClickListener {
            val selectedCurrency = when (currencyGroup.checkedRadioButtonId){
                R.id.optionZar -> "ZAR"
                R.id.optionUsd -> "USD"
                R.id.optionEur -> "EUR"
                else -> null
            }
            if (selectedCurrency == null){
                Toast.makeText(requireContext(), "Please select a currency", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.currency = selectedCurrency
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.main, SecuritySettings())
                .addToBackStack(null)
                .commit()
        }
    }
}