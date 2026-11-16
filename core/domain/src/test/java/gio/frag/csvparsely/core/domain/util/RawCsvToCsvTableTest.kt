package gio.frag.csvparsely.core.domain.util

import gio.frag.csvparsely.core.domain.model.CellValue
import gio.frag.csvparsely.core.domain.model.CellValue.Empty
import gio.frag.csvparsely.core.domain.model.CellValue.Integer
import gio.frag.csvparsely.core.domain.model.CellValue.Text
import gio.frag.csvparsely.core.domain.model.Column
import gio.frag.csvparsely.core.domain.model.ColumnType
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val LONG_MIN_VALUE_STRING = "-9223372036854775808"
private const val LONG_MAX_VALUE_STRING = "9223372036854775807"

class RawCsvToCsvTableTest {

    // One type per column

    @Test
    fun `empty values in an integer column become Empty`() {
        val table = singleColumnCSVTable("5", "", "7")
        val expectedType = ColumnType.INTEGER
        val expectedResult = listOf(Integer(5), Empty, Integer(7))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `whitespace-only value in an integer column becomes Empty`() {
        val table = singleColumnCSVTable("5", "   ", "7")
        val expectedType = ColumnType.INTEGER
        val expectedResult = listOf(Integer(5), Empty, Integer(7))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `whitespace-only value in a text column is kept unchanged`() {
        val table = singleColumnCSVTable("a", "   ", "b")
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Text("a"), Text("   "), Text("b"))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `column with only empty values is text and keeps them unchanged`() {
        val table = singleColumnCSVTable("", "  ", "\t")
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Text(""), Text("  "), Text("\t"))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `empty value in a date column becomes Empty`() {
        val table = singleColumnCSVTable("2001-04-20", "")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(date(2001, 4, 20), Empty)

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    // Integer columns

    @Test
    fun `signed whole numbers are integers`() {
        val table = singleColumnCSVTable("-3", "0", "+12")
        val expectedType = ColumnType.INTEGER
        val expectedResult = listOf(Integer(-3), Integer(0), Integer(12))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `surrounding whitespace is ignored for integers`() {
        val table = singleColumnCSVTable(" 30 ", "\t25\t")
        val expectedType = ColumnType.INTEGER
        val expectedResult = listOf(Integer(30), Integer(25))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `surrounding non-breaking spaces are ignored for integers`() {
        val table = singleColumnCSVTable(" 30 ", " ")
        val expectedType = ColumnType.INTEGER
        val expectedResult = listOf(Integer(30), Empty)

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `signed zero is an integer`() {
        val table = singleColumnCSVTable("-0", "+0")
        val expectedType = ColumnType.INTEGER
        val expectedResult = listOf(Integer(0), Integer(0))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `Long min and max are integers`() {
        val table = singleColumnCSVTable(LONG_MIN_VALUE_STRING, LONG_MAX_VALUE_STRING)
        val expectedType = ColumnType.INTEGER
        val expectedResult = listOf(Integer(Long.MIN_VALUE), Integer(Long.MAX_VALUE))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `one past Long max is text`() {
        val table = singleColumnCSVTable("9223372036854775808")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `one past Long min is text`() {
        val table = singleColumnCSVTable("-9223372036854775809")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `number too large for 64 bits is text`() {
        val table = singleColumnCSVTable("99999999999999999999")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `numbers with leading zero(s) are text and keep their zeros`() {
        val table = singleColumnCSVTable("0123", "0456")
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Text("0123"), Text("0456"))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `double zero is text`() {
        val table = singleColumnCSVTable("00")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `decimal number is text`() {
        val table = singleColumnCSVTable("1.5")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `other number notations are text`() {
        val values = listOf("1e5", "1_000", "1,000", "1 000", "0x1F", "--5", "+-5", "5-", "+", "-")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `non-ASCII digits are text`() {
        // Arabic-Indic and full-width digits; toLongOrNull alone would accept them.
        val values = listOf("١٢٣", "１２")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `one non-integer value makes the whole column text`() {
        val table = singleColumnCSVTable("1", "2", "3", "x")
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Text("1"), Text("2"), Text("3"), Text("x"))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    // Date columns

    @Test
    fun `date-time keeps its time, midnight included`() {
        val table = singleColumnCSVTable("1978-01-02T00:00:00")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(date(1978, 1, 2, LocalTime.MIDNIGHT))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `date-only value has no time`() {
        val table = singleColumnCSVTable("2001-04-20")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(date(2001, 4, 20))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `date-only value is not the same as midnight`() {
        val dateOnlyTable = singleColumnCSVTable("2001-04-20")
        val midnightTable = singleColumnCSVTable("2001-04-20T00:00")

        assertNotEquals(dateOnlyTable.cells(), midnightTable.cells())
    }

    @Test
    fun `date-time without seconds and with a fraction are dates`() {
        val table = singleColumnCSVTable("2001-04-20T10:15", "2001-04-20T10:15:30.123")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(
            date(2001, 4, 20, LocalTime.of(10, 15)),
            date(2001, 4, 20, LocalTime.of(10, 15, 30, 123_000_000)),
        )

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `dates with and without a time in one column are dates`() {
        val table = singleColumnCSVTable("2001-04-20", "1978-01-02T08:30:00")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(date(2001, 4, 20), date(1978, 1, 2, LocalTime.of(8, 30)))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `surrounding whitespace is ignored for dates`() {
        val table = singleColumnCSVTable(" 2001-04-20 ")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(date(2001, 4, 20))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `invalid calendar date is text`() {
        val table = singleColumnCSVTable("2023-02-30")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `leap day in a leap year is a date`() {
        val table = singleColumnCSVTable("2024-02-29", "2000-02-29")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(date(2024, 2, 29), date(2000, 2, 29))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `leap day in a non-leap year is text`() {
        val values = listOf("2023-02-29", "1900-02-29", "2023-02-29T10:15")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `invalid times are text`() {
        val values =
            listOf( "2001-04-20T24:00", "2001-04-20T23:60", "2001-04-20T23:59:60", "2001-04-20T10")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `non-ISO date formats are text`() {
        val values =
            listOf("20-04-2001", "2001/04/20", "04/20/2001", "2001-4-20", "2001-04-20 10:15")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `lowercase t is text`() {
        val table = singleColumnCSVTable("2001-04-20t10:15")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `signed or five-digit years are text`() {
        val values = listOf("+10000-01-01", "-0001-01-01", "+2001-04-20")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `year zero is text`() {
        val values = listOf("0000-01-01", "0000-01-01T00:00:00")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `year one is a date`() {
        val table = singleColumnCSVTable("0001-01-01")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(date(1, 1, 1))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `date-time with an offset or zone is text`() {
        val values = listOf("2001-04-20T10:15:30Z", "2001-04-20T10:15:30+02:00")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\""
            )
        }
    }

    @Test
    fun `dates mixed with integers are text`() {
        val table = singleColumnCSVTable("2001-04-20", "2001")
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Text("2001-04-20"), Text("2001"))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    // Text is the fallback

    @Test
    fun `mixed column is text and keeps values unchanged`() {
        val table = singleColumnCSVTable("5", "five")
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Text("5"), Text("five"))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `text values keep surrounding whitespace`() {
        val table = singleColumnCSVTable(" a ", "b ")
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Text(" a "), Text("b "))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    // Whole table

    @Test
    fun `each column is typed on its own and keeps its name and position`() {
        val table = RawCsv(
            header = listOf("name", "age", "born", ""),
            rows = listOf(
                listOf("Anna", "30", "2001-04-20", ""),
                listOf("Bram", "", "1978-01-02T00:00:00", "extra"),
            ),
            irregularRowCount = 1,
        ).toCsvTable()
        val expectedColumns = listOf(
            Column("name", ColumnType.TEXT),
            Column("age", ColumnType.INTEGER),
            Column("born", ColumnType.DATE),
            Column("", ColumnType.TEXT),
        )
        val expectedRows = listOf(
            listOf(Text("Anna"), Integer(30), date(2001, 4, 20), Text("")),
            listOf(Text("Bram"), Empty, date(1978, 1, 2, LocalTime.MIDNIGHT), Text("extra")),
        )
        val expectedIrregularRowCount = 1

        assertEquals(expectedColumns, table.columns)
        assertEquals(expectedRows, table.rows)
        assertEquals(expectedIrregularRowCount, table.irregularRowCount)
    }

    @Test
    fun `header-only file gives text columns and no rows`() {
        val table = RawCsv(
            header = listOf("a", "b"),
            rows = emptyList(),
            irregularRowCount = 0
        ).toCsvTable()
        val expectedColumns = listOf(Column("a", ColumnType.TEXT), Column("b", ColumnType.TEXT))
        val expectedRows = emptyList<List<CellValue>>()

        assertEquals(expectedColumns, table.columns)
        assertEquals(expectedRows, table.rows)
    }

    /** Converts a file with one column, named `value`, holding [values] from top to bottom. */
    private fun singleColumnCSVTable(vararg values: String): CsvTable =
        RawCsv(
            header = listOf("value"),
            rows = values.map { listOf(it) },
            irregularRowCount = 0,
        ).toCsvTable()

    private fun CsvTable.singleColumnTableType(): ColumnType = columns.single().type

    private fun CsvTable.cells(): List<CellValue> = rows.map { it.single() }

    private fun date(
        year: Int,
        month: Int,
        day: Int,
        time: LocalTime? = null
    ) = CellValue.Date(
        LocalDate.of(year, month, day),
        time
    )
}
