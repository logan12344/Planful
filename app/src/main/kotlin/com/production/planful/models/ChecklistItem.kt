package com.production.planful.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ChecklistItem (
    @SerializedName("name")
    var name: String,
    @SerializedName("checked")
    var checked: Boolean
) : Parcelable