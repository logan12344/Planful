package com.production.planful.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.extensions.eventsDB
import com.production.planful.extensions.notifyEvent
import com.production.planful.extensions.scheduleNextEventReminder
import com.production.planful.extensions.updateListWidget
import com.production.planful.helpers.EVENT_ID
import com.production.planful.helpers.Formatter
import com.production.planful.helpers.REMINDER_NOTIFICATION

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "planful:notificationreceiver"
        )
        wakelock.acquire(3000)

        ensureBackgroundThread {
            handleIntent(context, intent)
        }
    }

    private fun handleIntent(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EVENT_ID, -1L)
        if (id == -1L) {
            return
        }

        context.updateListWidget()
        val event = context.eventsDB.getEventOrTaskWithId(id)
        if (event == null || event.getReminders()
                .none { it.type == REMINDER_NOTIFICATION } || event.repetitionExceptions.contains(
                Formatter.getTodayCode()
            )
        ) {
            return
        }

        if (!event.repetitionExceptions.contains(Formatter.getDayCodeFromTS(event.startTS))) {
            context.notifyEvent(event)
        }
        context.scheduleNextEventReminder(event, false)
    }
}
