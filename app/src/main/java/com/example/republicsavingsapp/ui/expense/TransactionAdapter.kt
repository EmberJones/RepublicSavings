package com.example.republicsavingsapp.ui.expense

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.republicsavingsapp.CurrencyFormatter
import com.example.republicsavingsapp.Expenses
import com.example.republicsavingsapp.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(private var items: List<Expenses>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    fun updateData(newItems: List<Expenses>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expenses) {
            binding.transactionName.text = expense.expenseName
            val dateStr = SimpleDateFormat("d MMM", Locale.getDefault()).format(expense.expenseDate)
            binding.transactionCategoryDate.text = "${expense.expenseCategory} · $dateStr"
            val amount = expense.expenseAmount.toDoubleOrNull() ?: 0.0
            binding.transactionAmount.text = "-${CurrencyFormatter.format(amount)}"
        }
    }
}