package org.liamjd.cantilever.lambda.handlebars

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import kotlinx.datetime.*
import org.liamjd.cantilever.common.now
import java.time.format.DateTimeFormatter

/**
* A Handlebars helper to format a LocalDate or LocalDateTime object
 * @param inputFormat the format of the input date, defaults to dd/MM/yyyy
 * @return a formatted date string
 * There are some predefined ISO formats, or you can supply your own
 */
class LocalDateFormatter(private val inputFormat: String = "dd/MM/yyyy") : Helper<Any> {

    companion object {
        private const val ISO_DATE = "ISO_DATE"
        private const val ISO_LOCAL_DATE = "ISO_LOCAL_DATE"
        private const val ISO_ORDINAL_DATE = "ISO_ORDINAL_DATE"
        private const val ISO_WEEK_DATE = "ISO_WEEK_DATE"
        private const val ISO_LOCAL_DATE_TIME = "ISO_LOCAL_DATE_TIME"
    }

    override fun apply(value: Any?, options: Options): CharSequence {

        if (value != null) {
            val dateTime: LocalDateTime = when (value) {
                is LocalDate -> {
                    value.atStartOfDayIn(TimeZone.currentSystemDefault())
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                }

                is LocalDateTime ->
                    value

                is Instant -> {
                    value.toLocalDateTime(TimeZone.currentSystemDefault())
                }

                else -> {
                    println("Unable to convert value (${value::class}) to LocalDateTime; defaulting to now")
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                }
            }

            val outputFormatter =
                when (val format = options.param(0, options.hash<Any>("format", inputFormat)) as String) {
                    ISO_DATE -> {
                        DateTimeFormatter.ISO_DATE
                    }

                    ISO_LOCAL_DATE -> {
                        DateTimeFormatter.ISO_LOCAL_DATE
                    }

                    ISO_ORDINAL_DATE -> {
                        DateTimeFormatter.ISO_ORDINAL_DATE
                    }

                    ISO_WEEK_DATE -> {
                        DateTimeFormatter.ISO_WEEK_DATE
                    }
                    ISO_LOCAL_DATE_TIME -> {
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    }

                    else -> {
                        try {
                            DateTimeFormatter.ofPattern(format)
                        } catch (iae: IllegalArgumentException) {
                            println("Could not parse localDate format string '$format'; reverting to ISO_LOCAL_DATE")
                            DateTimeFormatter.ISO_LOCAL_DATE
                        }
                    }
                }
            return outputFormatter.format((dateTime).toJavaLocalDateTime())
        }
        return LocalDate.now().toString()
    }
}