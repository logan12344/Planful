package com.production.planful.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.production.planful.fragments.DayFragment
import com.production.planful.helpers.DAY_CODE
import com.production.planful.interfaces.NavigationListener

class MyDayPagerAdapter(
    fm: FragmentManager,
    private val mCodes: List<String>,
    private val mListener: NavigationListener
) :
    FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<DayFragment>()

    private var percentageListener: ((Int) -> Unit)? = null

    fun setPercentageListener(block: ((Int) -> Unit)) {
        percentageListener = block
    }

    override fun getCount() = mCodes.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val code = mCodes[position]
        bundle.putString(DAY_CODE, code)

        val fragment = DayFragment()
        fragment.setPercentageListener {
            percentageListener?.invoke(it)
        }
        fragment.arguments = bundle
        fragment.mListener = mListener

        mFragments.put(position, fragment)
        return fragment
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.refreshItems()
        }
    }

    fun printCurrentView(pos: Int) {
        mFragments[pos].printCurrentView()
    }
}
