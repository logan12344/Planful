package com.production.planful.services

import android.app.IntentService
import android.content.Intent
import com.production.planful.extensions.config
import com.production.planful.extensions.eventsDB
import com.production.planful.extensions.rescheduleReminder
import com.production.planful.helpers.EVENT_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(EVENT_ID, 0L)
            val event = eventsDB.getEventOrTaskWithId(eventId)
            rescheduleReminder(event, config.snoozeTime)
        }
    }
}
