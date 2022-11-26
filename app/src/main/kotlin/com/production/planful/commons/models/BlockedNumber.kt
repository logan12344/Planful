package com.production.planful.commons.models

data class BlockedNumber(
    val id: Long,
    val number: String,
    val normalizedNumber: String,
    val numberToCompare: String
)
