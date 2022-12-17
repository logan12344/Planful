package com.production.planful.extensions

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun <T> Gson.convertToJsonString(t: T): String {
    return toJson(t).toString()
}

inline fun <reified T> Gson.fromJson(json: String): T = this.fromJson(json, object: TypeToken<T>() {}.type)