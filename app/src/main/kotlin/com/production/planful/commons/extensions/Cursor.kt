package com.production.planful.commons.extensions

import android.database.Cursor

fun Cursor.getStringValue(key: String): String = getString(getColumnIndex(key))

fun Cursor.getIntValue(key: String) = getInt(getColumnIndex(key))

fun Cursor.getIntValueOrNull(key: String) =
    if (isNull(getColumnIndex(key))) null else getInt(getColumnIndex(key))

fun Cursor.getLongValue(key: String) = getLong(getColumnIndex(key))

