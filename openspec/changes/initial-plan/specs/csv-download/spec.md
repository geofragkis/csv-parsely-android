# Spec Delta

## Purpose

Fetches the remote CSV file into local temporary storage so it can be parsed, without ever exposing a partial or corrupted file.

## ADDED Requirements

### Requirement: Download the CSV to a local file
The system SHALL download the CSV from its configured URL and store it as a single file in the app's cache storage. A new download SHALL replace the previously stored file, so at most one copy is kept.

#### Scenario: Successful download
- **WHEN** the server responds with a success status and a body
- **THEN** the full body is stored as the local CSV file and made available for parsing

#### Scenario: Previous file is replaced
- **WHEN** a download succeeds while a file from an earlier download exists
- **THEN** only the new file remains

### Requirement: Never expose a partial file
The system SHALL make the downloaded file available for parsing only after the whole body has been written. Any partially written data SHALL be deleted when the download fails or is cancelled.

#### Scenario: Connection drops mid-download
- **WHEN** the connection fails after part of the body was written
- **THEN** the download fails with a network error and no partial file remains

#### Scenario: Download is cancelled
- **WHEN** the download is cancelled before it finishes
- **THEN** no partial file remains

### Requirement: Check storage before and during the download
The system SHALL fail with an insufficient-storage error, without writing anything, when the server reports a content length larger than the space available to the app. It SHALL fail with the same error when storage runs out during the write.

#### Scenario: Declared size exceeds available space
- **WHEN** the response's declared content length is larger than the available space
- **THEN** the download fails with an insufficient-storage error and no file is written

#### Scenario: Storage runs out while writing
- **WHEN** storage becomes full during the write
- **THEN** the download fails with an insufficient-storage error and the partial file is deleted

### Requirement: Report download failures by kind
The system SHALL report a failed download as one of: no connection or other network failure, an unsuccessful HTTP status (including the status code), or insufficient storage.

#### Scenario: Server returns an error status
- **WHEN** the server responds with status 404
- **THEN** the download fails with an HTTP error carrying code 404

#### Scenario: Device is offline
- **WHEN** the server cannot be reached
- **THEN** the download fails with a network error
