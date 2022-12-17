package com.production.planful.extensions

import android.app.Activity
import com.production.planful.BuildConfig
import com.production.planful.R
import com.production.planful.commons.activities.BaseSimpleActivity
import com.production.planful.commons.dialogs.RadioGroupDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.commons.models.RadioItem
import com.production.planful.dialogs.CustomEventRepeatIntervalDialog
import com.production.planful.helpers.DAY
import com.production.planful.helpers.IcsExporter
import com.production.planful.helpers.MONTH
import com.production.planful.helpers.WEEK
import com.production.planful.models.Event
import java.io.File
import java.util.*

fun BaseSimpleActivity.shareEvents(ids: List<Long>) {
    ensureBackgroundThread {
        val file = getTempFile()
        if (file == null) {
            toast(R.string.unknown_error_occurred)
            return@ensureBackgroundThread
        }

        val events = ArrayList<Event>()
        for (id in ids) {
            eventsDB.getEventOrTaskWithId(id)?.let { events.add(it) }
        }

        if (events.isEmpty()) {
            toast(R.string.no_items_found)
        }

        getFileOutputStream(file.toFileDirItem(this), true) {
            IcsExporter().exportEvents(this, it, events, false) { it ->
                if (it == IcsExporter.ExportResult.EXPORT_OK) {
                    sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
                }
            }
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
}

fun Activity.showEventRepeatIntervalDialog(curSeconds: Int, callback: (minutes: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        add(0)
        add(DAY)
        add(WEEK)
        add(MONTH)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getRepetitionText(value), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds)
            selectedIndex = index
    }

    RadioGroupDialog(this, items, selectedIndex) {
        if (it == -1) {
            CustomEventRepeatIntervalDialog(this) { it ->
                callback(it)
            }
        } else {
            callback(it as Int)
        }
    }
}
