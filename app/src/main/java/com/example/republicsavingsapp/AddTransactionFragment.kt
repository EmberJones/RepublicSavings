package com.example.republicsavingsapp.ui.transaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
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
import com.example.republicsavingsapp.Expenses
import java.io.File

class AddTransactionFragment : Fragment() {
    private var _binding: FragmentsAddTransactionBinding? = null
    private val binding get() = _binding!!
    private var categories: List<Category> = emptyList()
    private var selectedDate: Long = System.currentTimeMillis() // date AND time of day
    private var photoFilePath: String? = null
    private var type = TransactionType.EXPENSE
    private var editing: Expenses? = null
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) savePickedImage(uri)
    }

    //Camera app will save photo it's about to take
    private var cameraImageUri : Uri? = null

    //runs when cam closes, success (true) if photo was indeed taken
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraImageUri
        if (success && uri != null) savePickedImage(uri) //same pipeline as gallery photos
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
        val app = requireActivity().application as RepublicSavingsApp
        val editDocId = arguments?.getString(ARG_DOC_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = app.categoryRepository.getCategoriesForUser(CurrentUser.userID)
            val existing = editDocId?.let { id ->
                app.expenseRepository.getAllExpensesBetween(0L, Long.MAX_VALUE).firstOrNull { it.docId == id }
            }
            val b = _binding ?: return@launch // screen was left while loading
            categories = loaded
            b.categorySpinner.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_dropdown_item,
                loaded.map { it.categoryName }
            )
            if (existing != null) { editing = existing; fillForm(b, existing) }
        }
    }

    /** Pre-fills the screen when editing an existing transaction. */
    private fun fillForm(b: FragmentsAddTransactionBinding, e: Expenses) {
        b.transactionNameInput.setText(e.expenseName)
        b.amountInput.setText(e.expenseAmount)
        b.descriptionInput.setText(e.expenseDescription.orEmpty())
        b.includeInBudgetSwitch.isChecked = e.includeInBudget
        selectedDate = e.expenseDate
        updateDateText()
        updateTimeText()
        val index = categories.indexOfFirst { it.categoryName == e.expenseCategory }
        if (index >= 0) b.categorySpinner.setSelection(index)

        photoFilePath = e.photoFilePath
        e.photoFilePath?.let { ImageUtils.base64ToBitmap(it) }?.let { bitmap ->
            b.receiptPreviewImage.setImageBitmap(bitmap)
            b.receiptPreviewImage.visibility = View.VISIBLE
            b.receiptCameraIcon.visibility = View.GONE
            b.receiptPhotoHint.visibility = View.GONE
        }

        type = e.type
        b.typeToggle.check(
            if (e.type == TransactionType.INCOME) b.incomeToggleButton.id else b.expenseToggleButton.id
        )
        applyType() // refreshes the title and button text for edit mode
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
        binding.transactionNameInput.hint = if (expense) "e.g. Checkers" else "e.g. Salary"
        val verb = if (editing != null) "Edit" else "Add"
        binding.screenTitle.text = "$verb ${if (expense) "expense" else "income"}"
        binding.saveTransactionButton.text =
            if (editing != null) "Save changes" else if (expense) "Save expense" else "Save income"
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
            val options = arrayOf("Take Photo", "Choose From Gallery")
            AlertDialog.Builder(requireContext())
                .setTitle("Add Photo")
                .setItems(options) { _, which -> if (which == 0) {
                        launchCamera()
                    } else {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                .show()
        }
    }

    private fun launchCamera() {
        try {
            //make temp file to store image
            val appContext = requireContext().applicationContext
            val dir = File(appContext.cacheDir, "receipts").apply { mkdirs() }
            val file = File.createTempFile("receipt_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
            cameraImageUri = uri
            takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            toast("No camera app found")
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

        //shrink to maxSide on Longest edge
        val longest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (longest <= maxSide) return bitmap else{
            val scale = maxSide.toFloat() / longest
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(), true)
        }

        //turn upright if photo's orientation note states it is rotated
        val rotation = readRotation(resolver, uri)
        if (rotation == 0 ) return scaled
        val matrix = Matrix().apply{postRotate(rotation.toFloat())}
        return Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
    }

    private fun readRotation(resolver: android.content.ContentResolver, uri: Uri): Int {
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
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

        val existing = editing
        lifecycleScope.launch {
            if (existing != null) {
                repo.updateExpense(
                    existing.copy(
                        expenseName = name,
                        expenseDescription = description,
                        expenseAmount = amountText,
                        expenseCategory = category,
                        includeInBudget = includeInBudget,
                        photoFilePath = photoFilePath,
                        expenseDate = selectedDate,
                        type = type
                    )
                )
            } else {
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
            }
            if (_binding != null) findNavController().popBackStack()
        }
    }

    private fun toast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        /** Category label stored on income transactions. */
        const val INCOME_CATEGORY = "Income"
        const val ARG_DOC_ID = "expenseDocId"
    }
}