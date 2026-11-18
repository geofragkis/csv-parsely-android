package gio.frag.csvparsely.core.data

import gio.frag.csvparsely.core.data.download.CsvDownloader
import gio.frag.csvparsely.core.data.parse.CsvParser
import gio.frag.csvparsely.core.data.repository.CsvRepositoryImpl
import gio.frag.csvparsely.core.domain.model.CellValue
import gio.frag.csvparsely.core.domain.model.Column
import gio.frag.csvparsely.core.domain.model.ColumnType
import gio.frag.csvparsely.core.domain.usecase.LoadCsvTableUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals

/**
 * Created by Giorgos Fragkis on 18-11-2026
 *
 * Serves the sample file from a local MockWebServer and loads it through the real downloader, parser, repository and
 * use case, from HTTP to the typed table.
 */
class LoadCsvTableIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `the sample file becomes a typed table`() = runTest {
        // The exact bytes of issues.csv: CRLF line endings and no final newline
        val sampleBytes = checkNotNull(javaClass.getResourceAsStream("/issues.csv")).readBytes()
        server.enqueue(MockResponse.Builder().body(Buffer().write(sampleBytes)).build())
        val repository = CsvRepositoryImpl(
            url = server.url("/issues.csv").toString(),
            csvDownloader = CsvDownloader(
                okHttpClient = OkHttpClient(),
                directory = File(temporaryFolder.root, "csv"),
                ioDispatcher = Dispatchers.IO,
            ),
            csvParser = CsvParser(),
            ioDispatcher = Dispatchers.IO,
        )
        val loadCsvTable = LoadCsvTableUseCase(
            repository = repository,
            defaultDispatcher = Dispatchers.Default,
        )

        val table = loadCsvTable().getOrThrow()
        val expectedColumns = listOf(
            Column("First name", ColumnType.TEXT),
            Column("Sur name", ColumnType.TEXT),
            Column("Issue count", ColumnType.INTEGER),
            Column("Date of birth", ColumnType.DATE),
            Column("avatar", ColumnType.TEXT),
        )
        val expectedRows = listOf(
            listOf(
                CellValue.Text("Theo"),
                CellValue.Text("Jansen"),
                CellValue.Integer(5),
                dateAtMidnight(1978, 1, 2),
                CellValue.Text("https://api.multiavatar.com/2cdf5db9b4dee297b7.png"),
            ),
            listOf(
                CellValue.Text("Fiona"),
                CellValue.Text("de Vries"),
                CellValue.Integer(7),
                dateAtMidnight(1950, 11, 12),
                CellValue.Text("https://api.multiavatar.com/b9339cb9e7a833cd5e.png"),
            ),
            listOf(
                CellValue.Text("Petra"),
                CellValue.Text("Boersma"),
                CellValue.Integer(1),
                dateAtMidnight(2001, 4, 20),
                CellValue.Text("https://api.multiavatar.com/2672c49d6099f87274.png"),
            ),
        )

        assertEquals(expectedColumns, table.columns)
        assertEquals(expectedRows, table.rows)
    }

    /** A date-time at midnight without an offset, as the sample writes its dates of birth. */
    private fun dateAtMidnight(
        year: Int,
        month: Int,
        day: Int,
    ): CellValue.Date = CellValue.Date(
        date = LocalDate.of(year, month, day),
        time = LocalTime.MIDNIGHT,
        offset = null,
    )
}
