package gio.frag.csvparsely.feature.csv_viewer

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gio.frag.csvparsely.core.domain.model.CsvTable
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * The CSV screen for a given [state], with no ViewModel, so it can be tested on its own.
 */
@Composable
internal fun CsvViewerContent(
    state: CsvUiState,
    modifier: Modifier = Modifier,
) {
    val table = state.table
    Scaffold(
        modifier = modifier,
        topBar = { CsvTopBar() },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            table != null && table.rowCount > 0 -> CsvRowList(
                table = table,
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
    val labels = table.columns.mapIndexed { index, column ->
        column.name.ifBlank { stringResource(R.string.feature_csv_viewer_unnamed_column, index + 1) }
    }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
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
