package com.production.planful.extensions

fun String.getMonthCode() = if (length == 8) substring(0, 6) else ""
