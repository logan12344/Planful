package com.production.planful.activities

import android.os.Bundle
import android.view.View
import com.production.planful.R
import com.production.planful.adapters.StatisticsEventsAdapter
import com.production.planful.commons.activities.BaseSimpleActivity
import com.production.planful.commons.extensions.areSystemAnimationsEnabled
import com.production.planful.commons.extensions.getProperPrimaryColor
import com.production.planful.commons.extensions.getProperTextColor
import com.production.planful.commons.extensions.updateTextColors
import com.production.planful.commons.helpers.APP_LAUNCHER_NAME
import com.production.planful.commons.helpers.NavigationIcon
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.extensions.eventsHelper
import com.production.planful.extensions.seconds
import com.production.planful.helpers.Formatter
import com.production.planful.models.Event
import com.production.planful.models.TrackTargetItem
import kotlinx.android.synthetic.main.activity_about.about_nested_scrollview
import kotlinx.android.synthetic.main.activity_statistics.*
import kotlinx.android.synthetic.main.fragment_day.view.*

class StatisticsActivity : BaseSimpleActivity() {
    private var primaryColor = 0
    private var lastHash = 0

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        val textColor = getProperTextColor()
        primaryColor = getProperPrimaryColor()

        no_data.tvNoData1.setTextColor(textColor)
        no_data.tvNoData2.setTextColor(textColor)
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(about_nested_scrollview)
        setupToolbar(statistics_toolbar, NavigationIcon.Arrow)

        ensureBackgroundThread {
            val startDateTime = Formatter.getLocalDateTimeFromCode(Formatter.getTodayCode()).minusWeeks(1)
            val endDateTime = startDateTime.plusWeeks(7)
            eventsHelper.getEvents(startDateTime.seconds(), endDateTime.seconds()) { events ->
                receivedEvents(events)
            }
        }
    }

    private fun receivedEvents(events: ArrayList<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash) {
            return
        }
        lastHash = newHash

        val sorted = ArrayList<Event>()
        for (event in events) {
            if (event.trackTarget) sorted.add(event)
        }

        val sortedTarget = ArrayList<Pair<TrackTargetItem, Event>>()
        var daysCount = 0
        var daysDone = 0

        for (i in 0 until sorted.size) {
            if (i + 1 >= sorted.size) {
                daysCount++
                if (sorted[i].isTaskCompleted()) daysDone++
                sortedTarget.add(Pair(TrackTargetItem(daysCount, daysDone), sorted[i]))
                break
            }
            if (sorted[i].id == sorted[i+1].id) {
                daysCount++
                if (sorted[i].isTaskCompleted()) daysDone++
            } else {
                daysCount++
                if (sorted[i].isTaskCompleted()) daysDone++
                sortedTarget.add(Pair(TrackTargetItem(daysCount, daysDone), sorted[i]))
                daysCount = 0
                daysDone = 0
            }
        }

        runOnUiThread {
            updateEvents(sortedTarget)
        }
    }

    private fun updateEvents(events: ArrayList<Pair<TrackTargetItem, Event>>) {
        if (events.size > 0) {
            no_data.visibility = View.GONE
            day_events.visibility = View.VISIBLE

            day_events.adapter = StatisticsEventsAdapter(this, events)

            if (areSystemAnimationsEnabled) {
                day_events.scheduleLayoutAnimation()
            }
        }
    }
}