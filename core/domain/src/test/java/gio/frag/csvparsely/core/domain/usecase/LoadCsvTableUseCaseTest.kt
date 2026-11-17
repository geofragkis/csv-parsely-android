package gio.frag.csvparsely.core.domain.usecase

import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.CellValue.Integer
import gio.frag.csvparsely.core.domain.model.CellValue.Text
import gio.frag.csvparsely.core.domain.model.Column
import gio.frag.csvparsely.core.domain.model.ColumnType
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import gio.frag.csvparsely.core.domain.repository.CsvRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
class LoadCsvTableUseCaseTest {

    @Test
    fun `a loaded file returns the typed table`() = runTest {
        val rawCsv = RawCsv(
            header = listOf("First name", "Issue count"),
            rows = listOf(listOf("Theo", "5")),
        )
        val expectedTable = CsvTable(
            columns = listOf(
                Column("First name", ColumnType.TEXT),
                Column("Issue count", ColumnType.INTEGER),
            ),
            rows = listOf(listOf(Text("Theo"), Integer(5))),
        )
        val useCase = useCase(FakeCsvRepository { rawCsv })

        val result = useCase()

        assertEquals(expectedTable, result.getOrThrow())
    }

    @Test
    fun `a CsvError from the repository is passed on as is`() = runTest {
        val error = CsvError.Http(404)
        val useCase = useCase(FakeCsvRepository { throw error })

        val result = useCase()

        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `running out of memory becomes FileTooLarge with the original as cause`() = runTest {
        val outOfMemory = OutOfMemoryError("too large")
        val useCase = useCase(FakeCsvRepository { throw outOfMemory })

        val result = useCase()

        val error = assertIs<CsvError.FileTooLarge>(result.exceptionOrNull())
        assertSame(outOfMemory, error.cause)
    }

    @Test
    fun `an unknown exception becomes Unknown with the original as cause`() = runTest {
        val exception = IllegalStateException("boom")
        val useCase = useCase(FakeCsvRepository { throw exception })

        val result = useCase()

        val error = assertIs<CsvError.Unknown>(result.exceptionOrNull())
        assertSame(exception, error.cause)
    }

    @Test
    fun `cancellation is rethrown instead of becoming a failure`() = runTest {
        val useCase = useCase(FakeCsvRepository { throw CancellationException("cancelled") })

        assertFailsWith<CancellationException> { useCase() }
    }

    private fun TestScope.useCase(repository: CsvRepository) =
        LoadCsvTableUseCase(repository, StandardTestDispatcher(testScheduler))
}

/** A repository that runs [onLoad] instead of downloading and parsing. */
private class FakeCsvRepository(private val onLoad: () -> RawCsv) : CsvRepository {
    override suspend fun load(): RawCsv = onLoad()
}
