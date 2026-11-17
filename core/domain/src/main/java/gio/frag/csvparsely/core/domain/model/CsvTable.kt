package gio.frag.csvparsely.core.domain.model

/**
 * Created by Giorgos Fragkis on 15-11-2026
 *
 * The typed file, ready to show: each column has one type and every cell is converted to it.
 *
 * @property columns a list with the name and type of each column
 * @property rows a list of rows, each containing a list of cells of type [CellValue]
 */
data class CsvTable(
    val columns: List<Column>,
    val rows: List<List<CellValue>>,
)
