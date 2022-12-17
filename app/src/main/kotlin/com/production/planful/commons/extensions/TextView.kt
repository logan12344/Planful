package com.production.planful.commons.extensions

import android.widget.TextView
import androidx.annotation.StringRes

val TextView.value: String get() = text.toString().trim()

fun TextView.setTextOrBeGone(@StringRes textRes: Int?) {
    if (textRes != null) {
        beVisible()
        this.text = context.getString(textRes)
    } else {
        beGone()
    }
}
