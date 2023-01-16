package com.production.planful.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.production.planful.R
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.hideKeyboard
import com.production.planful.commons.extensions.setupDialogStuff
import com.production.planful.commons.extensions.showErrorToast

class ReminderWarningDialog(val activity: Activity, val callback: () -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_reminder_warning, null)

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { _, _ -> dialogConfirmed() }
            .setNeutralButton(R.string.settings, null)
            .setNegativeButton(R.string.battery_allow) { _, _ -> allowBattery() }
            .apply {
                activity.setupDialogStuff(
                    view,
                    this,
                    R.string.disclaimer,
                    cancelOnTouchOutside = false
                ) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        redirectToSettings()
                    }
                }
            }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }

    @SuppressLint("BatteryLife")
    private fun allowBattery() {
        activity.hideKeyboard()
        val pm: PowerManager = (activity.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager)
        if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    activity.startActivity(this)
                } catch (e: Exception) {
                    activity.showErrorToast(e)
                }
            }
        }
    }

    private fun redirectToSettings() {
        activity.hideKeyboard()
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            try {
                activity.startActivity(this)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }
    }
}
