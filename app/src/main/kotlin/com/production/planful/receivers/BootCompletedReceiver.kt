package com.production.planful.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.extensions.notifyRunningEvents
import com.production.planful.extensions.recheckCalDAVCalendars
import com.production.planful.extensions.scheduleAllEvents

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            context.apply {
                scheduleAllEvents()
                notifyRunningEvents()
                recheckCalDAVCalendars(true) {}
            }
        }
    }
}
