# Tasks

## 1. Module structure and build setup

- [x] 1.1 Create `:build-logic` with convention plugins (Android application, Android library, Compose, Hilt, JVM library) using the android-project-structure skill, and verify `./gradlew help` configures without errors
- [x] 1.2 Create empty modules `:core:domain` (JVM), `:core:data`, `:core:design-system`, `:feature:csv-viewer` with only their build files, wire them in `settings.gradle.kts` with the dependency direction from design.md, and verify `./gradlew assembleDebug` succeeds and the skill's graph check reports no cycles
- [x] 1.3 Add the Dependency Analysis Gradle Plugin (`com.autonomousapps.dependency-analysis`, a version that supports our AGP and Gradle), used during development only, and verify `./gradlew buildHealth` runs and produces a report
- [x] 1.4 Raise `minSdk` to 26, remove the dependencies `buildHealth` reports as unused that stay unused once the modules have code, and verify `./gradlew assembleDebug` succeeds
- [x] 1.5 Move the theme files from `:app` to `:core:design-system`, and verify the app builds and launches with the same theme

## 2. Dependency injection

- [x] 2.1 Add Hilt + KSP to the catalog and convention plugin, annotate the `Application` and `MainActivity`, and verify the app builds and launches

## 3. Domain models and type inference

- [x] 3.1 Add the domain models (`RawCsv`, `CsvTable`, `Column`, `ColumnType`, `CellValue`), `CsvError` and the `CsvRepository` interface in `:core:domain`, and verify `./gradlew :core:domain:build` succeeds
- [x] 3.2 Implement `RawCsv.toCsvTable()` as in design.md (Type inference): it decides each column's type (INTEGER, DATE with `LocalDate.parse`, `LocalDateTime.parse` and `OffsetDateTime.parse`, otherwise TEXT) and reuses the parsed rows; `CsvTable.row(index)` converts a row's cells with the `internal` `toCellValue()`, which turns an image URL in a TEXT column into `CellValue.Image`. Add unit tests for the `column-type-inference` scenarios, and verify `./gradlew :core:domain:test` passes
- [x] 3.3 Add the `@DefaultDispatcher` qualifier, then implement `LoadCsvTableUseCase` (repository + inference on the default dispatcher, `runCatching` + `fold` that rethrows `CancellationException`, maps `OutOfMemoryError` to `CsvError.FileTooLarge` and wraps unknown exceptions in `CsvError.Generic`) with unit tests using a fake repository, and verify `./gradlew :core:domain:test` passes

## 4. Download

- [x] 4.1 Add OkHttp, the `INTERNET` permission and `NetworkModule`, and verify the app builds
- [x] 4.2 Implement `CsvDownloader` as in design.md (Download to a file, then parse): copy the body straight into `download.csv`, a failed request or read gives `CsvError.Network`, a file that can't be created or written gives `CsvError.Storage`, and cancellation cancels the OkHttp call. Add MockWebServer unit tests for every `csv-download` scenario, and verify `./gradlew :core:data:testDebugUnitTest` passes

## 5. CSV parser and repository

- [x] 5.1 Implement `CsvParser` (state machine over a `Reader`) with unit tests for every `csv-parsing` scenario, plus a quote inside an unquoted field and text after a closing quote, and verify `./gradlew :core:data:testDebugUnitTest` passes
- [x] 5.2 Make `CsvParser.parse` take `open: () -> InputStream`: read the file as strict UTF-8 (`CodingErrorAction.REPORT`), and on `MalformedInputException` read it again as Windows-1252 with the default lenient decoder, with tests for `Daniëlle` in UTF-8 and in Windows-1252, and verify the parser tests pass
- [x] 5.3 Add the `@IoDispatcher` qualifier in `:core:domain` and its provider in `DispatchersModule`, then implement `CsvRepositoryImpl` (download, then parse on the IO dispatcher) and provide it in `DataModule` with the CSV URL and cache directory, and verify the app builds
- [x] 5.4 Add a JVM integration test serving the sample file's exact bytes from MockWebServer through repository and use case, asserting the typed `CsvTable` (column types and values), and verify it passes
- [x] 5.5 Add an instrumented test in `:core:data` that writes a generated 100,000-row CSV to the device's cache folder and runs the real `CsvParser` and `toCsvTable()` on it, asserting the row count and a generous time limit, so memory and speed are checked on a real device. Measure a 1,000,000-row file once on a device for the README (not kept as a test: it depends on the device's heap), and verify `./gradlew :core:data:connectedDebugAndroidTest` passes on an emulator or phone

