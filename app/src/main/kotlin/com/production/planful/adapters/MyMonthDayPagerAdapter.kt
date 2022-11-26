package com.production.planful.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.production.planful.fragments.MonthDayFragment
import com.production.planful.helpers.DAY_CODE
import com.production.planful.interfaces.NavigationListener

class MyMonthDayPagerAdapter(
    fm: FragmentManager,
    private val mCodes: List<String>,
    private val mListener: NavigationListener
) : FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<MonthDayFragment>()

    override fun getCount() = mCodes.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val code = mCodes[position]
        bundle.putString(DAY_CODE, code)

        val fragment = MonthDayFragment()
        fragment.arguments = bundle
        fragment.listener = mListener

        mFragments.put(position, fragment)
        return fragment
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateCalendar()
        }
    }

    fun printCurrentView(pos: Int) {
        mFragments[pos].printCurrentView()
    }

    fun getNewEventDayCode(pos: Int): String? = mFragments[pos].getNewEventDayCode()
}
