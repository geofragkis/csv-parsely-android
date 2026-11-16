# Spec Delta

## Purpose

Shows the downloaded and typed CSV data on a single screen, with clear loading, empty and error states and feedback through a snackbar.

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
The system SHALL show INTEGER values as plain digits with a sign for negatives, TEXT values unchanged, and empty values as a dash. DATE values SHALL be shown in the device's locale format: a value that has a time in the file SHALL always show that time (midnight included), and a date-only value SHALL show only the date.

#### Scenario: Date-time at midnight
- **WHEN** a DATE value is `1978-01-02T00:00:00` on a device set to English (US)
- **THEN** it is shown with its time, for example `Jan 2, 1978, 12:00 AM`

#### Scenario: Date-time in the afternoon
- **WHEN** a DATE value is `2020-05-01T14:30:00` on a device set to English (US)
- **THEN** it is shown as, for example, `May 1, 2020, 2:30 PM`

#### Scenario: Date-only value
- **WHEN** a DATE value is `2001-04-20` on a device set to English (US)
- **THEN** it is shown as a date only, for example `Apr 20, 2001`

#### Scenario: Empty value
- **WHEN** a value is empty
- **THEN** it is shown as `-`

### Requirement: Empty table message
The system SHALL show a message saying the file has no rows when the file has a header but no data rows.

#### Scenario: Header-only file
- **WHEN** the downloaded file has only a header
- **THEN** a no-rows message is shown instead of the list

### Requirement: Errors are shown on the screen
The system SHALL show, in place of the list, a message for the failure kind (no connection, server error with its code, not enough storage, empty file, malformed file, unsupported encoding, unexpected error) when loading fails. The message SHALL remain visible after a configuration change. A snackbar SHALL also announce the failure once.

#### Scenario: Offline on start
- **WHEN** loading fails because the device is offline
- **THEN** the screen says there is no connection and a snackbar announces it

#### Scenario: Unexpected failure
- **WHEN** loading fails for a reason that is not one of the known kinds
- **THEN** the screen shows a generic error message and the app does not crash

#### Scenario: Configuration change after a failure
- **WHEN** loading failed and a configuration change occurs
- **THEN** the error message and, if offered, the Retry button are still shown

### Requirement: Retry from the error screen
The system SHALL offer a Retry button on the error screen only for network-related and authentication failures: no connection, HTTP 401, 403, 408, 429 and 5xx. All other failures (insufficient storage, malformed, empty, unsupported encoding, other HTTP 4xx, unexpected errors) SHALL NOT offer Retry. Retry SHALL start the download again and SHALL NOT be available while a download is in progress.

#### Scenario: Retry succeeds
- **WHEN** the user presses Retry and the download now succeeds
- **THEN** the loading indicator is shown, then the rows

#### Scenario: File is not UTF-8
- **WHEN** loading fails because the file is not UTF-8 encoded
- **THEN** the screen says the file's encoding is not supported and offers no Retry

#### Scenario: Malformed file
- **WHEN** loading fails because the file has an unclosed quote
- **THEN** the screen says the file is malformed and offers no Retry

#### Scenario: Server error
- **WHEN** loading fails with HTTP 503
- **THEN** the screen shows a server error with code 503 and offers Retry

#### Scenario: Access denied
- **WHEN** loading fails with HTTP 401 or 403
- **THEN** the screen shows a server error with that code and offers Retry

#### Scenario: Not found
- **WHEN** loading fails with HTTP 404
- **THEN** the screen shows a server error with code 404 and offers no Retry

#### Scenario: No Retry while loading
- **WHEN** a download is in progress
- **THEN** no Retry action is available

### Requirement: Portrait orientation
The system SHALL lock the screen to portrait orientation on devices where the platform allows apps to restrict orientation.

#### Scenario: Phone rotated to landscape
- **WHEN** a phone is turned to landscape
- **THEN** the screen stays in portrait

### Requirement: Feedback is shown once
The system SHALL show each snackbar message once. It SHALL NOT appear again after a configuration change.

#### Scenario: Configuration change after an error snackbar
- **WHEN** an error snackbar was shown and a configuration change occurs
- **THEN** the snackbar is not shown a second time, while the error message stays on the screen
