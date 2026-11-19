package gio.frag.csvparsely.core.domain.inference

import gio.frag.csvparsely.core.domain.model.CellValue
import gio.frag.csvparsely.core.domain.model.CellValue.Empty
import gio.frag.csvparsely.core.domain.model.CellValue.Image
import gio.frag.csvparsely.core.domain.model.CellValue.Integer
import gio.frag.csvparsely.core.domain.model.CellValue.Text
import gio.frag.csvparsely.core.domain.model.ColumnType
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
class ColumnTypeInferenceTest {

    // One type per column

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
    fun `decimal number is text`() {
        val table = singleColumnCSVTable("1.5")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
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
    fun `invalid calendar date is text`() {
        val table = singleColumnCSVTable("2023-02-30")
        val expectedType = ColumnType.TEXT

        assertEquals(expectedType, table.singleColumnTableType())
    }

    @Test
    fun `years outside 0001 to 9999 are text`() {
        val values = listOf("0000-01-01", "-0001-01-01", "+10000-01-01", "+999999999-12-31T23:59-18:00")
        val expectedType = ColumnType.TEXT

        values.forEach { value ->
            assertEquals(
                expectedType,
                singleColumnCSVTable(value).singleColumnTableType(),
                "for \"$value\"",
            )
        }
    }

    @Test
    fun `values with and without an offset in one column are dates`() {
        val table = singleColumnCSVTable("2001-04-20", "1978-01-02T08:30:00", "2001-04-20T10:15+02:00")
        val expectedType = ColumnType.DATE
        val expectedResult = listOf(
            date(2001, 4, 20),
            date(1978, 1, 2, LocalTime.of(8, 30)),
            date(2001, 4, 20, LocalTime.of(10, 15), ZoneOffset.ofHours(2)),
        )

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

    // Image values

    @Test
    fun `sample avatar column is text with image values`() {
        val avatar = "https://api.multiavatar.com/2cdf5db9b4dee297b7.png"
        val table = singleColumnCSVTable(avatar)
        val expectedType = ColumnType.TEXT
        val expectedResult = listOf(Image(avatar))

        assertEquals(expectedType, table.singleColumnTableType())
        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `image URL with a query and an upper-case extension is an image`() {
        val url = "https://example.com/photo.JPG?size=2"
        val table = singleColumnCSVTable(url)
        val expectedResult = listOf(Image(url))

        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `image URL with surrounding whitespace is a trimmed image`() {
        val table = singleColumnCSVTable(" https://example.com/a.png ")
        val expectedResult = listOf(Image("https://example.com/a.png"))

        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `image URL over plain http stays text`() {
        val url = "http://example.com/a.png"
        val table = singleColumnCSVTable(url)
        val expectedResult = listOf(Text(url))

        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `URL that is not an image stays text`() {
        val url = "https://example.com/report.pdf"
        val table = singleColumnCSVTable(url)
        val expectedResult = listOf(Text(url))

        assertEquals(expectedResult, table.cells())
    }

    @Test
    fun `text that is not a valid URL stays text`() {
        // The space makes it an invalid URL
        val url = "https://example.com/my photo.png"
        val table = singleColumnCSVTable(url)
        val expectedResult = listOf(Text(url))

        assertEquals(expectedResult, table.cells())
    }

    /** Converts a file with one column, named `value`, holding [values] from top to bottom. */
    private fun singleColumnCSVTable(vararg values: String): CsvTable =
        RawCsv(
            header = listOf("value"),
            rows = values.map { listOf(it) },
        ).toCsvTable()

    private fun CsvTable.singleColumnTableType(): ColumnType = columns.single().type

    private fun CsvTable.cells(): List<CellValue> = allRows().map { it.single() }

    /** Every row of the table, read through [CsvTable.row] as the screen would. */
    private fun CsvTable.allRows(): List<List<CellValue>> = (0 until rowCount).map { index -> row(index) }

    private fun date(
        year: Int,
        month: Int,
        day: Int,
        time: LocalTime? = null,
        offset: ZoneOffset? = null,
    ) = CellValue.Date(
        LocalDate.of(year, month, day),
        time,
        offset,
    )
}
