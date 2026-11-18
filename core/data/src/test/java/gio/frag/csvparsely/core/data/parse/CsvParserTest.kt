package gio.frag.csvparsely.core.data.parse

import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.RawCsv
import org.junit.Test
import java.nio.charset.Charset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Created by Giorgos Fragkis on 18-11-2026
 */
class CsvParserTest {

    private val parser = CsvParser()

    // First row is the header

    @Test
    fun `sample file gives five column names and three rows`() {
        val csv = parse(SAMPLE_CSV)
        val expectedHeader = listOf("First name", "Sur name", "Issue count", "Date of birth", "avatar")
        val expectedFirstRow = listOf(
            "Theo",
            "Jansen",
            "5",
            "1978-01-02T00:00:00",
            "https://api.multiavatar.com/2cdf5db9b4dee297b7.png",
        )

        assertEquals(expectedHeader, csv.header)
        assertEquals(3, csv.rows.size)
        assertEquals(expectedFirstRow, csv.rows.first())
    }

    @Test
    fun `header only gives the column names and no rows`() {
        val csv = parse("a,b,c")
        val expected = RawCsv(header = listOf("a", "b", "c"), rows = emptyList())

        assertEquals(expected, csv)
    }

    // Fields separated by commas, with optional quoting

    @Test
    fun `comma inside quotes is part of the field`() {
        // The file:
        // name,count
        // "Jansen, Theo",5
        val csv = parse("name,count\n\"Jansen, Theo\",5")
        val expectedRow = listOf("Jansen, Theo", "5")

        assertEquals(expectedRow, csv.rows.single())
    }

    @Test
    fun `doubled quote inside quotes is one literal quote`() {
        // The file:
        // text,other
        // "He said ""hi""",x
        val csv = parse("text,other\n\"He said \"\"hi\"\"\",x")
        val expectedRow = listOf("He said \"hi\"", "x")

        assertEquals(expectedRow, csv.rows.single())
    }

    @Test
    fun `line break inside quotes is part of the field and does not end the record`() {
        // The file:
        // First name,Surname
        // "Fiona","de
        // Vries"
        // "Theo","Jansen"
        val csv = parse("First name,Surname\n\"Fiona\",\"de\nVries\"\n\"Theo\",\"Jansen\"")
        val expectedRows = listOf(
            listOf("Fiona", "de\nVries"),
            listOf("Theo", "Jansen"),
        )

        assertEquals(expectedRows, csv.rows)
    }

    @Test
    fun `empty unquoted and quoted fields are empty values`() {
        // The file:
        // a,b,c
        // a,,""
        // The row has an empty unquoted field (nothing between the two commas) and an empty quoted field ("")
        val csv = parse("a,b,c\na,,\"\"")
        val expectedRow = listOf("a", "", "")

        assertEquals(expectedRow, csv.rows.single())
    }

    // Any common line ending

    @Test
    fun `CRLF without a final newline reads every record and keeps no carriage return`() {
        val csv = parse("a,b\r\n1,2\r\n3,4")
        val expected = RawCsv(
            header = listOf("a", "b"),
            rows = listOf(listOf("1", "2"), listOf("3", "4")),
        )

        assertEquals(expected, csv)
    }

    @Test
    fun `CR-only line endings separate the records`() {
        val csv = parse("a,b\r1,2\r3,4\r")
        val expected = RawCsv(
            header = listOf("a", "b"),
            rows = listOf(listOf("1", "2"), listOf("3", "4")),
        )

        assertEquals(expected, csv)
    }

    @Test
    fun `LF-only line endings separate the records`() {
        val csv = parse("a,b\n1,2\n3,4\n")
        val expected = RawCsv(
            header = listOf("a", "b"),
            rows = listOf(listOf("1", "2"), listOf("3", "4")),
        )

        assertEquals(expected, csv)
    }

    // Ignore byte order mark and blank lines

    @Test
    fun `byte order mark is not part of the first column name`() {
        // The file starts with an invisible byte order mark (\uFEFF), then:
        // name,age
        // Theo,5
        val csv = parse("\uFEFFname,age\nTheo,5")
        val expectedHeader = listOf("name", "age")

        assertEquals(expectedHeader, csv.header)
    }

    @Test
    fun `blank lines before the header are skipped, also in a single-column file`() {
        // The file, starting with two empty lines:
        //
        //
        // note
        // a
        val csv = parse("\n\nnote\na")
        val expected = RawCsv(header = listOf("note"), rows = listOf(listOf("a")))

        assertEquals(expected, csv)
    }

