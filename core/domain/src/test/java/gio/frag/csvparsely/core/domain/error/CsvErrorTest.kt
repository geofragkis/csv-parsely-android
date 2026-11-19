package gio.frag.csvparsely.core.domain.error

import org.junit.Test
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Created by Giorgos Fragkis on 19-11-2026
 */
class CsvErrorTest {

    @Test
    fun `network failures and some server answers can be retried`() {
        val errors = listOf(
            CsvError.Network(cause = null),
            CsvError.Http(401),
            CsvError.Http(403),
            CsvError.Http(408),
            CsvError.Http(429),
            CsvError.Http(500),
            CsvError.Http(503),
            CsvError.Http(599),
        )

        errors.forEach { error ->
            assertTrue(error.canRetry, "for $error")
        }
    }

    @Test
    fun `problems with the file and unexpected errors can't be retried`() {
        val errors = listOf(
            CsvError.Http(400),
            CsvError.Http(404),
            CsvError.Http(499),
            CsvError.Storage(cause = IOException()),
            CsvError.EmptyFile(),
            CsvError.Malformed(line = 4),
            CsvError.FileTooLarge(cause = OutOfMemoryError()),
            CsvError.Generic(cause = IllegalStateException()),
        )

        errors.forEach { error ->
            assertFalse(error.canRetry, "for $error")
        }
    }
}
