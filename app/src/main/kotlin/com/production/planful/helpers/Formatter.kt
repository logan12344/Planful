package com.production.planful.helpers

import android.content.Context
import com.production.planful.R
import com.production.planful.extensions.config
import com.production.planful.extensions.seconds
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.util.*
import kotlin.math.min

object Formatter {
    const val DAYCODE_PATTERN = "YYYYMMdd"
    const val YEAR_PATTERN = "YYYY"
    const val TIME_PATTERN = "HHmmss"
    private const val MONTH_PATTERN = "MMM"
    private const val DAY_PATTERN = "d"
    private const val DAY_OF_WEEK_PATTERN = "EEE"
    private const val LONGEST_PATTERN = "MMMM d YYYY (EEEE)"
    private const val DATE_DAY_PATTERN = "d EEEE"
    private const val PATTERN_TIME_12 = "hh:mm a"
    private const val PATTERN_TIME_24 = "HH:mm"

    private const val PATTERN_HOURS_12 = "h a"
    private const val PATTERN_HOURS_24 = "HH"

    fun getDateFromCode(context: Context, dayCode: String, shortMonth: Boolean = false): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_PATTERN)
        val year = dateTime.toString(YEAR_PATTERN)
        val monthIndex = Integer.valueOf(dayCode.substring(4, 6))
        var month = getMonthName(context, monthIndex)
        if (shortMonth) {
            month = month.substring(0, min(month.length, 3))
        }

        var date = "$month $day"
        if (year != DateTime().toString(YEAR_PATTERN)) {
            date += " $year"
        }

        return date
    }

    fun getDayTitle(context: Context, dayCode: String, addDayOfWeek: Boolean = true): String {
        val date = getDateFromCode(context, dayCode, true)
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_OF_WEEK_PATTERN, Locale("en", "US"))
        return if (addDayOfWeek)
            "$date ($day)"
        else
            date
    }

    fun getDateDayTitle(dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        return dateTime.toString(DATE_DAY_PATTERN)
    }

    fun getLongMonthYear(context: Context, dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val monthIndex = Integer.valueOf(dayCode.substring(4, 6))
        val month = getMonthName(context, monthIndex)
        val year = dateTime.toString(YEAR_PATTERN)
        var date = month

        if (year != DateTime().toString(YEAR_PATTERN)) {
            date += " $year"
        }

        return date
    }

    fun getLongestDate(ts: Long): String = getDateTimeFromTS(ts).toString(LONGEST_PATTERN)

    fun getDate(context: Context, dateTime: DateTime, addDayOfWeek: Boolean = true) =
        getDayTitle(context, getDayCodeFromDateTime(dateTime), addDayOfWeek)

    fun getFullDate(context: Context, dateTime: DateTime): String {
        val day = dateTime.toString(DAY_PATTERN)
        val year = dateTime.toString(YEAR_PATTERN)
        val monthIndex = dateTime.monthOfYear
        val month = getMonthName(context, monthIndex)
        return "$month $day $year"
    }

    fun getTodayCode() = getDayCodeFromTS(getNowSeconds())

    fun getTodayDayNumber(): String = getDateTimeFromTS(getNowSeconds()).toString(DAY_PATTERN)

    fun getCurrentMonthShort(): String = getDateTimeFromTS(getNowSeconds()).toString(MONTH_PATTERN)

    fun getHours(context: Context, dateTime: DateTime): String = dateTime.toString(getHourPattern(context))

    fun getTime(context: Context, dateTime: DateTime): String = dateTime.toString(getTimePattern(context))

    fun getDateTimeFromCode(dayCode: String): DateTime =
        DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.UTC).parseDateTime(dayCode)

    fun getLocalDateTimeFromCode(dayCode: String): DateTime =
        DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.getDefault())
            .parseLocalDate(dayCode).toDateTimeAtStartOfDay()

    fun getTimeFromTS(context: Context, ts: Long) = getTime(context, getDateTimeFromTS(ts))

    fun getDayStartTS(dayCode: String) = getLocalDateTimeFromCode(dayCode).seconds()

    fun getDayEndTS(dayCode: String) =
        getLocalDateTimeFromCode(dayCode).plusDays(1).minusMinutes(1).seconds()

    fun getDayCodeFromDateTime(dateTime: DateTime): String = dateTime.toString(DAYCODE_PATTERN)

    fun getDateFromTS(ts: Long) = LocalDate(ts * 1000L, DateTimeZone.getDefault())

    fun getDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.getDefault())

    fun getUTCDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.UTC)

    // use manually translated month names, as DateFormat and Joda have issues with a lot of languages
    fun getMonthName(context: Context, id: Int): String =
        context.resources.getStringArray(R.array.months)[id - 1]

    fun getHourPattern(context: Context) =
        if (context.config.use24HourFormat) PATTERN_HOURS_24 else PATTERN_HOURS_12

    fun getTimePattern(context: Context) =
        if (context.config.use24HourFormat) PATTERN_TIME_24 else PATTERN_TIME_12

    fun getExportedTime(ts: Long): String {
        val dateTime = DateTime(ts, DateTimeZone.UTC)
        return "${dateTime.toString(DAYCODE_PATTERN)}T${dateTime.toString(TIME_PATTERN)}Z"
    }

    fun getDayCodeFromTS(ts: Long): String {
        val dayCode = getDateTimeFromTS(ts).toString(DAYCODE_PATTERN)
        return dayCode.ifEmpty { "0" }
    }

    fun getUTCDayCodeFromTS(ts: Long): String = getUTCDateTimeFromTS(ts).toString(DAYCODE_PATTERN)

    fun getYearFromDayCode(dayCode: String): String = getDateTimeFromCode(dayCode).toString(YEAR_PATTERN)

    fun getShiftedTS(dateTime: DateTime, toZone: DateTimeZone) =
        dateTime.withTimeAtStartOfDay().withZoneRetainFields(toZone).seconds()

    fun getShiftedLocalTS(ts: Long) =
        getShiftedTS(dateTime = getUTCDateTimeFromTS(ts), toZone = DateTimeZone.getDefault())

    fun getShiftedUtcTS(ts: Long) =
        getShiftedTS(dateTime = getDateTimeFromTS(ts), toZone = DateTimeZone.UTC)
}
