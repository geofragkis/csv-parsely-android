package gio.frag.csvparsely.core.domain.model

import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
class RawCsvTest {

    @Test
    fun `row shorter than the header is rejected`() {
        val header = listOf("a", "b")
        val rows = listOf(listOf("1"))

        assertFailsWith<IllegalArgumentException> {
            RawCsv(header = header, rows = rows)
        }
    }

    @Test
    fun `row longer than the header is rejected`() {
        val header = listOf("a", "b")
        val rows = listOf(listOf("1", "2", "3"))

        assertFailsWith<IllegalArgumentException> {
            RawCsv(header = header, rows = rows)
        }
    }
}
