package com.buzbuz.smartautoclicker.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Long.toFormatString(pattern: String, locale: Locale = Locale.getDefault()): String {
    val dateFormat = SimpleDateFormat(pattern, locale)
    dateFormat.timeZone = TimeZone.getTimeZone("GMT+00:00")

    return dateFormat.format(Date(this))
}

fun Long.toFormatHmsString(locale: Locale = Locale.getDefault()): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", locale)
    dateFormat.timeZone = TimeZone.getTimeZone("GMT+00:00")

    return dateFormat.format(Date(this))
}