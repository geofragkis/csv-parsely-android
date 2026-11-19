package gio.frag.csvparsely.core.data.download

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
import java.io.OutputStream

/**
 * Created by Giorgos Fragkis on 16-11-2026
 *
 * Downloads the CSV into `download.csv` in [directory], replacing the file of an earlier download.
 *
 * A failed or canceled download can leave a partial file. It is never parsed, because [download] throws, and the
 * next download overwrites it.
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
            .getOrElse { error ->
                // On cancellation, download is canceled with this scope, but execute() and the body reads block
                // their thread and don't notice. Canceling the call makes them throw at once, so the download stops
                if (error is CancellationException) call.cancel()
                throw error
            }
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

            val file = File(directory, FILE_NAME)
            // FileOutputStream empties the file first, so it replaces the one from an earlier download
            val fileOutput = runCatching { FileOutputStream(file) }
                .getOrElse {
                    // Can't create the file: a problem on the phone, not the network, so retrying won't help
                    throw storageError(it)
                }

            fileOutput.use { output ->
                copy(
                    input = response.body.byteStream(),
                    output = output,
                )
            }

            return file
        }
    }

    /**
     * Copies [input] into [output] in chunks of 8 KB, so the body is never held in memory as a whole. Reads and writes
     * are separate steps, so a dropped connection (network error) is told apart from a failed write to the phone's
     * storage, such as a full disk (storage error, since retrying won't help).
     */
    private fun CoroutineScope.copy(
        input: InputStream,
        output: OutputStream,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            // Each read waits for the next bytes from the connection; -1 means the whole body has arrived
            val read = runCatching { input.read(buffer) }
                .getOrElse { throw networkError(it) }
            if (read == -1) break

            runCatching { output.write(buffer, 0, read) }
                .getOrElse { throw storageError(it) }
        }
    }

    /**
     * Maps a failed request or read: an [IOException] becomes [CsvError.Network], anything else (such as running out
     * of memory) is passed on unchanged for the use case to map.
     */
    private fun CoroutineScope.networkError(error: Throwable): Throwable {
        // A canceled call also fails with an IOException; then the cancellation is the real reason
        ensureActive()
        return if (error is IOException) CsvError.Network(error) else error
    }

    /**
     * Maps a failed file creation or write: an [IOException] becomes [CsvError.Storage], anything else (such as running
     * out of memory) is passed on unchanged for the use case to map.
     */
    private fun storageError(error: Throwable): Throwable =
        if (error is IOException) CsvError.Storage(error) else error

    private companion object {
        const val FILE_NAME = "download.csv"
    }
}
