package com.production.planful.helpers

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.collection.LongSparseArray
import com.production.planful.R
import com.production.planful.commons.extensions.getProperPrimaryColor
import com.production.planful.commons.extensions.toast
import com.production.planful.commons.helpers.CHOPPED_LIST_DEFAULT_SIZE
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.extensions.*
import com.production.planful.models.Event
import com.production.planful.models.EventType

class EventsHelper(val context: Context) {
    private val config = context.config
    private val eventsDB = context.eventsDB
    private val eventTypesDB = context.eventTypesDB

    fun getEventTypes(
        activity: Activity,
        showWritableOnly: Boolean,
        callback: (eventTypes: ArrayList<EventType>) -> Unit
    ) {
        ensureBackgroundThread {
            var eventTypes = ArrayList<EventType>()
            try {
                eventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            } catch (ignored: Exception) {
            }

            if (showWritableOnly) {
                val caldavCalendars = activity.calDAVHelper.getCalDAVCalendars("", true)
                eventTypes = eventTypes.filter {
                    val eventType = it
                    it.caldavCalendarId == 0 || caldavCalendars.firstOrNull { it.id == eventType.caldavCalendarId }
                        ?.canWrite() == true
                }.toMutableList() as ArrayList<EventType>
            }

            activity.runOnUiThread {
                callback(eventTypes)
            }
        }
    }

    fun getEventTypesSync() = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>

    fun insertOrUpdateEventType(
        activity: Activity,
        eventType: EventType,
        callback: ((newEventTypeId: Long) -> Unit)? = null
    ) {
        ensureBackgroundThread {
            val eventTypeId = insertOrUpdateEventTypeSync(eventType)
            activity.runOnUiThread {
                callback?.invoke(eventTypeId)
            }
        }
    }

    fun insertOrUpdateEventTypeSync(eventType: EventType): Long {
        if (eventType.id != null && eventType.id!! > 0 && eventType.caldavCalendarId != 0) {
            context.calDAVHelper.updateCalDAVCalendar(eventType)
        }

        val newId = eventTypesDB.insertOrUpdate(eventType)
        if (eventType.id == null) {
            config.addDisplayEventType(newId.toString())

            if (config.quickFilterEventTypes.isNotEmpty()) {
                config.addQuickFilterEventType(newId.toString())
            } else {
                val eventTypes = getEventTypesSync()
                if (eventTypes.size == 2) {
                    eventTypes.forEach {
                        config.addQuickFilterEventType(it.id.toString())
                    }
                }
            }
        }
        return newId
    }

    fun getEventTypeIdWithTitle(title: String) = eventTypesDB.getEventTypeIdWithTitle(title) ?: -1L

    fun getEventTypeIdWithClass(classId: Int) = eventTypesDB.getEventTypeIdWithClass(classId) ?: -1L

    private fun getLocalEventTypeIdWithTitle(title: String) =
        eventTypesDB.getLocalEventTypeIdWithTitle(title) ?: -1L

    private fun getLocalEventTypeIdWithClass(classId: Int) =
        eventTypesDB.getLocalEventTypeIdWithClass(classId) ?: -1L

