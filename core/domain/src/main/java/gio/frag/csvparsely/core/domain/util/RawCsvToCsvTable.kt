package gio.frag.csvparsely.core.domain.util

import gio.frag.csvparsely.core.domain.model.CellValue
import gio.frag.csvparsely.core.domain.model.Column
import gio.frag.csvparsely.core.domain.model.ColumnType
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField

/** A whole number without leading zeros, optionally signed: `0`, `-3`, `+12`, but not `0123`. */
private val INTEGER_PATTERN = Regex("[+-]?(0|[1-9][0-9]*)")

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
 * Gives each column one type, decided from all of its non-empty values, and converts every cell to that type.
 *
 * A column is INTEGER when every non-empty value is a whole number that fits in a [Long], DATE when every non-empty
 * value is an ISO date or date-time, and TEXT otherwise. Surrounding whitespace is ignored while deciding.
 *
 * For example, this file:
 * ```
 * "First name","Sur name","Issue count","Date of birth","avatar"
 * "Theo","Jansen",5,"1978-01-02T00:00:00","https://api.multiavatar.com/2cdf5db9b4dee297b7.png"
 * "Fiona","de Vries",7,"1950-11-12T00:00:00","https://api.multiavatar.com/b9339cb9e7a833cd5e.png"
 * "Petra","Boersma",1,"2001-04-20T00:00:00","https://api.multiavatar.com/2672c49d6099f87274.png"
 * ```
 */
fun RawCsv.toCsvTable(): CsvTable {
    // Creating a list of types, ex: [TEXT, TEXT, INTEGER, DATE, TEXT]
    val types = header.indices.map { index ->
        val column = rows.map { row -> row[index] }
        columnType(column)
    }
    // Creating a list of columns, with their names and types
    val columns = header.zip(types) { name, type ->
        Column(name, type)
    }
    // Creating a list of rows, with their cells converted to their types
    val typedRows = rows.map { row ->
        row.mapIndexed { index, value ->
            value.toCellValue(types[index])
        }
    }

    // Return the CsvTable, ex:
    // CsvTable(
    //     columns = [
    //         Column(name = "First name", type = TEXT),
    //         Column(name = "Sur name", type = TEXT),
    //         Column(name = "Issue count", type = INTEGER),
    //         Column(name = "Date of birth", type = DATE),
    //         Column(name = "avatar", type = TEXT),
    //     ],
    //     rows = [
    //         [Text("Theo"), Text("Jansen"), Integer(5), Date(1978-01-02, time = 00:00), Text("https://...png")],
    //         [Text("Fiona"), Text("de Vries"), Integer(7), Date(1950-11-12, time = 00:00), Text("https://...png")],
    //         [Text("Petra"), Text("Boersma"), Integer(1), Date(2001-04-20, time = 00:00), Text("https://...png")],
    //     ],
    //     irregularRowCount = 0,
    // )
    return CsvTable(
        columns = columns,
        rows = typedRows,
        irregularRowCount = irregularRowCount,
    )
}

/**
 * Decides the type of one column from its values. Empty and blank values are ignored, and a column with no other
 * values is TEXT.
 *
 * For example: `["30", " 25 ", ""]` returns INTEGER, `["2001-04-20", ""]` returns DATE, `["5", "five"]` returns TEXT.
 */
private fun columnType(values: List<String>): ColumnType {
    val nonEmptyValues = values
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return when {
        nonEmptyValues.isEmpty() -> ColumnType.TEXT
        nonEmptyValues.all { it.toIntegerOrNull() != null } -> ColumnType.INTEGER
        nonEmptyValues.all { it.toDateOrNull() != null } -> ColumnType.DATE
        else -> ColumnType.TEXT
    }
}

/**
 * Converts one cell to the type of its column. A TEXT value is kept exactly as it is, and a blank value in an INTEGER
 * or DATE column becomes [CellValue.Empty].
 *
 * For example: `" 30 "` in an INTEGER column returns `Integer(30)`, `" "` in an INTEGER column returns `Empty`, and
 * `" a "` in a TEXT column returns `Text(" a ")`.
 */
private fun String.toCellValue(type: ColumnType): CellValue {
    val trimmed = trim()
    if (type != ColumnType.TEXT && trimmed.isEmpty()) return CellValue.Empty
    return when (type) {
        ColumnType.TEXT -> CellValue.Text(this)
        ColumnType.INTEGER -> CellValue.Integer(checkNotNull(trimmed.toIntegerOrNull()))
        ColumnType.DATE -> checkNotNull(trimmed.toDateOrNull())
    }
}

/**
 * Reads a whole number that fits in a [Long], or returns null.
 *
 * For example: `"+12"` returns 12, while `"0123"`, `"1.5"` and `"99999999999999999999"` return null.
 */
private fun String.toIntegerOrNull(): Long? =
    if (INTEGER_PATTERN.matches(this)) toLongOrNull() else null

/**
 * Reads an ISO date or date-time, or returns null. Tries a date first, then a date-time.
 *
 * For example: `"2001-04-20"` returns `Date(2001-04-20, time = null)`, `"1978-01-02T00:00:00"` returns
 * `Date(1978-01-02, time = 00:00)`, and `"2023-02-30"` and `"+10000-01-01"` return null.
 */
private fun String.toDateOrNull(): CellValue.Date? {
    parseOrNull {
        LocalDate.parse(this, DATE_FORMAT)
    }?.let { date ->
        return CellValue.Date(date, time = null)
    }
    parseOrNull {
        LocalDateTime.parse(this, DATE_TIME_FORMAT)
    }?.let { dateTime ->
        return CellValue.Date(dateTime.toLocalDate(), dateTime.toLocalTime())
    }
    return null
}

/**
 * Runs [parse] and returns its result, or null when the text is not a valid date.
 *
 * For example: `parseOrNull { LocalDate.parse("2001-04-20") }` returns 2001-04-20, and with `"20-04-2001"` it returns
 * null.
 */
private inline fun <T> parseOrNull(parse: () -> T): T? =
    try {
        parse()
    } catch (e: DateTimeParseException) {
        null
    }
