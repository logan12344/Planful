package com.production.planful.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.production.planful.extensions.config
import com.production.planful.extensions.recheckCalDAVCalendars
import com.production.planful.extensions.refreshCalDAVCalendars
import com.production.planful.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.config.caldavSync) {
            context.refreshCalDAVCalendars(context.config.caldavSyncedCalendarIds, false)
        }

        context.recheckCalDAVCalendars(true) {
            context.updateWidgets()
        }
    }
}
