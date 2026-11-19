package gio.frag.csvparsely.feature.csv_viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.usecase.LoadCsvTableUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * Loads the CSV when the screen opens and keeps what the screen shows in [state]. The ViewModel survives configuration
 * changes, so turning the phone or switching dark mode never starts a new download.
 */
@HiltViewModel
class CsvViewerViewModel @Inject constructor(
    private val loadCsvTable: LoadCsvTableUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CsvUiState())
    val state: StateFlow<CsvUiState> = _state.asStateFlow()

    init {
        loadCsv()
    }

    fun onEvent(event: CsvEvent) {
        when (event) {
            // Only after a failure that retrying can fix; a new load clears the error, so loads never overlap
            CsvEvent.Retry -> {
                if (_state.value.canRetry) {
                    loadCsv()
                }
            }
        }
    }

    private fun loadCsv() {
        // Keeps the table already shown, if any, and clears the previous error
        _state.update { state ->
            state.copy(
                isLoading = true,
                error = null,
            )
        }
        viewModelScope.launch {
            val result = loadCsvTable()
            _state.update { state ->
                state.withResult(result)
            }
        }
    }

    /** The state after a load finished: the new table on success, or the error while keeping the table shown. */
    private fun CsvUiState.withResult(result: Result<CsvTable>): CsvUiState =
        result.fold(
            onSuccess = { table ->
                copy(
                    isLoading = false,
                    table = table,
                )
            },
            onFailure = { error ->
                copy(
                    isLoading = false,
                    // The use case only fails with a CsvError
                    error = error as CsvError,
                )
            },
        )
}
