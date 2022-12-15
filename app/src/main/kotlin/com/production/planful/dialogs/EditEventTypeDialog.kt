package com.production.planful.dialogs

import android.app.Activity
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.commons.dialogs.ColorPickerDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.extensions.eventsHelper
import com.production.planful.helpers.OTHER_EVENT
import com.production.planful.models.EventType


class EditEventTypeDialog(
    val activity: Activity,
    var eventType: EventType? = null,
    val callback: (eventType: EventType) -> Unit
) {
    init {
        if (eventType == null) {
            eventType = EventType(null, "", activity.getProperPrimaryColor())
        }

        if (eventType?.caldavCalendarId == 0) {
            ColorPickerDialog(activity, eventType!!.color) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    eventType!!.color = color
                    ensureBackgroundThread {
                        eventTypeConfirmed()
                    }
                }
            }
        } else {
            SelectEventTypeColorDialog(activity, eventType!!) {
                eventType!!.color = it
                ensureBackgroundThread {
                    eventTypeConfirmed()
                }
            }
        }
    }

    private fun eventTypeConfirmed() {
        eventType!!.title = eventType!!.id?.plus(eventType!!.color.toLong()).toString()
        if (eventType!!.caldavCalendarId != 0) {
            eventType!!.caldavDisplayName = eventType!!.id?.plus(eventType!!.color.toLong()).toString()
        }

        eventType!!.id = activity.eventsHelper.insertOrUpdateEventTypeSync(eventType!!)

        if (eventType!!.id != -1L) {
            activity.runOnUiThread {
                callback(eventType!!)
            }
        } else {
            activity.toast(R.string.editing_calendar_failed)
        }
    }
}