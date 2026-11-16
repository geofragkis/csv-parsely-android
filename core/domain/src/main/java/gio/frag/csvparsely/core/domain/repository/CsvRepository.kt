package gio.frag.csvparsely.core.domain.repository

import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.RawCsv
import java.net.URL

/** Gets the CSV file and reads it into rows of text. */
interface CsvRepository {

    /** Downloads and parses the file. Throws a [CsvError] when that fails. */
    suspend fun load(url: String): RawCsv
}
