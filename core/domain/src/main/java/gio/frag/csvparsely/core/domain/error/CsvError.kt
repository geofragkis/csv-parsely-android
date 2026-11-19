package gio.frag.csvparsely.core.domain.error

/**
 * Created by Giorgos Fragkis on 15-11-2026
 *
 * Every way loading the CSV can fail. The repository throws it, the use case passes it on inside a `Result`, and the
 * screen shows a message for each kind.
 */
sealed class CsvError(cause: Throwable? = null) : Exception(cause) {

    /**
     * Whether loading again can help: true for network failures and for server answers that can change on a second
     * try (408, 429 and 5xx), false for problems with the file itself or anything unexpected. 401 and 403 are not
     * retried: the app sends no credentials, so the answer would be the same.
     */
    val canRetry: Boolean
        get() = when (this) {
            is Network -> true
            is Http -> code in RETRYABLE_HTTP_CODES || code in 500..599
            is Storage, is EmptyFile, is Malformed, is FileTooLarge, is Generic -> false
        }

    /** The server could not be reached or the connection dropped. */
    data class Network(
        override val cause: Throwable?
    ) : CsvError(cause)

    /** The server answered with a non-2xx status [code]. */
    data class Http(val code: Int) : CsvError()

    /** The file couldn't be created or written on the phone, for example because the storage is full. */
    data class Storage(
        override val cause: Throwable,
    ) : CsvError(cause)

    /** The file has no content at all, not even a header. */
    class EmptyFile : CsvError()

    /** A quoted field opened on [line] is never closed. */
    data class Malformed(val line: Int) : CsvError()

    /** The app ran out of memory while loading the file. The `OutOfMemoryError` is kept as the [cause]. */
    data class FileTooLarge(
        override val cause: Throwable
    ) : CsvError(cause)

    /** Anything we did not expect. The original exception is kept as the [cause]. */
    data class Generic(
        override val cause: Throwable
    ) : CsvError(cause)

    private companion object {
        val RETRYABLE_HTTP_CODES = setOf(408, 429)
    }
}
