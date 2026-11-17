package gio.frag.csvparsely.core.data

import android.system.ErrnoException
import android.system.OsConstants
import gio.frag.csvparsely.core.domain.error.CsvError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Created by Giorgos Fragkis on 16-11-2026
 *
 * Downloads the CSV into [directory]. The body is written to a temporary file first, so a failed
 * or canceled download never leaves a partial file behind.
 *
 */
class CsvDownloader(
    private val okHttpClient: OkHttpClient,
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Downloads [url] and returns the stored file. Throws a [CsvError] when that fails. Safe to call from any
     * thread: the blocking work runs on [ioDispatcher].
     */
    suspend fun download(url: String): File = coroutineScope {
        val call = okHttpClient.newCall(Request.Builder().url(url).build())
        val download = async(ioDispatcher) { store(call) }
        runCatching { download.await() }
            .fold(
                onSuccess = { file -> file },
                onFailure = { error ->
                    // On cancellation, download is canceled with this scope, but execute() and the body reads block their
                    // thread and don't notice. Canceling the call makes them throw at once, so the download stops and
                    // deletes its partial file
                    if (error is CancellationException) call.cancel()
                    throw error
                },
            )
    }

    /** Runs [call] and stores the response body. Blocks the thread, so it only runs on [ioDispatcher]. */
    private fun CoroutineScope.store(call: Call): File {
        // An IOException here means the server could not be reached
        val response = runCatching { call.execute() }
            .getOrElse { throw networkError(it) }

        response.use {
            if (!response.isSuccessful) throw CsvError.Http(response.code)
            // Creates the download folder on the first run; does nothing when it already exists
            directory.mkdirs()
            // The body is written here first, so a partial download never takes the place of the real file. Each download
            // gets its own name, so a canceled download that is still cleaning up can never delete the temporary file of a
            // newer one. createTempFile puts a random number between the prefix and the suffix and creates the empty
            // file in one step (download<number>.csv.tmp); if a file with that name already exists, it picks another
            // number, so the file is always new
            val tempFile = runCatching { File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, directory) }
                .getOrElse { throw writeError(it) }
            // The file the parser reads, only replaced once the whole body is stored
            val file = File(directory, FILE_NAME)
            // Also catches CancellationException on purpose: the partial file must be deleted when the download is
            // canceled too, and the exception is rethrown unchanged
            return runCatching {
                copy(response.body.byteStream(), tempFile)
                // Puts the complete file in place, replacing the one from an earlier download
                runCatching { Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
                    .getOrElse { throw writeError(it) }
            }.fold(
                // Everything was written and moved: the file is ready to parse
                onSuccess = { file },
                onFailure = { exception ->
                    // Removes the partial file so it is never left behind
                    tempFile.delete()
                    // Passes the failure on unchanged: a CsvError from copy or the move, or the CancellationException
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
    private fun CoroutineScope.copy(body: InputStream, target: File) {
        // Holds one chunk at a time, reused for every read
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        // Opens an output stream to the new, empty temporary file.
        // The use { } block closes the stream when the loop ends, also when it ends with an exception
        FileOutputStream(target).use { output ->
            while (true) {
                // Throws CancellationException when the coroutine was canceled (its job is no longer active),
                // so the loop stops between chunks
                ensureActive()
                val read = runCatching { body.read(buffer) }
                    .getOrElse {
                        // Reading the body failed, so the connection broke mid-download
                        throw networkError(it)
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

    /**
     * Maps a failed request or body read: an [IOException] becomes [CsvError.Network], anything else (such as
     * running out of memory) is passed on unchanged for the use case to map.
     */
    private fun CoroutineScope.networkError(error: Throwable): Throwable {
        // A canceled call also fails with an IOException; then the cancellation is the real reason
        ensureActive()
        return if (error is IOException) CsvError.Network(error) else error
    }

    /**
     * Maps a failed write: an [IOException] becomes [CsvError.InsufficientStorage] when the disk is full (ENOSPC)
     * and [CsvError.Unknown] otherwise; anything else is passed on unchanged for the use case to map.
     * Internal so the mapping can be tested with a real Android ErrnoException.
     */
    internal fun writeError(error: Throwable): Throwable {
        if (error !is IOException) return error
        val errno = (error.cause as? ErrnoException)?.errno
        return if (errno == OsConstants.ENOSPC) CsvError.InsufficientStorage(error)
        else CsvError.Unknown(error)
    }

    private companion object {
        const val FILE_NAME = "download.csv"
        const val TEMP_FILE_PREFIX = "download"
        const val TEMP_FILE_SUFFIX = ".csv.tmp"
    }
}
