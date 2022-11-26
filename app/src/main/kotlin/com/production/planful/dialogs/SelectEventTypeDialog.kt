package com.production.planful.dialogs

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.extensions.eventsHelper
import com.production.planful.models.EventType
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.views.MyCompatRadioButton
import kotlinx.android.synthetic.main.dialog_select_radio_group.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectEventTypeDialog(
    val activity: Activity, val currEventType: Long, val showCalDAVCalendars: Boolean, val showNewEventTypeOption: Boolean,
    val addLastUsedOneAsFirstOption: Boolean, val showOnlyWritable: Boolean, val callback: (eventType: EventType) -> Unit
) {
    private val NEW_EVENT_TYPE_ID = -2L
    private val LAST_USED_EVENT_TYPE_ID = -1L

    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false
    private var eventTypes = ArrayList<EventType>()

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_radio_group, null) as ViewGroup
        radioGroup = view.dialog_radio_group

        activity.eventsHelper.getEventTypes(activity, showOnlyWritable) {
            eventTypes = it
            activity.runOnUiThread {
                if (addLastUsedOneAsFirstOption) {
                    val lastUsedEventType = EventType(LAST_USED_EVENT_TYPE_ID, activity.getString(R.string.last_used_one), Color.TRANSPARENT, 0)
                    addRadioButton(lastUsedEventType)
                }
                eventTypes.filter { showCalDAVCalendars || it.caldavCalendarId == 0 }.forEach {
                    addRadioButton(it)
                }
                if (showNewEventTypeOption) {
                    val newEventType = EventType(NEW_EVENT_TYPE_ID, activity.getString(R.string.add_new_type), Color.TRANSPARENT, 0)
                    addRadioButton(newEventType)
                }
                wasInit = true
                activity.updateTextColors(view.dialog_radio_holder)
            }
        }

        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun addRadioButton(eventType: EventType) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as MyCompatRadioButton).apply {
            text = eventType.getDisplayTitle()
            isChecked = eventType.id == currEventType
            id = eventType.id!!.toInt()
        }

        if (eventType.color != Color.TRANSPARENT) {
            view.dialog_radio_color.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
        }

        view.setOnClickListener { viewClicked(eventType) }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(eventType: EventType) {
        if (!wasInit) {
            return
        }

        if (eventType.id == NEW_EVENT_TYPE_ID) {
            EditEventTypeDialog(activity) {
                callback(it)
                activity.hideKeyboard()
                dialog?.dismiss()
            }
        } else {
            callback(eventType)
            dialog?.dismiss()
        }
    }
}
