package com.production.planful.helpers

import android.content.Context
import com.production.planful.extensions.eventsHelper
import com.production.planful.interfaces.WeeklyCalendar
import com.production.planful.models.Event
import com.production.planful.commons.helpers.DAY_SECONDS
import com.production.planful.commons.helpers.WEEK_SECONDS
import java.util.*

class WeeklyCalendarImpl(val callback: WeeklyCalendar, val context: Context) {
    var mEvents = ArrayList<Event>()

    fun updateWeeklyCalendar(weekStartTS: Long) {
        val endTS = weekStartTS + 2 * WEEK_SECONDS
        context.eventsHelper.getEvents(weekStartTS - DAY_SECONDS, endTS) {
            mEvents = it
            callback.updateWeeklyCalendar(it)
        }
    }
}
