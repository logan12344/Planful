package com.production.planful.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.production.planful.R
import com.production.planful.activities.StatisticsActivity
import com.production.planful.commons.adapters.MyRecyclerViewAdapter
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.MEDIUM_ALPHA
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.commons.views.MyRecyclerView
import com.production.planful.dialogs.DeleteEventDialog
import com.production.planful.extensions.config
import com.production.planful.extensions.eventsHelper
import com.production.planful.extensions.handleEventDeleting
import com.production.planful.extensions.shareEvents
import com.production.planful.helpers.Formatter
import com.production.planful.models.Event
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
    val events: ArrayList<Event>,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private var isPrintVersion = false
    private val mediumMargin = activity.resources.getDimension(R.dimen.medium_margin).toInt()

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_day

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = events.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = events.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = events.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        createViewHolder(R.layout.event_statistics_item, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.bindView(event,
            allowSingleClick = true,
            allowLongClick = true
        ) { itemView, layoutPosition ->
            setupView(itemView, event)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = events.size

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            activity.getProperTextColor()
        }

        notifyDataSetChanged()
    }

    private fun setupView(view: View, event: Event) {
        view.apply {
            val startDayCode = Formatter.getDayCodeFromTS(event.startTS)
            val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
            event_item_holder.isSelected = selectedKeys.contains(event.id?.toInt())
            event_item_holder.background.applyColorFilter(textColor)
            event_item_title.text = event.title
            event_item_time.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(context, event.startTS)
            if (event.startTS != event.endTS) {
                val startDate = Formatter.getDayTitle(activity, startDayCode, false)
                val endDate = Formatter.getDayTitle(activity, endDayCode, false)
                val startDayString = " ($startDate)"
                if (!event.getIsAllDay()) {
                    val endTimeString = Formatter.getTimeFromTS(context, event.endTS)
                    val endDayString = " ($endDate)"
                    event_item_time.text = "${event_item_time.text}$startDayString - $endTimeString$endDayString"
                } else {
                    val endDayString = if (endDayCode != startDayCode) " - ($endDate)" else ""
                    event_item_time.text = "${event_item_time.text}$startDayString$endDayString"
                }
            }

            event_item_description.text = if (replaceDescriptionWithLocation) event.location else event.description.replace("\n", " ")
            event_item_description.beVisibleIf(displayDescription && event_item_description.text.isNotEmpty())
            event_item_color_bar.background.applyColorFilter(event.color)

            var newTextColor = textColor

            val adjustAlpha = if (event.isTask()) {
                dimCompletedTasks && event.isTaskCompleted()
            } else {
                dimPastEvents && event.isPastEvent && !isPrintVersion
            }

            if (adjustAlpha) {
                newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
            }

            event_item_time.setTextColor(newTextColor)
            event_item_title.setTextColor(newTextColor)
            event_item_days_count.setTextColor(newTextColor)
            event_item_progress.textColor = newTextColor
            event_item_description?.setTextColor(newTextColor)
            event_item_task_image.applyColorFilter(newTextColor)
            event_item_time_image.applyColorFilter(newTextColor)
            event_item_task_image.beVisibleIf(event.isTask())

            val daysCount: String = if (endDayCode.toInt().minus(startDayCode.toInt()) < 0) {
                "0/1"
            } else {
                val daysLimit = Formatter.getTodayCode().toInt().minus(startDayCode.toInt())
                val daysTodayLimit = endDayCode.toInt().minus(startDayCode.toInt())
                if (daysLimit > daysTodayLimit) "$daysTodayLimit/$daysTodayLimit" else {
                    if (daysLimit < 0) "0/$daysTodayLimit"
                    else "$daysLimit/$daysTodayLimit"
                }
            }
            event_item_days_count.text = daysCount

            val daysPercents: Int = if (endDayCode.toInt().minus(startDayCode.toInt()) < 0) {
                0
            } else {
                val daysLimit = Formatter.getTodayCode().toInt().minus(startDayCode.toInt())
                val daysTodayLimit = endDayCode.toInt().minus(startDayCode.toInt())
                if (daysLimit > daysTodayLimit) 100 else {
                    if (daysLimit < 0) 0
                    else daysLimit.times(100).div(daysTodayLimit)
                }
            }
            event_item_progress.setProgressWithAnimation(daysPercents.toFloat(), 1000)

            val startMargin = if (event.isTask()) {
                0
            } else {
                mediumMargin
            }

            (event_item_title.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin
        }
    }

    private fun shareEvents() = activity.shareEvents(selectedKeys.distinct().map { it.toLong() })

    private fun askConfirmDelete() {
        val eventIds = selectedKeys.map { it.toLong() }.toMutableList()
        val eventsToDelete = events.filter { selectedKeys.contains(it.id?.toInt()) }
        val timestamps = eventsToDelete.map { it.startTS }
        val positions = getSelectedItemPositions()

        val hasRepeatableEvent = eventsToDelete.any { it.repeatInterval > 0 }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) { it ->
            events.removeAll(eventsToDelete.toSet())

            ensureBackgroundThread {
                val nonRepeatingEventIDs =
                    eventsToDelete.asSequence().filter { it.repeatInterval == 0 }
                        .mapNotNull { it.id }.toMutableList()
                activity.eventsHelper.deleteEvents(nonRepeatingEventIDs, true)

                val repeatingEventIDs =
                    eventsToDelete.asSequence().filter { it.repeatInterval != 0 }
                        .mapNotNull { it.id }.toList()
                activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
                activity.runOnUiThread {
                    removeSelectedItems(positions)
                }
            }
        }
    }
}
