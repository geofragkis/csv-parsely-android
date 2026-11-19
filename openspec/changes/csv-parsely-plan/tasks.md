# Tasks

## 1. Module structure and build setup

- [x] 1.1 Create `:build-logic` with convention plugins (Android application, Android library, Compose, Hilt, JVM library) using the android-project-structure skill, and verify `./gradlew help` configures without errors
- [x] 1.2 Create empty modules `:core:domain` (JVM), `:core:data`, `:core:design-system`, `:feature:csv-viewer` with only their build files, wire them in `settings.gradle.kts` with the dependency direction from design.md, and verify `./gradlew assembleDebug` succeeds and the skill's graph check reports no cycles
- [x] 1.3 Add the Dependency Analysis Gradle Plugin (`com.autonomousapps.dependency-analysis`, a version that supports our AGP and Gradle), used during development only, and verify `./gradlew buildHealth` runs and produces a report
- [x] 1.4 Raise `minSdk` to 26, remove the dependencies `buildHealth` reports as unused that stay unused once the modules have code, and verify `./gradlew buildHealth` reports only the expected leftovers of the empty modules (the links between modules and the Compose libraries from the convention plugin) and `./gradlew assembleDebug` succeeds
- [x] 1.5 Move the theme files from `:app` to `:core:design-system`, and verify the app builds and launches with the same theme

## 2. Dependency injection

- [x] 2.1 Add Hilt + KSP to the catalog and convention plugin, annotate the `Application` and `MainActivity`, and verify the app builds and launches

## 3. Domain models and type inference

- [x] 3.1 Add the domain models (`RawCsv`, `CsvTable`, `Column`, `ColumnType`, `CellValue`), `CsvError` and the `CsvRepository` interface in `:core:domain`, and verify `./gradlew :core:domain:build` succeeds
- [x] 3.2 Implement `RawCsv.toCsvTable()` with unit tests for every `column-type-inference` scenario plus Long min/max, whitespace and leap-day edges, and verify `./gradlew :core:domain:test` passes
- [x] 3.3 Add the `@DefaultDispatcher` qualifier in `:core:domain` and `DispatchersModule` in `:core:data` providing it, then implement `LoadCsvTableUseCase` (repository + inference on the default dispatcher, `runCatching` + `fold` that rethrows `CancellationException` and wraps unknown exceptions in `CsvError.Generic`) with unit tests using a fake repository for success, `CsvError` passed on, unknown exception → `Generic`, and cancellation rethrown, and verify `./gradlew :core:domain:test` passes
- [x] 3.4 Add `CsvError.FileTooLarge` and map `OutOfMemoryError` to it in `LoadCsvTableUseCase`, with a unit test using a fake repository that throws `OutOfMemoryError`, and verify `./gradlew :core:domain:test` passes
- [x] 3.5 Replace the custom date formatters in `RawCsv.toCsvTable()` with `LocalDate.parse`, `LocalDateTime.parse` and `OffsetDateTime.parse`, add `offset: ZoneOffset?` to `CellValue.Date`, and type ISO date-times with an offset as DATE. Update the unit tests (remove the year-zero, signed-year and lowercase-`t` tests; add `Z`, `+02:00`, `-05:30`, a column mixing values with and without an offset, an invalid offset), and verify `./gradlew :core:domain:test` passes

## 4. Download

- [x] 4.1 Add OkHttp, the `INTERNET` permission and `NetworkModule`, and verify the app builds
- [x] 4.2 Implement `CsvDownloader` (temp file, separate read/write error handling with ENOSPC detection, cleanup, atomic rename) with MockWebServer unit tests for every `csv-download` scenario, including cancellation mid-copy, disk full while writing and another write failure, and verify `./gradlew :core:data:testDebugUnitTest` passes
- [x] 4.3 Simplify `CsvDownloader`: write straight to `download.csv` with `InputStream.copyTo` (no temporary file, move or cleanup) and map any `IOException` from the call or the copy to `CsvError.Network`; remove `CsvError.InsufficientStorage`, the ENOSPC check, `CsvDownloaderWriteErrorTest` and the Robolectric dependency; update `CsvDownloaderTest` to the `csv-download` scenarios, and verify `./gradlew :core:data:testDebugUnitTest` passes

