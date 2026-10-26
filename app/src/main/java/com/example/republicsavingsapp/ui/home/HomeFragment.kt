package com.example.republicsavingsapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.republicsavingsapp.CategoryTotal
import com.example.republicsavingsapp.CurrencyFormatter
import com.example.republicsavingsapp.CurrentUser
import com.example.republicsavingsapp.RepublicSavingsApp
import com.example.republicsavingsapp.databinding.FragmentHomeBinding
import com.example.republicsavingsapp.ui.categories.Category
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.google.android.material.color.MaterialColors.getColor
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.republicsavingsapp.databinding.ItemGoalProgressBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    /** Selectable periods for the goals chart. */
    private enum class Period(val label: String, val days: Int) {
        WEEK("Past week", 7),
        MONTH("Past month", 30),
        THREE_MONTHS("Past 3 months", 90),
        SIX_MONTHS("Past 6 months", 180),
        YEAR("Past year", 365)
    }

    private var period = Period.MONTH

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.periodButton.text = "${period.label} ▾"
        binding.periodButton.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                Period.values().forEachIndexed { i, p -> menu.add(0, i, i, p.label) }
                setOnMenuItemClickListener { item ->
                    period = Period.values()[item.itemId]
                    binding.periodButton.text = "${period.label} ▾"
                    loadGoalsChart() // only the chart follows the selector
                    true
                }
            }.show()
        }
        loadBudgetSummary()
        loadGoalsChart()
        loadGoalProgress() // the progress section is always the last 30 days
    }

    /** Budget ring: current calendar month, budgeted expenses only. */
    private fun loadBudgetSummary() {
        val app = requireActivity().application as RepublicSavingsApp
        lifecycleScope.launch {
            val categories = app.categoryRepository.getCategoriesForUser(CurrentUser.userID)
            val (start, end) = currentMonthRange()
            val totals = app.expenseRepository.getTotalsByCategory(start, end, budgetedOnly = true)
            val b = _binding ?: return@launch

            val budget = categories.sumOf { it.monthlyMax }
            val spent = totals.sumOf { it.total }
            val percent = if (budget > 0) ((spent / budget) * 100).toInt().coerceIn(0, 100) else 0

            b.budgetProgress.progress = percent
            b.budgetPercentText.text = "$percent%"
            b.budgetAmountText.text = "${CurrencyFormatter.formatWhole(spent)} of ${CurrencyFormatter.formatWhole(budget)}"
            b.budgetRemainingText.text = "${CurrencyFormatter.formatWhole((budget - spent).coerceAtLeast(0.0))} left this month"
            b.greetingName.text = CurrentUser.userName
        }
    }

    /** Spend per category over the chosen period, with min/max goals scaled to that period. */
    private fun loadGoalsChart() {
        val app = requireActivity().application as RepublicSavingsApp
        val (start, end) = periodRange(period.days)
        val goalFactor = period.days / 30.0
        lifecycleScope.launch {
            val categories = app.categoryRepository.getCategoriesForUser(CurrentUser.userID)
            val totals = app.expenseRepository.getTotalsByCategory(start, end, budgetedOnly = true)
            val b = _binding ?: return@launch
            drawGoalsChart(b, categories, totals, goalFactor)
        }
    }

    private fun drawGoalsChart(
        b: FragmentHomeBinding,
        categories: List<Category>,
        totals: List<CategoryTotal>,
        goalFactor: Double
    ) {
        val chart = b.categoryBarChart
        val textColor = getColor(chart, R.attr.colorOnSurface)
        val barColor = MaterialColors.getColor(chart, androidx.appcompat.R.attr.colorPrimary)
        chart.setNoDataTextColor(textColor)
        chart.setNoDataText("Add a category to see your goals")
        if (categories.isEmpty()) { chart.clear(); return }

        val spentByName = totals.associate { it.category to it.total }
        val barEntries = categories.mapIndexed { i, c ->
            BarEntry(i.toFloat(), (spentByName[c.categoryName] ?: 0.0).toFloat())
        }
        val maxEntries = categories.mapIndexed { i, c ->
            Entry(i.toFloat(), (c.monthlyMax * goalFactor).toFloat())
        }
        // Only categories with a minimum goal get a minimum marker.
        val minEntries = categories.mapIndexedNotNull { i, c ->
            if (c.monthlyMin > 0) Entry(i.toFloat(), (c.monthlyMin * goalFactor).toFloat()) else null
        }

        val spentSet = BarDataSet(barEntries, "Spent").apply {
            color = barColor
            valueTextSize = 10f
            valueTextColor = textColor
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = CurrencyFormatter.formatWhole(value.toDouble())
            }
        }

        val scatterSets = mutableListOf<ScatterDataSet>()
        scatterSets += ScatterDataSet(maxEntries, "Max goal").apply {
            setScatterShape(ScatterChart.ScatterShape.SQUARE)
            color = 0xFFD32F2F.toInt()
            scatterShapeSize = 22f
            setDrawValues(false)
        }
        if (minEntries.isNotEmpty()) {
            scatterSets += ScatterDataSet(minEntries, "Min goal").apply {
                setScatterShape(ScatterChart.ScatterShape.CIRCLE)
                color = 0xFF388E3C.toInt()
                scatterShapeSize = 22f
                setDrawValues(false)
            }
        }

        chart.apply {
            data = CombinedData().apply {
                setData(BarData(spentSet).apply { barWidth = 0.5f })
                setData(ScatterData(scatterSets.toList()))
            }
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.textColor = textColor
            legend.isEnabled = true
            legend.textColor = textColor
            legend.isWordWrapEnabled = true
            extraBottomOffset = 30f
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(categories.map { it.categoryName })
                labelRotationAngle = -45f
                textSize = 9f
                this.textColor = textColor
                // Padding so the first and last bars and markers aren't clipped.
                axisMinimum = -0.5f
                axisMaximum = categories.size - 0.5f
            }
            animateY(600)
            invalidate()
        }
    }
    /** Progress bars for the last 30 days, independent of the chart's period selector. */
    private fun loadGoalProgress() {
        android.util.Log.d("GoalProgress", "loadGoalProgress called")
        val app = requireActivity().application as RepublicSavingsApp
        val (start, end) = periodRange(30)
        viewLifecycleOwner.lifecycleScope.launch {
            val categories = app.categoryRepository.getCategoriesForUser(CurrentUser.userID)
            val totals = app.expenseRepository.getTotalsByCategory(start, end, budgetedOnly = true)
            android.util.Log.d("GoalProgress", "categories=${categories.size}, totals=${totals.size}, binding=${_binding != null}")
            val b = _binding ?: return@launch
            drawGoalProgress(b, categories, totals)
        }
    }

    private fun drawGoalProgress(b: FragmentHomeBinding, categories: List<Category>, totals: List<CategoryTotal>) {
        android.util.Log.d("GoalProgress", "drawGoalProgress called")
        val spentByName = totals.associate { it.category to it.total }
        b.goalProgressList.removeAllViews()
        for (cat in categories) {
            val progress = GoalProgress(spentByName[cat.categoryName] ?: 0.0, cat.monthlyMin, cat.monthlyMax)
            val row = ItemGoalProgressBinding.inflate(layoutInflater, b.goalProgressList, false)

            val (color, statusText) = when (progress.status) {
                GoalStatus.OVER_MAX ->
                    0xFFD32F2F.toInt() to "Over by ${CurrencyFormatter.formatWhole(-progress.remaining)}"
                GoalStatus.UNDER_MIN ->
                    0xFFEF6C00.toInt() to "${CurrencyFormatter.formatWhole(cat.monthlyMin - progress.spent)} below minimum"
                GoalStatus.ON_TRACK ->
                    0xFF388E3C.toInt() to "${CurrencyFormatter.formatWhole(progress.remaining)} left"
            }

            row.goalName.text = cat.categoryName
            row.goalStatus.text = statusText
            row.goalStatus.setTextColor(color)
            row.goalBar.setIndicatorColor(color)
            row.goalBar.progress = (progress.fraction * 100).toInt().coerceIn(0, 100)
            val minPart = if (cat.monthlyMin > 0) " · min ${CurrencyFormatter.formatWhole(cat.monthlyMin)}" else ""
            row.goalAmounts.text =
                "${CurrencyFormatter.formatWhole(progress.spent)} of ${CurrencyFormatter.formatWhole(cat.monthlyMax)}$minPart"

            b.goalProgressList.addView(row.root)
        }
    }
    /** Start of the day (days - 1) ago until the end of today. */
    private fun periodRange(days: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val start = cal.timeInMillis
        cal.timeInMillis = todayStart
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to (cal.timeInMillis - 1)
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