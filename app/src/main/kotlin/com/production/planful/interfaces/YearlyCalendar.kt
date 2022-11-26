package com.production.planful.interfaces

import android.util.SparseArray
import com.production.planful.models.DayYearly

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
