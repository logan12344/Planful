package com.production.planful.commons.dialogs

import android.app.Activity
import android.text.Html
import android.text.method.LinkMovementMethod
import com.production.planful.R
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.launchViewIntent
import com.production.planful.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_new_apps_icons.view.*

class NewAppsIconsDialog(val activity: Activity) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_new_apps_icons, null).apply {
            val dialerUrl = "https://play.google.com/store/apps/details?id=com.production.planful.dialer"
            val smsMessengerUrl = "https://play.google.com/store/apps/details?id=com.production.planful.smsmessenger"
            val voiceRecorderUrl = "https://play.google.com/store/apps/details?id=com.production.planful.voicerecorder"

            val text = String.format(
                activity.getString(R.string.new_app),
                dialerUrl, activity.getString(R.string.simple_dialer),
                smsMessengerUrl, activity.getString(R.string.simple_sms_messenger),
                voiceRecorderUrl, activity.getString(R.string.simple_voice_recorder)
            )

            new_apps_text.text = Html.fromHtml(text)
            new_apps_text.movementMethod = LinkMovementMethod.getInstance()
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false)
            }
    }
}
