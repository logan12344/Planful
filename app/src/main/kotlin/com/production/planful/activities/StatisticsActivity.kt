package com.production.planful.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import androidx.core.net.toUri
import com.production.planful.R
import com.production.planful.adapters.DayEventsAdapter
import com.production.planful.adapters.StatisticsEventsAdapter
import com.production.planful.commons.activities.BaseSimpleActivity
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.*
import com.production.planful.extensions.config
import com.production.planful.extensions.eventsHelper
import com.production.planful.helpers.*
import com.production.planful.models.Event
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.activity_about.about_nested_scrollview
import kotlinx.android.synthetic.main.activity_statistics.*
import kotlinx.android.synthetic.main.fragment_day.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*

class StatisticsActivity : BaseSimpleActivity() {
    private var primaryColor = 0
    private var lastHash = 0

    override fun getAppLauncherName() = intent.getStringExtra(APP_LAUNCHER_NAME) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        val textColor = getProperTextColor()
        val backgroundColor = getProperBackgroundColor()
        primaryColor = getProperPrimaryColor()

        no_data.tvNoData1.setTextColor(textColor)
        no_data.tvNoData2.setTextColor(textColor)
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(about_nested_scrollview)
        setupToolbar(statistics_toolbar, NavigationIcon.Arrow)
        ensureBackgroundThread {
            receivedEvents(eventsHelper.getTasksWithTrack(trackTarget = true))
        }
    }

    private fun receivedEvents(events: List<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash) {
            return
        }
        lastHash = newHash

        val replaceDescription = config.replaceDescription
        val sorted = ArrayList(
            events.sortedWith(
                compareBy({ !it.getIsAllDay() },
                    { it.startTS },
                    { it.endTS },
                    { it.title },
                    {
                        if (replaceDescription) it.location else it.description
                    })
            )
        )

        runOnUiThread {
            updateEvents(sorted)
        }
    }

    private fun updateEvents(events: ArrayList<Event>) {
        if (events.size > 0) {
            no_data.visibility = View.GONE
            day_events.visibility = View.VISIBLE

            StatisticsEventsAdapter(this, events, day_events) {
                editEvent(it as Event)
            }.apply {
                day_events.adapter = this
            }

            if (areSystemAnimationsEnabled) {
                day_events.scheduleLayoutAnimation()
            }
        }
    }
    private fun editEvent(event: Event) {
        Intent(this, getActivityToOpen(event.isTask())).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            putExtra(IS_TASK_COMPLETED, event.isTaskCompleted())
            startActivity(this)
        }
    }
}