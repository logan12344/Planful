package com.production.planful.interfaces

import com.production.planful.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
