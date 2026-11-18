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
- [x] 3.3 Add the `@DefaultDispatcher` qualifier in `:core:domain` and `DispatchersModule` in `:core:data` providing it, then implement `LoadCsvTableUseCase` (repository + inference on the default dispatcher, `runCatching` + `fold` that rethrows `CancellationException` and wraps unknown exceptions in `CsvError.Unknown`) with unit tests using a fake repository for success, `CsvError` passed on, unknown exception → `Unknown`, and cancellation rethrown, and verify `./gradlew :core:domain:test` passes
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
- [ ] 5.5 Add the `@IoDispatcher` qualifier in `:core:domain` and its provider in `DispatchersModule`, then implement `CsvRepositoryImpl` (download, then parse on the IO dispatcher) and bind it in `DataModule` with the CSV URL and cache directory, and verify the app builds
- [ ] 5.6 Add a JVM integration test serving the sample file's exact bytes from MockWebServer through repository and use case, asserting the typed `CsvTable` (column types and values), and verify it passes

## 6. Viewer screen

- [ ] 6.1 Before writing code, settle the snackbar open question in design.md with the user. Then implement `CsvUiState` (with `Failed(error, canRetry)`), `CsvEvent`, `CsvEffect` and `CsvViewerViewModel` (load in `init`, Retry accepted only in `Failed`, effects through a `Channel`) with unit tests for Loading → Loaded/NoRows/Failed, `canRetry` for each `CsvError` (true for Network and HTTP 401/403/408/429/5xx; false for Malformed, EmptyFile, FileTooLarge, other 4xx, Unknown), the error effect, Retry from `Failed`, and Retry ignored while loading, and verify `./gradlew :feature:csv-viewer:testDebugUnitTest` passes
- [ ] 6.2 Implement the stateless `CsvViewerContent` (loading indicator, `LazyColumn` of cards keyed by row index, value formatting (24-hour times; date-times with an offset converted to the device time zone), `Column <n>` labels, no-rows message, error message with a Retry button when `canRetry`) with string resources, and Compose UI tests for each `csv-viewer` display scenario, and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [ ] 6.3 Add the app-level `Scaffold` with the global `SnackbarHost`, connect `CsvViewerScreen` (state collection, effects → snackbar), lock `MainActivity` to portrait, and verify on a device: the sample's three cards appear, airplane mode shows the no-connection message with Retry plus a snackbar, switching dark mode keeps the error screen, and Retry recovers after reconnecting

## 7. Final checks

- [ ] 7.1 Write `README.md` covering the architecture, the module graph, the parsing and type rules, the known limits (memory, comma only, ISO dates, encodings other than UTF-8 and Windows-1252 show garbled text) and how to run the tests, and verify every command in it runs as written
- [ ] 7.2 Run `./gradlew buildHealth`, fix what it reports, then remove the Dependency Analysis plugin, and verify `./gradlew assembleDebug` succeeds without it
- [ ] 7.3 Run `./gradlew build connectedDebugAndroidTest` on a clean checkout, verify everything passes, and remove leftover template files (`ExampleUnitTest`, `ExampleInstrumentedTest`)

## Workflow follow-up

- Review the implementation against the specs, then archive the change with `/opsx:archive`.
- Optional, on its own branch after this change is done (no spec change): rewrite `CsvParser.records()` to produce records one at a time with a `Sequence` (`sequence { }` + `yield`) instead of returning a list, so the user can decide whether to present it.
- Nice to have, each as its own OpenSpec change after this one is archived (each changes the specs):
  - Multi-format date detection: one format per column picked from common patterns (`dd-MM-yyyy`, `dd/MM/yyyy`, `MM/dd/yyyy`, `dd.MM.yyyy`, `yyyy/MM/dd`, each with an optional time), strict resolving, day-first as the tie-breaker.
- To rethink, raised in code review of 3.2:
  - Parsing cost in `toCsvTable()`: every INTEGER and DATE cell is parsed twice (once to pick the column type, once to convert it, with `checkNotNull` relying on both passes agreeing), and every date-time value first fails the date-only parse with a thrown `DateTimeParseException` (a date-time with an offset first fails both the date and the local date-time parse). Measured at about 150 ms for 100k date-time cells on the desktop JVM; revisit if loading a large file feels slow on a device.
- To rethink at the end if there is time, raised in code review of 4.3:
  - Download errors: every `IOException` from the request or the copy becomes `CsvError.Network`, also a local file failure (cache folder can't be created, disk full). The screen then says "no connection" and offers a Retry that keeps failing. Decide whether local file failures should get their own kind (for example `Unknown`, without Retry).
