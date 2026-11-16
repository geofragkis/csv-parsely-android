package gio.frag.csvparsely.core.domain.util

import gio.frag.csvparsely.core.domain.model.CellValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField

/** Reads the ISO dates and date-times that make a column DATE. */
internal object DateFormatter {

    /**
     * An ISO date with a four-digit year from 0001 and no sign: `2001-04-20`, but not `0000-01-01`, `+10000-01-01` or
     * `-0001-01-01`. Unlike [DateTimeFormatter.ISO_LOCAL_DATE], which also accepts year 0000, signed years and years
     * with more than four digits.
     */
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
        // The year counted from 1 AD, so 0000 is out of range instead of being read as 1 BC
        .appendValue(ChronoField.YEAR_OF_ERA, 4)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        // The file gives no era, so every year is AD
        .parseDefaulting(ChronoField.ERA, 1)
        .toFormatter()
        // Rejects dates that don't exist, such as 2023-02-30, instead of moving them to the last valid day, 2023-02-28
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * A [DATE_FORMAT] date and an ISO time, joined by an uppercase `T`: `2001-04-20T10:15`, `2001-04-20T10:15:30.123`,
     * but not `2001-04-20t10:15`. Unlike [DateTimeFormatter.ISO_LOCAL_DATE_TIME], which also accepts a lowercase `t`.
     */
    private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
        .append(DATE_FORMAT)
        .appendLiteral('T')
        .append(DateTimeFormatter.ISO_LOCAL_TIME)
        .toFormatter()
        // Rejects dates and times that don't exist, such as 2023-02-30T10:15 or 2001-04-20T24:00, instead of moving
        // them to a valid one, 2023-02-28T10:15 or 2001-04-21T00:00
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * Reads an ISO date or date-time, or returns null. Tries a date first, then a date-time.
     *
     * For example: `"2001-04-20"` returns `Date(2001-04-20, time = null)`, `"1978-01-02T00:00:00"` returns
     * `Date(1978-01-02, time = 00:00)`, and `"2023-02-30"` and `"+10000-01-01"` return null.
     */
    fun parseOrNull(value: String): CellValue.Date? {
        tryParse {
            LocalDate.parse(value, DATE_FORMAT)
        }?.let { date ->
            return CellValue.Date(date, time = null)
        }
        tryParse {
            LocalDateTime.parse(value, DATE_TIME_FORMAT)
        }?.let { dateTime ->
            return CellValue.Date(dateTime.toLocalDate(), dateTime.toLocalTime())
        }
        return null
    }

    /**
     * Runs [parse] and returns its result, or null when the text is not a valid date.
     *
     * For example: `tryParse { LocalDate.parse("2001-04-20") }` returns 2001-04-20, and with `"20-04-2001"` it returns
     * null.
     */
    private inline fun <T> tryParse(parse: () -> T): T? =
        try {
            parse()
        } catch (e: DateTimeParseException) {
            null
        }
}
