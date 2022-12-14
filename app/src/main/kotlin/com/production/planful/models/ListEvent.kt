package com.production.planful.models

data class ListEvent(
    var id: Long,
    var startTS: Long,
    var endTS: Long,
    var title: String,
    var description: String,
    var isAllDay: Boolean,
    var color: Int,
    var location: String,
    var isPastEvent: Boolean,
    var isRepeatable: Boolean,
    var isTask: Boolean,
    var isTaskCompleted: Boolean,
    var getChecklist: String,
    var isCheckListEnable: Boolean
) : ListItem()
