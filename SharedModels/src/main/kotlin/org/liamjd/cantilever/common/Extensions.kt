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
 * Replace characters which are not valid for a URL with '-'
 * May result in duplicate - characters, which is not ideal but technically correct
 * Regex is "[;/?:@&=+\$, ]"
 */
fun String.toSlug(): String {
    val reserved = "[;/?:@&=+\$, ]"
    return this.replace(Regex(pattern = reserved), "-").lowercase()
}

/**
 * Replace characters which are not valid for a URL with '-'
 * This is identical to [String.toSlug()] except that we retain the '/' character
 */
fun String.toS3Key(): String {
    val reserved = "[;?:@&=+\$, ]"
    return this.replace(Regex(pattern = reserved), replacement = "-").lowercase()
}

/**
 * Remove the metadata block from a post, template or page, i.e. remove the first '---' ... '---' block from the file
 */
fun String.stripFrontMatter(): String {
    return this.substringAfter("---").substringAfter("---").trim()
}

/**
 * Extract the metadata block from a post, template or page, i.e. the content between the first '---' ... '---' block from the file
 */
fun String.getFrontMatter(): String {
    return this.substringAfter("---").substringBefore("---")
}

/**
 * Check if a string has a front matter block
 */
fun String.hasFrontMatter(): Boolean {
    return this.trim().startsWith("---")
}