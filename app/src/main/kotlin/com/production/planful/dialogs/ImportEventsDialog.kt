package com.production.planful.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.activities.SimpleActivity
import com.production.planful.extensions.config
import com.production.planful.extensions.eventTypesDB
import com.production.planful.extensions.eventsHelper
import com.production.planful.helpers.IcsImporter
import com.production.planful.helpers.IcsImporter.ImportResult.IMPORT_FAIL
import com.production.planful.helpers.IcsImporter.ImportResult.IMPORT_NOTHING_NEW
import com.production.planful.helpers.IcsImporter.ImportResult.IMPORT_OK
import com.production.planful.helpers.IcsImporter.ImportResult.IMPORT_PARTIAL
import com.production.planful.helpers.REGULAR_EVENT_TYPE_ID
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.dialog_import_events.view.*

class ImportEventsDialog(val activity: SimpleActivity, val path: String, val callback: (refreshView: Boolean) -> Unit) {
    private var currEventTypeId = REGULAR_EVENT_TYPE_ID
    private var currEventTypeCalDAVCalendarId = 0
    private val config = activity.config

    init {
        ensureBackgroundThread {
            if (activity.eventTypesDB.getEventTypeWithId(config.lastUsedLocalEventTypeId) == null) {
                config.lastUsedLocalEventTypeId = REGULAR_EVENT_TYPE_ID
            }

            val isLastCaldavCalendarOK = config.caldavSync && config.getSyncedCalendarIdsAsList().contains(config.lastUsedCaldavCalendarId)
            currEventTypeId = if (isLastCaldavCalendarOK) {
                val lastUsedCalDAVCalendar = activity.eventsHelper.getEventTypeWithCalDAVCalendarId(config.lastUsedCaldavCalendarId)
                if (lastUsedCalDAVCalendar != null) {
                    currEventTypeCalDAVCalendarId = config.lastUsedCaldavCalendarId
                    lastUsedCalDAVCalendar.id!!
                } else {
                    REGULAR_EVENT_TYPE_ID
                }
            } else {
                config.lastUsedLocalEventTypeId
            }

            activity.runOnUiThread {
                initDialog()
            }
        }
    }

    private fun initDialog() {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_import_events, null) as ViewGroup).apply {
            updateEventType(this)
            import_event_type_title.setOnClickListener {
                SelectEventTypeDialog(activity, currEventTypeId, true, true, false, true) {
                    currEventTypeId = it.id!!
                    currEventTypeCalDAVCalendarId = it.caldavCalendarId

                    config.lastUsedLocalEventTypeId = it.id!!
                    config.lastUsedCaldavCalendarId = it.caldavCalendarId

                    updateEventType(this)
                }
            }

            import_events_checkbox_holder.setOnClickListener {
                import_events_checkbox.toggle()
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.import_events) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(null)
                        activity.toast(R.string.importing)
                        ensureBackgroundThread {
                            val overrideFileEventTypes = view.import_events_checkbox.isChecked
                            val result = IcsImporter(activity).importEvents(path, currEventTypeId, currEventTypeCalDAVCalendarId, overrideFileEventTypes)
                            handleParseResult(result)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun updateEventType(view: ViewGroup) {
        ensureBackgroundThread {
            val eventType = activity.eventTypesDB.getEventTypeWithId(currEventTypeId)
            activity.runOnUiThread {
                view.import_event_type_title.setText(eventType!!.getDisplayTitle())
                view.import_event_type_color.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
            }
        }
    }

    private fun handleParseResult(result: IcsImporter.ImportResult) {
        activity.toast(
            when (result) {
                IMPORT_NOTHING_NEW -> R.string.no_new_items
                IMPORT_OK -> R.string.importing_successful
                IMPORT_PARTIAL -> R.string.importing_some_entries_failed
                else -> R.string.no_items_found
            }
        )
        callback(result != IMPORT_FAIL)
    }
}
