package gio.frag.csvparsely.core.data

import gio.frag.csvparsely.core.domain.error.CsvError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Created by Giorgos Fragkis on 17-11-2026
 *
 * Downloads from a local MockWebServer into a temporary folder.
 *
 * The downloader makes real HTTP requests with a real OkHttpClient. `server.start()` opens a socket on this machine
 * (127.0.0.1) and listens on a port; each request the downloader sends there gets the next response queued with
 * `server.enqueue(...)`. Real sockets are what let these tests drop a connection mid-body or slow the body down.
 */
class CsvDownloaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val server = MockWebServer()
    private lateinit var directory: File
    private lateinit var downloader: CsvDownloader

    @Before
    fun setUp() {
        server.start()
        directory = File(temporaryFolder.root, "csv")
        downloader = CsvDownloader(OkHttpClient(), directory, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `a successful download stores the full body`() = runTest {
        server.enqueue(MockResponse.Builder().body(SAMPLE_CSV).build())

        val file = downloader.download(url())

        assertEquals(SAMPLE_CSV, file.readText())
        assertEquals(listOf(FILE_NAME), directory.list()?.toList())
    }

    @Test
    fun `a new download replaces the previous file`() = runTest {
        server.enqueue(MockResponse.Builder().body("old").build())
        server.enqueue(MockResponse.Builder().body("new").build())

        // The first download stores "old" as the CSV file
        downloader.download(url())
        // The second download gets "new", which should replace the file from the first one
        val file = downloader.download(url())

        assertEquals("new", file.readText())
        assertEquals(listOf(FILE_NAME), directory.list()?.toList())
    }

    @Test
    fun `an error status fails with its HTTP code`() = runTest {
        server.enqueue(MockResponse.Builder().code(404).build())

        val error = assertFailsWith<CsvError.Http> { downloader.download(url()) }

        assertEquals(404, error.code)
        assertNoFiles()
    }

    @Test
    fun `an unreachable server fails with a network error`() = runTest {
        val url = url()
        server.close()

        assertFailsWith<CsvError.Network> { downloader.download(url) }
        assertNoFiles()
    }

    @Test
    fun `a connection dropped mid-download fails with a network error and leaves no partial file`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body(SAMPLE_CSV)
                .onResponseBody(SocketEffect.CloseSocket())
                .build()
        )

        assertFailsWith<CsvError.Network> { downloader.download(url()) }
        assertNoFiles()
    }

    @Test
    fun `a canceled download leaves no partial file`() = runTest {
        // Sends 1 KB every 100 ms, so the download is still running when it is canceled
        server.enqueue(
            MockResponse.Builder()
                .body(LARGE_BODY)
                .throttleBody(1024, 100, TimeUnit.MILLISECONDS)
                .build()
        )
        val download = launch(Dispatchers.IO) { downloader.download(url()) }
        // Waits in real time until the first bytes have been written to the temporary file
        withContext(Dispatchers.IO) {
            while (temporaryFile().length() == 0L) Thread.sleep(10)
        }

        download.cancelAndJoin()

        assertTrue(download.isCancelled)
        assertNoFiles()
    }

    @Test
    fun `a write failure that is not a full disk is not reported as insufficient storage`() = runTest {
        server.enqueue(MockResponse.Builder().body(SAMPLE_CSV).build())
        // A regular file where the download folder should be, so the temporary file cannot be created
        directory.writeText("")

        assertFailsWith<FileNotFoundException> { downloader.download(url()) }
        assertFalse(temporaryFile().exists())
    }

    /**
     * The address the test server listens on, for example `http://127.0.0.1:54321/`. The server picks a free port when
     * it starts, so the address is only known after `server.start()` and differs on every run. A fixed port such as
     * 8080 could already be in use on the machine running the tests, which would make them fail for no real reason.
     */
    private fun url(): String = server.url("/").toString()

    private fun temporaryFile() = File(directory, TEMP_FILE_NAME)

    private fun assertNoFiles() {
        assertEquals(emptyList(), directory.list()?.toList().orEmpty())
    }

    private companion object {
        const val FILE_NAME = "download.csv"
        const val TEMP_FILE_NAME = "download.csv.tmp"
        const val SAMPLE_CSV = "\"First name\",\"Issue count\"\r\n\"Theo\",5"
        val LARGE_BODY = "x".repeat(64 * 1024)
    }
}
