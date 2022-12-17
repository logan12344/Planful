package com.production.planful.adapters

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
import com.production.planful.helpers.*
import com.production.planful.models.*
import kotlinx.android.synthetic.main.event_list_item.view.*
import kotlinx.android.synthetic.main.event_list_section_day.view.*

class EventListAdapter(
    activity: SimpleActivity,
    var listItems: ArrayList<ListItem>,
    val allowLongClick: Boolean,
    val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescription = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val dimCompletedTasks = activity.config.dimCompletedTasks
    private val now = getNowSeconds()
    private var use24HourFormat = activity.config.use24HourFormat
    private var currentItemsHash = listItems.hashCode()
    private var isPrintVersion = false
    private val mediumMargin = activity.resources.getDimension(R.dimen.medium_margin).toInt()
    private val gson = Gson()

    init {
        setupDragListener(true)
        val firstNonPastSectionIndex = listItems.indexOfFirst { it is ListSectionDay && !it.isPastSection }
        if (firstNonPastSectionIndex != -1) {
            activity.runOnUiThread {
                recyclerView.scrollToPosition(firstNonPastSectionIndex)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_event_list

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = listItems.filter { it is ListEvent }.size

    override fun getIsItemSelectable(position: Int) = listItems[position] is ListEvent

    override fun getItemSelectionKey(position: Int) =
        (listItems.getOrNull(position) as? ListEvent)?.hashCode()

    override fun getItemKeyPosition(key: Int) =
        listItems.indexOfFirst { (it as? ListEvent)?.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyRecyclerViewAdapter.ViewHolder {
        val layoutId = when (viewType) {
            ITEM_SECTION_DAY -> R.layout.event_list_section_day
            ITEM_SECTION_MONTH -> R.layout.event_list_section_month
            else -> R.layout.event_list_item
        }
        return createViewHolder(layoutId, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val listItem = listItems[position]
        holder.bindView(
            listItem,
            true,
            allowLongClick && listItem is ListEvent
        ) { itemView, layoutPosition ->
            when (listItem) {
                is ListSectionDay -> setupListSectionDay(itemView, listItem)
                is ListEvent -> setupListEvent(itemView, listItem)
                is ListSectionMonth -> setupListSectionMonth(itemView, listItem)
            }
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = listItems.size

    override fun getItemViewType(position: Int) = when {
        listItems[position] is ListEvent -> ITEM_EVENT
        listItems[position] is ListSectionDay -> ITEM_SECTION_DAY
        else -> ITEM_SECTION_MONTH
    }

    fun updateListItems(newListItems: ArrayList<ListItem>) {
        if (newListItems.hashCode() != currentItemsHash) {
            currentItemsHash = newListItems.hashCode()
            listItems = newListItems.clone() as ArrayList<ListItem>
            recyclerView.resetItemCount()
            notifyDataSetChanged()
            finishActMode()
        }
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            activity.getProperTextColor()
        }
        notifyDataSetChanged()
    }

    private fun setupListEvent(view: View, listEvent: ListEvent) {
        view.apply {
            event_item_holder.isSelected = selectedKeys.contains(listEvent.hashCode())
            event_item_holder.background.applyColorFilter(textColor)
            event_item_title.text = listEvent.title
            event_item_title.checkViewStrikeThrough(listEvent.isTaskCompleted)
            event_item_time.text =
                if (listEvent.isAllDay) allDayString else Formatter.getTimeFromTS(
                    context,
                    listEvent.startTS
                )
            if (listEvent.startTS != listEvent.endTS) {
                if (!listEvent.isAllDay) {
                    event_item_time.text = "${event_item_time.text} - ${
                        Formatter.getTimeFromTS(
                            context,
                            listEvent.endTS
                        )
                    }"
                }

                val startCode = Formatter.getDayCodeFromTS(listEvent.startTS)
                val endCode = Formatter.getDayCodeFromTS(listEvent.endTS)
                if (startCode != endCode) {
                    event_item_time.text = "${event_item_time.text} (${Formatter.getDateDayTitle(endCode)})"
                }
            }

            event_item_description.text =
                if (replaceDescription) listEvent.location else listEvent.description.replace(
                    "\n",
                    " "
                )
            event_item_description.beVisibleIf(displayDescription && event_item_description.text.isNotEmpty())
            event_item_color_bar.background.applyColorFilter(listEvent.color)

            var newTextColor = textColor
            if (listEvent.isAllDay || listEvent.startTS <= now && listEvent.endTS <= now) {
                if (listEvent.isAllDay && Formatter.getDayCodeFromTS(listEvent.startTS) == Formatter.getDayCodeFromTS(
                        now
                    ) && !isPrintVersion
                ) {
                    newTextColor = properPrimaryColor
                }

                val adjustAlpha = if (listEvent.isTask) {
                    dimCompletedTasks && listEvent.isTaskCompleted
                } else {
                    dimPastEvents && listEvent.isPastEvent && !isPrintVersion
                }
                if (adjustAlpha) {
                    newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
                }
            } else if (listEvent.startTS <= now && listEvent.endTS >= now && !isPrintVersion) {
                newTextColor = properPrimaryColor
            }

            event_item_time.setTextColor(newTextColor)
            event_item_title.setTextColor(newTextColor)
            event_item_description.setTextColor(newTextColor)
            event_item_task_image.applyColorFilter(newTextColor)
            event_item_task_image.beVisibleIf(listEvent.isTask)
            event_item_time_image.applyColorFilter(newTextColor)
            toggle_mark_complete.beVisibleIf(listEvent.isTask)
            toggle_mark_complete.isChecked = listEvent.isTaskCompleted
            toggle_mark_complete.setOnClickListener{
                /*if (listEvent.isCheckListEnable) {
                    val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog).create()
                    val view = layoutInflater.inflate(R.layout.checklist_dialog, null)
                    val tvTitle = view.findViewById<TextView>(R.id.tvOptionsTitle)
                    val tvTitleDate = view.findViewById<TextView>(R.id.tvTitleDate)
                    val rvNestedInDialog = view.findViewById<RecyclerView>(R.id.rvNestedInDialog)
                    val llClose = view.findViewById<TextView>(R.id.closeBtn)
                    builder.setView(view)

                    tvTitle.text = listEvent.title
                    tvTitleDate.text = event_item_time.text

                    val checklistArray: ArrayList<ChecklistItem> = ArrayList()
                    var jsonArray: ArrayList<Pair<String, ArrayList<ChecklistItem>>> = ArrayList()
                    ensureBackgroundThread {
                        jsonArray = gson.fromJson(listEvent.getChecklist)

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

                        activity.runOnUiThread {
                            val checklistAdapterForDialog = ChecklistAdapterForDialog(checklistArray, rvNestedInDialog)
                            rvNestedInDialog.adapter = checklistAdapterForDialog
                            rvNestedInDialog.layoutManager = LinearLayoutManager(context)
                        }
                    }

                    llClose.setOnClickListener {
                        builder.cancel()
                    }

                    builder.setCanceledOnTouchOutside(false)
                    builder.setOnCancelListener {
                        var i = 0;
                        for (item in checklistArray) {
                            if (!item.checked) i++
                        }

                        jsonArray.add(Pair(dayCode, checklistArray))
                        val jsonString = gson.convertToJsonString(jsonArray)
                        ensureBackgroundThread {
                            val originalEvent = view.context.eventsDB.getEventOrTaskWithId(listEvent.id)
                            context.updateTaskCompletion(originalEvent!!, i == 0)
                            context.updateChecklist(originalEvent!!, jsonString)
                            activity.runOnUiThread {
                                listener?.refreshItems()
                            }
                        }
                    }
                    builder.show()

                    val displayMetrics = DisplayMetrics()
                    context.windowManager.defaultDisplay.getMetrics(displayMetrics)

                    builder.window?.setLayout((displayMetrics.widthPixels * 0.8).toInt(), -2)
                } else*/ setCompleted(view, listEvent)
            }

            val startMargin = if (listEvent.isTask) {
                0
            } else {
                mediumMargin
            }
            (event_item_title.layoutParams as ConstraintLayout.LayoutParams).marginStart =
                startMargin
        }
    }

    private fun setCompleted(view: View, event: ListEvent) {
        ensureBackgroundThread {
            val originalEvent = view.context.eventsDB.getEventOrTaskWithId(event.id)
            view.context.updateTaskCompletion(originalEvent!!, completed = !event.isTaskCompleted)
            activity.runOnUiThread {
                listener?.refreshItems()
            }
        }
    }

    private fun setupListSectionDay(view: View, listSectionDay: ListSectionDay) {
        view.event_section_title.apply {
            text = listSectionDay.title
            val dayColor = if (listSectionDay.isToday) properPrimaryColor else textColor
            setTextColor(dayColor)
        }
    }

    private fun setupListSectionMonth(view: View, listSectionMonth: ListSectionMonth) {
        view.event_section_title.apply {
            text = listSectionMonth.title
            setTextColor(properPrimaryColor)
        }
    }

    private fun shareEvents() = activity.shareEvents(getSelectedEventIds())

    private fun getSelectedEventIds() =
        listItems.filter { it is ListEvent && selectedKeys.contains(it.hashCode()) }
            .map { (it as ListEvent).id }.toMutableList() as ArrayList<Long>

    private fun askConfirmDelete() {
        val eventIds = getSelectedEventIds()
        val eventsToDelete =
            listItems.filter { selectedKeys.contains((it as? ListEvent)?.hashCode()) } as List<ListEvent>
        val timestamps = eventsToDelete.mapNotNull { (it as? ListEvent)?.startTS }

        val hasRepeatableEvent = eventsToDelete.any { it.isRepeatable }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) {
            listItems.removeAll(eventsToDelete)

            ensureBackgroundThread {
                val nonRepeatingEventIDs =
                    eventsToDelete.filter { !it.isRepeatable }.mapNotNull { it.id }.toMutableList()
                activity.eventsHelper.deleteEvents(nonRepeatingEventIDs, true)

                val repeatingEventIDs = eventsToDelete.filter { it.isRepeatable }.map { it.id }
                activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }
    }
}
