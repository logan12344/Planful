package com.production.planful.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun <T> Gson.convertToJsonString(t: T): String {
    return toJson(t).toString()
}

fun <T> Gson.convertToModel(jsonString: String, cls: Class<T>): T? {
    return try {
        fromJson(jsonString, cls)
    } catch (e: Exception) {
        null
    }
}

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)