package com.ui.local

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.R
import com.databinding.ItemLocalCityHeaderBinding
import com.databinding.ItemLocalCityOptionBinding
import com.viewmodel.local.LocalViewModel

class LocalCityOptionAdapter(
    private val selectedTitles: MutableSet<String>,
    private val onToggle: (String, Boolean) -> Unit,
    private val hitCountProvider: (String) -> Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class PickerItem {
        data class Header(val title: String) : PickerItem()
        data class City(val option: LocalViewModel.LocalOption) : PickerItem()
    }

    private val items = mutableListOf<PickerItem>()

    fun submitList(newItems: List<LocalViewModel.LocalOption>) {
        items.clear()

        val continentOrder = listOf("Asia", "Europe", "Africa", "Americas", "Oceania", "Custom")
        val grouped = newItems
            .groupBy { it.continent }
            .toSortedMap(compareBy { continent ->
                continentOrder.indexOf(continent).takeIf { it >= 0 } ?: Int.MAX_VALUE
            })

        grouped.forEach { (continent, cities) ->
            items += PickerItem.Header(continent)
            items += cities.map { PickerItem.City(it) }
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PickerItem.Header -> VIEW_TYPE_HEADER
            is PickerItem.City -> VIEW_TYPE_CITY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemLocalCityHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemLocalCityOptionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            CityViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PickerItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is PickerItem.City -> (holder as CityViewHolder).bind(item.option)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(
        private val binding: ItemLocalCityHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.tvContinentHeader.text = title
        }
    }

    inner class CityViewHolder(
        private val binding: ItemLocalCityOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocalViewModel.LocalOption) {
            binding.tvCityName.text = item.title
            binding.tvCityMeta.text = binding.root.context.getString(
                R.string.local_city_picker_meta_with_hits,
                item.countryName,
                item.continent,
                hitCountProvider(item.title)
            )

            binding.cbCity.setOnCheckedChangeListener(null)
            binding.cbCity.isChecked = selectedTitles.contains(item.title)
            binding.cbCity.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item.title, isChecked)
            }

            binding.root.setOnClickListener {
                binding.cbCity.isChecked = !binding.cbCity.isChecked
            }
        }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_CITY = 1
    }
}
