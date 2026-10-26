package com.example.republicsavingsapp.ui.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CategoryListAdapter(emptyList())
        binding.categoryListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryListRecyclerView.adapter = adapter

        setupIconPicker()
        loadCategories()
        binding.addCategoryButton.setOnClickListener { binding.categoryNameInput.requestFocus() }
        binding.createCategoryButton.setOnClickListener { saveCategory() }
    }

    private fun setupIconPicker() {
        val iconButtons = mapOf(
            "food" to binding.iconOption1, "heart" to binding.iconOption2,
            "gift" to binding.iconOption3, "paw" to binding.iconOption4
        )
        fun refresh() = iconButtons.forEach { (key, button) ->
            button.setBackgroundResource(if (key == selectedIcon) R.drawable.bg_icon_option_selected else R.drawable.bg_circle_button)
        }
        iconButtons.forEach { (key, button) -> button.setOnClickListener { selectedIcon = key; refresh() } }
        refresh()
    }

    private fun loadCategories() {
        val repo = (requireActivity().application as RepublicSavingsApp).categoryRepository
        lifecycleScope.launch { adapter.updateData(repo.getCategoriesForUser(CurrentUser.userID)) }
    }

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
        lifecycleScope.launch {
            repo.addCategory(CurrentUser.userID, name, selectedIcon, monthlyMax, monthlyMin)
            _binding?.let {
                it.categoryNameInput.text?.clear()
                it.monthlyMaxInput.text?.clear()
                it.monthlyMinInput.text?.clear()
            }
            loadCategories()
        }
    }

    private fun toast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}