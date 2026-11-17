package gio.frag.csvparsely.core.domain.model

/**
 * Created by Giorgos Fragkis on 15-11-2026
 *
 * The raw file as the parser read it: column names and rows of plain text, before any typing.
 *
 * Every row has exactly as many values as [header]. The parser pads a short row with empty values, and for a long row
 * it adds unnamed columns to [header] and pads every other row to match.
 *
 * @property header a list with the column names
 * @property rows a list of rows, each containing a list of strings
 */
data class RawCsv(
    val header: List<String>,
    val rows: List<List<String>>,
) {
    init {
        // The parser must pad every row to the size of the header, so this only fails when the parser
        // has a bug. It exists for extra safety.
        require(rows.all { row -> row.size == header.size }) { "Every row must have as many values as the header" }
    }
}
