package com.production.planful.extensions

import com.production.planful.helpers.Formatter
import com.production.planful.models.Event
import kotlin.math.pow

fun Long.isTsOnProperDay(event: Event): Boolean {
    val dateTime = Formatter.getDateTimeFromTS(this)
    val power = 2.0.pow((dateTime.dayOfWeek - 1).toDouble()).toInt()
    return event.repeatRule and power != 0
}
