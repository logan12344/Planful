package com.production.planful.extensions

import android.graphics.Paint
import android.widget.TextView
import com.production.planful.commons.extensions.addBit
import com.production.planful.commons.extensions.removeBit

fun TextView.checkViewStrikeThrough(addFlag: Boolean) {
    paintFlags = if (addFlag) {
        paintFlags.addBit(Paint.STRIKE_THRU_TEXT_FLAG)
    } else {
        paintFlags.removeBit(Paint.STRIKE_THRU_TEXT_FLAG)
    }
}
