package gio.frag.csvparsely.feature.csv_viewer.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gio.frag.csvparsely.feature.csv_viewer.CsvViewerViewModel

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * The CSV screen, connected to its ViewModel.
 */
@Composable
fun CsvViewerScreen(
    modifier: Modifier = Modifier,
    viewModel: CsvViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CsvViewerContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
