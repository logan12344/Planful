package com.production.planful.activities

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.ContactsContract.*
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.production.planful.BuildConfig
import com.production.planful.R
import com.production.planful.adapters.EventListAdapter
import com.production.planful.commons.dialogs.RadioGroupDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.*
import com.production.planful.commons.interfaces.RefreshRecyclerViewListener
import com.production.planful.commons.models.RadioItem
import com.production.planful.databases.EventsDatabase
import com.production.planful.dialogs.ReminderWarningDialog
import com.production.planful.extensions.*
import com.production.planful.fragments.*
import com.production.planful.helpers.*
import com.production.planful.helpers.Formatter
import com.production.planful.jobs.CalDAVUpdateListener
import com.production.planful.models.ListEvent
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import java.util.*

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private var showCalDAVRefreshToast = false
    private var mShouldFilterBeVisible = false
    private var mIsSearchOpen = false
    private var mLatestSearchQuery = ""
    private var mSearchMenuItem: MenuItem? = null
    private var shouldGoToTodayBeVisible = false
    private var goToTodayButton: MenuItem? = null
    private var currentFragments = ArrayList<MyFragmentHolder>()

    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredPrimaryColor = 0
    private var mStoredDayCode = ""
    private var mStoredIsSundayFirst = false
    private var mStoredMidnightSpan = true
    private var mStoredUse24HourFormat = false
    private var mStoredDimPastEvents = true
    private var mStoredDimCompletedTasks = true
    private var mStoredHighlightWeekends = false
    private var mStoredStartWeekWithCurrentDay = false
    private var mStoredHighlightWeekendsColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)
        setupOptionsMenu()
        refreshMenuItems()

        calendar_fab.beVisibleIf(config.storedView != YEARLY_VIEW && config.storedView != WEEKLY_VIEW)
        calendar_fab.setOnClickListener {
            openNewTask()
        }
        settings_fab.setOnClickListener { launchSettings() }

        storeStateVariables()

        if (!hasPermission(PERMISSION_WRITE_CALENDAR) || !hasPermission(PERMISSION_READ_CALENDAR)) {
            config.caldavSync = false
        }

        if (config.caldavSync) {
            refreshCalDAVCalendars(false)
        }

        swipe_refresh_layout.setOnRefreshListener {
            refreshCalDAVCalendars(true)
        }

        checkIsViewIntent()

        if (!checkIsOpenIntent()) {
            updateViewPager()
        }

        checkAppOnSDCard()

        if (savedInstanceState == null) {
            checkCalDAVUpdateListener()
        }

        if (!config.wasBatteryDisclaimerShown) {
            ReminderWarningDialog(this) {
                config.wasBatteryDisclaimerShown = true
                /*if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
                    allowBattery()
                }*/
            }
        }
        /*if (config.wasBatteryDisclaimerShown) {
            if (!(getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)) {
                allowBattery()
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        if (mStoredTextColor != getProperTextColor() || mStoredBackgroundColor != getProperBackgroundColor() || mStoredPrimaryColor != getProperPrimaryColor()
            || mStoredDayCode != Formatter.getTodayCode() || mStoredDimPastEvents != config.dimPastEvents || mStoredDimCompletedTasks != config.dimCompletedTasks
            || mStoredHighlightWeekends != config.highlightWeekends || mStoredHighlightWeekendsColor != config.highlightWeekendsColor
        ) {
            updateViewPager()
        }

        eventsHelper.getEventTypes(this, false) {
            val newShouldFilterBeVisible = it.size > 1 || config.displayEventTypes.isEmpty()
            if (newShouldFilterBeVisible != mShouldFilterBeVisible) {
                mShouldFilterBeVisible = newShouldFilterBeVisible
                refreshMenuItems()
            }
        }

        if (config.storedView == WEEKLY_VIEW) {
            if (mStoredIsSundayFirst != config.isSundayFirst || mStoredUse24HourFormat != config.use24HourFormat
                || mStoredMidnightSpan != config.showMidnightSpanningEventsAtTop || mStoredStartWeekWithCurrentDay != config.startWeekWithCurrentDay
            ) {
                updateViewPager()
            }
        }

        storeStateVariables()
        updateWidgets()
        updateTextColors(calendar_coordinator)

        search_holder.background = ColorDrawable(getProperBackgroundColor())
        checkSwipeRefreshAvailability()
        checkShortcuts()

        setupToolbar(main_toolbar, searchMenuItem = mSearchMenuItem)
        if (!mIsSearchOpen) {
            refreshMenuItems()
        }

        main_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        if (config.caldavSync) {
            updateCalDAVEvents()
        }
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            EventsDatabase.destroyInstance()
            stopCalDAVUpdateListener()
        }
    }

    fun refreshMenuItems() {
        shouldGoToTodayBeVisible =
            currentFragments.lastOrNull()?.shouldGoToTodayBeVisible() ?: false
        main_toolbar.menu.apply {
            goToTodayButton = findItem(R.id.go_to_today)
            findItem(R.id.print).isVisible = config.storedView != MONTHLY_DAILY_VIEW
            findItem(R.id.go_to_today).isVisible = shouldGoToTodayBeVisible && !mIsSearchOpen
            findItem(R.id.go_to_date).isVisible = config.storedView != EVENTS_LIST_VIEW
        }
    }

    private fun setupOptionsMenu() {
        setupSearch(main_toolbar.menu)
        main_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.share -> shareApp()
                R.id.change_view -> showViewDialog()
                R.id.go_to_today -> goToToday()
                R.id.go_to_date -> showGoToDateDialog()
                R.id.print -> printView()
                R.id.about -> startAboutActivity(R.string.app_name, BuildConfig.VERSION_NAME)
                R.id.statistics -> startStatisticsActivity()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onBackPressed() {
        if (mIsSearchOpen) {
            closeSearch()
        } else {
            swipe_refresh_layout.isRefreshing = false
            checkSwipeRefreshAvailability()
            when {
                currentFragments.size > 1 -> removeTopFragment()
                else -> super.onBackPressed()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIsOpenIntent()
        checkIsViewIntent()
    }

    private fun storeStateVariables() {
        mStoredTextColor = getProperTextColor()
        mStoredPrimaryColor = getProperPrimaryColor()
        mStoredBackgroundColor = getProperBackgroundColor()
        config.apply {
            mStoredIsSundayFirst = isSundayFirst
            mStoredUse24HourFormat = use24HourFormat
            mStoredDimPastEvents = dimPastEvents
            mStoredDimCompletedTasks = dimCompletedTasks
            mStoredHighlightWeekends = highlightWeekends
            mStoredHighlightWeekendsColor = highlightWeekendsColor
            mStoredMidnightSpan = showMidnightSpanningEventsAtTop
            mStoredStartWeekWithCurrentDay = startWeekWithCurrentDay
        }
        mStoredDayCode = Formatter.getTodayCode()
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        mSearchMenuItem = menu.findItem(R.id.search)
        (mSearchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (mIsSearchOpen) {
                        searchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(
            mSearchMenuItem,
            object : MenuItemCompat.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    mIsSearchOpen = true
                    search_holder.beVisible()
                    calendar_fab.beGone()
                    searchQueryChanged("")
                    refreshMenuItems()
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    mIsSearchOpen = false
                    search_holder.beGone()
                    calendar_fab.beVisibleIf(currentFragments.last() !is YearFragmentsHolder && currentFragments.last() !is WeekFragmentsHolder)
                    refreshMenuItems()
                    return true
                }
            })
    }

    private fun closeSearch() {
        mSearchMenuItem?.collapseActionView()
    }

    private fun checkCalDAVUpdateListener() {
        if (isNougatPlus()) {
            val updateListener = CalDAVUpdateListener()
            if (config.caldavSync) {
                if (!updateListener.isScheduled(applicationContext)) {
                    updateListener.scheduleJob(applicationContext)
                }
            } else {
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    private fun stopCalDAVUpdateListener() {
        if (isNougatPlus()) {
            if (!config.caldavSync) {
                val updateListener = CalDAVUpdateListener()
                updateListener.cancelJob(applicationContext)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val newEvent = getNewEventShortcut(appIconColor)
            val shortcuts = arrayListOf(newEvent)

            if (config.allowCreatingTasks) {
                shortcuts.add(getNewTaskShortcut(appIconColor))
            }

            try {
                shortcutManager.dynamicShortcuts = shortcuts
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getNewEventShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_event)
        val newEventDrawable = resources.getDrawable(R.drawable.shortcut_event, theme)
        (newEventDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_event_background)
            .applyColorFilter(appIconColor)
        val newEventBitmap = newEventDrawable.convertToBitmap()

        val newEventIntent = Intent(this, SplashActivity::class.java)
        newEventIntent.action = SHORTCUT_NEW_EVENT
        return ShortcutInfo.Builder(this, "new_event")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(newEventBitmap))
            .setIntent(newEventIntent)
            .build()
    }

    @SuppressLint("NewApi")
    private fun getNewTaskShortcut(appIconColor: Int): ShortcutInfo {
        val newTask = getString(R.string.new_task)
        val newTaskDrawable = resources.getDrawable(R.drawable.shortcut_task, theme)
        (newTaskDrawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_task_background)
            .applyColorFilter(appIconColor)
        val newTaskBitmap = newTaskDrawable.convertToBitmap()
        val newTaskIntent = Intent(this, SplashActivity::class.java)
        newTaskIntent.action = SHORTCUT_NEW_TASK
        return ShortcutInfo.Builder(this, "new_task")
            .setShortLabel(newTask)
            .setLongLabel(newTask)
            .setIcon(Icon.createWithBitmap(newTaskBitmap))
            .setIntent(newTaskIntent)
            .build()
    }

    private fun checkIsOpenIntent(): Boolean {
        val dayCodeToOpen = intent.getStringExtra(DAY_CODE) ?: ""
        val viewToOpen = intent.getIntExtra(VIEW_TO_OPEN, DAILY_VIEW)
        intent.removeExtra(VIEW_TO_OPEN)
        intent.removeExtra(DAY_CODE)
        if (dayCodeToOpen.isNotEmpty()) {
            calendar_fab.beVisible()
            if (viewToOpen != LAST_VIEW) {
                config.storedView = viewToOpen
            }
            updateViewPager(dayCodeToOpen)
            return true
        }

        val eventIdToOpen = intent.getLongExtra(EVENT_ID, 0L)
        val eventOccurrenceToOpen = intent.getLongExtra(EVENT_OCCURRENCE_TS, 0L)
        intent.removeExtra(EVENT_ID)
        intent.removeExtra(EVENT_OCCURRENCE_TS)
        if (eventIdToOpen != 0L && eventOccurrenceToOpen != 0L) {
            hideKeyboard()
            Intent(this, TaskActivity::class.java).apply {
                putExtra(EVENT_ID, eventIdToOpen)
                putExtra(EVENT_OCCURRENCE_TS, eventOccurrenceToOpen)
                startActivity(this)
            }
        }

        return false
    }

    private fun checkIsViewIntent() {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data
            if (uri?.authority?.equals("com.android.calendar") == true || uri?.authority?.substringAfter(
                    "@"
                ) == "com.android.calendar"
            ) {
                if (uri.path!!.startsWith("/events")) {
                    ensureBackgroundThread {
                        // intents like content://com.android.calendar/events/1756
                        val eventId = uri.lastPathSegment
                        val id = eventsDB.getEventIdWithLastImportId("%-$eventId")
                        if (id != null) {
                            hideKeyboard()
                            Intent(this, TaskActivity::class.java).apply {
                                putExtra(EVENT_ID, id)
                                startActivity(this)
                            }
                        } else {
                            toast(R.string.caldav_event_not_found, Toast.LENGTH_LONG)
                        }
                    }
                } else if (uri.path!!.startsWith("/time") || intent?.extras?.getBoolean(
                        "DETAIL_VIEW",
                        false
                    ) == true
                ) {
                    // clicking date on a third party widget: content://com.android.calendar/time/1507309245683
                    // or content://0@com.android.calendar/time/1584958526435
                    val timestamp = uri.pathSegments.last()
                    if (timestamp.areDigitsOnly()) {
                        openDayAt(timestamp.toLong())
                        return
                    }
                }
            }
        }
    }

    private fun showViewDialog() {
        val items = arrayListOf(
            RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
            RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
            RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view))
        )

        RadioGroupDialog(this, items, config.storedView) {
            resetActionBarTitle()
            closeSearch()
            updateView(it as Int)
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun shareApp() {
        val intend = Intent(Intent.ACTION_SEND)
        intend.type = "text/plain"
        intend.putExtra(Intent.EXTRA_SUBJECT, "Sharing APP")
        intend.putExtra(Intent.EXTRA_TEXT, "Planful, it's a flexible scheduler and handy tracker, you should download it and try it out!\nhttps://play.google.com/store/apps/details?id=com.production.planful.release")
        startActivity(Intent.createChooser(intend, "Share URL"))
    }

    private fun goToToday() {
        currentFragments.last().goToToday()
    }

    fun showGoToDateDialog() {
        currentFragments.last().showGoToDateDialog()
    }

    private fun printView() {
        currentFragments.last().printView()
    }

    private fun resetActionBarTitle() {
        main_toolbar.title = getString(R.string.app_name)
        main_toolbar.subtitle = ""
    }

    fun updateTitle(text: String) {
        main_toolbar.title = text
    }

    fun updateSubtitle(text: String) {
        main_toolbar.subtitle = text
    }

    fun toggleGoToTodayVisibility(beVisible: Boolean) {
        shouldGoToTodayBeVisible = beVisible
        if (goToTodayButton?.isVisible != beVisible) {
            refreshMenuItems()
        }
    }

    private fun updateCalDAVEvents() {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(showToasts = false, scheduleNextSync = true) {
                refreshViewPager()
            }
        }
    }

    private fun refreshCalDAVCalendars(showRefreshToast: Boolean) {
        showCalDAVRefreshToast = showRefreshToast
        if (showRefreshToast) {
            toast(R.string.refreshing)
        }
        updateCalDAVEvents()
        syncCalDAVCalendars {
            calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                calDAVChanged()
            }
        }
    }

    private fun calDAVChanged() {
        refreshViewPager()
        if (showCalDAVRefreshToast) {
            toast(R.string.refreshing_complete)
        }
        runOnUiThread {
            swipe_refresh_layout.isRefreshing = false
        }
    }

    private fun updateView(view: Int) {
        calendar_fab.beVisibleIf(view != YEARLY_VIEW && view != WEEKLY_VIEW)
        val dateCode = getDateCodeToDisplay(view)
        config.storedView = view
        checkSwipeRefreshAvailability()
        updateViewPager(dateCode)
        if (goToTodayButton?.isVisible == true) {
            shouldGoToTodayBeVisible = false
            refreshMenuItems()
        }
    }

    private fun getDateCodeToDisplay(newView: Int): String? {
        val fragment = currentFragments.last()
        val currentView = fragment.viewType
        if (newView == EVENTS_LIST_VIEW || currentView == EVENTS_LIST_VIEW) {
            return null
        }

        val fragmentDate = fragment.getCurrentDate()
        val viewOrder = arrayListOf(DAILY_VIEW, WEEKLY_VIEW, MONTHLY_VIEW, YEARLY_VIEW)
        val currentViewIndex =
            viewOrder.indexOf(if (currentView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else currentView)
        val newViewIndex =
            viewOrder.indexOf(if (newView == MONTHLY_DAILY_VIEW) MONTHLY_VIEW else newView)

        return if (fragmentDate != null && currentViewIndex <= newViewIndex) {
            getDateCodeFormatForView(newView, fragmentDate)
        } else {
            getDateCodeFormatForView(newView, DateTime())
        }
    }

    private fun getDateCodeFormatForView(view: Int, date: DateTime): String {
        return when (view) {
            WEEKLY_VIEW -> getDatesWeekDateTime(date)
            YEARLY_VIEW -> date.toString()
            else -> Formatter.getDayCodeFromDateTime(date)
        }
    }

    private fun updateViewPager(dayCode: String? = null) {
        val fragment = getFragmentsHolder()
        currentFragments.forEach {
            try {
                supportFragmentManager.beginTransaction().remove(it).commitNow()
            } catch (ignored: Exception) {
                return
            }
        }

        currentFragments.clear()
        currentFragments.add(fragment)
        val bundle = Bundle()
        val fixedDayCode = fixDayCode(dayCode)

        when (config.storedView) {
            DAILY_VIEW -> bundle.putString(DAY_CODE, fixedDayCode ?: Formatter.getTodayCode())
            WEEKLY_VIEW -> bundle.putString(
                WEEK_START_DATE_TIME,
                fixedDayCode ?: getDatesWeekDateTime(DateTime())
            )
            MONTHLY_VIEW, MONTHLY_DAILY_VIEW -> bundle.putString(
                DAY_CODE,
                fixedDayCode ?: Formatter.getTodayCode()
            )
            YEARLY_VIEW -> bundle.putString(YEAR_TO_OPEN, fixedDayCode)
        }

        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        main_toolbar.navigationIcon = null
    }

    private fun fixDayCode(dayCode: String? = null): String? = when {
        config.storedView == WEEKLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> getDatesWeekDateTime(
            Formatter.getDateTimeFromCode(dayCode)
        )
        config.storedView == YEARLY_VIEW && (dayCode?.length == Formatter.DAYCODE_PATTERN.length) -> Formatter.getYearFromDayCode(
            dayCode
        )
        else -> dayCode
    }

    private fun openNewTask() {
        hideKeyboard()
        val lastFragment = currentFragments.last()
        val allowChangingDay =
            lastFragment !is DayFragmentsHolder && lastFragment !is MonthDayFragmentsHolder
        launchNewTaskIntent(lastFragment.getNewEventDayCode(), allowChangingDay)
    }

    fun openMonthFromYearly(dateTime: DateTime) {
        if (currentFragments.last() is MonthFragmentsHolder) {
            return
        }

        val fragment = MonthFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment).commitNow()
        resetActionBarTitle()
        calendar_fab.beVisible()
        showBackNavigationArrow()
    }

    fun openDayFromMonthly(dateTime: DateTime) {
        if (currentFragments.last() is DayFragmentsHolder) {
            return
        }

        val fragment = DayFragmentsHolder()
        currentFragments.add(fragment)
        val bundle = Bundle()
        bundle.putString(DAY_CODE, Formatter.getDayCodeFromDateTime(dateTime))
        fragment.arguments = bundle
        try {
            supportFragmentManager.beginTransaction().add(R.id.fragments_holder, fragment)
                .commitNow()
            showBackNavigationArrow()
        } catch (_: Exception) {
        }
    }

    private fun getFragmentsHolder() = when (config.storedView) {
        DAILY_VIEW -> DayFragmentsHolder()
        MONTHLY_VIEW -> MonthFragmentsHolder()
        MONTHLY_DAILY_VIEW -> MonthDayFragmentsHolder()
        YEARLY_VIEW -> YearFragmentsHolder()
        else -> WeekFragmentsHolder()
    }

    private fun removeTopFragment() {
        supportFragmentManager.beginTransaction().remove(currentFragments.last()).commit()
        currentFragments.removeAt(currentFragments.size - 1)
        toggleGoToTodayVisibility(currentFragments.last().shouldGoToTodayBeVisible())
        currentFragments.last().apply {
            refreshEvents()
            updateActionBarTitle()
        }
        calendar_fab.beGoneIf(currentFragments.size == 1 && config.storedView == YEARLY_VIEW)
        if (currentFragments.size > 1) {
            showBackNavigationArrow()
        } else {
            main_toolbar.navigationIcon = null
        }
    }

    private fun showBackNavigationArrow() {
        main_toolbar.navigationIcon = resources.getColoredDrawableWithColor(
            R.drawable.ic_arrow_left_vector,
            getProperStatusBarColor().getContrastColor()
        )
    }

    private fun refreshViewPager() {
        runOnUiThread {
            if (!isDestroyed) {
                currentFragments.last().refreshEvents()
            }
        }
    }

    private fun launchSettings() {
        hideKeyboard()
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun searchQueryChanged(text: String) {
        mLatestSearchQuery = text
        search_placeholder_2.beGoneIf(text.length >= 2)
        if (text.length >= 2) {
            eventsHelper.getEventsWithSearchQuery(text, this) { searchedText, events ->
                if (searchedText == mLatestSearchQuery) {
                    search_results_list.beVisibleIf(events.isNotEmpty())
                    search_placeholder.beVisibleIf(events.isEmpty())
                    val listItems = getEventListItems(events)
                    val eventsAdapter = EventListAdapter(this, listItems, true, this, search_results_list) {
                            hideKeyboard()
                            if (it is ListEvent) {
                                Intent(applicationContext, TaskActivity::class.java).apply {
                                    putExtra(EVENT_ID, it.id)
                                    startActivity(this)
                                }
                            }
                        }

                    search_results_list.adapter = eventsAdapter
                }
            }
        } else {
            search_placeholder.beVisible()
            search_results_list.beGone()
        }
    }

    private fun checkSwipeRefreshAvailability() {
        swipe_refresh_layout.isEnabled =
            config.caldavSync && config.pullToRefresh && config.storedView != WEEKLY_VIEW
        if (!swipe_refresh_layout.isEnabled) {
            swipe_refresh_layout.isRefreshing = false
        }
    }

    // only used at active search
    override fun refreshItems() {
        searchQueryChanged(mLatestSearchQuery)
        refreshViewPager()
    }

    private fun openDayAt(timestamp: Long) {
        val dayCode = Formatter.getDayCodeFromTS(timestamp / 1000L)
        calendar_fab.beVisible()
        config.storedView = DAILY_VIEW
        updateViewPager(dayCode)
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
