package com.example.republicsavingsapp.ui.transaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.ImageUtils
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.TransactionType
import com.example.republicsavingsapp.databinding.FragmentsAddTransactionBinding
import com.example.republicsavingsapp.ui.categories.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionFragment : Fragment() {
    private var _binding: FragmentsAddTransactionBinding? = null
    private val binding get() = _binding!!
    private var categories: List<Category> = emptyList()
    private var selectedDate: Long = System.currentTimeMillis() // date AND time of day
    private var photoFilePath: String? = null
    private var type = TransactionType.EXPENSE

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) savePickedImage(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentsAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCategoriesIntoSpinner()
        setupTypeToggle()
        setupDatePicker()
        setupTimePicker()
        setupPhotoPicker()

        binding.closeButton.setOnClickListener { findNavController().popBackStack() }
        binding.saveTransactionButton.setOnClickListener { saveTransaction() }
        binding.saveTextButton.setOnClickListener { saveTransaction() }
    }

    private fun loadCategoriesIntoSpinner() {
        val repo = (requireActivity().application as RepublicSavingsApp).categoryRepository
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = repo.getCategoriesForUser(CurrentUser.userID)
            val b = _binding ?: return@launch // screen was left while loading
            categories = loaded
            b.categorySpinner.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_dropdown_item,
                loaded.map { it.categoryName }
            )
        }
    }

    /** Expense/Income toggle. Income has no category and never counts toward the budget. */
    private fun setupTypeToggle() {
        binding.typeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            type = if (checkedId == binding.incomeToggleButton.id) TransactionType.INCOME
            else TransactionType.EXPENSE
            applyType()
        }
        applyType()
    }

    private fun applyType() {
        val expense = type == TransactionType.EXPENSE
        val expenseOnly = if (expense) View.VISIBLE else View.GONE
        binding.categoryLabel.visibility = expenseOnly
        binding.categorySpinner.visibility = expenseOnly
        binding.includeInBudgetCard.visibility = expenseOnly
        binding.screenTitle.text = if (expense) "Add expense" else "Add income"
        binding.transactionNameInput.hint = if (expense) "e.g. Checkers" else "e.g. Salary"
        binding.saveTransactionButton.text = if (expense) "Save expense" else "Save income"
    }

    private fun setupDatePicker() {
        updateDateText()
        binding.dateInput.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    cal.set(year, month, day) // keeps the chosen time of day
                    selectedDate = cal.timeInMillis
                    updateDateText()
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePicker() {
        updateTimeText()
        binding.timeInput.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    selectedDate = cal.timeInMillis // keeps the chosen date
                    updateTimeText()
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
            ).show()
        }
    }

    private fun updateDateText() {
        binding.dateInput.text = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(selectedDate)
    }

    private fun updateTimeText() {
        binding.timeInput.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedDate)
    }

    private fun setupPhotoPicker() {
        binding.receiptPhotoTarget.setOnClickListener {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun savePickedImage(uri: Uri) {
        val resolver = requireContext().contentResolver
        viewLifecycleOwner.lifecycleScope.launch {
            // Decode a downsized copy off the main thread (Firestore documents max out at 1 MiB)
            val bitmap = withContext(Dispatchers.IO) { decodeScaled(resolver, uri) } ?: return@launch
            val encoded = withContext(Dispatchers.IO) { ImageUtils.bitmapToBase64(bitmap) }
            val b = _binding ?: return@launch
            photoFilePath = encoded
            b.receiptPreviewImage.setImageBitmap(bitmap)
            b.receiptPreviewImage.visibility = View.VISIBLE
            b.receiptCameraIcon.visibility = View.GONE
            b.receiptPhotoHint.visibility = View.GONE
        }
    }

    /** Decodes the image at most [maxSide] px on its longest edge, without loading the full-size bitmap. */
    private fun decodeScaled(resolver: android.content.ContentResolver, uri: Uri, maxSide: Int = 800): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }

    private fun saveTransaction() {
        val isIncome = type == TransactionType.INCOME
        val name = binding.transactionNameInput.text.toString().trim()
        val amountText = binding.amountInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim().ifEmpty { null }

        if (!isIncome && categories.isEmpty()) { toast("Create a category first"); return }
        if (name.isEmpty()) { toast("Give the transaction a name"); return }
        val amountValue = amountText.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) { toast("Enter a valid amount"); return }

        // Income isn't tied to a budget category and never counts toward the budget.
        val category = if (isIncome) INCOME_CATEGORY
        else categories[binding.categorySpinner.selectedItemPosition].categoryName
        val includeInBudget = !isIncome && binding.includeInBudgetSwitch.isChecked
        val repo = (requireActivity().application as RepublicSavingsApp).expenseRepository

        // Stop double taps from saving twice
        binding.saveTransactionButton.isEnabled = false
        binding.saveTextButton.isEnabled = false

        // Fragment scope (not view scope) so the save still finishes if the user leaves quickly
        lifecycleScope.launch {
            repo.addExpense(
                name = name,
                amount = amountText,
                category = category,
                includeInBudget = includeInBudget,
                description = description,
                date = selectedDate,
                photoFilePath = photoFilePath,
                type = type
            )
            if (_binding != null) findNavController().popBackStack()
        }
    }

    private fun toast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        /** Category label stored on income transactions. */
        const val INCOME_CATEGORY = "Income"
    }
}