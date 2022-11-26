package com.production.planful.commons.helpers

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long) = this.map { selector(it) }.sum()

inline fun <T> Iterable<T>.sumByInt(selector: (T) -> Int) = this.map { selector(it) }.sum()
