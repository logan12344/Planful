package com.production.planful.extensions

import com.production.planful.helpers.MONTH
import com.production.planful.helpers.WEEK
import com.production.planful.helpers.YEAR

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0
