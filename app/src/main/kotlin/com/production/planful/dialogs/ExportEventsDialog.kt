package com.production.planful.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.production.planful.R
import com.production.planful.activities.SimpleActivity
import com.production.planful.adapters.FilterEventTypeAdapter
import com.production.planful.commons.dialogs.FilePickerDialog
import com.production.planful.commons.extensions.*
import com.production.planful.commons.helpers.ensureBackgroundThread
import com.production.planful.extensions.config
import com.production.planful.extensions.eventsHelper
import kotlinx.android.synthetic.main.dialog_export_events.view.*
import java.io.File

class ExportEventsDialog(
    val activity: SimpleActivity, val path: String, val hidePath: Boolean,
    val callback: (file: File, eventTypes: ArrayList<Long>) -> Unit
) {
    private var realPath = if (path.isEmpty()) activity.internalStoragePath else path
    private val config = activity.config

    init {
        val view = (activity.layoutInflater.inflate(
            R.layout.dialog_export_events,
            null
        ) as ViewGroup).apply {
            export_events_folder.setText(activity.humanizePath(realPath))
            export_events_filename.setText("${activity.getString(R.string.events)}_${activity.getCurrentFormattedDateTime()}")
            export_past_events_checkbox.isChecked = config.exportPastEvents
            export_past_events_checkbox_holder.setOnClickListener {
                export_past_events_checkbox.toggle()
            }

            if (hidePath) {
                export_events_folder_hint.beGone()
                export_events_folder.beGone()
            } else {
                export_events_folder.setOnClickListener {
                    activity.hideKeyboard(export_events_filename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        export_events_folder.setText(activity.humanizePath(it))
                        realPath = it
                    }
                }
            }

            activity.eventsHelper.getEventTypes(activity, false) {
                val eventTypes = HashSet<String>()
                it.mapTo(eventTypes) { it.id.toString() }

                export_events_types_list.adapter = FilterEventTypeAdapter(activity, it, eventTypes)
                if (it.size > 1) {
                    export_events_pick_types.beVisible()
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.export_events) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = view.export_events_filename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(realPath, "$filename.ics")
                                if (!hidePath && file.exists()) {
                                    activity.toast(R.string.name_taken)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.lastExportPath = file.absolutePath.getParentPath()
                                    config.exportPastEvents =
                                        view.export_past_events_checkbox.isChecked

                                    val eventTypes =
                                        (view.export_events_types_list.adapter as FilterEventTypeAdapter).getSelectedItemsList()
                                    callback(file, eventTypes)
                                    alertDialog.dismiss()
                                }
                            }
                            else -> activity.toast(R.string.invalid_name)
                        }
                    }
                }
            }
    }
}
