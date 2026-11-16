package gio.frag.csvparsely.core.data.repository

import gio.frag.csvparsely.core.data.CsvDownloader
import gio.frag.csvparsely.core.domain.di.IoDispatcher
import gio.frag.csvparsely.core.domain.model.RawCsv
import gio.frag.csvparsely.core.domain.repository.CsvRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
class CsvRepositoryImpl @Inject constructor(
    private val csvDownloader: CsvDownloader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CsvRepository {

    override suspend fun load(url: String): RawCsv {

        withContext(ioDispatcher) {
            csvDownloader.downloadFile(URL(url))
        }
        return RawCsv(
            header = listOf("Name", "Age", "City"),
            rows = listOf(
                listOf("Alice", "30", "New York"),
                listOf("Bob", "25", "Los Angeles"),
                listOf("Charlie", "35", "Chicago")
            )
        )
    }
}