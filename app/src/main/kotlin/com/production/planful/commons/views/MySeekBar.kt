package com.production.planful.commons.views

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import com.production.planful.commons.extensions.applyColorFilter

class MySeekBar : SeekBar {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        progressDrawable.applyColorFilter(textColor)
        thumb?.applyColorFilter(textColor)
    }
}
