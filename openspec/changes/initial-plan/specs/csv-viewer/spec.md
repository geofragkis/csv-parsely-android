# Spec Delta

## Purpose

Shows the downloaded and typed CSV data on a single screen, with clear loading, empty and error states and feedback through a snackbar.

## ADDED Requirements

### Requirement: Load the data when the screen opens
The system SHALL start downloading and parsing the CSV when the screen opens, and SHALL show a loading indicator until it finishes. A configuration change such as a rotation SHALL NOT start a new download.

#### Scenario: Opening the app
- **WHEN** the user opens the app
- **THEN** a loading indicator is shown until the data or an error is available

#### Scenario: Rotation during or after loading
- **WHEN** the device is rotated
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
The system SHALL show INTEGER values as plain digits with a sign for negatives, DATE values as a date in the device's locale format followed by the time only when the time is not midnight, TEXT values unchanged, and empty values as a dash.

#### Scenario: Date at midnight
- **WHEN** a DATE value is `1978-01-02T00:00:00` on a device set to English (US)
- **THEN** it is shown as a date only, for example `Jan 2, 1978`

#### Scenario: Empty value
- **WHEN** a value is empty
- **THEN** it is shown as `-`

### Requirement: Empty table message
The system SHALL show a message saying the file has no rows when the file has a header but no data rows.

#### Scenario: Header-only file
- **WHEN** the downloaded file has only a header
- **THEN** a no-rows message is shown instead of the list

### Requirement: Errors are shown in a snackbar with Retry
The system SHALL show a snackbar with a message for the failure kind (no connection, server error with its code, not enough storage, empty file, malformed file, unsupported encoding) when loading fails. It SHALL offer a Retry action for every kind except unsupported encoding, where downloading again cannot help. The snackbar SHALL stay until it is dismissed or Retry is pressed. Retry SHALL start the download again.

#### Scenario: Offline on start
- **WHEN** loading fails because the device is offline
- **THEN** a snackbar says there is no connection and offers Retry

#### Scenario: File is not UTF-8
- **WHEN** loading fails because the file is not UTF-8 encoded
- **THEN** a snackbar says the file's encoding is not supported, offers no Retry, and stays until dismissed

#### Scenario: Retry succeeds
- **WHEN** the user presses Retry and the download now succeeds
- **THEN** the loading indicator is shown, then the rows

### Requirement: Irregular rows are reported
The system SHALL show a snackbar with the number of irregular rows when the file loaded but some rows had a different field count than the header. The rows SHALL still be shown.

#### Scenario: Two irregular rows
- **WHEN** a file with two irregular rows has loaded
- **THEN** the rows are shown and a snackbar says 2 rows had a different number of fields than the header

### Requirement: Feedback is shown once
The system SHALL show each snackbar message once. It SHALL NOT appear again after a configuration change.

#### Scenario: Rotation after an error snackbar
- **WHEN** an error snackbar was shown and the device is rotated
- **THEN** the same message is not shown a second time
