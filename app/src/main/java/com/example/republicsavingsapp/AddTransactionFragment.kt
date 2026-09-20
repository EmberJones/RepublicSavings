package com.example.republicsavingsapp.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentsAddTransactionBinding
import com.example.republicsavingsapp.ui.categories.Category
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionFragment : Fragment() {
    private var _binding: FragmentsAddTransactionBinding? = null
    private val binding get() = _binding!!
    private var categories: List<Category> = emptyList()
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentsAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCategoriesIntoSpinner()
        setupDatePicker()

        binding.closeButton.setOnClickListener { findNavController().popBackStack() }
        binding.saveTransactionButton.setOnClickListener { saveTransaction() }
        binding.saveTextButton.setOnClickListener { saveTransaction() }
    }

    private fun loadCategoriesIntoSpinner() {
        val repo = (requireActivity().application as RepublicSavingsApp).categoryRepository
        lifecycleScope.launch {
            categories = repo.getCategoriesForUser(CurrentUser.userID)
            val names = categories.map { it.categoryName }
            binding.categorySpinner.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, names
            )
        }
    }

    private fun setupDatePicker() {
        updateDateText()
        binding.dateInput.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    cal.set(year, month, day)
                    selectedDate = cal.timeInMillis
                    updateDateText()
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateText() {
        val format = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        binding.dateInput.text = format.format(selectedDate)
    }

    private fun saveTransaction() {
        val name = binding.transactionNameInput.text.toString().trim()
        val amountText = binding.amountInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim().ifEmpty { null }
        val includeInBudget = binding.includeInBudgetSwitch.isChecked

        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), "Create a category first", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Give the transaction a name", Toast.LENGTH_SHORT).show()
            return
        }
        val amountValue = amountText.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategory = categories[binding.categorySpinner.selectedItemPosition].categoryName
        val repo = (requireActivity().application as RepublicSavingsApp).expenseRepository

        lifecycleScope.launch {
            repo.addExpense(
                name = name,
                amount = amountText,
                category = selectedCategory,
                includeInBudget = includeInBudget,
                description = description,
                date = selectedDate
            )
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}