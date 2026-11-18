package gio.frag.csvparsely.core.data.repository

import gio.frag.csvparsely.core.data.download.CsvDownloader
import gio.frag.csvparsely.core.data.parse.CsvParser
import gio.frag.csvparsely.core.domain.model.RawCsv
import gio.frag.csvparsely.core.domain.repository.CsvRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Created by Giorgos Fragkis on 16-11-2026
 *
 * Downloads the CSV from [url] and parses it.
 */
class CsvRepositoryImpl(
    private val url: String,
    private val csvDownloader: CsvDownloader,
    private val csvParser: CsvParser,
    private val ioDispatcher: CoroutineDispatcher,
) : CsvRepository {

    override suspend fun load(): RawCsv {
        val file = csvDownloader.download(url)
        // Parsing reads the file from disk, so it runs on the IO dispatcher
        return withContext(ioDispatcher) {
            csvParser.parse { file.inputStream() }
        }
    }
}
