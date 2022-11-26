package com.production.planful.models

data class ListSectionDay(
    val title: String,
    val code: String,
    val isToday: Boolean,
    val isPastSection: Boolean
) : ListItem()
