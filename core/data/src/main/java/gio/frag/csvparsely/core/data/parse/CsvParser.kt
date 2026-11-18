package gio.frag.csvparsely.core.data.parse

import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.RawCsv
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.MalformedInputException

/**
 * Created by Giorgos Fragkis on 17-11-2026
 *
 * Turns comma-separated text into a [RawCsv]: the first record is the header, every following record is a row.
 *
 * A record is one entry of the file: its fields, separated by commas, up to the line ending that closes it. It is
 * usually one line:
 * ```
 * First name,Surname
 * "Fiona","de Vries"
 * ```
 * This text has two lines and also has two records: the header `listOf("First name", "Surname")` and the row
 * `listOf("Fiona", "de Vries")`.
 * ```
 * First name,Surname
 * "Theo","Jansen"
 * "Fiona","de Vries"
 * ```
 * This text has three lines and also has three records: the header `listOf("First name", "Surname")` and the rows
 * `listOf("Theo", "Jansen")` and `listOf("Fiona", "de Vries")`.
 *
 * But a quoted field can contain line breaks, so one record can span several lines:
 * ```
 * First name,Surname
 * "Fiona","de
 * Vries"
 * ```
 * This text has three lines but only two records: the header `listOf("First name", "Surname")` and the row
 * `listOf("Fiona", "de\nVries")`, where the line break is part of the surname.
 *
 * The parser reads the file one character at a time instead of loading it all into memory first. While reading, it
 * keeps in memory the records it has finished so far and the record it is reading now.
 *
 * A record ends with any of the common line endings, made of CR (Carriage Return, `'\r'`) and LF (Line Feed, `'\n'`):
 *
 * | Line ending | Characters | Used by                                    |
 * |-------------|------------|--------------------------------------------|
 * | CRLF        | `\r\n`     | Windows, and the CSV standard (RFC 4180)   |
 * | LF          | `\n`       | Linux, macOS, Android                      |
 * | CR          | `\r`       | Classic macOS                              |
 */
class CsvParser {

    /** Where the parser is inside the current field. */
    private enum class State {
        /** Nothing of the field has been read yet. */
        FIELD_START,

        /** Inside a field that did not start with a quote. */
        UNQUOTED,

        /** Inside a quoted field, so commas and line breaks are part of the value. */
        IN_QUOTES,

        /** Just read a quote inside a quoted field: it either closes the field or, doubled, stands for one quote. */
        QUOTE_IN_QUOTES,
    }

    /**
     * Reads the file that [open] opens and returns the header and rows. [open] is called once, or twice when the file
     * is not valid UTF-8, and every stream it returns is closed here.
     *
     * The file is read as UTF-8. When it is not valid UTF-8, it is read again as Windows-1252, the default of Excel on
     * Windows, so the encoding never stops a file from loading: a file in another encoding shows wrong letters instead.
     *
     * Rows shorter than the header are padded with empty values. A row longer than the header adds unnamed (`""`)
     * columns, and every other row is padded to match.
     *
     * Throws [CsvError.EmptyFile] when there is no record other than empty lines, and [CsvError.Malformed] when a
     * quoted field is never closed. Other errors from the stream are passed on unchanged.
     */
    fun parse(open: () -> InputStream): RawCsv {
        // Fails on bytes that aren't valid UTF-8, so we can retry as Windows-1252
        val utf8Decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)

        val records = runCatching {
            open().use { input ->
                records(InputStreamReader(input, utf8Decoder).buffered())
            }
        }.recoverCatching { error ->
            if (error !is MalformedInputException) throw error
            // A plain charset never fails, it shows unknown bytes as �
            open().use { input ->
                records(InputStreamReader(input, WINDOWS_1252).buffered())
            }
        }.getOrThrow()

        // The header is the first record that is not an empty line
        val headerIndex = records.indexOfFirst { it.isNotEmpty() }
        if (headerIndex == -1) throw CsvError.EmptyFile()
        val header = records[headerIndex]

        val rows = mutableListOf<List<String>>()
        // Every record after the header is a row
        records.subList(headerIndex + 1, records.size)
            .forEach { record ->
                when {
                    record.isNotEmpty() -> rows.add(record)
                    // In a single-column file an empty line is an empty value
                    header.size == 1 -> rows.add(listOf(""))
                    // In a file with several columns an empty line is skipped
                    else -> Unit
                }
            }

