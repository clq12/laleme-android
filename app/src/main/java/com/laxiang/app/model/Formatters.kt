package com.laxiang.app.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatDateTime(millis: Long): String =
    dateTimeFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))

fun formatDate(millis: Long): String =
    dateFormatter.format(LocalDate.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))

fun formatTime(millis: Long): String =
    timeFormatter.format(LocalTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()))

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return when {
        minutes <= 0 -> "${remainder}秒"
        remainder == 0 -> "${minutes}分钟"
        else -> "${minutes}分${remainder}秒"
    }
}

fun formatElapsedSeconds(seconds: Long): String {
    val minutes = seconds / 60
    val remainder = seconds % 60
    return String.format("%02d:%02d", minutes, remainder)
}
