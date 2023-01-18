package com.production.planful.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.setupDialogStuff

class ReminderWarningDialog(val activity: Activity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_reminder_warning, null)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .apply {
                activity.setupDialogStuff(
                    view,
                    this,
                    R.string.disclaimer,
                    cancelOnTouchOutside = false
                ) {
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }
}
