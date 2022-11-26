package com.production.planful.fragments

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.production.planful.R
import com.production.planful.activities.MainActivity
import com.production.planful.extensions.config
import com.production.planful.extensions.getViewBitmap
import com.production.planful.extensions.printBitmap
import com.production.planful.helpers.YEAR_LABEL
import com.production.planful.helpers.YearlyCalendarImpl
import com.production.planful.interfaces.YearlyCalendar
import com.production.planful.models.DayYearly
import com.production.planful.views.SmallMonthView
import com.production.planful.commons.extensions.getProperPrimaryColor
import com.production.planful.commons.extensions.getProperTextColor
import com.production.planful.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.fragment_year.view.*
import org.joda.time.DateTime

class YearFragment : Fragment(), YearlyCalendar {
    private var mYear = 0
    private var mSundayFirst = false
    private var isPrintVersion = false
    private var lastHash = 0
    private var mCalendar: YearlyCalendarImpl? = null

    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_year, container, false)
        mYear = requireArguments().getInt(YEAR_LABEL)
        requireContext().updateTextColors(mView.calendar_holder)
        setupMonths()

        mCalendar = YearlyCalendarImpl(this, requireContext(), mYear)
        return mView
    }

    override fun onPause() {
        super.onPause()
        mSundayFirst = requireContext().config.isSundayFirst
    }

    override fun onResume() {
        super.onResume()
        val sundayFirst = requireContext().config.isSundayFirst
        if (sundayFirst != mSundayFirst) {
            mSundayFirst = sundayFirst
            setupMonths()
        }
        updateCalendar()
    }

    fun updateCalendar() {
        mCalendar?.getEvents(mYear)
    }

    private fun setupMonths() {
        val dateTime = DateTime().withDate(mYear, 2, 1).withHourOfDay(12)
        val days = dateTime.dayOfMonth().maximumValue
        mView.month_2.setDays(days)

        val now = DateTime()

        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            var dayOfWeek = dateTime.withMonthOfYear(i).dayOfWeek().get()
            if (!mSundayFirst) {
                dayOfWeek--
            }

            val monthLabel = mView.findViewById<TextView>(resources.getIdentifier("month_${i}_label", "id", requireContext().packageName))
            val curTextColor = when {
                isPrintVersion -> resources.getColor(R.color.theme_light_text_color)
                else -> requireContext().getProperTextColor()
            }

            monthLabel.setTextColor(curTextColor)

            monthView.firstDay = dayOfWeek
            monthView.setOnClickListener {
                (activity as MainActivity).openMonthFromYearly(DateTime().withDate(mYear, i, 1))
            }
        }

        if (!isPrintVersion) {
            markCurrentMonth(now)
        }
    }

    private fun markCurrentMonth(now: DateTime) {
        if (now.year == mYear) {
            val monthLabel = mView.findViewById<TextView>(resources.getIdentifier("month_${now.monthOfYear}_label", "id", requireContext().packageName))
            monthLabel.setTextColor(requireContext().getProperPrimaryColor())

            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_${now.monthOfYear}", "id", requireContext().packageName))
            monthView.todaysId = now.dayOfMonth
        }
    }

    override fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int) {
        if (!isAdded)
            return

        if (hashCode == lastHash) {
            return
        }

        lastHash = hashCode
        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            monthView.setEvents(events.get(i))
        }
    }

    fun printCurrentView() {
        isPrintVersion = true
        setupMonths()
        toggleSmallMonthPrintModes()

        requireContext().printBitmap(mView.calendar_holder.getViewBitmap())

        isPrintVersion = false
        setupMonths()
        toggleSmallMonthPrintModes()
    }

    private fun toggleSmallMonthPrintModes() {
        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            monthView.togglePrintMode()
        }
    }
}
