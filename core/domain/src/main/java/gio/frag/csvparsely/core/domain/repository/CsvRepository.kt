package gio.frag.csvparsely.core.domain.repository

import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.RawCsv

/**
 * Created by Giorgos Fragkis on 15-11-2026
 *
 * Gets the CSV file and reads it into rows of text.
 */
interface CsvRepository {

    /** Downloads and parses the file. Throws a [CsvError] when that fails. */
    suspend fun load(): RawCsv
}
