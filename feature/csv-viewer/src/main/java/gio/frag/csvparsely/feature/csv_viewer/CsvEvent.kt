package gio.frag.csvparsely.feature.csv_viewer

/**
 * Created by Giorgos Fragkis on 19-11-2026
 *
 * What the user can do on the CSV screen.
 */
sealed interface CsvEvent {

    /** Load the file again after a failure. */
    data object Retry : CsvEvent
}
