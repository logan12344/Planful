package com.production.planful.commons.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.launchUpgradeToProIntent
import com.production.planful.commons.extensions.launchViewIntent
import com.production.planful.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_upgrade_to_pro.view.*

class UpgradeToProDialog(val activity: Activity) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_upgrade_to_pro, null).apply {
            upgrade_to_pro.text = activity.getString(R.string.upgrade_to_pro_long)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.upgrade) { dialog, which -> upgradeApp() }
            .setNeutralButton(
                R.string.more_info,
                null
            )     // do not dismiss the dialog on pressing More Info
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(
                    view,
                    this,
                    R.string.upgrade_to_pro,
                    cancelOnTouchOutside = false
                ) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        moreInfo()
                    }
                }
            }
    }

    private fun upgradeApp() {
        activity.launchUpgradeToProIntent()
    }

    private fun moreInfo() {
        activity.launchViewIntent("https://simplemobiletools.com/upgrade_to_pro")
    }
}
