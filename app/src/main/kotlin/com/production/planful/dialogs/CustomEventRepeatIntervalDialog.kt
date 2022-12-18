package com.production.planful.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.commons.extensions.*
import com.production.planful.helpers.DAY
import kotlinx.android.synthetic.main.dialog_custom_event_repeat_interval.view.*

class CustomEventRepeatIntervalDialog(
    val activity: Activity,
    val callback: (seconds: Int) -> Unit
) {
    private var dialog: AlertDialog? = null
    private var view = activity.layoutInflater.inflate(R.layout.dialog_custom_event_repeat_interval, null) as ViewGroup

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, i -> confirmRepeatInterval() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    view.dialog_custom_repeat_interval_value.onTextChangeListener {
                        view.dialog_custom_repeat_interval_holder.hint =
                            if (it.isEmpty()) view.resources.getString(R.string.days) else view.resources.getQuantityString(R.plurals.custom_days, it.toInt(), it.toInt())
                    }
                    dialog = alertDialog
                    alertDialog.showKeyboard(view.dialog_custom_repeat_interval_value)
                }
            }
    }

    private fun confirmRepeatInterval() {
        val value = view.dialog_custom_repeat_interval_value.value
        val days = Integer.valueOf(value.ifEmpty { "0" })
        callback(days * DAY)
        activity.hideKeyboard()
        dialog?.dismiss()
    }
}
