package com.example.republicsavingsapp.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.R
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentCategoriesBinding
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CategoryListAdapter
    private var selectedIcon = "food"
    private var iconButtons: Map<String, ImageButton> = emptyMap()

    /** The category being edited, or null when the form is creating a new one. */
    private var editing: Category? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CategoryListAdapter(emptyList()) { category -> showActions(category) }
        binding.categoryListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryListRecyclerView.adapter = adapter

        setupIconPicker()
        loadCategories()
        // The "+" button leaves edit mode (if on) and jumps to the empty form
        binding.addCategoryButton.setOnClickListener {
            exitEditMode()
            binding.categoryNameInput.requestFocus()
        }
        binding.createCategoryButton.setOnClickListener { saveCategory() }
    }

    private fun setupIconPicker() {
        iconButtons = mapOf(
            "food" to binding.iconOption1, "heart" to binding.iconOption2,
            "gift" to binding.iconOption3, "paw" to binding.iconOption4
        )
        iconButtons.forEach { (key, button) -> button.setOnClickListener { selectedIcon = key; refreshIcons() } }
        refreshIcons()
    }

    private fun refreshIcons() {
        iconButtons.forEach { (key, button) ->
            button.setBackgroundResource(
                if (key == selectedIcon) R.drawable.bg_icon_option_selected else R.drawable.bg_circle_button
            )
        }
    }

    private fun loadCategories() {
        val repo = (requireActivity().application as RepublicSavingsApp).categoryRepository
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = repo.getCategoriesForUser(CurrentUser.userID)
            if (_binding != null) adapter.updateData(loaded)
        }
    }

    // ---- edit / delete ----

    private fun showActions(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle(category.categoryName)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                if (which == 0) startEdit(category) else confirmDelete(category)
            }
            .show()
    }

    private fun startEdit(category: Category) {
        editing = category
        if (category.categoryIcon in iconButtons) selectedIcon = category.categoryIcon
        binding.createCategoryHeader.text = "EDIT CATEGORY"
        binding.categoryNameInput.setText(category.categoryName)
        binding.categoryNameInput.isEnabled = false // expenses refer to a category by its name
        binding.monthlyMaxInput.setText(plain(category.monthlyMax))
        binding.monthlyMinInput.setText(if (category.monthlyMin > 0) plain(category.monthlyMin) else "")
        binding.createCategoryButton.text = "Save changes"
        refreshIcons()
        binding.root.post { binding.root.smoothScrollTo(0, binding.createCategoryHeader.top) }
    }

    private fun exitEditMode() {
        val b = _binding ?: return
        editing = null
        selectedIcon = "food"
        b.createCategoryHeader.text = "CREATE NEW CATEGORY"
        b.categoryNameInput.isEnabled = true
        b.categoryNameInput.text?.clear()
        b.monthlyMaxInput.text?.clear()
        b.monthlyMinInput.text?.clear()
        b.createCategoryButton.text = "Add Category"
        refreshIcons()
    }

    private fun confirmDelete(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete category?")
            .setMessage("\"${category.categoryName}\" will be removed. Its expenses are kept, but no longer count toward a category goal.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                val repo = (requireActivity().application as RepublicSavingsApp).categoryRepository
                lifecycleScope.launch {
                    repo.deleteCategory(category)
                    if (_binding != null) {
                        if (editing?.docId == category.docId) exitEditMode()
                        loadCategories()
                    }
                }
            }
            .show()
    }

    // ---- create / save ----

    private fun saveCategory() {
        val name = binding.categoryNameInput.text.toString().trim()
        val monthlyMax = binding.monthlyMaxInput.text.toString().trim().toDoubleOrNull()
        // The minimum is optional: blank means "no minimum goal" (0).
        val minText = binding.monthlyMinInput.text.toString().trim()
        val monthlyMin = if (minText.isEmpty()) 0.0 else minText.toDoubleOrNull()

        if (name.isEmpty()) { toast("Give the category a name"); return }
        if (monthlyMax == null || monthlyMax <= 0) { toast("Enter a valid monthly maximum"); return }
        if (monthlyMin == null || monthlyMin < 0) { toast("Enter a valid monthly minimum, or leave it blank"); return }
        if (monthlyMin > monthlyMax) { toast("The minimum can't be higher than the maximum"); return }

        val repo = (requireActivity().application as RepublicSavingsApp).categoryRepository
        val existing = editing
        lifecycleScope.launch {
            if (existing != null) {
                repo.updateCategory(
                    existing.copy(categoryIcon = selectedIcon, monthlyMax = monthlyMax, monthlyMin = monthlyMin)
                )
            } else {
                repo.addCategory(CurrentUser.userID, name, selectedIcon, monthlyMax, monthlyMin)
            }
            exitEditMode()
            if (_binding != null) loadCategories()
        }
    }

    /** 2000.0 -> "2000", 12.5 -> "12.5" so the edit boxes don't show ".0". */
    private fun plain(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun toast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}