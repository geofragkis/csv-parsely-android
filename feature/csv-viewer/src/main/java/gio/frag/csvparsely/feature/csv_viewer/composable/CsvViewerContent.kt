package gio.frag.csvparsely.feature.csv_viewer.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import gio.frag.csvparsely.core.design_system.theme.CsvParselyTheme
import gio.frag.csvparsely.core.domain.error.CsvError
import gio.frag.csvparsely.core.domain.inference.toCsvTable
import gio.frag.csvparsely.core.domain.model.CsvTable
import gio.frag.csvparsely.core.domain.model.RawCsv
import gio.frag.csvparsely.feature.csv_viewer.model.CsvEvent
import gio.frag.csvparsely.feature.csv_viewer.model.CsvUiState
import gio.frag.csvparsely.feature.csv_viewer.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * The CSV screen for a given [state], with no ViewModel, so it can be tested on its own.
 *
 * @param onEvent receives what the user does, such as pressing Retry
 */
@Composable
internal fun CsvViewerContent(
    state: CsvUiState,
    onEvent: (CsvEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.error?.snackbarMessage()
    val retryLabel = stringResource(R.string.feature_csv_viewer_retry)

    // Shown while the error is set
    LaunchedEffect(state.error) {
        if (errorMessage == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = errorMessage,
            actionLabel = if (state.canRetry) retryLabel else null,
            duration = SnackbarDuration.Indefinite,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> onEvent(CsvEvent.Retry)
            // Nothing to do: Retry is the only action
            SnackbarResult.Dismissed -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { CsvTopBar() },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.table != null && state.table.rowCount > 0 -> CsvRowList(
                table = state.table,
                modifier = contentModifier,
            )

            state.isLoading -> Box(
                modifier = contentModifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            }

            else -> CsvEmptyState(modifier = contentModifier)
        }
    }
}

/** The snackbar message for this error. */
@Composable
private fun CsvError.snackbarMessage(): String =
    when (this) {
        is CsvError.Network -> stringResource(R.string.feature_csv_viewer_error_network)
        is CsvError.Http -> stringResource(R.string.feature_csv_viewer_error_http, code)
        is CsvError.Storage -> stringResource(R.string.feature_csv_viewer_error_storage)
        is CsvError.EmptyFile -> stringResource(R.string.feature_csv_viewer_error_empty_file)
        is CsvError.Malformed -> stringResource(R.string.feature_csv_viewer_error_malformed, line)
        is CsvError.FileTooLarge -> stringResource(R.string.feature_csv_viewer_error_file_too_large)
        is CsvError.Generic -> stringResource(R.string.feature_csv_viewer_error_generic)
    }

/** Shown when nothing is loading and there are no rows to show. */
@Composable
private fun CsvEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.feature_csv_viewer_inbox),
            // The text below says it already
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.feature_csv_viewer_empty),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** A plain bar instead of Material's `TopAppBar`, which is still experimental. */
@Composable
private fun CsvTopBar() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.feature_csv_viewer_title),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun CsvRowList(
    table: CsvTable,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val labels = remember(table) {
        table.columns.mapIndexed { index, column ->
            column.name.ifBlank {
                resources.getString(
                    R.string.feature_csv_viewer_unnamed_column,
                    index + 1,
                )
            }
        }
    }
    val locale = LocalConfiguration.current.locales[0]

    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            count = table.rowCount,
            // Rows can repeat, so their position is the unique, stable key
            key = { index -> index },
            contentType = { "row" },
        ) { index ->
            // Converts only the rows on screen, once each
            val cells = remember(table, index) { table.row(index) }
            CsvRowCard(
                labels = labels,
                cells = cells,
                dateFormatter = dateFormatter,
            )
        }
    }
}

@Preview
@Composable
private fun CsvViewerContentLoadingPreview() {
    CsvParselyTheme {
        CsvViewerContent(
            state = CsvUiState(isLoading = true),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun CsvViewerContentRowsPreview() {
    val table = RawCsv(
        header = listOf("First name", "Sur name", "Issue count", "Date of birth", "avatar"),
        rows = listOf(
            listOf("Theo", "Jansen", "5", "1978-01-02T00:00:00", "https://api.multiavatar.com/2cdf5db9b4dee297b7.png"),
            listOf("Fiona", "de Vries", "7", "1950-11-12T00:00:00", "https://api.multiavatar.com/b9339cb9e7a833cd5e.png"),
            listOf("Petra", "Boersma", "1", "2001-04-20T00:00:00", "https://api.multiavatar.com/2672c49d6099f87274.png"),
        ),
    ).toCsvTable()
    CsvParselyTheme {
        CsvViewerContent(
            state = CsvUiState(
                isLoading = false,
                table = table,
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun CsvViewerContentEmptyPreview() {
    CsvParselyTheme {
        CsvViewerContent(
            state = CsvUiState(isLoading = false),
            onEvent = {},
        )
    }
}

// The snackbar is shown from a LaunchedEffect, so it appears in the preview's interactive mode
@Preview
@Composable
private fun CsvViewerContentErrorWithRetryPreview() {
    CsvParselyTheme {
        CsvViewerContent(
            state = CsvUiState(
                isLoading = false,
                error = CsvError.Network(cause = null),
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun CsvViewerContentErrorWithoutRetryPreview() {
    CsvParselyTheme {
        CsvViewerContent(
            state = CsvUiState(
                isLoading = false,
                error = CsvError.Malformed(line = 3),
            ),
            onEvent = {},
        )
    }
}
