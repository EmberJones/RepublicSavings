package com.example.republicsavingsapp.ui.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.republicsavingsapp.R
import com.example.republicsavingsapp.databinding.ItemCategoryRowBinding
import java.util.Locale

class CategoryListAdapter(private var categories: List<Category>) :
    RecyclerView.Adapter<CategoryListAdapter.ViewHolder>() {

    fun updateData(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCategoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(categories[position])
    override fun getItemCount() = categories.size

    class ViewHolder(private val itemBinding: ItemCategoryRowBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(category: Category) {
            itemBinding.rowCategoryName.text = category.categoryName
            itemBinding.rowCategoryMax.text = String.format(Locale.getDefault(), "R%,.2f monthly max", category.monthlyMax)
            itemBinding.rowCategoryIcon.setImageResource(when (category.categoryIcon) {
                "food" -> R.drawable.ic_food
                "heart" -> R.drawable.ic_heart
                "gift" -> R.drawable.ic_gift
                "paw" -> R.drawable.ic_paw
                else -> R.drawable.ic_category_placeholder
            })
        }
    }
}