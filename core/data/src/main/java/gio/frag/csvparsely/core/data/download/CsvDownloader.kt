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
            // Copies the body into the file in chunks of 8 KB, so it is never held in memory as a whole.
            // FileOutputStream empties the file first, so it replaces the one from an earlier download. Any
            // IOException while reading or writing is reported as a network error: a dropped connection, but also a
            // full disk, which is rare for a small file in the cache directory
            runCatching {
                FileOutputStream(file).use { output ->
                    response.body.byteStream().copyTo(output)
                }
            }.getOrElse { throw networkError(it) }
            return file
        }
    }

    /**
     * Maps a failed request or copy: an [IOException] becomes [CsvError.Network], anything else (such as running out
     * of memory) is passed on unchanged for the use case to map.
     */
    private fun CoroutineScope.networkError(error: Throwable): Throwable {
        // A canceled call also fails with an IOException; then the cancellation is the real reason
        ensureActive()
        return if (error is IOException) CsvError.Network(error) else error
    }

    private companion object {
        const val FILE_NAME = "download.csv"
    }
}
