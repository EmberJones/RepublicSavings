package com.example.republicsavingsapp.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.republicsavingsapp.CategoryTotal
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentExpenseBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class ExpenseFragment : Fragment() {
    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBreakdown()
    }

    private fun loadBreakdown() {
        val app = requireActivity().application as RepublicSavingsApp
        lifecycleScope.launch {
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.MONTH, -1)
            val totals = app.expenseRepository.getTotalsByCategory(cal.timeInMillis, end)

            binding.totalAmountText.text = String.format(Locale.getDefault(), "R%,.0f", totals.sumOf { it.total })
            setupPieChart(totals)
        }
    }

    private fun setupPieChart(totals: List<CategoryTotal>) {
        if (totals.isEmpty()) {
            binding.categoryPieChart.clear()
            binding.categoryPieChart.setNoDataText("No expenses yet")
            return
        }

        val entries = totals.map { PieEntry(it.total.toFloat(), it.category) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 2f
            valueTextSize = 11f
        }

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(binding.categoryPieChart))

        binding.categoryPieChart.apply {
            data = pieData
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 45f
            setEntryLabelTextSize(10f)
            legend.isWordWrapEnabled = true
            animateY(600)
            invalidate()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}