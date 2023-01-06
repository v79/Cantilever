package org.liamjd.cantilever.common

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Get the current date
 */
fun LocalDate.Companion.now(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/**
 * Replace characters which are not value for a URL with '-'
 * May result in duplicate - characters, which is not ideal but technically correct
 * Regex is "[;/?:@&=+\$, ]"
 */
fun String.toSlug() : String {
    val reserved = "[;/?:@&=+\$, ]"
    return this.replace(Regex(pattern = reserved),"-").lowercase()
}
