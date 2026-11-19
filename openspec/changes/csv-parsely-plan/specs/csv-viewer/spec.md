# Spec Delta

## Purpose

Shows the downloaded and typed CSV data on a single screen, with clear loading and empty states, and errors in a snackbar.

## ADDED Requirements

### Requirement: Load the data when the screen opens
The system SHALL start downloading and parsing the CSV when the screen opens, and SHALL show a loading indicator until it finishes. A configuration change (for example dark mode, locale, font size or window size) SHALL NOT start a new download.

#### Scenario: Opening the app
- **WHEN** the user opens the app
- **THEN** a loading indicator is shown until the data or an error is available

#### Scenario: Configuration change during or after loading
- **WHEN** a configuration change occurs
- **THEN** the screen keeps its current state and no new download starts

### Requirement: One card per row
The system SHALL show each data row as a card in a vertically scrolling list, in file order. Each card SHALL list every column as its name followed by the formatted value. Cards SHALL NOT react to taps.

#### Scenario: Sample file
- **WHEN** the sample file has loaded
- **THEN** three cards are shown, and the first shows `First name` `Theo`, `Sur name` `Jansen`, `Issue count` `5`, `Date of birth` and `avatar` with their values

#### Scenario: Unnamed column
- **WHEN** a column has no name in the header
- **THEN** its values are labelled `Column <n>`, where n is its 1-based position

### Requirement: Format values by type
The system SHALL show INTEGER values as plain digits with a sign for negatives, TEXT values unchanged, and empty values as a dash. DATE values SHALL be shown with the date in the device's locale format: a value that has a time in the file SHALL always show that time in 24-hour format (`HH:mm`, midnight as `00:00`), and a date-only value SHALL show only the date.

#### Scenario: Date-time at midnight
- **WHEN** a DATE value is `1978-01-02T00:00:00` on a device set to English (US)
- **THEN** it is shown with its time, for example `Jan 2, 1978, 00:00`

#### Scenario: Date-time in the afternoon
- **WHEN** a DATE value is `2020-05-01T14:30:00` on a device set to English (US)
- **THEN** it is shown as, for example, `May 1, 2020, 14:30`

#### Scenario: Date-only value
- **WHEN** a DATE value is `2001-04-20` on a device set to English (US)
- **THEN** it is shown as a date only, for example `Apr 20, 2001`

#### Scenario: Empty value
- **WHEN** a value is empty
- **THEN** it is shown as `-`

### Requirement: Date-times with an offset in the device's time zone
The system SHALL convert a DATE value that has an offset to the device's time zone and show it like any other date-time. A DATE value without an offset SHALL be shown as written and never converted.

#### Scenario: Date-time with an offset
- **WHEN** a DATE value is `2001-04-20T10:15:30+02:00` on a device set to English (US) in the time zone UTC+01:00
- **THEN** it is shown in the device's time zone, for example `Apr 20, 2001, 09:15`

#### Scenario: Date of birth without an offset
- **WHEN** a DATE value is `1978-01-02T00:00:00` on a device in any time zone
- **THEN** it is shown as `Jan 2, 1978, 00:00`, never moved to another day

### Requirement: Empty state
The system SHALL show an empty state, an icon with the message "Nothing to show", instead of the list when nothing is loading and there are no rows to show: when the file has a header but no data rows, or when loading failed before any rows were shown. Rows that are already shown SHALL stay visible when an error is shown.

#### Scenario: Header-only file
- **WHEN** the downloaded file has only a header
- **THEN** the empty state is shown instead of the list, and no error snackbar

#### Scenario: Rows shown with an error
- **WHEN** the screen has rows to show and an error to show at the same time
- **THEN** the rows stay visible, the error snackbar is shown, and the empty state is not shown

### Requirement: Errors are shown in a snackbar
The system SHALL show, when loading fails, a snackbar with a message for the failure kind (no connection, server error with its code, empty file, malformed file, file too large to show, unexpected error). The snackbar SHALL stay on screen as long as the failure is shown, and SHALL be shown again after a configuration change.

#### Scenario: Offline on start
- **WHEN** loading fails because the device is offline
- **THEN** the empty state is shown and the snackbar says there is no connection

#### Scenario: Unexpected failure
- **WHEN** loading fails for a reason that is not one of the known kinds
- **THEN** the snackbar shows a generic error message and the app does not crash

#### Scenario: Configuration change after a failure
- **WHEN** loading failed and a configuration change occurs
- **THEN** the snackbar with its message and, if offered, its Retry action is still shown

### Requirement: Retry from the error snackbar
The system SHALL offer a Retry action on the error snackbar only for network-related and authentication failures: no connection, HTTP 401, 403, 408, 429 and 5xx. All other failures (malformed, empty, file too large, other HTTP 4xx, unexpected errors) SHALL NOT offer Retry. Retry SHALL start the download again and SHALL NOT be available while a download is in progress.

#### Scenario: Retry succeeds
- **WHEN** the user presses Retry and the download now succeeds
- **THEN** the loading indicator is shown, then the rows

#### Scenario: Malformed file
- **WHEN** loading fails because the file has an unclosed quote
- **THEN** the snackbar says the file is malformed and offers no Retry

#### Scenario: File too large to show
- **WHEN** loading fails because the app runs out of memory while reading the file
- **THEN** the snackbar says the file is too large to show, offers no Retry, and the app does not crash

#### Scenario: Server error
- **WHEN** loading fails with HTTP 503
- **THEN** the snackbar shows a server error with code 503 and offers Retry

#### Scenario: Access denied
- **WHEN** loading fails with HTTP 401 or 403
- **THEN** the snackbar shows a server error with that code and offers Retry

#### Scenario: Not found
- **WHEN** loading fails with HTTP 404
- **THEN** the snackbar shows a server error with code 404 and offers no Retry

#### Scenario: No Retry while loading
- **WHEN** a download is in progress
- **THEN** no Retry action is available

### Requirement: Portrait orientation
The system SHALL lock the screen to portrait orientation on devices where the platform allows apps to restrict orientation.

#### Scenario: Phone rotated to landscape
- **WHEN** a phone is turned to landscape
- **THEN** the screen stays in portrait
