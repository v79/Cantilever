package org.liamjd.cantilever.lambda.handlebars

import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import org.liamjd.cantilever.common.now
import java.time.format.DateTimeFormatter

/**
 * Only handles LocalDate with no time component
 */
class LocalDateFormatter(private val inputFormat: String = "dd/MM/yyyy") : Helper<Any> {

    companion object {
        private const val ISO_DATE = "ISO_DATE"
        private const val ISO_LOCAL_DATE = "ISO_LOCAL_DATE"
        private const val ISO_ORDINAL_DATE = "ISO_ORDINAL_DATE"
        private const val ISO_WEEK_DATE = "ISO_WEEK_DATE"
    }

    override fun apply(value: Any?, options: Options): CharSequence {

        if (value != null) {
            val outputFormatter = when (val format = options.param(0, options.hash<Any>("format", inputFormat)) as String) {
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

                else -> {
                    try {
                        DateTimeFormatter.ofPattern(format)
                    } catch (iae: IllegalArgumentException) {
                        println("Could not parse localDate format string '$format'; reverting to ISO_LOCAL_DATE")
                        DateTimeFormatter.ISO_LOCAL_DATE
                    }
                }
            }
            return outputFormatter.format((value as LocalDate).toJavaLocalDate())
        }
        return LocalDate.now().toString()
    }
}