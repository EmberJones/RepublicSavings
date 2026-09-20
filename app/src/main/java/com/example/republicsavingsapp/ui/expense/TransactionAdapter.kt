package com.example.republicsavingsapp.ui.expense

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.republicsavingsapp.CurrencyFormatter
import com.example.republicsavingsapp.Expenses
import com.example.republicsavingsapp.R
import com.example.republicsavingsapp.databinding.ItemTransactionBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private var items: List<Expenses>,
    private val onReceiptClick: (String) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    fun updateData(newItems: List<Expenses>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            onReceiptClick
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    class ViewHolder(
        private val binding: ItemTransactionBinding,
        private val onReceiptClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expenses) {
            binding.transactionName.text = expense.expenseName

            val dateStr = SimpleDateFormat("d MMM", Locale.getDefault())
                .format(expense.expenseDate)
            binding.transactionCategoryDate.text =
                "${expense.expenseCategory} · $dateStr"

            val amount = expense.expenseAmount.toDoubleOrNull() ?: 0.0
            binding.transactionAmount.text = "-${CurrencyFormatter.format(amount)}"

            bindReceipt(expense.photoFilePath)
        }

        private fun bindReceipt(path: String?) {
            if (path.isNullOrBlank()) {
                binding.receiptThumbnail.visibility = View.GONE
                binding.receiptThumbnail.setOnClickListener(null)
                return
            }

            binding.receiptThumbnail.visibility = View.VISIBLE
            binding.receiptThumbnail.load(File(path)) {
                crossfade(true)
                placeholder(R.drawable.ic_receipt_placeholder)
                error(R.drawable.ic_receipt_placeholder)
            }

            binding.receiptThumbnail.setOnClickListener { onReceiptClick(path) }
        }
    }
}