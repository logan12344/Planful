package com.production.planful.commons.dialogs

import android.view.animation.AnimationUtils
import com.production.planful.R
import com.production.planful.commons.activities.BaseSimpleActivity
import com.production.planful.commons.extensions.applyColorFilter
import com.production.planful.commons.extensions.getAlertDialogBuilder
import com.production.planful.commons.extensions.getProperTextColor
import com.production.planful.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_call_confirmation.view.*

class CallConfirmationDialog(
    val activity: BaseSimpleActivity,
    val callee: String,
    private val callback: () -> Unit
) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_call_confirmation, null)

    init {
        view.call_confirm_phone.applyColorFilter(activity.getProperTextColor())
        activity.getAlertDialogBuilder()
            .setNegativeButton(R.string.cancel, null)
            .apply {
                val title = String.format(activity.getString(R.string.call_person), callee)
                activity.setupDialogStuff(view, this, titleText = title) { alertDialog ->
                    view.call_confirm_phone.apply {
                        startAnimation(
                            AnimationUtils.loadAnimation(
                                activity,
                                R.anim.pulsing_animation
                            )
                        )
                        setOnClickListener {
                            callback.invoke()
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
