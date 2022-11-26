package com.production.planful.services

import android.app.IntentService
import android.content.Intent
import com.production.planful.extensions.cancelNotification
import com.production.planful.extensions.cancelPendingIntent
import com.production.planful.extensions.eventsDB
import com.production.planful.extensions.updateTaskCompletion
import com.production.planful.helpers.ACTION_MARK_COMPLETED
import com.production.planful.helpers.EVENT_ID

class MarkCompletedService : IntentService("MarkCompleted") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && intent.action == ACTION_MARK_COMPLETED) {
            val taskId = intent.getLongExtra(EVENT_ID, 0L)
            val task = eventsDB.getTaskWithId(taskId)
            if (task != null) {
                updateTaskCompletion(task, true)
                cancelPendingIntent(task.id!!)
                cancelNotification(task.id!!)
            }
        }
    }
}
