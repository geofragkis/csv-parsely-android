package gio.frag.csvparsely.core.domain.model

/** The single type of a column, decided from all of its non-empty values. */
enum class ColumnType {
    TEXT,
    INTEGER,
    DATE,
}
