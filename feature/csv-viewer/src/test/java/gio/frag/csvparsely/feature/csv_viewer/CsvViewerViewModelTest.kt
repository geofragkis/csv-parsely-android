package gio.frag.csvparsely.feature.csv_viewer

import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.RawCsv
import gio.frag.csvparsely.core.domain.repository.CsvRepository
import gio.frag.csvparsely.core.domain.usecase.LoadCsvTableUseCase
import gio.frag.csvparsely.feature.csv_viewer.model.CsvEvent
import gio.frag.csvparsely.feature.csv_viewer.model.CsvUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val SAMPLE_CSV = RawCsv(
    header = listOf("First name", "Issue count"),
    rows = listOf(listOf("Theo", "5")),
)

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * Runs the ViewModel with the real use case and a fake repository, on a test dispatcher that only runs coroutines
 * when the test lets it (advanceUntilIdle).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CsvViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope runs on the main dispatcher, which doesn't exist in JVM tests
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the screen starts loading`() = runTest(testDispatcher) {
        val viewModel = viewModel(FakeCsvRepository { SAMPLE_CSV })

        val expectedState = CsvUiState(
            isLoading = true,
            table = null,
            error = null,
        )

        assertEquals(expectedState, viewModel.state.value)
    }

    @Test
    fun `a network failure fails with Retry`() = runTest(testDispatcher) {
        val error = CsvError.Network(cause = null)
        val viewModel = viewModel(FakeCsvRepository { throw error })
        val expectedState = CsvUiState(
            isLoading = false,
            table = null,
            error = error,
        )

        advanceUntilIdle()

        assertEquals(expectedState, viewModel.state.value)
        assertTrue(viewModel.state.value.canRetry)
    }

    @Test
    fun `Retry after a failure loads again`() = runTest(testDispatcher) {
        var failNextLoad = true
        val repository = FakeCsvRepository {
            if (failNextLoad) {
                failNextLoad = false
                throw CsvError.Network(cause = null)
            }
            SAMPLE_CSV
        }
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(CsvEvent.Retry)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.table?.rowCount)
        assertNull(viewModel.state.value.error)
        assertEquals(2, repository.loadCount)
    }

    @Test
    fun `Retry while loading is ignored`() = runTest(testDispatcher) {
        // The load waits until the test completes it, so the screen stays in Loading
        val loadResult = CompletableDeferred<RawCsv>()
        val repository = FakeCsvRepository { loadResult.await() }
        val viewModel = viewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(CsvEvent.Retry)
        loadResult.complete(SAMPLE_CSV)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.table?.rowCount)
        assertEquals(1, repository.loadCount)
    }

    private fun viewModel(repository: CsvRepository) = CsvViewerViewModel(
        loadCsvTable = LoadCsvTableUseCase(
            repository = repository,
            defaultDispatcher = testDispatcher,
        ),
    )
}

/** A repository that runs [onLoad] instead of downloading and parsing, and counts how often it was asked. */
private class FakeCsvRepository(private val onLoad: suspend () -> RawCsv) : CsvRepository {

    var loadCount = 0
        private set

    override suspend fun load(): RawCsv {
        loadCount++
        return onLoad()
    }
}
