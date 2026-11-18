# Spec Delta

## Purpose

Fetches the remote CSV file into the app's cache storage so it can be parsed.

## ADDED Requirements

### Requirement: Download the CSV to a local file
The system SHALL download the CSV from its configured URL and store it as a single file in the app's cache storage. A new download SHALL replace the previously stored file, so at most one copy is kept.

#### Scenario: Successful download
- **WHEN** the server responds with a success status and a body
- **THEN** the full body is stored as the local CSV file and made available for parsing

#### Scenario: Previous file is replaced
- **WHEN** a download succeeds while a file from an earlier download exists
- **THEN** only the new file remains

### Requirement: Stop a cancelled download
The system SHALL stop a download as soon as it is cancelled, also while it is waiting for data.

#### Scenario: Cancelled while waiting for the body
- **WHEN** the download is cancelled while it waits for more of the body
- **THEN** the download stops at once

### Requirement: Report download failures by kind
The system SHALL report a failed download as one of: a network failure, an unsuccessful HTTP status (including the status code), or an unexpected error. Any failure while receiving or saving the body SHALL be reported as a network failure.

#### Scenario: Saving the file fails
- **WHEN** writing the file fails (for example the storage is full)
- **THEN** the download fails with a network error

#### Scenario: Server returns an error status
- **WHEN** the server responds with status 404
- **THEN** the download fails with an HTTP error carrying code 404

#### Scenario: Device is offline
- **WHEN** the server cannot be reached
- **THEN** the download fails with a network error
