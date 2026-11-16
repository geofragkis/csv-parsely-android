# Proposal

## Why

The app must download a CSV file from a remote URL and show its contents, with every value converted to a string, an integer or a date. The column layout is not fixed: the reviewers will validate the app with other, unknown CSV files, so the app must work on any well-formed CSV, not only on the sample file.

## What Changes

- Split the single `:app` module into a multi-module clean architecture: `:build-logic` (convention plugins), `:core:domain`, `:core:data`, `:core:design-system`, `:feature:csv-viewer`, with `:app` as the entry point.
- Add Hilt for dependency injection and raise `minSdk` from 24 to 26 so `java.time` is available for date values.
- Download the CSV with OkHttp to a temporary file in the app's cache directory. Check free space first and never leave a partial file behind.
- Add a hand-written CSV parser (no third-party CSV library) that streams the file row by row and handles quoted fields, escaped quotes, line breaks inside quotes, CRLF/LF/CR line endings, a UTF-8 BOM and a missing final newline. Files that are not valid UTF-8 are rejected with a clear error, never shown with corrupted characters.
- Infer each column's type (TEXT, INTEGER or DATE) from its content and convert every cell to that type.
- Keep rows whose field count differs from the header: pad short rows and keep extra fields as unnamed columns.
- Show the rows in a single screen: a `LazyColumn` of non-clickable cards, a loading indicator, and a snackbar for errors (with a Retry action).
- Add unit tests (parser, type inference, download, ViewModel), a JVM integration test from HTTP to typed table, and Compose UI tests, written together with the code they cover.

## Capabilities

### New Capabilities
- `csv-download`: fetching the remote CSV into a temporary local file, including storage checks and failure cleanup.
- `csv-parsing`: turning a CSV text stream into a header and rows of raw fields, including irregular-row handling.
- `column-type-inference`: deciding each column's type (TEXT, INTEGER, DATE) and converting the cells to it.
- `csv-viewer`: the screen that shows the typed table, its loading state and user feedback.

### Modified Capabilities
<!-- None: the project has no existing specs. -->

## Impact

- **Code**: new modules `:build-logic`, `:core:domain`, `:core:data`, `:core:design-system`, `:feature:csv-viewer`. `:app` becomes a thin Hilt entry point, and its theme files move to `:core:design-system`.
- **Dependencies**: Hilt (+ KSP), OkHttp, Kotlin coroutines, lifecycle ViewModel/runtime Compose, hilt-navigation-compose. Test only: MockWebServer, kotlinx-coroutines-test, Compose UI test. Development only: the Dependency Analysis Gradle Plugin, to find unused or wrongly declared dependencies while coding; removed before the change is finished.
- **Build**: `minSdk` raised from 24 to 26. Gradle configuration moves into convention plugins.
- **Permissions**: `INTERNET` added to the manifest.
- **Out of scope**: Room/offline storage, pagination of files larger than memory, image loading for URL columns, a detail screen, custom loader animations.
- **Possible follow-ups (separate changes, only after this one is done)**: recognising common non-ISO date formats per column; reading Windows-1252 files when strict UTF-8 decoding fails.
