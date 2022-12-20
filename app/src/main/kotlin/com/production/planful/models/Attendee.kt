package com.production.planful.models

data class Attendee(
    val contactId: Int,
    var name: String,
    val email: String,
    var status: Int,
    var photoUri: String,
    var isMe: Boolean,
    var relationship: Int
)
