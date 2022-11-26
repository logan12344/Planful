package com.production.planful.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.DAY_SECONDS
import com.simplemobiletools.commons.helpers.MONTH_SECONDS
import com.simplemobiletools.commons.helpers.WEEK_SECONDS
import kotlinx.android.synthetic.main.dialog_custom_period_picker.view.*

class CustomPeriodPickerDialog(val activity: Activity, val callback: (value: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private var view = (activity.layoutInflater.inflate(R.layout.dialog_custom_period_picker, null) as ViewGroup)

    init {
        view.dialog_custom_period_value.setText("")
        view.dialog_radio_view.check(R.id.dialog_radio_days)
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmReminder() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(view.dialog_custom_period_value)
                }
            }
    }

    private fun calculatePeriod(selectedPeriodValue: Int, selectedPeriodValueType: Int) = when (selectedPeriodValueType) {
        R.id.dialog_radio_days -> selectedPeriodValue * DAY_SECONDS
        R.id.dialog_radio_weeks -> selectedPeriodValue * WEEK_SECONDS
        else -> selectedPeriodValue * MONTH_SECONDS
    }

    private fun confirmReminder() {
        val value = view.dialog_custom_period_value.value
        val type = view.dialog_radio_view.checkedRadioButtonId
        val periodValue = if (value.isEmpty()) {
            "0"
        } else {
            value
        }

        val period = calculatePeriod(Integer.valueOf(periodValue), type)
        callback(period)
        activity.hideKeyboard()
        dialog?.dismiss()
    }
}
