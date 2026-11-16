package gio.frag.csvparsely.core.domain.model

/**
 * The typed file, ready to show: each column has one type and every cell is converted to it.
 *
 * @property columns a list with the name and type of each column
 * @property rows a list of rows, each containing a list of cells of type [CellValue]
 * @property irregularRowCount the number of rows the parser padded or extended because their field count differed from
 * the header
 */
data class CsvTable(
    val columns: List<Column>,
    val rows: List<List<CellValue>>,
    val irregularRowCount: Int,
)
