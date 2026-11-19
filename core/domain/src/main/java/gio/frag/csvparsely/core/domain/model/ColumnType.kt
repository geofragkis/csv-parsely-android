package gio.frag.csvparsely.core.domain.model

/**
 * Created by Giorgos Fragkis on 15-11-2026
 *
 * The single type of a column, decided from all of its non-empty values.
 */
enum class ColumnType {
    TEXT,
    INTEGER,
    DATE,
}
