package com.production.planful.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.production.planful.extensions.config
import com.production.planful.extensions.eventsDB
import com.production.planful.extensions.rescheduleReminder
import com.production.planful.helpers.EVENT_ID
import com.production.planful.commons.extensions.showPickSecondsDialogHelper
import com.production.planful.commons.helpers.ensureBackgroundThread

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showPickSecondsDialogHelper(config.snoozeTime, true, cancelCallback = { dialogCancelled() }) {
            ensureBackgroundThread {
                val eventId = intent.getLongExtra(EVENT_ID, 0L)
                val event = eventsDB.getEventOrTaskWithId(eventId)
                config.snoozeTime = it / 60
                rescheduleReminder(event, it / 60)
                runOnUiThread {
                    finishActivity()
                }
            }
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
