package com.production.planful.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.production.planful.R
import com.production.planful.adapters.ChecklistAdapter
import com.production.planful.commons.dialogs.ConfirmationAdvancedDialog
import com.production.planful.commons.dialogs.RadioGroupDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.*
import com.production.planful.commons.models.RadioItem
import com.production.planful.dialogs.*
import com.production.planful.extensions.*
import com.production.planful.helpers.*
import com.production.planful.helpers.Formatter
import com.production.planful.models.ChecklistItem
import com.production.planful.models.Event
import com.production.planful.models.EventType
import com.production.planful.models.Reminder
import kotlinx.android.synthetic.main.activity_task.*
import org.joda.time.DateTime
import java.util.*
import kotlin.math.pow

class TaskActivity : SimpleActivity() {
    private var mEventTypeId = REGULAR_EVENT_TYPE_ID
    private lateinit var mTaskStartDateTime: DateTime
    private lateinit var mTaskEndDateTime: DateTime
    private lateinit var mTask: Event
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var checklistArray: ArrayList<ChecklistItem>
    private lateinit var jsonArray: ArrayList<Pair<String, ArrayList<ChecklistItem>>>
    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var mReminder1Type = REMINDER_NOTIFICATION
    private var mReminder2Type = REMINDER_NOTIFICATION
    private var mReminder3Type = REMINDER_NOTIFICATION
    private var mRepeatInterval = 0
    private var mRepeatLimit = 0L
    private var mRepeatRule = 0
    private var mTaskOccurrenceTS = 0L
    private var mOriginalStartTS = 0L
    private var mOriginalEndTS = 0L
    private var mTaskCompleted = false
    private var mLastSavePromptTS = 0L
    private var mIsNewTask = true
    private var dayCode = Formatter.getTodayCode()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)
        setupOptionsMenu()
        refreshMenuItems()

        if (checkAppSideloading()) {
            return
        }

        val intent = intent ?: return
        updateColors()
        val taskId = intent.getLongExtra(EVENT_ID, 0L)
        ensureBackgroundThread {
            val task = eventsDB.getTaskWithId(taskId)
            if (taskId != 0L && task == null) {
                hideKeyboard()
                finish()
                return@ensureBackgroundThread
            }

            val storedEventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            val localEventType = storedEventTypes.firstOrNull { it.id == config.lastUsedLocalEventTypeId }
            runOnUiThread {
                if (!isDestroyed && !isFinishing) {
                    gotTask(savedInstanceState, localEventType, task)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(task_toolbar, NavigationIcon.Arrow)
    }

    private fun refreshMenuItems() {
        if (::mTask.isInitialized) {
            task_toolbar.menu.apply {
                findItem(R.id.delete).isVisible = mTask.id != null
                findItem(R.id.duplicate).isVisible = mTask.id != null
                findItem(R.id.share).isVisible = mTask.id != null
            }
        }
    }

    private fun setupOptionsMenu() {
        task_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.save -> saveCurrentTask()
                R.id.delete -> deleteTask()
                R.id.duplicate -> duplicateTask()
                R.id.share -> shareEvents(arrayListOf(mTask.id!!))
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun isTaskChanged(): Boolean {
        if (!this::mTask.isInitialized) {
            return false
        }

        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }
        
        val hasTimeChanged = if (mOriginalStartTS == 0L) {
            mTask.startTS != newStartTS || mTask.endTS != newEndTS
        } else {
            mOriginalStartTS != newStartTS || mOriginalEndTS != newEndTS
        }

        val reminders = getReminders()
        val originalReminders = mTask.getReminders()
        if (task_title.text.toString() != mTask.title ||
            task_description.text.toString() != mTask.description ||
            reminders != originalReminders ||
            mRepeatInterval != mTask.repeatInterval ||
            mRepeatRule != mTask.repeatRule ||
            mEventTypeId != mTask.eventType ||
            hasTimeChanged
        ) {
            return true
        }

        return false
    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - mLastSavePromptTS > SAVE_DISCARD_PROMPT_INTERVAL && isTaskChanged()) {
            mLastSavePromptTS = System.currentTimeMillis()
            ConfirmationAdvancedDialog(
                this,
                "",
                R.string.save_before_closing,
                R.string.save,
                R.string.discard
            ) {
                if (it) {
                    saveCurrentTask()
                } else {
                    super.onBackPressed()
                }
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::mTask.isInitialized) {
            return
        }

        outState.apply {
            putSerializable(TASK, mTask)
            putLong(START_TS, mTaskStartDateTime.seconds())
            putLong(END_TS, mTaskEndDateTime.seconds())
            putLong(EVENT_TYPE_ID, mEventTypeId)

            putInt(REMINDER_1_MINUTES, mReminder1Minutes)
            putInt(REMINDER_2_MINUTES, mReminder2Minutes)
            putInt(REMINDER_3_MINUTES, mReminder3Minutes)

            putInt(REPEAT_INTERVAL, mRepeatInterval)
            putInt(REPEAT_RULE, mRepeatRule)
            putLong(REPEAT_LIMIT, mRepeatLimit)

            putLong(EVENT_TYPE_ID, mEventTypeId)
            putBoolean(IS_NEW_EVENT, mIsNewTask)
            putLong(ORIGINAL_START_TS, mOriginalStartTS)
            putLong(ORIGINAL_END_TS, mOriginalEndTS)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (!savedInstanceState.containsKey(START_TS)) {
            hideKeyboard()
            finish()
            return
        }

        savedInstanceState.apply {
            mTask = getSerializable(TASK) as Event
            mTaskStartDateTime = Formatter.getDateTimeFromTS(getLong(START_TS))
            mTaskEndDateTime = Formatter.getDateTimeFromTS(getLong(END_TS))
            mEventTypeId = getLong(EVENT_TYPE_ID)

            mReminder1Minutes = getInt(REMINDER_1_MINUTES)
            mReminder2Minutes = getInt(REMINDER_2_MINUTES)
            mReminder3Minutes = getInt(REMINDER_3_MINUTES)

            mRepeatInterval = getInt(REPEAT_INTERVAL)
            mRepeatRule = getInt(REPEAT_RULE)
            mRepeatLimit = getLong(REPEAT_LIMIT)
            mEventTypeId = getLong(EVENT_TYPE_ID)
            mIsNewTask = getBoolean(IS_NEW_EVENT)
            mOriginalStartTS = getLong(ORIGINAL_START_TS)
            mOriginalEndTS = getLong(ORIGINAL_END_TS)
        }

        updateEventType()
        checkRepeatTexts(mRepeatInterval)
        checkRepeatRule()
        updateActionBarTitle()
    }

    private fun gotTask(savedInstanceState: Bundle?, localEventType: EventType?, task: Event?) {
        if (localEventType == null) {
            config.lastUsedLocalEventTypeId = REGULAR_EVENT_TYPE_ID
        }

        mEventTypeId =
            if (config.defaultEventTypeId == -1L) config.lastUsedLocalEventTypeId else config.defaultEventTypeId

        if (task != null) {
            mTask = task
            mTaskOccurrenceTS = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
            mTaskCompleted = intent.getBooleanExtra(IS_TASK_COMPLETED, false)
            if (savedInstanceState == null) {
                setupEditTask()
            }

            if (intent.getBooleanExtra(IS_DUPLICATE_INTENT, false)) {
                mTask.id = null
                task_toolbar.title = getString(R.string.new_task)
            }
        } else {
            mTask = Event(null)
            config.apply {
                mReminder1Minutes = if (usePreviousEventReminders && lastEventReminderMinutes1 >= -1) lastEventReminderMinutes1 else defaultReminder1
                mReminder2Minutes = if (usePreviousEventReminders && lastEventReminderMinutes2 >= -1) lastEventReminderMinutes2 else defaultReminder2
                mReminder3Minutes = if (usePreviousEventReminders && lastEventReminderMinutes3 >= -1) lastEventReminderMinutes3 else defaultReminder3
            }

            if (savedInstanceState == null) {
                setupNewTask()
            }
        }

        dayCode = Formatter.getDayCodeFromTS(mTaskOccurrenceTS)

        setupRecycle()

        task_all_day.setOnCheckedChangeListener { _, isChecked -> toggleAllDay(isChecked) }
        task_all_day_holder.setOnClickListener {
            task_all_day.toggle()
        }

        task_target.setOnCheckedChangeListener { _, isChecked -> toggleTarget(isChecked) }
        task_target_holder.setOnClickListener {
            task_target.toggle()
        }

        task_checklist.setOnCheckedChangeListener { _, isChecked -> toggleChecklist(isChecked) }
        task_checklist_holder.setOnClickListener {
            task_checklist.toggle()
        }

        checklist_complete.setOnClickListener {
            checklistArray.add(ChecklistItem("", false))
            checklistAdapter.notifyItemInserted(checklistArray.size)
        }

        task_start_date.setOnClickListener { setupStartDate() }
        task_start_time.setOnClickListener { setupStartTime() }
        task_end_date.setOnClickListener { setupEndDate() }
        task_end_time.setOnClickListener { setupEndTime() }
        task_repetition.setOnClickListener { showRepeatIntervalDialog() }
        task_repetition_rule_holder.setOnClickListener { showRepetitionRuleDialog() }
        task_repetition_limit_holder.setOnClickListener { showRepetitionTypePicker() }
        task_type_holder.setOnClickListener { showEventTypeDialog() }

        task_reminder_1.setOnClickListener {
            handleNotificationAvailability {
                showReminder1Dialog()
            }
        }

        task_reminder_2.setOnClickListener { showReminder2Dialog() }
        refreshMenuItems()

        if (savedInstanceState == null) {
            updateEventType()
            updateTexts()
        }
    }

    private fun setupRecycle() {
        checklistArray = ArrayList()
        jsonArray = ArrayList()
        if (mTask.checklist != "") {
            jsonArray = gson.fromJson(mTask.checklist)

            var ifDayExist = false

            for (item in jsonArray) {
                if (item.first == dayCode) {
                    ifDayExist = true
                    checklistArray.addAll(item.second)
                    jsonArray.remove(item)
                    break
                }
            }

            if (!ifDayExist) {
                for (item in jsonArray) {
                    val listAccountCloned = ArrayList(item.second)
                    for (arrayItem in listAccountCloned) {
                        arrayItem.checked = false
                    }
                    checklistArray.addAll(listAccountCloned)
                    break
                }
            }
        } else {
            checklistArray.add(ChecklistItem("", false))
        }

        checklistAdapter = ChecklistAdapter(this, recycle_checklist, checklistArray)
        recycle_checklist.adapter = checklistAdapter
        recycle_checklist.layoutManager = LinearLayoutManager(this)
    }

    private fun setupEditTask() {
        mIsNewTask = false
        val realStart = if (mTaskOccurrenceTS == 0L) mTask.startTS else mTaskOccurrenceTS
        val duration = mTask.endTS - mTask.startTS
        mOriginalStartTS = realStart
        mOriginalEndTS = realStart + duration
        
        mTaskStartDateTime = Formatter.getDateTimeFromTS(realStart)
        mTaskEndDateTime = Formatter.getDateTimeFromTS(realStart + duration)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        task_toolbar.title = getString(R.string.edit_task)

        mEventTypeId = mTask.eventType
        mReminder1Minutes = mTask.reminder1Minutes
        mReminder2Minutes = mTask.reminder2Minutes
        mReminder3Minutes = mTask.reminder3Minutes
        mReminder1Type = mTask.reminder1Type
        mReminder2Type = mTask.reminder2Type
        mReminder3Type = mTask.reminder3Type
        mRepeatInterval = mTask.repeatInterval
        mRepeatLimit = mTask.repeatLimit
        mRepeatRule = mTask.repeatRule

        task_title.setText(mTask.title)
        task_description.setText(mTask.description)
        task_all_day.isChecked = mTask.getIsAllDay()
        toggleAllDay(mTask.getIsAllDay())
        toggleTarget(mTask.trackTarget)
        toggleChecklist(mTask.checklistEnable)
        checkRepeatTexts(mRepeatInterval)
    }

    private fun setupNewTask() {
        val startTS = intent.getLongExtra(NEW_EVENT_START_TS, 0L)
        mTaskStartDateTime = Formatter.getDateTimeFromTS(startTS)

        val endTS = intent.getLongExtra(NEW_EVENT_START_TS, 0L)
        mTaskEndDateTime = Formatter.getDateTimeFromTS(endTS)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        task_title.requestFocus()
        task_toolbar.title = getString(R.string.new_task)

        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }
        
        mTask.apply {
            this.startTS = newStartTS
            this.endTS = newEndTS
            reminder1Minutes = mReminder1Minutes
            reminder1Type = mReminder1Type
            reminder2Minutes = mReminder2Minutes
            reminder2Type = mReminder2Type
            reminder3Minutes = mReminder3Minutes
            reminder3Type = mReminder3Type
            eventType = mEventTypeId
        }
    }

    private fun saveCurrentTask() {
        ensureBackgroundThread {
            saveTask()
        }
    }

    private fun saveTask() {
        val newTitle = task_title.value
        if (newTitle.isEmpty()) {
            toast(R.string.title_empty)
            runOnUiThread {
                task_title.requestFocus()
            }
            return
        }

        var newStartTS: Long
        var newEndTS: Long
        getStartEndTimes().apply {
            newStartTS = first
            newEndTS = second
        }

        if (newStartTS > newEndTS) {
            toast(R.string.end_task_before_start)
            return
        }

        val wasRepeatable = mTask.repeatInterval > 0

        val reminders = getReminders()
        if (!task_all_day.isChecked) {
            if ((reminders.getOrNull(2)?.minutes ?: 0) < -1) {
                reminders.removeAt(2)
            }

            if ((reminders.getOrNull(1)?.minutes ?: 0) < -1) {
                reminders.removeAt(1)
            }

            if ((reminders.getOrNull(0)?.minutes ?: 0) < -1) {
                reminders.removeAt(0)
            }
        }

        val reminder1 = reminders.getOrNull(0) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder2 = reminders.getOrNull(1) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)
        val reminder3 = reminders.getOrNull(2) ?: Reminder(REMINDER_OFF, REMINDER_NOTIFICATION)

        config.apply {
            if (usePreviousEventReminders) {
                lastEventReminderMinutes1 = reminder1.minutes
                lastEventReminderMinutes2 = reminder2.minutes
                lastEventReminderMinutes3 = reminder3.minutes
            }
        }

        config.lastUsedLocalEventTypeId = mEventTypeId
        mTask.apply {
            startTS = mTaskStartDateTime.withSecondOfMinute(0).withMillisOfSecond(0).seconds()
            endTS = mTaskEndDateTime.withSecondOfMinute(0).withMillisOfSecond(0).seconds()
            title = newTitle
            description = task_description.value

            // migrate completed task to the new completed tasks db
            if (!wasRepeatable && mTask.isTaskCompleted()) {
                mTask.flags = mTask.flags.removeBit(FLAG_TASK_COMPLETED)
                ensureBackgroundThread {
                    updateTaskCompletion(copy(startTS = mOriginalStartTS), true)
                }
            }

            flags = mTask.flags.addBitIf(task_all_day.isChecked, FLAG_ALL_DAY)
            lastUpdated = System.currentTimeMillis()
            eventType = mEventTypeId
            type = TYPE_TASK

            reminder1Minutes = reminder1.minutes
            reminder1Type = mReminder1Type
            reminder2Minutes = reminder2.minutes
            reminder2Type = mReminder2Type
            reminder3Minutes = reminder3.minutes
            reminder3Type = mReminder3Type

            repeatInterval = mRepeatInterval
            repeatLimit = if (repeatInterval == 0) 0 else mRepeatLimit
            repeatRule = mRepeatRule
        }

        if (mTask.getReminders().isNotEmpty()) {
            handleNotificationPermission { granted ->
                if (granted) {
                    ensureBackgroundThread {
                        storeTask(wasRepeatable)
                    }
                } else {
                    toast(R.string.no_post_notifications_permissions)
                }
            }
        } else {
            storeTask(wasRepeatable)
        }
    }

    private fun storeTask(wasRepeatable: Boolean) {
        jsonArray.add(Pair(dayCode, checklistArray))

        if (mTask.id == null) {
            mTask.apply {
                checklistEnable = task_checklist.isChecked
                checklist = gson.convertToJsonString(jsonArray)
                trackTarget = task_target.isChecked
            }
            eventsHelper.insertTask(mTask, true) {
                hideKeyboard()

                if (DateTime.now().isAfter(mTaskStartDateTime.millis)) {
                    if (mTask.repeatInterval == 0 && mTask.getReminders()
                            .any { it.type == REMINDER_NOTIFICATION }) {
                        notifyEvent(mTask)
                    }
                }

                finish()
            }
        } else {
            if (mRepeatInterval > 0 && wasRepeatable) {
                runOnUiThread {
                    showEditRepeatingTaskDialog()
                }
            } else {
                hideKeyboard()
                mTask.apply {
                    checklistEnable = task_checklist.isChecked
                    checklist = gson.convertToJsonString(jsonArray)
                    trackTarget = task_target.isChecked
                }
                eventsHelper.updateEvent(mTask, updateAtCalDAV = false, showToasts = true) {
                    finish()
                }
            }
        }
    }

    private fun showEditRepeatingTaskDialog() {
        EditRepeatingEventDialog(this, isTask = true) {
            hideKeyboard()
            when (it) {
                0 -> {
                    ensureBackgroundThread {
                        eventsHelper.addEventRepetitionException(
                            mTask.id!!,
                            mTaskOccurrenceTS,
                            true
                        )
                        mTask.apply {
                            parentId = id!!.toLong()
                            id = null
                            repeatRule = 0
                            repeatInterval = 0
                            repeatLimit = 0
                            checklistEnable = task_checklist.isChecked
                            checklist = gson.convertToJsonString(jsonArray)
                            trackTarget = task_target.isChecked
                        }

                        eventsHelper.insertTask(mTask, showToasts = true) {
                            finish()
                        }
                    }
                }
                1 -> {
                    ensureBackgroundThread {
                        eventsHelper.addEventRepeatLimit(mTask.id!!, mTaskOccurrenceTS)
                        for (i in 0 until jsonArray.size) {
                            if (jsonArray[i].first != dayCode) {
                                jsonArray[i].second.clear()
                                jsonArray[i].second.addAll(checklistArray)
                            }
                        }
                        mTask.apply {
                            id = null
                            checklistEnable = task_checklist.isChecked
                            checklist = gson.convertToJsonString(jsonArray)
                            trackTarget = task_target.isChecked
                        }

                        eventsHelper.insertTask(mTask, showToasts = true) {
                            finish()
                        }
                    }
                }
                2 -> {
                    ensureBackgroundThread {
                        eventsHelper.addEventRepeatLimit(mTask.id!!, mTaskOccurrenceTS)
                        for (i in 0 until jsonArray.size) {
                            if (jsonArray[i].first != dayCode) {
                                jsonArray[i].second.clear()
                                jsonArray[i].second.addAll(checklistArray)
                            }
                        }
                        mTask.apply {
                            checklistEnable = task_checklist.isChecked
                            checklist = gson.convertToJsonString(jsonArray)
                            trackTarget = task_target.isChecked
                        }
                        eventsHelper.updateEvent(mTask, updateAtCalDAV = false, showToasts = true) {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun deleteTask() {
        if (mTask.id == null) {
            return
        }

        DeleteEventDialog(this, arrayListOf(mTask.id!!), mTask.repeatInterval > 0, isTask = true) {
            ensureBackgroundThread {
                when (it) {
                    DELETE_SELECTED_OCCURRENCE -> eventsHelper.addEventRepetitionException(
                        mTask.id!!,
                        mTaskOccurrenceTS,
                        true
                    )
                    DELETE_FUTURE_OCCURRENCES -> eventsHelper.addEventRepeatLimit(
                        mTask.id!!,
                        mTaskOccurrenceTS
                    )
                    DELETE_ALL_OCCURRENCES -> eventsHelper.deleteEvent(mTask.id!!, true)
                }

                runOnUiThread {
                    hideKeyboard()
                    finish()
                }
            }
        }
    }

    private fun duplicateTask() {
        // the activity has the singleTask launchMode to avoid some glitches, so finish it before relaunching
        hideKeyboard()
        finish()
        Intent(this, TaskActivity::class.java).apply {
            putExtra(EVENT_ID, mTask.id)
            putExtra(IS_DUPLICATE_INTENT, true)
            startActivity(this)
        }
    }

    private fun setupStartDate() {
        hideKeyboard()
        val datePicker = DatePickerDialog(
            this,
            getDatePickerDialogTheme(),
            startDateSetListener,
            mTaskStartDateTime.year,
            mTaskStartDateTime.monthOfYear - 1,
            mTaskStartDateTime.dayOfMonth
        )

        datePicker.datePicker.firstDayOfWeek =
            if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datePicker.show()
    }

    private fun setupStartTime() {
        hideKeyboard()
        TimePickerDialog(
            this,
            getTimePickerDialogTheme(),
            startTimeSetListener,
            mTaskStartDateTime.hourOfDay,
            mTaskStartDateTime.minuteOfHour,
            config.use24HourFormat
        ).show()
    }

    private fun setupEndDate() {
        hideKeyboard()
        val datepicker = DatePickerDialog(
            this,
            getDatePickerDialogTheme(),
            endDateSetListener,
            mTaskEndDateTime.year,
            mTaskEndDateTime.monthOfYear - 1,
            mTaskEndDateTime.dayOfMonth
        )

        datepicker.datePicker.firstDayOfWeek =
            if (config.isSundayFirst) Calendar.SUNDAY else Calendar.MONDAY
        datepicker.show()
    }

    private fun setupEndTime() {
        hideKeyboard()
        TimePickerDialog(
            this,
            getTimePickerDialogTheme(),
            endTimeSetListener,
            mTaskEndDateTime.hourOfDay,
            mTaskEndDateTime.minuteOfHour,
            config.use24HourFormat
        ).show()
    }

    private val startDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            dateSet(year, monthOfYear, dayOfMonth, true)
        }

    private val startTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        timeSet(hourOfDay, minute, true)
    }

    private val endDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            dateSet(year, monthOfYear, dayOfMonth, false)
        }

    private val endTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        timeSet(hourOfDay, minute, false)
    }

    private fun dateSet(year: Int, month: Int, day: Int, isStart: Boolean) {
        if (isStart) {
            val diff = mTaskEndDateTime.seconds() - mTaskStartDateTime.seconds()

            mTaskStartDateTime = mTaskStartDateTime.withDate(year, month + 1, day)
            updateStartDateText()
            checkRepeatRule()

            mTaskEndDateTime = mTaskStartDateTime.plusSeconds(diff.toInt())
            updateEndTexts()
        } else {
            mTaskEndDateTime = mTaskEndDateTime.withDate(year, month + 1, day)
            updateEndDateText()
        }
    }

    private fun timeSet(hours: Int, minutes: Int, isStart: Boolean) {
        try {
            if (isStart) {
                val diff = mTaskEndDateTime.seconds() - mTaskStartDateTime.seconds()

                mTaskStartDateTime =
                    mTaskStartDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
                updateStartTimeText()

                mTaskEndDateTime = mTaskStartDateTime.plusSeconds(diff.toInt())
                updateEndTexts()
            } else {
                mTaskEndDateTime = mTaskEndDateTime.withHourOfDay(hours).withMinuteOfHour(minutes)
                updateEndTimeText()
            }
        } catch (e: Exception) {
            timeSet(hours + 1, minutes, isStart)
            return
        }
    }

    private fun updateTexts() {
        updateStartTexts()
        updateEndTexts()
        updateReminderTexts()
        updateRepetitionText()
    }

    private fun checkRepeatRule() {
        if (mRepeatInterval.isXWeeklyRepetition()) {
            val day = mRepeatRule
            if (day == MONDAY_BIT || day == TUESDAY_BIT || day == WEDNESDAY_BIT || day == THURSDAY_BIT || day == FRIDAY_BIT || day == SATURDAY_BIT || day == SUNDAY_BIT) {
                setRepeatRule(2.0.pow((mTaskStartDateTime.dayOfWeek - 1).toDouble()).toInt())
            }
        } else if (mRepeatInterval.isXMonthlyRepetition() || mRepeatInterval.isXYearlyRepetition()) {
            if (mRepeatRule == REPEAT_LAST_DAY && !isLastDayOfTheMonth()) {
                mRepeatRule = REPEAT_SAME_DAY
            }
            checkRepetitionRuleText()
        }
    }

    private fun updateStartTexts() {
        updateStartDateText()
        updateStartTimeText()
    }

    private fun updateStartDateText() {
        task_start_date.text = Formatter.getDate(this, mTaskStartDateTime)
        checkStartEndValidity()
    }

    private fun updateStartTimeText() {
        task_start_time.text = Formatter.getTime(this, mTaskStartDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTexts() {
        updateEndDateText()
        updateEndTimeText()
    }

    private fun updateEndDateText() {
        task_end_date.text = Formatter.getDate(this, mTaskEndDateTime)
        checkStartEndValidity()
    }

    private fun updateEndTimeText() {
        task_end_time.text = Formatter.getTime(this, mTaskEndDateTime)
        checkStartEndValidity()
    }

    private fun checkStartEndValidity() {
        val textColor = if (mTaskStartDateTime.isAfter(mTaskEndDateTime)) resources.getColor(R.color.red_text) else getProperTextColor()
        task_end_date.setTextColor(textColor)
        task_end_time.setTextColor(textColor)
    }

    private fun resetTime() {
        if (mTaskEndDateTime.isBefore(mTaskStartDateTime) &&
            mTaskStartDateTime.dayOfMonth() == mTaskEndDateTime.dayOfMonth() &&
            mTaskStartDateTime.monthOfYear() == mTaskEndDateTime.monthOfYear()
        ) {

            mTaskEndDateTime =
                mTaskEndDateTime.withTime(
                    mTaskStartDateTime.hourOfDay,
                    mTaskStartDateTime.minuteOfHour,
                    mTaskStartDateTime.secondOfMinute,
                    0
                )
            updateEndTimeText()
            checkStartEndValidity()
        }
    }

    private fun toggleAllDay(isChecked: Boolean) {
        hideKeyboard()
        task_start_time.beGoneIf(isChecked)
        task_end_time.beGoneIf(isChecked)
        resetTime()
    }

    private fun toggleTarget(isChecked: Boolean) {
        hideKeyboard()
        task_target.isChecked = isChecked
    }

    private fun toggleChecklist(isChecked: Boolean) {
        hideKeyboard()
        task_checklist.isChecked = isChecked
        recycle_checklist.beVisibleIf(isChecked)
        checklist_complete.beVisibleIf(isChecked)
    }

    private fun updateReminderTexts() {
        updateReminder1Text()
        updateReminder2Text()
    }

    private fun updateReminder1Text() {
        task_reminder_1.text = getFormattedMinutes(mReminder1Minutes)
    }

    private fun updateReminder2Text() {
        task_reminder_2.apply {
            beGoneIf(task_reminder_2.isGone() && mReminder1Minutes == REMINDER_OFF)
            if (mReminder2Minutes == REMINDER_OFF) {
                text = resources.getString(R.string.add_another_reminder)
                alpha = 0.4f
            } else {
                text = getFormattedMinutes(mReminder2Minutes)
                alpha = 1f
            }
        }
    }

    private fun showReminder1Dialog() {
        showPickSecondsDialogHelper(mReminder1Minutes) {
            mReminder1Minutes = if (it == -1 || it == 0) it else it / 60
            updateReminderTexts()
        }
    }

    private fun showReminder2Dialog() {
        showPickSecondsDialogHelper(mReminder2Minutes) {
            mReminder2Minutes = if (it == -1 || it == 0) it else it / 60
            updateReminderTexts()
        }
    }

    private fun getStartEndTimes(): Pair<Long, Long> {
        return if (mTask.getIsAllDay()) {
            val newStartTS = mTaskStartDateTime.withTimeAtStartOfDay().seconds()
            val newEndTS = mTaskEndDateTime.withTimeAtStartOfDay().withHourOfDay(12).seconds()
            Pair(newStartTS, newEndTS)
        } else {
            val newStartTS = mTaskStartDateTime.seconds()
            val newEndTS = mTaskEndDateTime.seconds()
            Pair(newStartTS, newEndTS)
        }
    }

    private fun showEventTypeDialog() {
        hideKeyboard()
        EditEventTypeDialog(activity = this) {
            mEventTypeId = it.id!!
            updateEventType()
        }
    }

    private fun updateEventType() {
        ensureBackgroundThread {
            val eventType = eventTypesDB.getEventTypeWithId(mEventTypeId)
            if (eventType != null) {
                runOnUiThread {
                    task_type_color.setFillWithStroke(eventType.color, getProperBackgroundColor())
                }
            }
        }
    }

    private fun getReminders(): ArrayList<Reminder> {
        var reminders = arrayListOf(
            Reminder(mReminder1Minutes, mReminder1Type),
            Reminder(mReminder2Minutes, mReminder2Type),
            Reminder(mReminder3Minutes, mReminder3Type)
        )
        reminders = reminders.filter { it.minutes != REMINDER_OFF }.sortedBy { it.minutes }
            .toMutableList() as ArrayList<Reminder>
        return reminders
    }

    private fun updateColors() {
        updateTextColors(task_scrollview)
        val textColor = getProperTextColor()
        arrayOf(
            task_time_image, task_reminder_image, task_type_image, task_repetition_image, task_start_date_image, task_end_date_image,
            task_target_image
        ).forEach {
            it.applyColorFilter(textColor)
        }
    }

    private fun showRepeatIntervalDialog() {
        showEventRepeatIntervalDialog(mRepeatInterval) {
            setRepeatInterval(it)
        }
    }

    private fun setRepeatInterval(interval: Int) {
        mRepeatInterval = interval
        updateRepetitionText()
        checkRepeatTexts(interval)

        when {
            mRepeatInterval.isXWeeklyRepetition() -> setRepeatRule(
                2.0.pow((mTaskStartDateTime.dayOfWeek - 1).toDouble()).toInt()
            )
            mRepeatInterval.isXMonthlyRepetition() -> setRepeatRule(REPEAT_SAME_DAY)
            mRepeatInterval.isXYearlyRepetition() -> setRepeatRule(REPEAT_SAME_DAY)
        }
    }

    private fun checkRepeatTexts(limit: Int) {
        task_repetition_limit_holder.beGoneIf(limit == 0)
        checkRepetitionLimitText()

        task_repetition_rule_holder.beVisibleIf(mRepeatInterval.isXWeeklyRepetition() || mRepeatInterval.isXMonthlyRepetition() || mRepeatInterval.isXYearlyRepetition())
        checkRepetitionRuleText()
    }

    private fun showRepetitionTypePicker() {
        hideKeyboard()
        RepeatLimitTypePickerDialog(this, mRepeatLimit, mTaskStartDateTime.seconds()) {
            setRepeatLimit(it)
        }
    }

    private fun setRepeatLimit(limit: Long) {
        mRepeatLimit = limit
        checkRepetitionLimitText()
    }

    private fun checkRepetitionLimitText() {
        task_repetition_limit.text = when {
            mRepeatLimit == 0L -> {
                task_repetition_limit_label.text = getString(R.string.repeat)
                resources.getString(R.string.forever)
            }
            mRepeatLimit > 0 -> {
                task_repetition_limit_label.text = getString(R.string.repeat_till)
                val repeatLimitDateTime = Formatter.getDateTimeFromTS(mRepeatLimit)
                Formatter.getFullDate(this, repeatLimitDateTime)
            }
            else -> {
                task_repetition_limit_label.text = getString(R.string.repeat)
                "${-mRepeatLimit} ${getString(R.string.times)}"
            }
        }
    }

    private fun showRepetitionRuleDialog() {
        hideKeyboard()
        when {
            mRepeatInterval.isXWeeklyRepetition() -> RepeatRuleWeeklyDialog(this, mRepeatRule) {
                setRepeatRule(it)
            }
            mRepeatInterval.isXMonthlyRepetition() -> {
                val items = getAvailableMonthlyRepetitionRules()
                RadioGroupDialog(this, items, mRepeatRule) {
                    setRepeatRule(it as Int)
                }
            }
            mRepeatInterval.isXYearlyRepetition() -> {
                val items = getAvailableYearlyRepetitionRules()
                RadioGroupDialog(this, items, mRepeatRule) {
                    setRepeatRule(it as Int)
                }
            }
        }
    }

    private fun getAvailableMonthlyRepetitionRules(): ArrayList<RadioItem> {
        val items = arrayListOf(
            RadioItem(
                REPEAT_SAME_DAY,
                getString(R.string.repeat_on_the_same_day_monthly)
            )
        )

        items.add(
            RadioItem(
                REPEAT_ORDER_WEEKDAY,
                getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY)
            )
        )
        if (isLastWeekDayOfMonth()) {
            items.add(
                RadioItem(
                    REPEAT_ORDER_WEEKDAY_USE_LAST,
                    getRepeatXthDayString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)
                )
            )
        }

        if (isLastDayOfTheMonth()) {
            items.add(
                RadioItem(
                    REPEAT_LAST_DAY,
                    getString(R.string.repeat_on_the_last_day_monthly)
                )
            )
        }
        return items
    }

    private fun getAvailableYearlyRepetitionRules(): ArrayList<RadioItem> {
        val items = arrayListOf(
            RadioItem(
                REPEAT_SAME_DAY,
                getString(R.string.repeat_on_the_same_day_yearly)
            )
        )

        items.add(
            RadioItem(
                REPEAT_ORDER_WEEKDAY,
                getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY)
            )
        )
        if (isLastWeekDayOfMonth()) {
            items.add(
                RadioItem(
                    REPEAT_ORDER_WEEKDAY_USE_LAST,
                    getRepeatXthDayInMonthString(true, REPEAT_ORDER_WEEKDAY_USE_LAST)
                )
            )
        }

        return items
    }

    private fun isLastDayOfTheMonth() = mTaskStartDateTime.dayOfMonth == mTaskStartDateTime.dayOfMonth().withMaximumValue().dayOfMonth

    private fun isLastWeekDayOfMonth() = mTaskStartDateTime.monthOfYear != mTaskStartDateTime.plusDays(7).monthOfYear

    private fun getRepeatXthDayString(includeBase: Boolean, repeatRule: Int): String {
        val dayOfWeek = mTaskStartDateTime.dayOfWeek
        val base = getBaseString(dayOfWeek)
        val order = getOrderString(repeatRule)
        val dayString = getDayString(dayOfWeek)
        return if (includeBase) {
            "$base $order $dayString"
        } else {
            val everyString = getString(if (isMaleGender(mTaskStartDateTime.dayOfWeek)) R.string.every_m else R.string.every_f)
            "$everyString $order $dayString"
        }
    }

    private fun getBaseString(day: Int): String {
        return getString(
            if (isMaleGender(day)) {
                R.string.repeat_every_m
            } else {
                R.string.repeat_every_f
            }
        )
    }

    private fun isMaleGender(day: Int) = day == 1 || day == 2 || day == 4 || day == 5

    private fun getOrderString(repeatRule: Int): String {
        val dayOfMonth = mTaskStartDateTime.dayOfMonth
        var order = (dayOfMonth - 1) / 7 + 1
        if (isLastWeekDayOfMonth() && repeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST) {
            order = -1
        }

        val isMale = isMaleGender(mTaskStartDateTime.dayOfWeek)
        return getString(
            when (order) {
                1 -> if (isMale) R.string.first_m else R.string.first_f
                2 -> if (isMale) R.string.second_m else R.string.second_f
                3 -> if (isMale) R.string.third_m else R.string.third_f
                4 -> if (isMale) R.string.fourth_m else R.string.fourth_f
                5 -> if (isMale) R.string.fifth_m else R.string.fifth_f
                else -> if (isMale) R.string.last_m else R.string.last_f
            }
        )
    }

    private fun getDayString(day: Int): String {
        return getString(
            when (day) {
                1 -> R.string.monday_alt
                2 -> R.string.tuesday_alt
                3 -> R.string.wednesday_alt
                4 -> R.string.thursday_alt
                5 -> R.string.friday_alt
                6 -> R.string.saturday_alt
                else -> R.string.sunday_alt
            }
        )
    }

    private fun getRepeatXthDayInMonthString(includeBase: Boolean, repeatRule: Int): String {
        val weekDayString = getRepeatXthDayString(includeBase, repeatRule)
        val monthString = resources.getStringArray(R.array.in_months)[mTaskStartDateTime.monthOfYear - 1]
        return "$weekDayString $monthString"
    }

    private fun setRepeatRule(rule: Int) {
        mRepeatRule = rule
        checkRepetitionRuleText()
        if (rule == 0) {
            setRepeatInterval(0)
        }
    }

    private fun checkRepetitionRuleText() {
        when {
            mRepeatInterval.isXWeeklyRepetition() -> {
                task_repetition_rule.text =
                    if (mRepeatRule == EVERY_DAY_BIT) getString(R.string.every_day) else getSelectedDaysString(
                        mRepeatRule
                    )
            }
            mRepeatInterval.isXMonthlyRepetition() -> {
                val repeatString =
                    if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                        R.string.repeat else R.string.repeat_on

                task_repetition_rule_label.text = getString(repeatString)
                task_repetition_rule.text = getMonthlyRepetitionRuleText()
            }
            mRepeatInterval.isXYearlyRepetition() -> {
                val repeatString =
                    if (mRepeatRule == REPEAT_ORDER_WEEKDAY_USE_LAST || mRepeatRule == REPEAT_ORDER_WEEKDAY)
                        R.string.repeat else R.string.repeat_on

                task_repetition_rule_label.text = getString(repeatString)
                task_repetition_rule.text = getYearlyRepetitionRuleText()
            }
        }
    }

    private fun getMonthlyRepetitionRuleText() = when (mRepeatRule) {
        REPEAT_SAME_DAY -> getString(R.string.the_same_day)
        REPEAT_LAST_DAY -> getString(R.string.the_last_day)
        else -> getRepeatXthDayString(false, mRepeatRule)
    }

    private fun getYearlyRepetitionRuleText() = when (mRepeatRule) {
        REPEAT_SAME_DAY -> getString(R.string.the_same_day)
        else -> getRepeatXthDayInMonthString(false, mRepeatRule)
    }

    private fun updateRepetitionText() {
        task_repetition.text = getRepetitionText(mRepeatInterval)
    }

    private fun updateActionBarTitle() {
        task_toolbar.title = if (mIsNewTask) {
            getString(R.string.new_task)
        } else {
            getString(R.string.edit_task)
        }
    }
}
