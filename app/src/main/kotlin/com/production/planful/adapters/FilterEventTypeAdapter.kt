package com.production.planful.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.production.planful.R
import com.production.planful.activities.SimpleActivity
import com.production.planful.commons.extensions.getProperBackgroundColor
import com.production.planful.commons.extensions.getProperPrimaryColor
import com.production.planful.commons.extensions.getProperTextColor
import com.production.planful.commons.extensions.setFillWithStroke
import com.production.planful.models.EventType
import kotlinx.android.synthetic.main.filter_event_type_view.view.*

class FilterEventTypeAdapter(
    val activity: SimpleActivity,
    val eventTypes: List<EventType>,
    val displayEventTypes: Set<String>
) :
    RecyclerView.Adapter<FilterEventTypeAdapter.ViewHolder>() {
    private val selectedKeys = HashSet<Long>()

    init {
        eventTypes.forEachIndexed { index, eventType ->
            if (displayEventTypes.contains(eventType.id.toString())) {
                selectedKeys.add(eventType.id!!)
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, eventType: EventType, pos: Int) {
        if (select) {
            selectedKeys.add(eventType.id!!)
        } else {
            selectedKeys.remove(eventType.id)
        }

        notifyItemChanged(pos)
    }

    fun getSelectedItemsList() =
        selectedKeys.asSequence().map { it }.toMutableList() as ArrayList<Long>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.filter_event_type_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        holder.bindView(eventType)
    }

    override fun getItemCount() = eventTypes.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(eventType: EventType): View {
            val isSelected = selectedKeys.contains(eventType.id)
            itemView.apply {
                filter_event_type_checkbox.isChecked = isSelected
                filter_event_type_checkbox.setColors(
                    activity.getProperTextColor(),
                    activity.getProperPrimaryColor(),
                    activity.getProperBackgroundColor()
                )
                filter_event_type_checkbox.text = eventType.getDisplayTitle()
                filter_event_type_color.setFillWithStroke(
                    eventType.color,
                    activity.getProperBackgroundColor()
                )
                filter_event_type_holder.setOnClickListener { viewClicked(!isSelected, eventType) }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean, eventType: EventType) {
            toggleItemSelection(select, eventType, adapterPosition)
        }
    }
}
