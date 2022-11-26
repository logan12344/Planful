package com.production.planful.dialogs

import android.text.TextUtils
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.production.planful.R
import com.production.planful.activities.SimpleActivity
import com.production.planful.commons.extensions.beVisibleIf
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.setupDialogStuff
import com.production.planful.commons.views.MyAppCompatCheckbox
import com.production.planful.extensions.calDAVHelper
import com.production.planful.extensions.config
import kotlinx.android.synthetic.main.calendar_item_account.view.*
import kotlinx.android.synthetic.main.calendar_item_calendar.view.*
import kotlinx.android.synthetic.main.dialog_select_calendars.view.*

class SelectCalendarsDialog(val activity: SimpleActivity, val callback: () -> Unit) {
    private var prevAccount = ""
    private var view =
        (activity.layoutInflater.inflate(R.layout.dialog_select_calendars, null) as ViewGroup)

    init {
        val ids = activity.config.getSyncedCalendarIdsAsList()
        val calendars = activity.calDAVHelper.getCalDAVCalendars("", true)
        view.apply {
            dialog_select_calendars_placeholder.beVisibleIf(calendars.isEmpty())
            dialog_select_calendars_holder.beVisibleIf(calendars.isNotEmpty())
        }

        val sorted = calendars.sortedWith(compareBy({ it.accountName }, { it.displayName }))
        sorted.forEach {
            if (prevAccount != it.accountName) {
                prevAccount = it.accountName
                addCalendarItem(false, it.accountName)
            }

            addCalendarItem(true, it.displayName, it.id, ids.contains(it.id))
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmSelection() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.select_caldav_calendars)
            }
    }

    private fun addCalendarItem(
        isEvent: Boolean,
        text: String,
        tag: Int = 0,
        shouldCheck: Boolean = false
    ) {
        val layout =
            if (isEvent) R.layout.calendar_item_calendar else R.layout.calendar_item_account
        val calendarItem =
            activity.layoutInflater.inflate(layout, view.dialog_select_calendars_holder, false)

        if (isEvent) {
            calendarItem.calendar_item_calendar_switch.apply {
                this.tag = tag
                this.text = text
                isChecked = shouldCheck
                calendarItem.setOnClickListener {
                    toggle()
                }
            }
        } else {
            calendarItem.calendar_item_account.text = text
        }

        view.dialog_select_calendars_holder.addView(calendarItem)
    }

    private fun confirmSelection() {
        val calendarIds = ArrayList<Int>()
        val childCnt = view.dialog_select_calendars_holder.childCount
        for (i in 0..childCnt) {
            val child = view.dialog_select_calendars_holder.getChildAt(i)
            if (child is RelativeLayout) {
                val check = child.getChildAt(0)
                if (check is MyAppCompatCheckbox && check.isChecked) {
                    calendarIds.add(check.tag as Int)
                }
            }
        }

        activity.config.caldavSyncedCalendarIds = TextUtils.join(",", calendarIds)
        callback()
    }
}
