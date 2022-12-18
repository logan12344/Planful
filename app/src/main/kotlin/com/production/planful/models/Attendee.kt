package com.production.planful.models

import android.content.Context
import android.graphics.drawable.Drawable
import android.provider.CalendarContract
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions

data class Attendee(
    val contactId: Int,
    var name: String,
    val email: String,
    var status: Int,
    var photoUri: String,
    var isMe: Boolean,
    var relationship: Int
)
