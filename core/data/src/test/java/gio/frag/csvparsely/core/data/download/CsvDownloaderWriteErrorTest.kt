package gio.frag.csvparsely.core.data.download

import android.system.ErrnoException
import android.system.OsConstants
import gio.frag.csvparsely.core.domain.error.CsvError
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Created by Giorgos Fragkis on 17-11-2026
 *
 * Runs with Robolectric, because ErrnoException and OsConstants are only empty stubs in a plain JVM test.
 */
@RunWith(RobolectricTestRunner::class)
class CsvDownloaderWriteErrorTest {

    private val downloader = CsvDownloader(OkHttpClient(), File("unused"), Dispatchers.IO)

    @Test
    fun `a full disk becomes InsufficientStorage`() {
        val error = IOException(ErrnoException("write", OsConstants.ENOSPC))

        assertEquals(CsvError.InsufficientStorage(error), downloader.writeError(error))
    }

    @Test
    fun `another system error becomes Unknown`() {
        val error = IOException(ErrnoException("write", OsConstants.EACCES))

        assertEquals(CsvError.Unknown(error), downloader.writeError(error))
    }

    @Test
    fun `a write failure without a system error becomes Unknown`() {
        val error = IOException("Stream closed")

        assertEquals(CsvError.Unknown(error), downloader.writeError(error))
    }

    @Test
    fun `an error that is not an IOException is passed on unchanged`() {
        val error = OutOfMemoryError()

        assertSame(error, downloader.writeError(error))
    }
}