        // The widest record decides how many columns the table has
        val width = maxOf(header.size, rows.maxOfOrNull { it.size } ?: 0)

        return RawCsv(
            header = header.padEnd(width),
            rows = rows.map { it.padEnd(width) },
        )
    }

    /**
     * Reads all of [reader] and returns its records in order. A completely empty line gives an empty list; a line with
     * only `""` gives one empty value, so the two can be told apart.
     *
     * CRLF, LF and CR each end a record. Inside quotes, they are part of the value instead.
     */
    private fun records(reader: BufferedReader): List<List<String>> {
        // The records read so far
        val records = mutableListOf<List<String>>()
        var state = State.FIELD_START
        // The record being read
        val record = RecordBuilder()
        // The line being read, counted from 1, and the line where the current quoted field opened
        var line = 1
        var quoteLine = 0
        // Whether the character read just before the current one was a CR ('\r'). When a LF ('\n') follows a CR, the
        // two are one CRLF line ending, so the LF must not end a second record or count a second line. False before
        // anything has been read
        var previousWasCr = false

        // Reads the 1st character as its UTF-16 code. Returns -1 when there is nothing left to read.
        var next = reader.read()
        // Skips a byte order mark by reading the 2nd character
        if (next == BOM) next = reader.read()
        while (next != END_OF_FILE) {
            val char = next.toChar()
            if (state == State.IN_QUOTES) {

                if (char == '"') {
                    state = State.QUOTE_IN_QUOTES
                } else {
                    record.append(char)
                }

            } else {
                when (char) {
                    ',' -> {
                        record.endField()
                        state = State.FIELD_START
                    }

                    '\r' -> {
                        records.add(record.endRecord())
                        state = State.FIELD_START
                    }

                    '\n' -> {
                        // After a CR the record has already ended: CRLF is one line ending
                        if (!previousWasCr) records.add(record.endRecord())
                        state = State.FIELD_START
                    }

                    '"' -> {
                        when (state) {
                            State.FIELD_START -> {
                                // The opening quote adds no character, but the record is no longer an empty line
                                record.start()
                                quoteLine = line
                                state = State.IN_QUOTES
                            }
                            // A doubled quote inside a quoted field stands for one quote
                            State.QUOTE_IN_QUOTES -> {
                                record.append(char)
                                state = State.IN_QUOTES
                            }
                            // A quote inside an unquoted field is kept as it is
                            else -> record.append(char)
                        }
                    }

                    else -> {
                        // Also text after a closing quote, which is kept instead of rejecting the file
                        record.append(char)
                        state = State.UNQUOTED
                    }
                }
            }
            // If \r, the line ended so do line++
            // If \n, ask: was the character just before this an \r?
            // - No: then this \n is the line ending on its own (Linux/Mac), the line ended so do line++
            // - Yes: then this \n is just the tail of a Windows \r\n. The \r already ended the line,
            //   so the \n is ignored.
            if (char == '\r' || (char == '\n' && !previousWasCr)) line++

            previousWasCr = char == '\r'
            // Reads the next character
            next = reader.read()
        }
        if (state == State.IN_QUOTES) throw CsvError.Malformed(quoteLine)
        // The last record has no line ending
        if (record.started) records.add(record.endRecord())
        return records
    }

    /** Adds empty values to the end until the list has [width] values. */
    private fun List<String>.padEnd(width: Int): List<String> =
        if (size == width) this else this + List(width - size) { "" }

    private companion object {
        /**
         * The byte order mark (U+FEFF): an invisible character that Excel puts at the start of a file saved as
         * "CSV UTF-8". If it were kept, the first column name would be `"\uFEFFFirst name"`, which looks like
         * `First name` on screen but is not equal to it.
         */
        const val BOM = 0xFEFF

        /** What [Reader.read] returns when there is nothing left to read. */
        const val END_OF_FILE = -1

        /** The encoding Excel on Windows uses for "CSV (Comma delimited)", read when a file is not valid UTF-8. */
        val WINDOWS_1252: Charset = Charset.forName("windows-1252")
    }
}
