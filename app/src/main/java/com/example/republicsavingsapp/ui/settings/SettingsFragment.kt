package com.example.republicsavingsapp.ui.settings

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val currencyOptions = listOf("ZAR", "USD", "EUR")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTheme()
        setupCurrency()
        setupNotifications()
        setupUnlock()
    }

    // --- Theme ---

    private fun setupTheme() {
        binding.themeOptionLight.setOnClickListener { applyTheme(AppCompatDelegate.MODE_NIGHT_NO) }
        binding.themeOptionDark.setOnClickListener { applyTheme(AppCompatDelegate.MODE_NIGHT_YES) }
        binding.themeOptionSystem.setOnClickListener { applyTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
        reflectCurrentTheme()
    }

    private fun applyTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        requireContext().getSharedPreferences("app_prefs", 0).edit()
            .putInt("theme_mode", mode)
            .apply()
        reflectCurrentTheme()
    }

    private fun reflectCurrentTheme() {
        val current = AppCompatDelegate.getDefaultNightMode()
        binding.themeOptionLight.setTypeface(binding.themeOptionLight.typeface,
            if (current == AppCompatDelegate.MODE_NIGHT_NO) Typeface.BOLD else Typeface.NORMAL)
        binding.themeOptionDark.setTypeface(binding.themeOptionDark.typeface,
            if (current == AppCompatDelegate.MODE_NIGHT_YES) Typeface.BOLD else Typeface.NORMAL)
        binding.themeOptionSystem.setTypeface(binding.themeOptionSystem.typeface,
            if (current == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) Typeface.BOLD else Typeface.NORMAL)
    }

    // --- Currency ---

    private fun setupCurrency() {
        val app = requireActivity().application as RepublicSavingsApp
        viewLifecycleOwner.lifecycleScope.launch {
            val user = app.userRepository.getUserById(CurrentUser.userID)
            binding.currencyValue.text = user?.currency ?: "ZAR"
        }

        binding.currencyRow.setOnClickListener {
            val currentIndex = currencyOptions.indexOf(binding.currencyValue.text.toString()).coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("Choose currency")
                .setSingleChoiceItems(currencyOptions.toTypedArray(), currentIndex) { dialog, which ->
                    val selected = currencyOptions[which]
                    binding.currencyValue.text = selected
                    CurrentUser.updateCurrency(selected)
                    viewLifecycleOwner.lifecycleScope.launch {
                        app.userRepository.updateCurrency(CurrentUser.userID, selected)
                    }
                    dialog.dismiss()
                }
                .show()
        }
    }

    // --- Notifications (placeholder - no notification system exists yet) ---

    private fun setupNotifications() {
        binding.notificationsRow.setOnClickListener {
            Toast.makeText(requireContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Unlock / reveal account details ---

    private fun setupUnlock() {
        binding.unlockButton.setOnClickListener {
            val entered = binding.unlockPassword.text.toString()
            if (entered.isEmpty()) {
                Toast.makeText(requireContext(), "Enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val app = requireActivity().application as RepublicSavingsApp
            viewLifecycleOwner.lifecycleScope.launch {
                val correct = app.userRepository.isCorrectUsernameAndPassword(CurrentUser.userName, entered)
                if (correct) {
                    val user = app.userRepository.getUserById(CurrentUser.userID)
                    binding.accountLockedTitle.text = "Account unlocked"
                    binding.accountLockedSubtitle.text = user?.email ?: ""
                    binding.unlockPassword.visibility = View.GONE
                    binding.unlockButton.visibility = View.GONE
                } else {
                    Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}