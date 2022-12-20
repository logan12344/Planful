package com.production.planful.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.production.planful.R
import com.production.planful.activities.StatisticsActivity
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.MEDIUM_ALPHA
import com.production.planful.commons.views.MyRecyclerView
import com.production.planful.extensions.config
import com.production.planful.helpers.Formatter
import com.production.planful.models.Event
import com.production.planful.models.TrackTargetItem
import kotlinx.android.synthetic.main.event_list_item.view.event_item_color_bar
import kotlinx.android.synthetic.main.event_list_item.view.event_item_description
import kotlinx.android.synthetic.main.event_list_item.view.event_item_holder
import kotlinx.android.synthetic.main.event_list_item.view.event_item_task_image
import kotlinx.android.synthetic.main.event_list_item.view.event_item_time
import kotlinx.android.synthetic.main.event_list_item.view.event_item_time_image
import kotlinx.android.synthetic.main.event_list_item.view.event_item_title
import kotlinx.android.synthetic.main.event_statistics_item.view.*

class StatisticsEventsAdapter(
    activity: StatisticsActivity,
    val events: ArrayList<Pair<TrackTargetItem, Event>>
) : RecyclerView.Adapter<StatisticsEventsAdapter.StatisticsViewHolder>() {

    private val allDayString = activity.resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val mediumMargin = activity.resources.getDimension(R.dimen.medium_margin).toInt()

    inner class StatisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.event_statistics_item, parent, false)
        return StatisticsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        val event = events[position]
        setupView(holder.itemView, event)
    }

    override fun getItemCount() = events.size

    private fun setupView(view: View, event: Pair<TrackTargetItem, Event>) {
        val textColor = view.context.getProperTextColor()
        view.apply {
            val startDayCode = Formatter.getDayCodeFromTS(event.second.startTS)
            val endDayCode = Formatter.getDayCodeFromTS(event.second.endTS)
            event_item_holder.background.applyColorFilter(textColor)
            event_item_title.text = event.second.title
            event_item_time.text = if (event.second.getIsAllDay()) allDayString else Formatter.getTimeFromTS(context, event.second.startTS)
            if (event.second.startTS != event.second.endTS) {
                val startDate = Formatter.getDayTitle(context, startDayCode, false)
                val endDate = Formatter.getDayTitle(context, endDayCode, false)
                val startDayString = " ($startDate)"
                if (!event.second.getIsAllDay()) {
                    val endTimeString = Formatter.getTimeFromTS(context, event.second.endTS)
                    val endDayString = " ($endDate)"
                    event_item_time.text = "${event_item_time.text}$startDayString - $endTimeString$endDayString"
                } else {
                    val endDayString = if (endDayCode != startDayCode) " - ($endDate)" else ""
                    event_item_time.text = "${event_item_time.text}$startDayString$endDayString"
                }
            }

            event_item_description.text = if (replaceDescriptionWithLocation) event.second.location else event.second.description.replace("\n", " ")
            event_item_description.beVisibleIf(displayDescription && event_item_description.text.isNotEmpty())
            event_item_color_bar.background.applyColorFilter(event.second.color)

            event_item_time.setTextColor(textColor)
            event_item_title.setTextColor(textColor)
            event_item_days_count.setTextColor(textColor)
            event_item_progress.textColor = textColor
            event_item_description?.setTextColor(textColor)
            event_item_task_image.applyColorFilter(textColor)
            event_item_time_image.applyColorFilter(textColor)
            event_item_task_image.beVisibleIf(event.second.isTask())

            val daysCount = event.first.daysCount
            val daysDone = event.first.daysDone
            val daysLimit = "$daysDone/$daysCount"
            event_item_days_count.text = daysLimit

            val daysTodayLimitProgress = if (daysDone == 0) 0 else daysDone.times(100).div(daysCount)
            event_item_progress.setProgressWithAnimation(daysTodayLimitProgress.toFloat(), 1000)

            val startMargin = if (event.second.isTask()) {
                0
            } else {
                mediumMargin
            }

            (event_item_title.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin
        }
    }
}
