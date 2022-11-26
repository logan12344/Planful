package com.production.planful.dialogs

import android.app.Activity
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.extensions.calDAVHelper
import com.production.planful.models.EventType
import com.production.planful.commons.dialogs.ColorPickerDialog
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.getProperBackgroundColor
import com.production.planful.commons.extensions.setFillWithStroke
import com.production.planful.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_select_event_type_color.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectEventTypeColorDialog(val activity: Activity, val eventType: EventType, val callback: (color: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false
    private val colors = activity.calDAVHelper.getAvailableCalDAVCalendarColors(eventType)

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_event_type_color, null) as ViewGroup
        radioGroup = view.dialog_select_event_type_color_radio
        view.dialog_select_event_type_other_value.setOnClickListener {
            showCustomColorPicker()
        }

        colors.forEachIndexed { index, value ->
            addRadioButton(index, value)
        }

        wasInit = true
        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                }

                if (colors.isEmpty()) {
                    showCustomColorPicker()
                }
            }
    }

    private fun addRadioButton(colorKey: Int, color: Int) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as RadioButton).apply {
            text = if (color == 0) activity.getString(R.string.transparent) else String.format("#%06X", 0xFFFFFF and color)
            isChecked = color == eventType.color
            id = colorKey
        }

        view.dialog_radio_color.setFillWithStroke(color, activity.getProperBackgroundColor())
        view.setOnClickListener {
            viewClicked(colorKey)
        }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(colorKey: Int) {
        if (!wasInit)
            return

        callback(colors[colorKey])
        dialog?.dismiss()
    }

    private fun showCustomColorPicker() {
        ColorPickerDialog(activity, eventType.color) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                callback(color)
            }
            dialog?.dismiss()
        }
    }
}
