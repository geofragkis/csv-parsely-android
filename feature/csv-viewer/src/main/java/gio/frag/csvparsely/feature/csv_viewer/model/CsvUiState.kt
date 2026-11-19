package gio.frag.csvparsely.feature.csv_viewer.model

import androidx.compose.runtime.Immutable
import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.model.CsvTable

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * What the CSV screen shows. Each part is shown on its own, so rows and an error can be on screen together: the list
 * when [table] has rows, the loading indicator while [isLoading] and there are no rows, the empty state when nothing
 * is loading and there are no rows, and the error snackbar while [error] is set.
 *
 * @property isLoading whether the file is being downloaded and read
 * @property table the last table that was loaded, or null before the first success
 * @property error why the last load failed, or null when it didn't
 */
@Immutable
data class CsvUiState(
    val isLoading: Boolean = true,
    val table: CsvTable? = null,
    val error: CsvError? = null,
) {

    /** Whether the error snackbar offers Retry. */
    val canRetry: Boolean
        get() = error?.canRetry == true
}
