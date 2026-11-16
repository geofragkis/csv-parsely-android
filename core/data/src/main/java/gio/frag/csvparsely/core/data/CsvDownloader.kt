package gio.frag.csvparsely.core.data

import android.system.ErrnoException
import android.system.OsConstants
import gio.frag.csvparsely.core.domain.error.CsvError
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.BufferedSource
import okio.sink
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Downloads the CSV into [directory]. The body is written to a temporary file first, so a failed or cancelled
 * download never leaves a partial file behind.
 */
class CsvDownloader(
    private val okHttpClient: OkHttpClient,
    private val directory: File,
) {

    /** Downloads [url] and returns the stored file. Throws a [CsvError] when that fails. */
    suspend fun download(url: HttpUrl): File {
        val request = Request.Builder().url(url).build()
        val response = runCatching { okHttpClient.newCall(request).execute() }
            .getOrElse { throw CsvError.Network(it) }

        response.use {
            if (!response.isSuccessful) throw CsvError.Http(response.code)
            directory.mkdirs()
            val tempFile = File(directory, TEMP_FILE_NAME)
            val file = File(directory, FILE_NAME)
            runCatching {
                copy(response.body.source(), tempFile)
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }.onFailure {
                tempFile.delete()
            }.getOrThrow()
            return file
        }
    }

    /** Reads and writes in separate steps, so a dropped connection is not mistaken for a full disk. */
    private suspend fun copy(source: BufferedSource, target: File) {
        val sink = runCatching { target.sink() }.getOrElse { throw writeError(it) }
        sink.use {
            val buffer = Buffer()
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = runCatching { source.read(buffer, CHUNK_SIZE) }
                    .getOrElse { throw CsvError.Network(it) }
                if (read == -1L) break
                runCatching { sink.write(buffer, read) }
                    .getOrElse { throw writeError(it) }
            }
        }
    }

    private fun writeError(error: Throwable): CsvError =
        if (error.isOutOfSpace()) CsvError.InsufficientStorage(error) else CsvError.Unknown(error)

    private fun Throwable.isOutOfSpace(): Boolean =
        generateSequence(this) { it.cause }.any { it is ErrnoException && it.errno == OsConstants.ENOSPC }

    private companion object {
        const val FILE_NAME = "download.csv"
        const val TEMP_FILE_NAME = "download.csv.tmp"
        const val CHUNK_SIZE = 8 * 1024L
    }
}
