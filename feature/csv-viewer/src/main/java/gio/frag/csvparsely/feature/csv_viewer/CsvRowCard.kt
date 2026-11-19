package gio.frag.csvparsely.feature.csv_viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import gio.frag.csvparsely.core.domain.model.CellValue
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val EMPTY_VALUE = "-"

// Always 24-hour, whatever the device's 12/24-hour setting
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * One row of the table: its image values on the left, and every other column as its name with the formatted value.
 *
 * @param labels the name shown for each column
 * @param cells the row's values, one per column
 * @param dateFormatter formats the date part of a DATE value in the device's locale
 */
@Composable
internal fun CsvRowCard(
    labels: List<String>,
    cells: List<CellValue>,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    val labelledCells: List<Pair<String, CellValue>> = labels.zip(cells)

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (cells.any { it is CellValue.Image }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    labelledCells.forEach { (label, cell) ->
                        if (cell is CellValue.Image) {
                            CellImage(
                                url = cell.url,
                                label = label,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                labelledCells.forEach { (label, cell) ->
                    if (cell !is CellValue.Image) {
                        CellLine(
                            label = label,
                            value = cell.formatted(dateFormatter),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CellImage(
    url: String,
    label: String,
) {
    // The same avatar while loading and when loading fails
    val defaultAvatar = painterResource(R.drawable.feature_csv_viewer_default_avatar)
    AsyncImage(
        model = url,
        contentDescription = label,
        modifier = Modifier.size(48.dp),
        placeholder = defaultAvatar,
        error = defaultAvatar,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun CellLine(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(92.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** The value as shown on the card, for example `5`, `Jan 2, 1978, 00:00` or `-` for an empty value. */
private fun CellValue.formatted(dateFormatter: DateTimeFormatter): String =
    when (this) {
        is CellValue.Text -> value.ifBlank { EMPTY_VALUE }
        // This never happens, because the card shows images on the left; it fails if a future change shows one as a line
        is CellValue.Image -> error("Images are shown on the left of the card, never as a line")
        is CellValue.Integer -> value.toString()
        is CellValue.Date -> formatted(dateFormatter)
        CellValue.Empty -> EMPTY_VALUE
    }

/**
 * The date and the time when the file gave one. A value with an offset is first moved to the device's time zone;
 * one without is shown as written.
 */
private fun CellValue.Date.formatted(dateFormatter: DateTimeFormatter): String {
    if (time == null) {
        return date.format(dateFormatter)
    }
    val dateTime = if (offset == null) {
        LocalDateTime.of(date, time)
    } else {
        OffsetDateTime.of(date, time, offset)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    }
    return "${dateTime.format(dateFormatter)}, ${dateTime.format(TIME_FORMATTER)}"
}
