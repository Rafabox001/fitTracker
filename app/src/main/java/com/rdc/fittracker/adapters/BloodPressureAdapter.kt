package com.rdc.fittracker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rdc.fittracker.R
import com.rdc.fittracker.databinding.FitTrackerListItemBinding
import com.rdc.fittracker.databinding.FitTrackerListItemHighValueBinding
import com.rdc.fittracker.model.BloodPressureFitReadingData

class BloodPressureAdapter(private val bloodPressureList: List<BloodPressureFitReadingData>) :
        RecyclerView.Adapter<BloodPressureAdapter.BaseViewHolder<*>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        return when (viewType) {
            TYPE_NORMAL -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.fit_tracker_list_item, parent, false)
                BloodPressureViewHolder(view)
            }
            TYPE_HIGH -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.fit_tracker_list_item_high_value, parent, false)
                HighBloodPressureViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unsupported layout")
        }
    }

    //-----------onCreateViewHolder: bind view with data model---------
    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        val element = bloodPressureList[position]
        when (holder) {
            is BloodPressureViewHolder -> holder.bind(element)
            is HighBloodPressureViewHolder -> holder.bind(element)
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int = bloodPressureList.size

    override fun getItemViewType(position: Int) : Int {
        val element = bloodPressureList[position]
        return if (element.isHigh){
            TYPE_HIGH
        }else {
            TYPE_NORMAL
        }
    }

    class HighBloodPressureViewHolder(view: View) : BaseViewHolder<BloodPressureFitReadingData>(view) {
        private val binding = FitTrackerListItemHighValueBinding.bind(view)

        override fun bind(item: BloodPressureFitReadingData) {
            binding.systolicValue.text = item.systolic.toString()
            binding.diastolicValue.text = item.diastolic.toString()
            binding.dateValue.text = item.date
            binding.timeValue.text = item.time
        }

    }

    class BloodPressureViewHolder(view: View) : BaseViewHolder<BloodPressureFitReadingData>(view) {
        private val binding = FitTrackerListItemBinding.bind(view)

        override fun bind(item: BloodPressureFitReadingData) {
            binding.systolicValue.text = item.systolic.toString()
            binding.diastolicValue.text = item.diastolic.toString()
            binding.dateValue.text = item.date
            binding.timeValue.text = item.time
        }

    }

    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_HIGH = 1
    }

}