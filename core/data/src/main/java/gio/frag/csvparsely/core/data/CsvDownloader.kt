package gio.frag.csvparsely.core.data

import android.system.ErrnoException
import android.system.OsConstants
import gio.frag.csvparsely.core.domain.error.CsvError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Downloads the CSV into [directory]. The body is written to a temporary file first, so a failed
 * or canceled download never leaves a partial file behind.
 */
class CsvDownloader(
    private val okHttpClient: OkHttpClient,
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /** Downloads [url] and returns the stored file. Throws a [CsvError] when that fails. */
    suspend fun download(url: String): File {
        val request = Request.Builder().url(url).build()
        // An IOException here means the server could not be reached
        val response = runCatching { okHttpClient.newCall(request).execute() }
            .getOrElse { throw CsvError.Network(it) }

        response.use {
            if (!response.isSuccessful) throw CsvError.Http(response.code)
            // Creates the download folder on the first run; does nothing when it already exists
            directory.mkdirs()
            // The body is written here first, so a partial download never takes the place of the real file
            val tempFile = File(directory, TEMP_FILE_NAME)
            // The file the parser reads, only replaced once the whole body is stored
            val file = File(directory, FILE_NAME)
            // Also catches CancellationException on purpose: the partial file must be deleted when the download is
            // canceled too, and the exception is rethrown unchanged
            return runCatching {
                copy(response.body.byteStream(), tempFile)
                // Puts the complete file in place, replacing the one from an earlier download
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }.fold(
                // Everything was written and moved: the file is ready to parse
                onSuccess = { file },
                onFailure = { exception ->
                    // Removes the partial file so it is never left behind
                    tempFile.delete()
                    // Passes the failure on unchanged: a CsvError from copy, an IOException from the move (the use
                    // case turns it into Unknown), or the CancellationException
                    throw exception
                },
            )
        }
    }

    /**
     * Streams the response [body] into [target] in chunks, so the file is never held in memory as a whole.
     *
     * The body is not downloaded yet when this starts: [body] reads straight from the connection. Each read waits
     * until some bytes have arrived and copies up to one buffer (8 KB) of them, returning how many it copied, which
     * can be fewer than 8 KB because data arrives in small network packets. Only that many bytes are written before
     * the next read reuses the same buffer. A read returns -1 once the whole body has arrived.
     *
     * Reads and writes are separate steps, so a dropped connection is not mistaken for a full disk.
     */
    private suspend fun copy(body: InputStream, target: File) = withContext(ioDispatcher) {
        // Holds one chunk at a time, reused for every read
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        // Opens an output stream to the temporary file, overwriting anything left from an earlier download.
        // The use { } block closes the stream when the loop ends, also when it ends with an exception
        FileOutputStream(target).use { output ->
            while (true) {
                // Throws CancellationException when the coroutine was canceled (its job is no longer active),
                // so the loop stops between chunks
                currentCoroutineContext().ensureActive()
                val read = runCatching { body.read(buffer) }
                    .getOrElse {
                        // Reading the body failed, so the connection broke mid-download
                        throw CsvError.Network(it)
                    }
                // -1 means the whole body has been read, so there is nothing left to write
                if (read == -1) break
                runCatching { output.write(buffer, 0, read) }
                    .getOrElse {
                        throw writeError(it)
                    }
            }
        }
    }

    private fun writeError(error: Throwable): CsvError {
        val errno = (error.cause as? ErrnoException)?.errno
        return if (errno == OsConstants.ENOSPC) CsvError.InsufficientStorage(error)
        else CsvError.Unknown(error)
    }

    private companion object {
        const val FILE_NAME = "download.csv"
        const val TEMP_FILE_NAME = "download.csv.tmp"
    }
}
