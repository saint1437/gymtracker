package com.sol.gymtracker.ui.text

import java.util.Calendar

private const val NBSP = '\u00A0'
private const val MINUS = '\u2212'

/** 12450.0 -> "12 450", 2.5 -> "2,5" */
fun formatKg(value: Double): String {
    val rounded = Math.round(value * 10.0) / 10.0
    val negative = rounded < 0
    val absolute = kotlin.math.abs(rounded)
    val whole = absolute.toLong()
    val fraction = Math.round((absolute - whole) * 10.0).toInt().coerceIn(0, 9)
    val grouped = whole.toString().reversed().chunked(3).joinToString(NBSP.toString()).reversed()
    val number = if (fraction == 0) grouped else "$grouped,$fraction"
    return if (negative) "$MINUS$number" else number
}

fun formatKgWithUnit(value: Double): String = "${formatKg(value)}$NBSP" + "кг"

/** Chronometer for the workout screen: 47:12 */
fun formatTimer(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)

/** Human duration for history: "47 мин", "1 ч 05 мин" */
fun formatDurationRu(seconds: Long): String {
    val totalMinutes = seconds / 60
    if (totalMinutes <= 0) return "меньше минуты"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours$NBSP" + "ч " + "%02d".format(minutes) + NBSP + "мин"
    else "$totalMinutes$NBSP" + "мин"
}

/** "пн, 28 окт" */
fun formatDateRu(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val weekday = WEEKDAYS[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    val month = MONTHS[calendar.get(Calendar.MONTH)]
    return "$weekday, ${calendar.get(Calendar.DAY_OF_MONTH)} $month"
}

/** "3 × 8–10 · отдых 120 с" */
fun formatSetSummary(sets: Int, repMin: Int, repMax: Int, restSeconds: Int): String =
    "$sets × $repMin–$repMax · отдых $restSeconds$NBSP" + "с"

/** "60 × 10, 9 · запас 2", null when there is no previous data */
fun previousSetsSummary(sets: List<Pair<Double, Int>>, averageRir: Int): String? {
    if (sets.isEmpty()) return null
    val detail = sets.joinToString(", ") { (weight, reps) -> "${formatKg(weight)} × $reps" }
    return "$detail · запас $averageRir"
}

private val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
private val MONTHS = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
