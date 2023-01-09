package org.liamjd.cantilever.common

import kotlinx.datetime.*

/**
 * Get the current date
 */
fun LocalDate.Companion.now(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/**
 * Get the current date and time
 */
fun LocalDateTime.Companion.now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/**
 * Convert a java.time.Instant to a kotlinx.datetime.Instant
 */
fun java.time.Instant.toLocalDateTime() =
    kotlinx.datetime.Instant.fromEpochMilliseconds(this.toEpochMilli()).toLocalDateTime(TimeZone.currentSystemDefault())

/**
 * Replace characters which are not value for a URL with '-'
 * May result in duplicate - characters, which is not ideal but technically correct
 * Regex is "[;/?:@&=+\$, ]"
 */
fun String.toSlug(): String {
    val reserved = "[;/?:@&=+\$, ]"
    return this.replace(Regex(pattern = reserved), "-").lowercase()
}
