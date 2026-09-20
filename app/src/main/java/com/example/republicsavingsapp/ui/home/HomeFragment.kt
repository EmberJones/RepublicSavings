package com.example.republicsavingsapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.republicsavingsapp.CategoryTotal
import com.example.republicsavingsapp.CurrencyFormatter
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentHomeBinding
import com.example.republicsavingsapp.ui.categories.Category
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBudgetSummary()
    }

    private fun loadBudgetSummary() {
        val app = requireActivity().application as RepublicSavingsApp
        lifecycleScope.launch {
            val categories = app.categoryRepository.getCategoriesForUser(CurrentUser.userID)
            val (start, end) = currentMonthRange()
            val totals = app.expenseRepository.getTotalsByCategory(start, end)

            val budget = categories.sumOf { it.monthlyMax }
            val spent = totals.sumOf { it.total }
            val percent = if (budget > 0) ((spent / budget) * 100).toInt().coerceIn(0, 100) else 0

            binding.budgetProgress.progress = percent
            binding.budgetPercentText.text = "$percent%"
            binding.budgetAmountText.text = "${CurrencyFormatter.formatWhole(spent)} of ${CurrencyFormatter.formatWhole(budget)}"
            binding.budgetRemainingText.text = "${CurrencyFormatter.formatWhole((budget - spent).coerceAtLeast(0.0))} left this month"
            binding.greetingName.text = CurrentUser.userName

            setupCategoryBarChart(categories, totals)
        }
    }

    private fun setupCategoryBarChart(categories: List<Category>, totals: List<CategoryTotal>) {
        val spentByName = totals.associate { it.category to it.total }
        val entries = categories.mapIndexed { index, cat ->
            BarEntry(index.toFloat(), (spentByName[cat.categoryName] ?: 0.0).toFloat())
        }
        val labels = categories.map { it.categoryName }

        val textColor = MaterialColors.getColor(
            binding.categoryBarChart, R.attr.colorOnSurface
        )

        val dataSet = BarDataSet(entries, "Spent").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 10f
            valueTextColor = textColor
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return CurrencyFormatter.formatWhole(value.toDouble())
                }
            }
        }

        binding.categoryBarChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.textColor = textColor
            extraBottomOffset = 40f
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(labels)
                labelRotationAngle = -45f
                textSize = 9f
                setAvoidFirstLastClipping(true)
                this.textColor = textColor
            }
            setFitBars(true)
            animateY(600)
            invalidate()
        }
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to (cal.timeInMillis - 1)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}