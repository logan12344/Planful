package com.production.planful.dialogs

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.commons.dialogs.ColorPickerDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.commons.views.MyCompatRadioButton
import com.production.planful.extensions.eventsHelper
import com.production.planful.models.EventType
import kotlinx.android.synthetic.main.dialog_select_radio_group.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectEventTypeDialog(
    val activity: Activity,
    var eventType: EventType? = null,
    val callback: (eventType: EventType) -> Unit
) {

    init {
        activity.runOnUiThread {
            if (eventType == null) {
                eventType = EventType(null, activity.getString(R.string.regular_event), activity.getProperPrimaryColor())
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
