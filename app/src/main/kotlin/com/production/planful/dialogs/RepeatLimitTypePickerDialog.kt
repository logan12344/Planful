package com.production.planful.dialogs

import android.app.Activity
import android.app.DatePickerDialog
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.getDatePickerDialogTheme
import com.production.planful.commons.extensions.setupDialogStuff
import com.production.planful.extensions.config
import com.production.planful.extensions.seconds
import com.production.planful.helpers.Formatter
import com.production.planful.helpers.getNowSeconds
import kotlinx.android.synthetic.main.dialog_repeat_limit_type_picker.view.*
import org.joda.time.DateTime
import java.util.*

class RepeatLimitTypePickerDialog(
    val activity: Activity,
    var repeatLimit: Long,
    val startTS: Long,
    val callback: (repeatLimit: Long) -> Unit
) {
    private var dialog: AlertDialog? = null
    private var view: View

    init {
        view =
            activity.layoutInflater.inflate(R.layout.dialog_repeat_limit_type_picker, null).apply {
                repeat_type_date.setOnClickListener { showRepetitionLimitDialog() }
                repeat_type_forever.setOnClickListener {
                    callback(0)
                    dialog?.dismiss()
                }
            }

        view.dialog_radio_view.check(getCheckedItem())

        if (repeatLimit in 1..startTS) {
            repeatLimit = startTS
        }

        updateRepeatLimitText()

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmRepetition() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                    activity.currentFocus?.clearFocus()
                }
            }
    }

    private fun getCheckedItem() = when {
        repeatLimit > 0 -> R.id.repeat_type_till_date
        else -> R.id.repeat_type_forever
    }

    private fun updateRepeatLimitText() {
        if (repeatLimit <= 0) {
            repeatLimit = getNowSeconds()
        }

        val repeatLimitDateTime = Formatter.getDateTimeFromTS(repeatLimit)
        view.repeat_type_date.setText(Formatter.getFullDate(activity, repeatLimitDateTime))
    }

    private fun confirmRepetition() {
        when (view.dialog_radio_view.checkedRadioButtonId) {
            R.id.repeat_type_till_date -> callback(repeatLimit)
            R.id.repeat_type_forever -> callback(0)
        }
        dialog?.dismiss()
    }

    private fun showRepetitionLimitDialog() {
        val repeatLimitDateTime = Formatter.getDateTimeFromTS(if (repeatLimit != 0L) repeatLimit else getNowSeconds())
        val datepicker = DatePickerDialog(
            activity,
            activity.getDatePickerDialogTheme(),
            repetitionLimitDateSetListener,
            repeatLimitDateTime.year,
            repeatLimitDateTime.monthOfYear - 1,
            repeatLimitDateTime.dayOfMonth
        )

        datepicker.datePicker.firstDayOfWeek =
            if (activity.config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datepicker.show()
    }

    private val repetitionLimitDateSetListener =
        DatePickerDialog.OnDateSetListener { v, year, monthOfYear, dayOfMonth ->
            val repeatLimitDateTime = DateTime().withDate(year, monthOfYear + 1, dayOfMonth).withTime(23, 59, 59, 0)
            repeatLimit = if (repeatLimitDateTime.seconds() < startTS) {
                0
            } else {
                repeatLimitDateTime.seconds()
            }

            updateRepeatLimitText()
            view.dialog_radio_view.check(R.id.repeat_type_till_date)
        }
}
