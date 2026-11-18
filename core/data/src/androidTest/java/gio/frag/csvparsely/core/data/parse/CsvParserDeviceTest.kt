package gio.frag.csvparsely.core.data.parse

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import gio.frag.csvparsely.core.domain.inference.toCsvTable
import gio.frag.csvparsely.core.domain.model.CsvTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * Created by Giorgos Fragkis on 18-11-2026
 *
 * Parses and types a large file on a real device or emulator, where the memory heap is smaller and the CPU slower
 * than in the JVM unit tests.
 */
@RunWith(AndroidJUnit4::class)
class CsvParserDeviceTest {

    // Android test method names can't contain spaces, so this one uses camel case
    @Test
    fun hundredThousandRowsAreParsedAndTypedWithinTenSeconds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "large.csv")
        file.bufferedWriter().use { writer ->
            writer.write("id,name,date\r\n")
            repeat(100_000) { index ->
                writer.write("$index,\"Name $index\",2001-04-20T10:15:00\r\n")
            }
        }

        lateinit var table: CsvTable
        val duration = measureTime {
            table = CsvParser()
                .parse { file.inputStream() }
                .toCsvTable()
        }

        assertEquals(100_000, table.rowCount)
        assertTrue("took $duration", duration < 10.seconds)
    }

    // Temporary (task 5.8): measures 1,000,000 rows on a device, then is removed again
    @Test
    fun millionRowsAreParsedAndTypedWithinTwoMinutes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "very-large.csv")
        file.bufferedWriter().use { writer ->
            writer.write("id,name,date\r\n")
            repeat(1_000_000) { index ->
                writer.write("$index,\"Name $index\",2001-04-20T10:15:00\r\n")
            }
        }

        lateinit var table: CsvTable
        val duration = measureTime {
            table = CsvParser()
                .parse { file.inputStream() }
                .toCsvTable()
        }

        assertEquals(1_000_000, table.rowCount)
        assertTrue("took $duration", duration < 120.seconds)
    }
}