## 6. Viewer screen

- [x] 6.1 Implement `CsvUiState(isLoading, table, error)` with `canRetry`, `CsvEvent` and `CsvViewerViewModel` (load in `init`, Retry accepted only when `canRetry`, a new load keeps the table already shown) with unit tests for starting to load, a failure with Retry, `canRetry` for each `CsvError`, Retry after a failure, and Retry ignored while loading, and verify `./gradlew :feature:csv-viewer:testDebugUnitTest` passes
- [x] 6.2 Replace the template colors in `:core:design-system` with the Rabobank light and dark color schemes from design.md (Theme: Rabobank colors), remove the `dynamicColor` parameter and the Android 12 check, and verify the app builds and launches in the new colors in light and dark mode on an Android 12+ device
- [x] 6.3 Add Coil (Compose + OkHttp network) to the catalog and `:feature:csv-viewer`, then implement the stateless `CsvViewerContent` in the screen's own `Scaffold` with the top bar, the loading indicator and a `LazyColumn` of cards keyed by row index that reads each row with `table.row(index)`, as in design.md (Screen layout): `CellValue.Image` values on the left with `AsyncImage` (the default avatar drawable as placeholder and error), the other columns as name and value side by side, value formatting (24-hour times; date-times with an offset converted to the device time zone) and `Column <n>` labels, with string resources and Compose UI tests for the loading, card and formatting scenarios in `csv-viewer`, and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [x] 6.4 Add the empty state (the Inbox vector drawable and "Nothing to show") shown when nothing is loading and there are no rows to show, with a Compose UI test for a header-only file, and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [x] 6.5 Add the error snackbar: a `SnackbarHost` in the screen's `Scaffold`, shown from `LaunchedEffect(error)` with `SnackbarDuration.Indefinite`, one message string per `CsvError` from design.md, and a Retry action sending `CsvEvent.Retry` when `canRetry`, with Compose UI tests for the snackbar scenarios in `csv-viewer` (message per kind, Retry offered or not, Retry sends the event, a Retry that fails again with the same code shows the snackbar again), and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [x] 6.6 Connect `CsvViewerScreen` (state collection) inside the app-level `Scaffold`, lock `MainActivity` to portrait, and verify on a device: the sample's three cards appear with their avatars (or the default avatar if the URLs fail), a large file (100,000+ rows) scrolls fast up and down without dropped frames or a crash, airplane mode shows the empty state and the no-connection snackbar with Retry, switching dark mode keeps the snackbar, and Retry recovers after reconnecting

## 7. Final checks

- [x] 7.1 Write `README.md` covering the architecture, the module graph, the parsing and type rules, the known limits (memory, comma only, ISO dates, encodings other than UTF-8 and Windows-1252 show garbled text, a row that fails to convert while scrolling is not handled and crashes the app), how to run the tests, and a short note on how AI was used, and verify every command in it runs as written
- [ ] 7.2 Run `./gradlew buildHealth`, fix what it reports, then remove the Dependency Analysis plugin, and verify `./gradlew assembleDebug` succeeds without it
- [ ] 7.3 Run `./gradlew build connectedDebugAndroidTest` on a clean checkout, verify everything passes, and remove leftover template files (`ExampleUnitTest`, `ExampleInstrumentedTest`)
