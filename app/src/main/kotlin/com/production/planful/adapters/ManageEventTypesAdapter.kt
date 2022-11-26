package com.production.planful.adapters

import android.view.*
import android.widget.PopupMenu
import com.production.planful.R
import com.production.planful.activities.SimpleActivity
import com.production.planful.extensions.eventsHelper
import com.production.planful.helpers.REGULAR_EVENT_TYPE_ID
import com.production.planful.interfaces.DeleteEventTypesListener
import com.production.planful.models.EventType
import com.production.planful.commons.adapters.MyRecyclerViewAdapter
import com.production.planful.commons.dialogs.ConfirmationDialog
import com.production.planful.commons.dialogs.RadioGroupDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.models.RadioItem
import com.production.planful.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.item_event_type.view.*

class ManageEventTypesAdapter(
    activity: SimpleActivity, val eventTypes: ArrayList<EventType>, val listener: DeleteEventTypesListener?, recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_event_type

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editEventType()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = eventTypes.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = eventTypes.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = eventTypes.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_event_type, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        holder.bindView(eventType, true, true) { itemView, layoutPosition ->
            setupView(itemView, eventType)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = eventTypes.size

    private fun getItemWithKey(key: Int): EventType? = eventTypes.firstOrNull { it.id?.toInt() == key }

    private fun getSelectedItems() = eventTypes.filter { selectedKeys.contains(it.id?.toInt()) } as ArrayList<EventType>

    private fun setupView(view: View, eventType: EventType) {
        view.apply {
            event_item_frame.isSelected = selectedKeys.contains(eventType.id?.toInt())
            event_type_title.text = eventType.getDisplayTitle()
            event_type_color.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
            event_type_title.setTextColor(textColor)

            overflow_menu_icon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflow_menu_icon.setOnClickListener {
                showPopupMenu(overflow_menu_anchor, eventType)
            }
        }
    }

    private fun showPopupMenu(view: View, eventType: EventType) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val eventTypeId = eventType.id!!.toInt()
                when (item.itemId) {
                    R.id.cab_edit -> {
                        executeItemMenuOperation(eventTypeId) {
                            itemClick(eventType)
                        }
                    }
                    R.id.cab_delete -> {
                        executeItemMenuOperation(eventTypeId) {
                            askConfirmDelete()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(eventTypeId: Int, callback: () -> Unit) {
        selectedKeys.clear()
        selectedKeys.add(eventTypeId)
        callback()
    }

    private fun editEventType() {
        itemClick.invoke(getSelectedItems().first())
        finishActMode()
    }

    private fun askConfirmDelete() {
        val eventTypes = eventTypes.filter { selectedKeys.contains(it.id?.toInt()) }.map { it.id } as ArrayList<Long>

        activity.eventsHelper.doEventTypesContainEvents(eventTypes) {
            activity.runOnUiThread {
                if (it) {
                    val MOVE_EVENTS = 0
                    val DELETE_EVENTS = 1
                    val res = activity.resources
                    val items = ArrayList<RadioItem>().apply {
                        add(RadioItem(MOVE_EVENTS, res.getString(R.string.move_events_into_default)))
                        add(RadioItem(DELETE_EVENTS, res.getString(R.string.remove_affected_events)))
                    }
                    RadioGroupDialog(activity, items) {
                        deleteEventTypes(it == DELETE_EVENTS)
                    }
                } else {
                    ConfirmationDialog(activity) {
                        deleteEventTypes(true)
                    }
                }
            }
        }
    }

    private fun deleteEventTypes(deleteEvents: Boolean) {
        val eventTypesToDelete = getSelectedItems()

        for (key in selectedKeys) {
            val type = getItemWithKey(key) ?: continue
            if (type.id == REGULAR_EVENT_TYPE_ID) {
                activity.toast(R.string.cannot_delete_default_type)
                eventTypesToDelete.remove(type)
                toggleItemSelection(false, getItemKeyPosition(type.id!!.toInt()))
                break
            }
        }

        if (listener?.deleteEventTypes(eventTypesToDelete, deleteEvents) == true) {
            val positions = getSelectedItemPositions()
            eventTypes.removeAll(eventTypesToDelete)
            removeSelectedItems(positions)
        }
    }
}