## 5. CSV parser and repository

- [x] 5.1 Before writing code, discuss the memory open question in design.md with the user. Then implement `CsvParser` (state machine over a `Reader`) with unit tests for every `csv-parsing` scenario, and verify `./gradlew :core:data:testDebugUnitTest` passes
- [x] 5.2 Open the file with a strict UTF-8 decoder (`CodingErrorAction.REPORT`) and map `MalformedInputException` to `CsvError.UnsupportedEncoding`, with tests for UTF-8 `Daniëlle` (parsed) and Windows-1252 `Daniëlle` (rejected), and verify the parser tests pass
- [x] 5.3 Change `CsvParser.parse` to take `open: () -> InputStream`: read the file as strict UTF-8, and on `MalformedInputException` read it again as Windows-1252 with the default lenient decoder; remove `CsvError.UnsupportedEncoding`; change the Windows-1252 test to expect `Daniëlle`, and verify the parser tests pass
- [x] 5.4 Add adversarial parser tests (quote inside an unquoted field, text after a closing quote, mixed line endings, Unicode/emoji, very long field, 100,000 generated rows within a time bound), fix any failures, and verify all parser tests pass
- [x] 5.5 Add the `@IoDispatcher` qualifier in `:core:domain` and its provider in `DispatchersModule`, then implement `CsvRepositoryImpl` (download, then parse on the IO dispatcher) and bind it in `DataModule` with the CSV URL and cache directory, and verify the app builds
- [x] 5.6 Add a JVM integration test serving the sample file's exact bytes from MockWebServer through repository and use case, asserting the typed `CsvTable` (column types and values), and verify it passes
- [x] 5.7 Add an instrumented test in `:core:data` that writes a generated 100,000-row CSV to the device's cache folder and runs the real `CsvParser` and `toCsvTable()` on it, asserting the row count and a generous time limit, so memory and speed are checked on a real device; add the AndroidX test dependencies `:core:data` needs, and verify `./gradlew :core:data:connectedDebugAndroidTest` passes on an emulator or phone
- [x] 5.8 Make `CsvTable` convert cells when a row is read: `class CsvTable(val columns: List<Column>, private val rows: List<List<String>>)` with `rowCount` and `row(index): List<CellValue>`; `toCsvTable()` only decides the column types and reuses the parsed rows; `toCellValue()` becomes `internal`. Update the inference and integration tests to read rows through `row(index)`. Then temporarily add a 1,000,000-row device test next to the 100,000-row one, run both on a device to measure, and remove the 1,000,000-row test again (its result goes in the README: it depends on the device's heap, so it would fail on small emulators); verify `./gradlew :core:domain:test :core:data:testDebugUnitTest` passes

## 6. Viewer screen

- [x] 6.1 Implement `CsvUiState(isLoading, table, error)` with `canRetry`, `CsvEvent` and `CsvViewerViewModel` (load in `init`, Retry accepted only when `canRetry`, a new load keeps the table already shown) with unit tests for starting to load, rows, no rows and failure states, `canRetry` for each `CsvError` (true for Network and HTTP 408/429/5xx; false for HTTP 401/403, Storage, Malformed, EmptyFile, FileTooLarge, other 4xx, Generic), Retry after a failure, and Retry ignored while loading, and verify `./gradlew :feature:csv-viewer:testDebugUnitTest` passes
- [x] 6.2 Replace the template colors in `:core:design-system` with the Rabobank light and dark color schemes from design.md (Theme: Rabobank colors), remove the `dynamicColor` parameter, the Android 12 check and the template's commented-out blocks, and verify the app builds and launches in the new colors in light and dark mode on an Android 12+ device
- [x] 6.3 Add Coil (Compose + OkHttp network) to the catalog and `:feature:csv-viewer`, then implement the stateless `CsvViewerContent` in the screen's own `Scaffold` with the top bar, the loading indicator and a `LazyColumn` of cards keyed by row index that reads each row with `table.row(index)`, as in design.md (Screen layout): image values on the left with `AsyncImage` (the default avatar drawable as placeholder and error), the other columns as name and value side by side, value formatting (24-hour times; date-times with an offset converted to the device time zone) and `Column <n>` labels, with string resources and Compose UI tests for the loading, card, formatting and image scenarios in `csv-viewer`, and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [x] 6.4 Add `CellValue.Image(url)` in `:core:domain`: `toCellValue()` turns a TEXT cell into `Image` when it is an image URL by the rule in design.md (`java.net.URI`; `URISyntaxException` means not an image, anything else is rethrown), otherwise `Text`, with JVM unit tests for the image scenarios in `column-type-inference`. In `:feature:csv-viewer`, remove `isImageUrl` and split each row's cells by type in `CsvRowCard`. Verify `./gradlew :core:domain:test :feature:csv-viewer:connectedDebugAndroidTest` passes
- [x] 6.5 Add the empty state (the Inbox vector drawable and "Nothing to show") shown when nothing is loading and there are no rows to show, with Compose UI tests for a header-only file and for a failure before any rows, and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [x] 6.6 Add the error snackbar: a `SnackbarHost` in the screen's `Scaffold`, shown from `LaunchedEffect(error)` with `SnackbarDuration.Indefinite`, one message string per `CsvError` from design.md, and a Retry action sending `CsvEvent.Retry` when `canRetry` (rows stay visible with an error), with Compose UI tests for the snackbar scenarios in `csv-viewer` (message per kind, Retry offered or not, Retry sends the event), and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [x] 6.7 Connect `CsvViewerScreen` (state collection) inside the app-level `Scaffold`, lock `MainActivity` to portrait, and verify on a device: the sample's three cards appear with their avatars (or the default avatar if the URLs fail), a large file (100,000+ rows) scrolls fast up and down without dropped frames or a crash, airplane mode shows the empty state and the no-connection snackbar with Retry, switching dark mode keeps the snackbar, and Retry recovers after reconnecting

## 7. Final checks

- [ ] 7.1 Go through every test with the user and remove the ones that protect nothing: a case the app can't reach, or a check another test already covers. Keep the tests for crashes, wrong data and the spec scenarios, and verify `./gradlew test connectedDebugAndroidTest` passes
- [ ] 7.2 Write `README.md` covering the architecture, the module graph, the parsing and type rules, the known limits (memory, comma only, ISO dates, encodings other than UTF-8 and Windows-1252 show garbled text, a row that fails to convert while scrolling is not handled and crashes the app) how to run the tests, and a short note on how AI was used: Claude Code assisted the developer, who made every decision and reviewed each step (one task per pull request, reviewed before merging), so the app was not vibe coded; OpenSpec was used for planning (the plan is in `openspec/`), and verify every command in it runs as written
- [ ] 7.3 Run `./gradlew buildHealth`, fix what it reports, then remove the Dependency Analysis plugin, and verify `./gradlew assembleDebug` succeeds without it
- [ ] 7.4 Run `./gradlew build connectedDebugAndroidTest` on a clean checkout, verify everything passes, and remove leftover template files (`ExampleUnitTest`, `ExampleInstrumentedTest`)

## Workflow follow-up

- Review the implementation against the specs, then archive the change with `/opsx:archive`.
- Optional, on its own branch after this change is done (no spec change): rewrite `CsvParser.records()` to produce records one at a time with a `Sequence` (`sequence { }` + `yield`) instead of returning a list, so the user can decide whether to present it.
- Nice to have, each as its own OpenSpec change after this one is archived (each changes the specs):
  - Multi-format date detection: one format per column picked from common patterns (`dd-MM-yyyy`, `dd/MM/yyyy`, `MM/dd/yyyy`, `dd.MM.yyyy`, `yyyy/MM/dd`, each with an optional time), strict resolving, day-first as the tie-breaker.
- To rethink, raised in code review of 3.2:
  - Parsing cost in `toCsvTable()`: every INTEGER and DATE cell is parsed once to pick the column type, and again when its row is shown (with `checkNotNull` relying on both agreeing), and every date-time value first fails the date-only parse with a thrown `DateTimeParseException` (a date-time with an offset first fails both the date and the local date-time parse). Measured at about 150 ms for 100k date-time cells on the desktop JVM; revisit if loading a large file feels slow on a device.
