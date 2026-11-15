package gio.frag.csvparsely.core.domain.model

/**
 * A column of the table.
 *
 * @property name the name from the header, empty when the header gives none
 * @property type the type of every value in the column
 */
data class Column(
    val name: String,
    val type: ColumnType,
)
