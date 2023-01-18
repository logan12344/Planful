package com.production.planful.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import com.production.planful.R
import com.production.planful.commons.dialogs.RadioGroupDialog
import com.production.planful.commons.dialogs.SelectAlarmSoundDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.*
import com.production.planful.commons.models.AlarmSound
import com.production.planful.commons.models.RadioItem
import com.production.planful.extensions.config
import com.production.planful.extensions.eventsHelper
import com.production.planful.extensions.updateWidgets
import com.production.planful.helpers.*
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.InputStream

class SettingsActivity : SimpleActivity() {
    private val GET_RINGTONE_URI = 1
    private val PICK_IMPORT_SOURCE_INTENT = 2

    private var mStoredPrimaryColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        mStoredPrimaryColor = getProperPrimaryColor()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(settings_toolbar, NavigationIcon.Arrow)
        setupSettingItems()
    }

    private fun setupSettingItems() {
        setupCustomizeColors()
        setupCustomizeNotifications()
        setupSundayFirst()
        setupRunInBackground()
        setupVibrate()
        setupReminderSound()
        setupReminderAudioStream()
        setupUseSameSnooze()
        setupLoopReminders()
        setupSnoozeTime()
        setupCustomizeWidgetColors()
        updateTextColors(settings_holder)
        checkPrimaryColor()

        arrayOf(
            settings_color_customization_label,
            settings_general_settings_label,
            settings_reminders_label
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }

        arrayOf(
            settings_color_customization_holder,
            settings_general_settings_holder,
            settings_reminders_holder
        ).forEach {
            it.background.applyColorFilter(getProperBackgroundColor().getContrastColor())
        }
    }

    override fun onPause() {
        super.onPause()
        mStoredPrimaryColor = getProperPrimaryColor()
    }

    override fun onStop() {
        super.onStop()
        val reminders = sortedSetOf(
            config.defaultReminder1,
            config.defaultReminder2,
            config.defaultReminder3
        ).filter { it != REMINDER_OFF }
        config.defaultReminder1 = reminders.getOrElse(0) { REMINDER_OFF }
        config.defaultReminder2 = reminders.getOrElse(1) { REMINDER_OFF }
        config.defaultReminder3 = reminders.getOrElse(2) { REMINDER_OFF }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == GET_RINGTONE_URI && resultCode == RESULT_OK && resultData != null) {
            val newAlarmSound = storeNewYourAlarmSound(resultData)
            updateReminderSound(newAlarmSound)
        } else if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        }
    }

    private fun checkPrimaryColor() {
        if (getProperPrimaryColor() != mStoredPrimaryColor) {
            ensureBackgroundThread {
                val eventTypes = eventsHelper.getEventTypesSync()
                if (eventTypes.filter { it.caldavCalendarId == 0 }.size == 1) {
                    val eventType = eventTypes.first { it.caldavCalendarId == 0 }
                    eventType.color = getProperPrimaryColor()
                    eventsHelper.insertOrUpdateEventTypeSync(eventType)
                }
            }
        }
    }

    private fun setupRunInBackground() {
        run_in_background.isChecked = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
        run_in_background_holder.setOnClickListener {
            allowBattery()
            run_in_background.toggle()
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupCustomizeNotifications() {
        settings_customize_notifications_holder.beVisibleIf(isOreoPlus())

        if (settings_customize_notifications_holder.isGone()) {
            settings_reminder_sound_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        settings_customize_notifications_holder.setOnClickListener {
            launchCustomizeNotificationsIntent()
        }
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = config.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            config.isSundayFirst = settings_sunday_first.isChecked
        }
    }

    private fun setupReminderSound() {
        settings_reminder_sound_holder.beGoneIf(isOreoPlus())
        settings_reminder_sound.text = config.reminderSoundTitle

        settings_reminder_sound_holder.setOnClickListener {
            SelectAlarmSoundDialog(this,
                config.reminderSoundUri,
                config.reminderAudioStream,
                GET_RINGTONE_URI,
                RingtoneManager.TYPE_NOTIFICATION,
                false,
                onAlarmPicked = {
                    if (it != null) {
                        updateReminderSound(it)
                    }
                },
                onAlarmSoundDeleted = {
                    if (it.uri == config.reminderSoundUri) {
                        val defaultAlarm = getDefaultAlarmSound(RingtoneManager.TYPE_NOTIFICATION)
                        updateReminderSound(defaultAlarm)
                    }
                })
        }
    }

    private fun updateReminderSound(alarmSound: AlarmSound) {
        config.reminderSoundTitle = alarmSound.title
        config.reminderSoundUri = alarmSound.uri
        settings_reminder_sound.text = alarmSound.title
    }

    private fun setupReminderAudioStream() {
        settings_reminder_audio_stream.text = getAudioStreamText()
        settings_reminder_audio_stream_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_stream)),
                RadioItem(AudioManager.STREAM_SYSTEM, getString(R.string.system_stream)),
                RadioItem(
                    AudioManager.STREAM_NOTIFICATION,
                    getString(R.string.notification_stream)
                ),
                RadioItem(AudioManager.STREAM_RING, getString(R.string.ring_stream))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.reminderAudioStream) {
                config.reminderAudioStream = it as Int
                settings_reminder_audio_stream.text = getAudioStreamText()
            }
        }
    }

    private fun getAudioStreamText() = getString(
        when (config.reminderAudioStream) {
            AudioManager.STREAM_ALARM -> R.string.alarm_stream
            AudioManager.STREAM_SYSTEM -> R.string.system_stream
            AudioManager.STREAM_NOTIFICATION -> R.string.notification_stream
            else -> R.string.ring_stream
        }
    )

    private fun setupVibrate() {
        settings_vibrate.isChecked = config.vibrateOnReminder
        settings_vibrate_holder.setOnClickListener {
            settings_vibrate.toggle()
            config.vibrateOnReminder = settings_vibrate.isChecked
        }
    }

    private fun setupLoopReminders() {
        settings_loop_reminders.isChecked = config.loopReminders
        settings_loop_reminders_holder.setOnClickListener {
            settings_loop_reminders.toggle()
            config.loopReminders = settings_loop_reminders.isChecked
        }
    }

    private fun setupUseSameSnooze() {
        settings_snooze_time_holder.beVisibleIf(config.useSameSnooze)
        settings_use_same_snooze.isChecked = config.useSameSnooze
        setupSnoozeBackgrounds()
        settings_use_same_snooze_holder.setOnClickListener {
            settings_use_same_snooze.toggle()
            config.useSameSnooze = settings_use_same_snooze.isChecked
            settings_snooze_time_holder.beVisibleIf(config.useSameSnooze)
            setupSnoozeBackgrounds()
        }
    }

    private fun setupSnoozeBackgrounds() {
        if (config.useSameSnooze) {
            settings_use_same_snooze_holder.background =
                resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            settings_use_same_snooze_holder.background =
                resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeTime()
        settings_snooze_time_holder.setOnClickListener {
            showPickSecondsDialogHelper(config.snoozeTime, true) {
                config.snoozeTime = it / 60
                updateSnoozeTime()
            }
        }
    }

    private fun updateSnoozeTime() {
        settings_snooze_time.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun setupCustomizeWidgetColors() {
        settings_customize_widget_colors_holder.setOnClickListener {
            Intent(this, WidgetListConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun parseFile(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var importedItems = 0
        val configValues = LinkedHashMap<String, Any>()
        inputStream.bufferedReader().use {
            while (true) {
                try {
                    val line = it.readLine() ?: break
                    val split = line.split("=".toRegex(), 2)
                    if (split.size == 2) {
                        configValues[split[0]] = split[1]
                    }
                    importedItems++
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }

        for ((key, value) in configValues) {
            when (key) {
                IS_USING_SHARED_THEME -> config.isUsingSharedTheme = value.toBoolean()
                TEXT_COLOR -> config.textColor = value.toInt()
                BACKGROUND_COLOR -> config.backgroundColor = value.toInt()
                PRIMARY_COLOR -> config.primaryColor = value.toInt()
                ACCENT_COLOR -> config.accentColor = value.toInt()
                APP_ICON_COLOR -> {
                    if (getAppIconColors().contains(value.toInt())) {
                        config.appIconColor = value.toInt()
                        checkAppIconColor()
                    }
                }
                USE_ENGLISH -> config.useEnglish = value.toBoolean()
                WAS_USE_ENGLISH_TOGGLED -> config.wasUseEnglishToggled = value.toBoolean()
                WIDGET_BG_COLOR -> config.widgetBgColor = value.toInt()
                WIDGET_TEXT_COLOR -> config.widgetTextColor = value.toInt()
                WEEK_NUMBERS -> config.showWeekNumbers = value.toBoolean()
                START_WEEKLY_AT -> config.startWeeklyAt = value.toInt()
                VIBRATE -> config.vibrateOnReminder = value.toBoolean()
                LAST_EVENT_REMINDER_MINUTES -> config.lastEventReminderMinutes1 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_2 -> config.lastEventReminderMinutes2 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_3 -> config.lastEventReminderMinutes3 = value.toInt()
                DISPLAY_PAST_EVENTS -> config.displayPastEvents = value.toInt()
                FONT_SIZE -> config.fontSize = value.toInt()
                LIST_WIDGET_VIEW_TO_OPEN -> config.listWidgetViewToOpen = value.toInt()
                REMINDER_AUDIO_STREAM -> config.reminderAudioStream = value.toInt()
                DISPLAY_DESCRIPTION -> config.displayDescription = value.toBoolean()
                REPLACE_DESCRIPTION -> config.replaceDescription = value.toBoolean()
                SHOW_GRID -> config.showGrid = value.toBoolean()
                LOOP_REMINDERS -> config.loopReminders = value.toBoolean()
                DIM_PAST_EVENTS -> config.dimPastEvents = value.toBoolean()
                DIM_COMPLETED_TASKS -> config.dimCompletedTasks = value.toBoolean()
                ALLOW_CHANGING_TIME_ZONES -> config.allowChangingTimeZones = value.toBoolean()
                USE_PREVIOUS_EVENT_REMINDERS -> config.usePreviousEventReminders = value.toBoolean()
                DEFAULT_REMINDER_1 -> config.defaultReminder1 = value.toInt()
                DEFAULT_REMINDER_2 -> config.defaultReminder2 = value.toInt()
                DEFAULT_REMINDER_3 -> config.defaultReminder3 = value.toInt()
                PULL_TO_REFRESH -> config.pullToRefresh = value.toBoolean()
                DEFAULT_START_TIME -> config.defaultStartTime = value.toInt()
                DEFAULT_DURATION -> config.defaultDuration = value.toInt()
                USE_SAME_SNOOZE -> config.useSameSnooze = value.toBoolean()
                SNOOZE_TIME -> config.snoozeTime = value.toInt()
                USE_24_HOUR_FORMAT -> config.use24HourFormat = value.toBoolean()
                SUNDAY_FIRST -> config.isSundayFirst = value.toBoolean()
                HIGHLIGHT_WEEKENDS -> config.highlightWeekends = value.toBoolean()
                HIGHLIGHT_WEEKENDS_COLOR -> config.highlightWeekendsColor = value.toInt()
                ALLOW_CREATING_TASKS -> config.allowCreatingTasks = value.toBoolean()
            }
        }

        runOnUiThread {
            val msg = if (configValues.size > 0) R.string.settings_imported_successfully else R.string.no_entries_for_importing
            toast(msg)

            setupSettingItems()
            updateWidgets()
        }
    }

    @SuppressLint("BatteryLife")
    private fun allowBattery() {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", packageName, null)
            try {
                startActivity(this)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}
