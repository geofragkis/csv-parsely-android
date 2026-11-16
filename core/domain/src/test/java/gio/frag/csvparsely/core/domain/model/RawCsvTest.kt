package gio.frag.csvparsely.core.domain.model

import org.junit.Test
import kotlin.test.assertFailsWith

class RawCsvTest {

    @Test
    fun `row shorter than the header is rejected`() {
        val header = listOf("a", "b")
        val rows = listOf(listOf("1"))

        assertFailsWith<IllegalArgumentException> {
            RawCsv(header = header, rows = rows, irregularRowCount = 1)
        }
    }

    @Test
    fun `row longer than the header is rejected`() {
        val header = listOf("a", "b")
        val rows = listOf(listOf("1", "2", "3"))

        assertFailsWith<IllegalArgumentException> {
            RawCsv(header = header, rows = rows, irregularRowCount = 1)
        }
    }

    @Test
    fun `negative irregular row count is rejected`() {
        val header = listOf("a")
        val rows = listOf(listOf("1"))

        assertFailsWith<IllegalArgumentException> {
            RawCsv(header = header, rows = rows, irregularRowCount = -1)
        }
    }

    @Test
    fun `irregular row count above the row count is rejected`() {
        val header = listOf("a")
        val rows = listOf(listOf("1"))

        assertFailsWith<IllegalArgumentException> {
            RawCsv(header = header, rows = rows, irregularRowCount = 2)
        }
    }
}
