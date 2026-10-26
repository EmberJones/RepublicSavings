package com.example.republicsavingsapp.ui.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.republicsavingsapp.CurrencyFormatter
import com.example.republicsavingsapp.R
import com.example.republicsavingsapp.databinding.ItemCategoryRowBinding

class CategoryListAdapter(
    private var categories: List<Category>,
    private val onItemClick: (Category) -> Unit = {}
) : RecyclerView.Adapter<CategoryListAdapter.ViewHolder>() {

    fun updateData(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCategoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemClick)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(categories[position])
    override fun getItemCount() = categories.size

    class ViewHolder(
        private val binding: ItemCategoryRowBinding,
        private val onItemClick: (Category) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.rowCategoryName.text = category.categoryName
            val minPart = if (category.monthlyMin > 0) " · ${CurrencyFormatter.format(category.monthlyMin)} min" else ""
            binding.rowCategoryMax.text = "${CurrencyFormatter.format(category.monthlyMax)} monthly max$minPart"
            binding.rowCategoryIcon.setImageResource(when (category.categoryIcon) {
                "food" -> R.drawable.ic_food
                "heart" -> R.drawable.ic_heart
                "gift" -> R.drawable.ic_gift
                "paw" -> R.drawable.ic_paw
                else -> R.drawable.ic_category_placeholder
            })
            binding.root.setOnClickListener { onItemClick(category) }
        }
    }
}