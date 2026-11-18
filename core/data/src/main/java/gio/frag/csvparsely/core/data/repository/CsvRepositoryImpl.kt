package gio.frag.csvparsely.core.data.repository

import gio.frag.csvparsely.core.data.download.CsvDownloader
import gio.frag.csvparsely.core.domain.model.RawCsv
import gio.frag.csvparsely.core.domain.repository.CsvRepository
import javax.inject.Inject

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
class CsvRepositoryImpl @Inject constructor(
    private val csvDownloader: CsvDownloader
) : CsvRepository {

    override suspend fun load(): RawCsv {

        csvDownloader.download("https://raw.githubusercontent.com/RabobankDev/AssignmentCSV/main/issues.csv")

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