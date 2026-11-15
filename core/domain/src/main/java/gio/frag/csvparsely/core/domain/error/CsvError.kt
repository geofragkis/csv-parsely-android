package gio.frag.csvparsely.core.domain.error

/**
 * Every way loading the CSV can fail. It travels inside a `Result`, and the screen shows a message for each kind.
 */
sealed class CsvError(cause: Throwable? = null) : Exception(cause) {

    /** The server could not be reached or the connection dropped. */
    data object Network : CsvError()

    /** The server answered with a non-2xx status [code]. */
    data class Http(val code: Int) : CsvError()

    /** There is not enough free space to store the downloaded file. */
    data object InsufficientStorage : CsvError()

    /** The file has no content at all, not even a header. */
    data object EmptyFile : CsvError()

    /** A quoted field opened on [line] is never closed. */
    data class Malformed(val line: Int) : CsvError()

    /** The file is not valid UTF-8. */
    data object UnsupportedEncoding : CsvError()

    /** Anything we did not expect. The original exception is kept as the [cause]. */
    class Unknown(cause: Throwable) : CsvError(cause)
}