    @Test
    fun `trailing blank lines in a file with several columns give no rows`() {
        // The file, ending with two empty lines:
        // a,b
        // 1,2
        //
        //
        val csv = parse("a,b\r\n1,2\r\n\r\n\r\n")
        val expectedRows = listOf(listOf("1", "2"))

        assertEquals(expectedRows, csv.rows)
    }

    @Test
    fun `blank line in a single-column file is a row with an empty value`() {
        // The file:
        // note
        // a
        //
        // b
        val csv = parse("note\na\n\nb")
        val expectedRows = listOf(listOf("a"), listOf(""), listOf("b"))

        assertEquals(expectedRows, csv.rows)
    }

    @Test
    fun `line with only a quoted empty value is a padded row`() {
        // The file:
        // a,b,c
        // ""
        // 1,2,3
        val csv = parse("a,b,c\n\"\"\n1,2,3")
        val expectedRows = listOf(
            listOf("", "", ""),
            listOf("1", "2", "3"),
        )

        assertEquals(expectedRows, csv.rows)
    }

    // Keep rows with an irregular field count

    @Test
    fun `short row is padded with empty values`() {
        val csv = parse("a,b,c\n1,2")
        val expectedRow = listOf("1", "2", "")

        assertEquals(expectedRow, csv.rows.single())
    }

    @Test
    fun `long row adds an unnamed column and pads the other rows`() {
        val csv = parse("a,b\n1,2\n3,4,5")
        val expected = RawCsv(
            header = listOf("a", "b", ""),
            rows = listOf(listOf("1", "2", ""), listOf("3", "4", "5")),
        )

        assertEquals(expected, csv)
    }

    // Only UTF-8 encoded files

    @Test
    fun `non-ASCII text encoded as UTF-8 is parsed`() {
        val csv = parse("name\nDaniëlle")
        val expectedRows = listOf(listOf("Daniëlle"))

        assertEquals(expectedRows, csv.rows)
    }

    @Test
    fun `text encoded as Windows-1252 fails with an unsupported-encoding error`() {
        // In Windows-1252 the ë is the single byte 0xEB, which is not valid UTF-8
        val bytes = "name\nDaniëlle".toByteArray(Charset.forName("windows-1252"))

        assertFailsWith<CsvError.UnsupportedEncoding> { parser.parse(bytes.inputStream()) }
    }

    // Reject files that cannot be parsed

    @Test
    fun `unclosed quote fails with the line where it opened`() {
        // The file, with line numbers:
        // 1: a,b
        // 2: "x
        // 3: y",1
        // 4: "open,4
        // 5: 5,6
        // The record on lines 2 and 3 spans two lines, so the quote on line 4 opens in the third record
        val text = "a,b\n\"x\ny\",1\n\"open,4\n5,6"

        val error = assertFailsWith<CsvError.Malformed> { parse(text) }

        assertEquals(4, error.line)
    }

    @Test
    fun `empty file fails with an empty-file error`() {
        assertFailsWith<CsvError.EmptyFile> { parse("") }
    }

    @Test
    fun `file with only a byte order mark and blank lines fails with an empty-file error`() {
        // The file is an invisible byte order mark (\uFEFF) followed by three empty lines, ended by CRLF, LF and CR
        assertFailsWith<CsvError.EmptyFile> { parse("\uFEFF\r\n\n\r") }
    }

    /** Parses [text] encoded as UTF-8, as the downloaded file would be. */
    private fun parse(text: String): RawCsv = parser.parse(text.byteInputStream())

    private companion object {
        /** The sample file: quoted strings, unquoted integers, CRLF line endings and no final newline. */
        const val SAMPLE_CSV =
            "\"First name\",\"Sur name\",\"Issue count\",\"Date of birth\",\"avatar\"\r\n" +
                "\"Theo\",\"Jansen\",5,\"1978-01-02T00:00:00\",\"https://api.multiavatar.com/2cdf5db9b4dee297b7.png\"\r\n" +
                "\"Fiona\",\"de Vries\",7,\"1950-11-12T00:00:00\",\"https://api.multiavatar.com/b9339cb9e7a833cd5e.png\"\r\n" +
                "\"Petra\",\"Boersma\",1,\"2001-04-20T00:00:00\",\"https://api.multiavatar.com/2672c49d6099f87274.png\""
    }
}
