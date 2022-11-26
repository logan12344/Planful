package com.production.planful.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.production.planful.R
import com.production.planful.activities.SimpleActivity
import com.production.planful.commons.extensions.getProperTextColor
import com.production.planful.models.MyTimeZone
import kotlinx.android.synthetic.main.item_select_time_zone.view.*

class SelectTimeZoneAdapter(
    val activity: SimpleActivity,
    var timeZones: ArrayList<MyTimeZone>,
    val itemClick: (Any) -> Unit
) :
    RecyclerView.Adapter<SelectTimeZoneAdapter.ViewHolder>() {
    val textColor = activity.getProperTextColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.item_select_time_zone, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeZone = timeZones[position]
        holder.bindView(timeZone)
    }

    override fun getItemCount() = timeZones.size

    fun updateTimeZones(newTimeZones: ArrayList<MyTimeZone>) {
        timeZones = newTimeZones.clone() as ArrayList<MyTimeZone>
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(timeZone: MyTimeZone): View {
            itemView.apply {
                item_time_zone_title.text = timeZone.zoneName
                item_time_zone_shift.text = timeZone.title

                item_time_zone_title.setTextColor(textColor)
                item_time_zone_shift.setTextColor(textColor)

                item_select_time_zone_holder.setOnClickListener {
                    itemClick(timeZone)
                }
            }

            return itemView
        }
    }
}
