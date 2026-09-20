package com.example.republicsavingsapp.ui.expense

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.republicsavingsapp.CategoryTotal
import com.example.republicsavingsapp.CurrencyFormatter
import com.example.republicsavingsapp.Expenses
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentExpenseBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseFragment : Fragment() {
    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TransactionAdapter
    private var allExpenses: List<Expenses> = emptyList()

    // label shown -> how many of that Calendar unit to go back
    private val periods = listOf(
        Triple("Past day", Calendar.DAY_OF_YEAR, 1),
        Triple("Past week", Calendar.WEEK_OF_YEAR, 1),
        Triple("Past month", Calendar.MONTH, 1),
        Triple("Past 3 months", Calendar.MONTH, 3),
        Triple("Past 6 months", Calendar.MONTH, 6),
        Triple("Past year", Calendar.YEAR, 1)
    )
    private var selectedPeriodIndex = 2 // defaults to "Past month"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TransactionAdapter(emptyList())
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRecyclerView.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = filterExpenses(s?.toString().orEmpty())
        })

        setupPeriodFilter()
        loadBreakdown()
    }

    private fun setupPeriodFilter() {
        binding.periodFilter.text = periods[selectedPeriodIndex].first
        binding.periodFilter.setOnClickListener {
            val popup = PopupMenu(requireContext(), binding.periodFilter)
            periods.forEachIndexed { index, period -> popup.menu.add(0, index, index, period.first) }
            popup.setOnMenuItemClickListener { item ->
                selectedPeriodIndex = item.itemId
                binding.periodFilter.text = periods[selectedPeriodIndex].first
                loadBreakdown()
                true
            }
            popup.show()
        }
    }

    private fun currentRange(): Pair<Long, Long> {
        val (_, field, amount) = periods[selectedPeriodIndex]
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(field, -amount)
        return cal.timeInMillis to end
    }

    private fun loadBreakdown() {
        val app = requireActivity().application as RepublicSavingsApp
        lifecycleScope.launch {
            val (start, end) = currentRange()

            val totals = app.expenseRepository.getTotalsByCategory(start, end)
            binding.totalAmountText.text = CurrencyFormatter.formatWhole(totals.sumOf { it.total })
            setupPieChart(totals)

            allExpenses = app.expenseRepository.getAllExpensesBetween(start, end)
            filterExpenses(binding.searchInput.text.toString())
        }
    }

    private fun filterExpenses(query: String) {
        val filtered = if (query.isBlank()) {
            allExpenses
        } else {
            allExpenses.filter {
                it.expenseName.contains(query, ignoreCase = true) ||
                        it.expenseCategory.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filtered)
    }

    private fun setupPieChart(totals: List<CategoryTotal>) {
        if (totals.isEmpty()) {
            binding.categoryPieChart.clear()
            binding.categoryPieChart.setNoDataText("No expenses yet")
            return
        }

        val textColor = com.google.android.material.color.MaterialColors.getColor(
            binding.categoryPieChart, com.google.android.material.R.attr.colorOnSurface
        )

        val entries = totals.map { PieEntry(it.total.toFloat(), it.category) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 2f
            valueTextSize = 11f
            valueTextColor = textColor
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
            setEntryLabelColor(textColor)
            legend.isWordWrapEnabled = true
            legend.textColor = textColor
            animateY(600)
            invalidate()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}