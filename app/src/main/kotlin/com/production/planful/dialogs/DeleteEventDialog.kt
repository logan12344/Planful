package com.production.planful.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.commons.extensions.beVisibleIf
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.setupDialogStuff
import com.production.planful.helpers.DELETE_ALL_OCCURRENCES
import com.production.planful.helpers.DELETE_FUTURE_OCCURRENCES
import com.production.planful.helpers.DELETE_SELECTED_OCCURRENCE
import kotlinx.android.synthetic.main.dialog_delete_event.view.*

class DeleteEventDialog(
    val activity: Activity,
    eventIds: List<Long>,
    hasRepeatableEvent: Boolean,
    isTask: Boolean = false,
    val callback: (deleteRule: Int) -> Unit
) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_delete_event, null).apply {
            delete_event_repeat_description.beVisibleIf(hasRepeatableEvent)
            delete_event_radio_view.beVisibleIf(hasRepeatableEvent)
            if (!hasRepeatableEvent) {
                delete_event_radio_view.check(R.id.delete_event_all)
            }

            if (eventIds.size > 1) {
                delete_event_repeat_description.setText(R.string.selection_contains_repetition)
            }

            if (isTask) {
                delete_event_repeat_description.setText(R.string.task_is_repeatable)
            } else {
                delete_event_repeat_description.setText(R.string.event_is_repeatable)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.yes) { dialog, which -> dialogConfirmed(view as ViewGroup) }
            .setNegativeButton(R.string.no, null)
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun dialogConfirmed(view: ViewGroup) {
        val deleteRule = when (view.delete_event_radio_view.checkedRadioButtonId) {
            R.id.delete_event_all -> DELETE_ALL_OCCURRENCES
            R.id.delete_event_future -> DELETE_FUTURE_OCCURRENCES
            else -> DELETE_SELECTED_OCCURRENCE
        }
        dialog?.dismiss()
        callback(deleteRule)
    }
}
