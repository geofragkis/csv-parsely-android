package gio.frag.csvparsely.core.domain.inference

import gio.frag.csvparsely.core.domain.model.CellValue
import gio.frag.csvparsely.core.domain.model.Column
import gio.frag.csvparsely.core.domain.model.ColumnType
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

/** A whole number without leading zeros, optionally signed: `0`, `-3`, `+12`, but not `0123`. */
private val INTEGER_PATTERN = Regex("[+-]?(0|[1-9][0-9]*)")

/**
 * Created by Giorgos Fragkis on 16-11-2026
 *
 * Gives each column one type, decided from all of its non-empty values, and converts every cell to that type.
 *
 * A column is INTEGER when every non-empty value is a whole number that fits in a [Long], DATE when every non-empty
 * value is an ISO date or date-time (with or without an offset), and TEXT otherwise. Surrounding whitespace is ignored
 * while deciding.
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
    //         [Text("Theo"), Text("Jansen"), Integer(5), Date(1978-01-02, 00:00, null), Text("https://...png")],
    //         [Text("Fiona"), Text("de Vries"), Integer(7), Date(1950-11-12, 00:00, null), Text("https://...png")],
    //         [Text("Petra"), Text("Boersma"), Integer(1), Date(2001-04-20, 00:00, null), Text("https://...png")],
    //     ],
    // )
    return CsvTable(
        columns = columns,
        rows = typedRows,
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

    return with(nonEmptyValues) {
        when {
            isEmpty() -> ColumnType.TEXT
            all { it.toIntegerOrNull() != null } -> ColumnType.INTEGER
            all { it.toDateOrNull() != null } -> ColumnType.DATE
            else -> ColumnType.TEXT
        }
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
 * Reads an ISO date, date-time or date-time with an offset, or returns null. Uses the strict ISO parsers of
 * `java.time`, which reject dates and times that don't exist, such as `2023-02-30` or `24:00`.
 *
 * For example: `"2001-04-20"` returns `Date(2001-04-20, time = null, offset = null)`, `"1978-01-02T00:00:00"` returns
 * `Date(1978-01-02, time = 00:00, offset = null)`, `"2001-04-20T10:15+02:00"` returns
 * `Date(2001-04-20, time = 10:15, offset = +02:00)`, and `"2023-02-30"` returns null.
 */
private fun String.toDateOrNull(): CellValue.Date? {
    // Each parser throws when the text is not in its format, so the next one is tried
    runCatching { LocalDate.parse(this) }.getOrNull()
        ?.let { date ->
            return CellValue.Date(
                date = date,
                time = null,
                offset = null,
            )
        }

    runCatching { LocalDateTime.parse(this) }.getOrNull()
        ?.let { dateTime ->
            return CellValue.Date(
                date = dateTime.toLocalDate(),
                time = dateTime.toLocalTime(),
                offset = null,
            )
        }

    runCatching { OffsetDateTime.parse(this) }.getOrNull()
        ?.let { dateTime ->
            return CellValue.Date(
                date = dateTime.toLocalDate(),
                time = dateTime.toLocalTime(),
                offset = dateTime.offset,
            )
        }
    return null
}
