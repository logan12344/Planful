package com.production.planful.activities

import android.os.Bundle
import com.production.planful.R
import com.production.planful.adapters.ManageEventTypesAdapter
import com.production.planful.commons.extensions.toast
import com.production.planful.commons.extensions.updateTextColors
import com.production.planful.commons.helpers.NavigationIcon
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.dialogs.SelectEventTypeDialog
import com.production.planful.extensions.eventsHelper
import com.production.planful.interfaces.DeleteEventTypesListener
import com.production.planful.models.EventType
import kotlinx.android.synthetic.main.activity_manage_event_types.*

class ManageEventTypesActivity : SimpleActivity(), DeleteEventTypesListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event_types)
        setupOptionsMenu()

        getEventTypes()
        updateTextColors(manage_event_types_list)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(manage_event_types_toolbar, NavigationIcon.Arrow)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        SelectEventTypeDialog(this, eventType?.copy()) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        eventsHelper.getEventTypes(this, false) {
            val adapter = ManageEventTypesAdapter(this, it, this, manage_event_types_list) {
                showEventTypeDialog(it as EventType)
            }
            manage_event_types_list.adapter = adapter
        }
    }

    private fun setupOptionsMenu() {
        manage_event_types_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_event_type -> showEventTypeDialog()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun deleteEventTypes(
        eventTypes: ArrayList<EventType>,
        deleteEvents: Boolean
    ): Boolean {
        if (eventTypes.any { it.caldavCalendarId != 0 }) {
            toast(R.string.unsync_caldav_calendar)
            if (eventTypes.size == 1) {
                return false
            }
        }

        ensureBackgroundThread {
            eventsHelper.deleteEventTypes(eventTypes, deleteEvents)
        }
        return true
    }
}
