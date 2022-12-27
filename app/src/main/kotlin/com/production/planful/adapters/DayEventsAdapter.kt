package com.production.planful.adapters

import android.graphics.Color
import android.util.DisplayMetrics
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.production.planful.R
import com.production.planful.activities.SimpleActivity
import com.production.planful.commons.adapters.MyRecyclerViewAdapter
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.MEDIUM_ALPHA
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.commons.interfaces.RefreshRecyclerViewListener
import com.production.planful.commons.views.MyRecyclerView
import com.production.planful.dialogs.DeleteEventDialog
import com.production.planful.extensions.*
import com.production.planful.helpers.Formatter
import com.production.planful.models.ChecklistItem
import com.production.planful.models.Event
import kotlinx.android.synthetic.main.event_list_item.view.*

class DayEventsAdapter(
    activity: SimpleActivity,
    val events: ArrayList<Event>,
    recyclerView: MyRecyclerView,
    val listener: RefreshRecyclerViewListener?,
    var dayCode: String,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private var isPrintVersion = false
    private val mediumMargin = activity.resources.getDimension(R.dimen.medium_margin).toInt()
    private val gson = Gson()
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
        createViewHolder(R.layout.event_list_item, parent)

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
            var isTaskComplete = event.isTaskCompleted()
            val checklistArray: ArrayList<ChecklistItem> = ArrayList()
            var jsonArray: ArrayList<Pair<String, ArrayList<ChecklistItem>>> = ArrayList()
            if (event.checklistEnable) {
                jsonArray = gson.fromJson(event.checklist)

                var ifDayExist = false

                for (item in jsonArray) {
                    if (item.first == dayCode) {
                        ifDayExist = true
                        checklistArray.addAll(item.second)
                        jsonArray.remove(item)
                        break
                    }
                }

                if (!ifDayExist) {
                    for (item in jsonArray) {
                        val listAccountCloned = ArrayList(item.second)
                        for (arrayItem in listAccountCloned) {
                            arrayItem.checked = false
                        }
                        checklistArray.addAll(listAccountCloned)
                        break
                    }
                }

                for (item in checklistArray) {
                    if (!item.checked) {
                        isTaskComplete = false
                        break
                    }
                }
            }

            event_item_holder.isSelected = selectedKeys.contains(event.id?.toInt())
            event_item_holder.background.applyColorFilter(textColor)
            event_item_title.text = event.title
            event_item_title.checkViewStrikeThrough(isTaskComplete)
            event_item_time.text =
                if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(
                    context,
                    event.startTS
                )
            if (event.startTS != event.endTS) {
                val startDayCode = Formatter.getDayCodeFromTS(event.startTS)
                val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                val startDate = Formatter.getDayTitle(activity, startDayCode, false)
                val endDate = Formatter.getDayTitle(activity, endDayCode, false)
                val startDayString = if (startDayCode != dayCode) " ($startDate)" else ""
                if (!event.getIsAllDay()) {
                    val endTimeString = Formatter.getTimeFromTS(context, event.endTS)
                    val endDayString = if (endDayCode != dayCode) " ($endDate)" else ""
                    event_item_time.text = "${event_item_time.text}$startDayString - $endTimeString$endDayString"
                } else {
                    val endDayString = if (endDayCode != dayCode) " - ($endDate)" else ""
                    event_item_time.text = "${event_item_time.text}$startDayString$endDayString"
                }
            }

            event_item_description.text = if (replaceDescriptionWithLocation) event.location else event.description.replace("\n", " ")
            event_item_description.beVisibleIf(displayDescription && event_item_description.text.isNotEmpty())
            event_item_color_bar.background.applyColorFilter(event.color)

            var newTextColor = textColor

            val adjustAlpha = if (event.isTask()) {
                dimCompletedTasks && isTaskComplete
            } else {
                dimPastEvents && event.isPastEvent && !isPrintVersion
            }

            if (adjustAlpha) {
                newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
            }

            event_item_time.setTextColor(newTextColor)
            event_item_title.setTextColor(newTextColor)
            event_item_description?.setTextColor(newTextColor)
            event_item_task_image.applyColorFilter(newTextColor)
            event_item_time_image.applyColorFilter(newTextColor)
            event_item_task_image.beVisibleIf(event.isTask())
            toggle_mark_complete.beVisibleIf(event.isTask())
            toggle_mark_complete.isChecked = isTaskComplete
            toggle_mark_complete.setOnClickListener {
                if (event.checklistEnable) {
                    val builder = AlertDialog.Builder(context).create()
                    val view = layoutInflater.inflate(R.layout.checklist_dialog, null)
                    val tvTitle = view.findViewById<TextView>(R.id.tvOptionsTitle)
                    val tvTitleDate = view.findViewById<TextView>(R.id.tvTitleDate)
                    val rvNestedInDialog = view.findViewById<RecyclerView>(R.id.rvNestedInDialog)
                    val llClose = view.findViewById<TextView>(R.id.closeBtn)

                    if (context.baseConfig.backgroundColor.getContrastColor() == Color.WHITE) {
                        builder.window?.setBackgroundDrawable(context.getDrawable(R.drawable.rounded_corner_black))
                    } else {
                        builder.window?.setBackgroundDrawable(context.getDrawable(R.drawable.rounded_corner_white))
                    }
                    builder.setView(view)

                    tvTitle.text = event.title
                    tvTitleDate.text = event_item_time.text

                    val checklistAdapterForDialog = ChecklistAdapterForDialog(context, checklistArray, rvNestedInDialog)
                    rvNestedInDialog.adapter = checklistAdapterForDialog
                    rvNestedInDialog.layoutManager = LinearLayoutManager(context)

                    llClose.setOnClickListener {
                        builder.cancel()
                    }

                    builder.setCanceledOnTouchOutside(false)
                    builder.setOnCancelListener {
                        var i = 0
                        for (item in checklistArray) {
                            if (!item.checked) i++
                        }

                        jsonArray.add(Pair(dayCode, checklistArray))
                        ensureBackgroundThread {
                            context.updateChecklist(event, gson.convertToJsonString(jsonArray))
                            context.updateTaskCompletion(event, i == 0)
                            activity.runOnUiThread {
                                notifyDataSetChanged()
                                listener?.refreshItems()
                            }
                        }
                    }
                    builder.show()

                    val displayMetrics = DisplayMetrics()
                    context.windowManager.defaultDisplay.getMetrics(displayMetrics)

                    builder.window?.setLayout((displayMetrics.widthPixels * 0.8).toInt(), -2)
                } else setCompleted(view, event, isTaskComplete)
            }

            val startMargin = if (event.isTask()) {
                0
            } else {
                mediumMargin
            }

            (event_item_title.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin
        }
    }

    private fun setCompleted(view: View, event: Event, isTaskComplete: Boolean) {
        ensureBackgroundThread {
            view.context.updateTaskCompletion(event, completed = !isTaskComplete)
            activity.runOnUiThread {
                notifyDataSetChanged()
                listener?.refreshItems()
            }
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
