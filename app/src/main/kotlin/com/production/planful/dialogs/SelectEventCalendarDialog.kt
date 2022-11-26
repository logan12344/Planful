package com.production.planful.dialogs

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.extensions.eventsHelper
import com.production.planful.helpers.STORED_LOCALLY_ONLY
import com.production.planful.models.CalDAVCalendar
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.dialog_select_radio_group.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectEventCalendarDialog(val activity: Activity, val calendars: List<CalDAVCalendar>, val currCalendarId: Int, val callback: (id: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_radio_group, null) as ViewGroup
        radioGroup = view.dialog_radio_group

        ensureBackgroundThread {
            calendars.forEach {
                val localEventType = activity.eventsHelper.getEventTypeWithCalDAVCalendarId(it.id)
                if (localEventType != null) {
                    it.color = localEventType.color
                }
            }

            activity.runOnUiThread {
                calendars.forEach {
                    addRadioButton(it.getFullTitle(), it.id, it.color)
                }
                addRadioButton(activity.getString(R.string.store_locally_only), STORED_LOCALLY_ONLY, Color.TRANSPARENT)
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

    private fun addRadioButton(title: String, typeId: Int, color: Int) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as RadioButton).apply {
            text = title
            isChecked = typeId == currCalendarId
            id = typeId
        }

        if (typeId != STORED_LOCALLY_ONLY) {
            view.dialog_radio_color.setFillWithStroke(color, activity.getProperBackgroundColor())
        }

        view.setOnClickListener { viewClicked(typeId) }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(typeId: Int) {
        if (wasInit) {
            callback(typeId)
            dialog?.dismiss()
        }
    }
}
