package gio.frag.csvparsely.core.domain.usecase

import gio.frag.csvparsely.core.domain.di.DefaultDispatcher
import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.repository.CsvRepository
import gio.frag.csvparsely.core.domain.util.toCsvTable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Loads the CSV file and gives every column a type. */
class LoadCsvTableUseCase @Inject constructor(
    private val repository: CsvRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Returns the typed table, or a failure holding a [CsvError]. An unexpected exception becomes [CsvError.Unknown].
     * A canceled load is not a failure: its `CancellationException` is rethrown.
     */
    suspend operator fun invoke(url: String): Result<CsvTable> {
        return runCatching {
            val rawCsv = repository.load(url)
            // Typing goes over every cell, so it runs off the main thread
            withContext(defaultDispatcher) {
                rawCsv.toCsvTable()
            }
        }.fold(
            onSuccess = { csvTable -> Result.success(csvTable) },
            onFailure = { exception ->
                when (exception) {
                    is CsvError -> Result.failure(exception)
                    is CancellationException -> throw exception
                    else -> Result.failure(CsvError.Unknown(exception))
                }
            }
        )
    }
}
