package com.example.republicsavingsapp.ui.settings

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.republicsavingsapp.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.themeOptionLight.setOnClickListener {
            // user has selected light mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            binding.themeOptionLight.setTypeface(binding.themeOptionLight.typeface, Typeface.BOLD)
            binding.themeOptionDark.setTypeface(binding.themeOptionDark.typeface, Typeface.NORMAL)
            binding.themeOptionSystem.setTypeface(binding.themeOptionSystem.typeface, Typeface.NORMAL)

        }

        binding.themeOptionDark.setOnClickListener {
            // user has selected dark mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            binding.themeOptionDark.setTypeface(binding.themeOptionDark.typeface, Typeface.BOLD)
            binding.themeOptionLight.setTypeface(binding.themeOptionLight.typeface, Typeface.NORMAL)
            binding.themeOptionSystem.setTypeface(binding.themeOptionSystem.typeface, Typeface.NORMAL)
        }

        binding.themeOptionSystem.setOnClickListener {
            // user has selected system mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            binding.themeOptionSystem.setTypeface(binding.themeOptionSystem.typeface, Typeface.BOLD)
            binding.themeOptionLight.setTypeface(binding.themeOptionLight.typeface, Typeface.NORMAL)
            binding.themeOptionDark.setTypeface(binding.themeOptionDark.typeface, Typeface.NORMAL)
        }

        return binding.root
    }


    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}