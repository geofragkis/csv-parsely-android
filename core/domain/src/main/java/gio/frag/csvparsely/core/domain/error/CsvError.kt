package gio.frag.csvparsely.core.domain.error

/**
 * Every way loading the CSV can fail. It travels inside a `Result`, and the screen shows a message for each kind.
 */
sealed class CsvError(cause: Throwable? = null) : Exception(cause) {

    /** The server could not be reached or the connection dropped. */
    data class Network(
        override val cause: Throwable?
    ) : CsvError(cause)

    /** The server answered with a non-2xx status [code]. */
    data class Http(val code: Int) : CsvError()

    /** There is not enough free space to store the downloaded file. */
    data class InsufficientStorage(
        override val cause: Throwable?
    ) : CsvError(cause)

    /** The file has no content at all, not even a header. */
    class EmptyFile : CsvError()

    /** A quoted field opened on [line] is never closed. */
    data class Malformed(val line: Int) : CsvError()

    /** The file is not valid UTF-8. */
    data class UnsupportedEncoding(
        override val cause: Throwable?
    ) : CsvError(cause)

    /** Anything we did not expect. The original exception is kept as the [cause]. */
    data class Unknown(
        override val cause: Throwable
    ) : CsvError(cause)
}
