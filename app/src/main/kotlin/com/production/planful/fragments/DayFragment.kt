package com.production.planful.fragments

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.production.planful.R
import com.production.planful.activities.MainActivity
import com.production.planful.activities.SimpleActivity
import com.production.planful.activities.TaskActivity
import com.production.planful.adapters.DayEventsAdapter
import com.production.planful.commons.extensions.*
import com.production.planful.commons.interfaces.RefreshRecyclerViewListener
import com.production.planful.extensions.config
import com.production.planful.extensions.eventsHelper
import com.production.planful.extensions.getViewBitmap
import com.production.planful.extensions.printBitmap
import com.production.planful.helpers.*
import com.production.planful.interfaces.NavigationListener
import com.production.planful.models.Event
import kotlinx.android.synthetic.main.fragment_day.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*

class DayFragment : Fragment(), RefreshRecyclerViewListener {
    var mListener: NavigationListener? = null
    private var mTextColor = 0
    private var mDayCode = ""
    private var lastHash = 0

    private lateinit var mHolder: RelativeLayout

    private var percentageListener: ((Int) -> Unit)? = null

    fun setPercentageListener(block: ((Int) -> Unit)) {
        percentageListener = block
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_day, container, false)
        mHolder = view.day_holder

        mHolder.no_data.tvNoData1.setTextColor(view.context.getProperTextColor())
        mHolder.no_data.tvNoData2.setTextColor(view.context.getProperTextColor())
        mDayCode = requireArguments().getString(DAY_CODE)!!
        setupButtons()
        return view
    }

    override fun onResume() {
        super.onResume()
        refreshItems()
    }

    private fun setupButtons() {
        mTextColor = requireContext().getProperTextColor()

        mHolder.top_left_arrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        mHolder.top_right_arrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        val day = Formatter.getDayTitle(requireContext(), mDayCode)
        if (mDayCode == Formatter.getTodayCode()) {
            mHolder.top_value.background = requireContext().getDrawable(R.drawable.red_border)
        }
        mHolder.top_value.apply {
            text = day
            contentDescription = text
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
            setTextColor(context.getProperTextColor())
        }
    }

    private fun receivedEvents(events: List<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash || !isAdded) {
            return
        }
        lastHash = newHash

        val replaceDescription = requireContext().config.replaceDescription
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

        activity?.runOnUiThread {
            updateEvents(sorted)
        }
    }

    private fun updateEvents(events: ArrayList<Event>) {
        if (activity == null) return
        if (events.size > 0) {
            mHolder.no_data.visibility = View.GONE
            mHolder.day_events.visibility = View.VISIBLE
            val tasksCount = events.count { it.isTask() }
            if (tasksCount != 0) {
                val percent = 100 * events.count { it.isTaskCompleted() } / tasksCount
                val day = Formatter.getDayTitle(requireContext(), mDayCode)
                val spannable = SpannableString(day.plus(" $percent%"))
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    day.lastIndex + 1, spannable.lastIndex + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                mHolder.top_value.text = spannable
            }
        } else {
            mHolder.no_data.visibility = View.VISIBLE
            mHolder.day_events.visibility = View.GONE
        }

        DayEventsAdapter(activity as SimpleActivity, events, mHolder.day_events, this,  mDayCode) {
            editEvent(it as Event)
        }.apply {
            mHolder.day_events.adapter = this
        }

        if (requireContext().areSystemAnimationsEnabled) {
            mHolder.day_events.scheduleLayoutAnimation()
        }
    }

    private fun editEvent(event: Event) {
        Intent(context, TaskActivity::class.java).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            putExtra(IS_TASK_COMPLETED, event.isTaskCompleted())
            startActivity(this)
        }
    }

    fun printCurrentView() {
        mHolder.apply {
            top_left_arrow.beGone()
            top_right_arrow.beGone()
            top_value.setTextColor(resources.getColor(R.color.theme_light_text_color))
            (day_events.adapter as? DayEventsAdapter)?.togglePrintMode()

            Handler().postDelayed({
                requireContext().printBitmap(day_holder.getViewBitmap())

                Handler().postDelayed({
                    top_left_arrow.beVisible()
                    top_right_arrow.beVisible()
                    top_value.setTextColor(requireContext().getProperTextColor())
                    (day_events.adapter as? DayEventsAdapter)?.togglePrintMode()
                }, 1000)
            }, 1000)
        }
    }

    override fun refreshItems() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        context?.eventsHelper?.getEvents(startTS, endTS) {
            receivedEvents(it)
        }
    }
}
