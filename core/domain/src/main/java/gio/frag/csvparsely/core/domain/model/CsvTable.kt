package gio.frag.csvparsely.core.domain.model

import gio.frag.csvparsely.core.domain.inference.toCellValue

/**
 * Created by Giorgos Fragkis on 15-11-2026
 *
 * The file ready to show: each column has one type, and a row's cells are converted to those types when the row is
 * read, so the table never holds a second, typed copy of the whole file.
 *
 * @property columns a list with the name and type of each column
 * @property rows each cell as written in the file
 */
class CsvTable(
    val columns: List<Column>,
    private val rows: List<List<String>>,
) {

    /** How many rows the table has. */
    val rowCount: Int
        get() = rows.size

    /** The row at [index], with every cell converted to its column's type. */
    fun row(index: Int): List<CellValue> =
        rows[index].mapIndexed { column, text ->
            text.toCellValue(columns[column].type)
        }
}
