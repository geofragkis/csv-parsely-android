package gio.frag.csvparsely.core.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** One cell of the table, converted to the type of its column. */
sealed interface CellValue {

    /**
     * A value from a TEXT column.
     *
     * @property value the text exactly as it was in the file
     */
    data class Text(val value: String) : CellValue

    /**
     * A value from an INTEGER column.
     *
     * A [Long] rather than an [Int], so large whole numbers such as IDs or timestamps in milliseconds still count as
     * integers instead of falling back to text.
     *
     * @property value the whole number
     */
    data class Integer(val value: Long) : CellValue

    /**
     * A value from a DATE column.
     *
     * @property date the calendar date
     * @property time the time of day, null when the file gave only a date, so a date-only value is never mistaken
     * for midnight
     */
    data class Date(val date: LocalDate, val time: LocalTime?) : CellValue

    /** An empty or blank value in an INTEGER or DATE column. */
    data object Empty : CellValue
}
