package com.production.planful.interfaces

interface WeekFragmentListener {
    fun scrollTo(y: Int)

    fun updateHoursTopMargin(margin: Int)

    fun getCurrScrollY(): Int

    fun updateRowHeight(rowHeight: Int)

    fun getFullFragmentHeight(): Int
}
