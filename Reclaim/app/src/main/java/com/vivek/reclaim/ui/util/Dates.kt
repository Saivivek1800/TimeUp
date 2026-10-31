package com.vivek.reclaim.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "28 Nov" for short banners. */
fun formatShortDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM", locale))

/** "November 24, 2026" for the confirmation dialog. */
fun formatLongDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
    Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("MMMM d, yyyy", locale))