    fun getEventTypeWithCalDAVCalendarId(calendarId: Int) =
        eventTypesDB.getEventTypeWithCalDAVCalendarId(calendarId)

    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean) {
        val typesToDelete = eventTypes.asSequence()
            .filter { it.caldavCalendarId == 0 && it.id != REGULAR_EVENT_TYPE_ID }.toMutableList()
        val deleteIds = typesToDelete.map { it.id }.toMutableList()
        val deletedSet = deleteIds.map { it.toString() }.toHashSet()
        config.removeDisplayEventTypes(deletedSet)

        if (deleteIds.isEmpty()) {
            return
        }

        for (eventTypeId in deleteIds) {
            if (deleteEvents) {
                deleteEventsWithType(eventTypeId!!)
            } else {
                eventsDB.resetEventsWithType(eventTypeId!!)
            }
        }

        eventTypesDB.deleteEventTypes(typesToDelete)

        if (getEventTypesSync().size == 1) {
            config.quickFilterEventTypes = HashSet()
        }
    }

    fun insertEvent(
        event: Event,
        addToCalDAV: Boolean,
        showToasts: Boolean,
        callback: ((id: Long) -> Unit)? = null
    ) {
        if (event.startTS > event.endTS) {
            callback?.invoke(0)
            return
        }

        event.id = eventsDB.insertOrUpdate(event)

        context.updateWidgets()
        context.scheduleNextEventReminder(event, showToasts)

        if (addToCalDAV && config.caldavSync && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS) {
            context.calDAVHelper.insertCalDAVEvent(event)
        }

        callback?.invoke(event.id!!)
    }

    fun insertTask(task: Event, showToasts: Boolean, callback: () -> Unit) {
        task.id = eventsDB.insertOrUpdate(task)
        context.updateWidgets()
        context.scheduleNextEventReminder(task, showToasts)
        callback()
    }

    fun insertEvents(events: ArrayList<Event>, addToCalDAV: Boolean) {
        try {
            for (event in events) {
                if (event.startTS > event.endTS) {
                    context.toast(R.string.end_before_start, Toast.LENGTH_LONG)
                    continue
                }

                event.id = eventsDB.insertOrUpdate(event)

                context.scheduleNextEventReminder(event, false)
                if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS && config.caldavSync) {
                    context.calDAVHelper.insertCalDAVEvent(event)
                }
            }
        } finally {
            context.updateWidgets()
        }
    }

    fun updateEvent(
        event: Event,
        updateAtCalDAV: Boolean,
        showToasts: Boolean,
        callback: (() -> Unit)? = null
    ) {
        eventsDB.insertOrUpdate(event)

        context.updateWidgets()
        context.scheduleNextEventReminder(event, showToasts)
        if (updateAtCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
            context.calDAVHelper.updateCalDAVEvent(event)
        }
        callback?.invoke()
    }

    fun deleteAllEvents() {
        ensureBackgroundThread {
            val eventIds = eventsDB.getEventIds().toMutableList()
            deleteEvents(eventIds, true)
        }
    }

    fun deleteEvent(id: Long, deleteFromCalDAV: Boolean) =
        deleteEvents(arrayListOf(id), deleteFromCalDAV)

    fun deleteEvents(ids: MutableList<Long>, deleteFromCalDAV: Boolean) {
        if (ids.isEmpty()) {
            return
        }

        ids.chunked(CHOPPED_LIST_DEFAULT_SIZE).forEach {
            val eventsWithImportId = eventsDB.getEventsByIdsWithImportIds(it)
            eventsDB.deleteEvents(it)

            it.forEach {
                context.cancelNotification(it)
                context.cancelPendingIntent(it)
            }

            if (deleteFromCalDAV && config.caldavSync) {
                eventsWithImportId.forEach {
                    context.calDAVHelper.deleteCalDAVEvent(it)
                }
            }

            deleteChildEvents(it as MutableList<Long>, deleteFromCalDAV)
            context.updateWidgets()
        }
    }

    private fun deleteChildEvents(ids: List<Long>, deleteFromCalDAV: Boolean) {
        val childIds = eventsDB.getEventIdsWithParentIds(ids).toMutableList()
        if (childIds.isNotEmpty()) {
            deleteEvents(childIds, deleteFromCalDAV)
        }
    }

    private fun deleteEventsWithType(eventTypeId: Long) {
        val eventIds = eventsDB.getEventIdsByEventType(eventTypeId).toMutableList()
        deleteEvents(eventIds, true)
    }

    fun addEventRepeatLimit(eventId: Long, limitTS: Long) {
        val time = Formatter.getDateTimeFromTS(limitTS)
        eventsDB.updateEventRepetitionLimit(limitTS - time.hourOfDay, eventId)
        context.cancelNotification(eventId)
        context.cancelPendingIntent(eventId)
        if (config.caldavSync) {
            val event = eventsDB.getEventWithId(eventId)
            if (event != null && event.getCalDAVCalendarId() != 0) {
                context.calDAVHelper.updateCalDAVEvent(event)
            }
        }
    }

    fun doEventTypesContainEvents(
        eventTypeIds: ArrayList<Long>,
        callback: (contain: Boolean) -> Unit
    ) {
        ensureBackgroundThread {
            val eventIds = eventsDB.getEventIdsByEventType(eventTypeIds)
            callback(eventIds.isNotEmpty())
        }
    }

    fun getEventsWithSearchQuery(
        text: String,
        activity: Activity,
        callback: (searchedText: String, events: List<Event>) -> Unit
    ) {
        ensureBackgroundThread {
            val searchQuery = "%$text%"
            val events = eventsDB.getEventsForSearch(searchQuery)
            val displayEventTypes = config.displayEventTypes
            val filteredEvents =
                events.filter { displayEventTypes.contains(it.eventType.toString()) }

            val eventTypeColors = LongSparseArray<Int>()
            eventTypesDB.getEventTypes().forEach {
                eventTypeColors.put(it.id!!, it.color)
            }

            filteredEvents.forEach {
                it.updateIsPastEvent()
                it.color = eventTypeColors.get(it.eventType) ?: context.getProperPrimaryColor()
            }

            activity.runOnUiThread {
                callback(text, filteredEvents)
            }
        }
    }

    fun addEventRepetitionException(parentEventId: Long, occurrenceTS: Long, addToCalDAV: Boolean) {
        ensureBackgroundThread {
            val parentEvent =
                eventsDB.getEventOrTaskWithId(parentEventId) ?: return@ensureBackgroundThread
            var repetitionExceptions = parentEvent.repetitionExceptions
            repetitionExceptions.add(Formatter.getDayCodeFromTS(occurrenceTS))
            repetitionExceptions =
                repetitionExceptions.distinct().toMutableList() as ArrayList<String>

            eventsDB.updateEventRepetitionExceptions(repetitionExceptions.toString(), parentEventId)
            context.scheduleNextEventReminder(parentEvent, false)

            if (addToCalDAV && config.caldavSync) {
                context.calDAVHelper.insertEventRepeatException(parentEvent, occurrenceTS)
            }
        }
    }

    fun getEvents(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean = true,
        callback: (events: ArrayList<Event>) -> Unit
    ) {
        ensureBackgroundThread {
            getEventsSync(fromTS, toTS, eventId, applyTypeFilter, callback)
        }
    }

    fun getEventsSync(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean,
        callback: (events: ArrayList<Event>) -> Unit
    ) {
        val birthDayEventId = getLocalBirthdaysEventTypeId(createIfNotExists = false)
        val anniversaryEventId = getAnniversariesEventTypeId(createIfNotExists = false)

        var events = ArrayList<Event>()
        if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                callback(ArrayList())
                return
            } else {
                try {
                    val typesList = context.config.getDisplayEventTypessAsList()

                    events.addAll(
                        eventsDB.getOneTimeEventsFromToWithTypes(toTS, fromTS, typesList)
                            .toMutableList() as ArrayList<Event>
                    )
                } catch (e: Exception) {
                }
            }
        } else {
            events.addAll(eventsDB.getTasksFromTo(fromTS, toTS, ArrayList()))

            events.addAll(
                if (eventId == -1L) {
                    eventsDB.getOneTimeEventsOrTasksFromTo(toTS, fromTS)
                        .toMutableList() as ArrayList<Event>
                } else {
                    eventsDB.getOneTimeEventFromToWithId(eventId, toTS, fromTS)
                        .toMutableList() as ArrayList<Event>
                }
            )
        }

        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter))

        events = events
            .asSequence()
            .distinct()
            .filterNot { it.repetitionExceptions.contains(Formatter.getDayCodeFromTS(it.startTS)) }
            .toMutableList() as ArrayList<Event>

        val eventTypeColors = LongSparseArray<Int>()
        context.eventTypesDB.getEventTypes().forEach {
            eventTypeColors.put(it.id!!, it.color)
        }

        events.forEach {
            if (it.isTask()) updateIsTaskCompleted(it)
            it.updateIsPastEvent()
            val originalEvent = eventsDB.getEventWithId(it.id!!)
            if (originalEvent != null &&
                (birthDayEventId != -1L && it.eventType == birthDayEventId) or
                (anniversaryEventId != -1L && it.eventType == anniversaryEventId)
            ) {
                val eventStartDate = Formatter.getDateFromTS(it.startTS)
                val originalEventStartDate = Formatter.getDateFromTS(originalEvent.startTS)
                if (it.hasMissingYear().not()) {
                    val years = (eventStartDate.year - originalEventStartDate.year).coerceAtLeast(0)
                    if (years > 0) {
                        it.title = "${it.title} ($years)"
                    }
                }
            }
            it.color = eventTypeColors.get(it.eventType) ?: context.getProperPrimaryColor()
        }

        callback(events)
    }

    fun createPredefinedEventType(
        title: String,
        @ColorRes colorResId: Int,
        type: Int,
        caldav: Boolean = false
    ): Long {
        val eventType = EventType(
            id = null,
            title = title,
            color = context.resources.getColor(colorResId),
            type = type
        )

        // check if the event type already exists but without the type (e.g. BIRTHDAY_EVENT) so as to avoid duplication
        val originalEventTypeId = if (caldav) {
            getEventTypeIdWithTitle(title)
        } else {
            getLocalEventTypeIdWithTitle(title)
        }
        if (originalEventTypeId != -1L) {
            eventType.id = originalEventTypeId
        }

        return insertOrUpdateEventTypeSync(eventType)
    }

    fun getLocalBirthdaysEventTypeId(createIfNotExists: Boolean = true): Long {
        var eventTypeId = getLocalEventTypeIdWithClass(BIRTHDAY_EVENT)
        if (eventTypeId == -1L && createIfNotExists) {
            val birthdays = context.getString(R.string.birthdays)
            eventTypeId = createPredefinedEventType(
                birthdays,
                R.color.default_birthdays_color,
                BIRTHDAY_EVENT
            )
        }
        return eventTypeId
    }

    fun getAnniversariesEventTypeId(createIfNotExists: Boolean = true): Long {
        var eventTypeId = getLocalEventTypeIdWithClass(ANNIVERSARY_EVENT)
        if (eventTypeId == -1L && createIfNotExists) {
            val anniversaries = context.getString(R.string.anniversaries)
            eventTypeId = createPredefinedEventType(
                anniversaries,
                R.color.default_anniversaries_color,
                ANNIVERSARY_EVENT
            )
        }
        return eventTypeId
    }

    fun getRepeatableEventsFor(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean = false
    ): List<Event> {
        val events = if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                return ArrayList()
            } else {
                eventsDB.getRepeatableEventsOrTasksWithTypes(
                    toTS,
                    context.config.getDisplayEventTypessAsList()
                ).toMutableList() as ArrayList<Event>
            }
        } else {
            if (eventId == -1L) {
                eventsDB.getRepeatableEventsOrTasksWithTypes(toTS)
                    .toMutableList() as ArrayList<Event>
            } else {
                eventsDB.getRepeatableEventsOrTasksWithId(eventId, toTS)
                    .toMutableList() as ArrayList<Event>
            }
        }

        val startTimes = LongSparseArray<Long>()
        val newEvents = ArrayList<Event>()
        events.forEach {
            startTimes.put(it.id!!, it.startTS)
            if (it.repeatLimit >= 0) {
                newEvents.addAll(getEventsRepeatingTillDateOrForever(fromTS, toTS, startTimes, it))
            } else {
                newEvents.addAll(getEventsRepeatingXTimes(fromTS, toTS, startTimes, it))
            }
        }

        return newEvents
    }

    private fun getEventsRepeatingXTimes(
        fromTS: Long,
        toTS: Long,
        startTimes: LongSparseArray<Long>,
        event: Event
    ): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.repeatLimit < 0 && event.startTS <= toTS) {
            if (event.repeatInterval.isXWeeklyRepetition()) {
                if (event.startTS.isTsOnProperDay(event)) {
                    if (event.isOnProperWeek(startTimes)) {
                        if (event.endTS >= fromTS) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                        event.repeatLimit++
                    }
                }
            } else {
                if (event.endTS >= fromTS) {
                    event.copy().apply {
                        updateIsPastEvent()
                        color = event.color
                        events.add(this)
                    }
                } else if (event.getIsAllDay()) {
                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                    if (dayCode == endDayCode) {
                        event.copy().apply {
                            updateIsPastEvent()
                            color = event.color
                            events.add(this)
                        }
                    }
                }
                event.repeatLimit++
            }
            event.addIntervalTime(original)
        }
        return events
    }

    private fun getEventsRepeatingTillDateOrForever(
        fromTS: Long,
        toTS: Long,
        startTimes: LongSparseArray<Long>,
        event: Event
    ): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.startTS <= toTS && (event.repeatLimit == 0L || event.repeatLimit >= event.startTS)) {
            if (event.endTS >= fromTS) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    event.copy().apply {
                        updateIsPastEvent()
                        color = event.color
                        events.add(this)
                    }
                }
            }

            if (event.getIsAllDay()) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.endTS >= toTS && event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                    if (dayCode == endDayCode) {
                        event.copy().apply {
                            updateIsPastEvent()
                            color = event.color
                            events.add(this)
                        }
                    }
                }
            }
            event.addIntervalTime(original)
        }
        return events
    }

    fun updateIsTaskCompleted(event: Event) {
        val task = context.completedTasksDB.getTaskWithIdAndTs(event.id!!, startTs = event.startTS)
        event.flags = task?.flags ?: event.flags
    }

    fun getRunningEventsOrTasks(): List<Event> {
        val ts = getNowSeconds()
        val events =
            eventsDB.getOneTimeEventsOrTasksFromTo(ts, ts).toMutableList() as ArrayList<Event>
        events.addAll(getRepeatableEventsFor(ts, ts))
        events.forEach {
            if (it.isTask()) updateIsTaskCompleted(it)
        }
        return events
    }

    fun getEventsToExport(eventTypes: ArrayList<Long>): ArrayList<Event> {
        val currTS = getNowSeconds()
        var events = ArrayList<Event>()
        if (config.exportPastEvents) {
            events.addAll(eventsDB.getAllEventsWithTypes(eventTypes))
        } else {
            events.addAll(eventsDB.getOneTimeFutureEventsWithTypes(currTS, eventTypes))
            events.addAll(eventsDB.getRepeatableFutureEventsWithTypes(currTS, eventTypes))
        }

        events = events.distinctBy { it.id } as ArrayList<Event>
        return events
    }
}
