package com.production.planful.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TrackTargetItem (
    @SerializedName("daysCount")
    var daysCount: Int,
    @SerializedName("daysDone")
    var daysDone: Int
) : Parcelable
