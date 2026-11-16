package gio.frag.csvparsely.core.domain.model

/**
 * The raw file as the parser read it: column names and rows of plain text, before any typing.
 *
 * Every row has exactly as many values as [header]. The parser pads a short row with empty values, and for a long row
 * it adds unnamed columns to [header] and pads every other row to match.
 *
 * @property header a list with the column names
 * @property rows a list of rows, each containing a list of strings
 * @property irregularRowCount the number of rows that have a different field count from the header
 */
data class RawCsv(
    val header: List<String>,
    val rows: List<List<String>>,
    val irregularRowCount: Int,
)
