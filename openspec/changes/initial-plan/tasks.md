# Tasks

## 1. Module structure and build setup

- [ ] 1.1 Create `:build-logic` with convention plugins (Android application, Android library, Compose, Hilt, JVM library) using the android-project-structure skill, and verify `./gradlew help` configures without errors
- [ ] 1.2 Create empty modules `:core:domain` (JVM), `:core:data`, `:core:design-system`, `:feature:csv-viewer` with only their build files, wire them in `settings.gradle.kts` with the dependency direction from design.md, and verify `./gradlew assembleDebug` succeeds and the skill's graph check reports no cycles
- [ ] 1.3 Raise `minSdk` to 26, remove template dependencies that nothing uses, and verify `./gradlew assembleDebug` succeeds
- [ ] 1.4 Move the theme files from `:app` to `:core:design-system`, and verify the app builds and launches with the same theme

## 2. Dependency injection

- [ ] 2.1 Add Hilt + KSP to the catalog and convention plugin, annotate the `Application` and `MainActivity`, add `DispatchersModule` with `@IoDispatcher`/`@DefaultDispatcher` (qualifiers in `:core:domain`), and verify the app builds and launches

## 3. Domain models and type inference

- [ ] 3.1 Add the domain models (`RawCsv`, `CsvTable`, `Column`, `ColumnType`, `CellValue`), `CsvError` and the `CsvRepository` interface in `:core:domain`, and verify `./gradlew :core:domain:build` succeeds
- [ ] 3.2 Implement `ColumnTypeInference` with unit tests for every `column-type-inference` scenario plus Long min/max, whitespace and leap-day edges, and verify `./gradlew :core:domain:test` passes
- [ ] 3.3 Implement `LoadCsvTableUseCase` (repository + inference on the default dispatcher) with unit tests using a fake repository for success and error propagation, and verify `./gradlew :core:domain:test` passes

## 4. Download

- [ ] 4.1 Add OkHttp, the `INTERNET` permission and `NetworkModule`, and verify the app builds
- [ ] 4.2 Implement `CsvDownloader` (temp file, storage check, separate read/write error handling, cleanup, atomic rename) with MockWebServer unit tests for every `csv-download` scenario, and verify `./gradlew :core:data:testDebugUnitTest` passes

## 5. CSV parser and repository

- [ ] 5.1 Implement `CsvParser` (state machine over a `Reader`) with unit tests for every `csv-parsing` scenario, and verify `./gradlew :core:data:testDebugUnitTest` passes
- [ ] 5.2 Open the file with a strict UTF-8 decoder (`CodingErrorAction.REPORT`) and map `MalformedInputException` to `CsvError.UnsupportedEncoding`, with tests for UTF-8 `Daniëlle` (parsed) and Windows-1252 `Daniëlle` (rejected), and verify the parser tests pass
- [ ] 5.3 Add adversarial parser tests (quote inside an unquoted field, text after a closing quote, mixed line endings, Unicode/emoji, very long field, 100,000 generated rows within a time bound), fix any failures, and verify all parser tests pass
- [ ] 5.4 Implement `CsvRepositoryImpl` (download, then parse on the IO dispatcher) and bind it in `DataModule` with the CSV URL and cache directory, and verify the app builds
- [ ] 5.5 Add a JVM integration test serving the sample file's exact bytes from MockWebServer through repository and use case, asserting the typed `CsvTable` (column types and values), and verify it passes

## 6. Viewer screen

- [ ] 6.1 Implement `CsvUiState`, `CsvEvent`, `CsvEffect` and `CsvViewerViewModel` (load in `init`, Retry, effects through a `Channel`) with unit tests for Loading → Loaded/NoRows/Failed, the error effect for each `CsvError` (with Retry offered for every kind except `UnsupportedEncoding`), the irregular-row effect and Retry, and verify `./gradlew :feature:csv-viewer:testDebugUnitTest` passes
- [ ] 6.2 Implement the stateless `CsvViewerContent` (loading indicator, `LazyColumn` of cards keyed by row index, value formatting, `Column <n>` labels, no-rows message) with string resources, and Compose UI tests for each `csv-viewer` display scenario, and verify `./gradlew :feature:csv-viewer:connectedDebugAndroidTest` passes
- [ ] 6.3 Add the app-level `Scaffold` with the global `SnackbarHost`, connect `CsvViewerScreen` (state collection, effects → snackbar with Retry), and verify on a device: the sample's three cards appear, airplane mode shows the no-connection snackbar, and Retry recovers after reconnecting

## 7. Final checks

- [ ] 7.1 Write `README.md` covering the architecture, the module graph, the parsing and type rules, the known limits (memory, comma only, ISO dates) and how to run the tests, and verify every command in it runs as written
- [ ] 7.2 Run `./gradlew build connectedDebugAndroidTest` on a clean checkout, verify everything passes, and remove leftover template files (`ExampleUnitTest`, `ExampleInstrumentedTest`)

## Workflow follow-up

- Review the implementation against the specs, then archive the change with `/opsx:archive`.
- Nice to have, each as its own OpenSpec change after this one is archived (each changes the specs):
  - Multi-format date detection: one format per column picked from common patterns (`dd-MM-yyyy`, `dd/MM/yyyy`, `MM/dd/yyyy`, `dd.MM.yyyy`, `yyyy/MM/dd`, ISO with an offset, each with an optional time), strict resolving, day-first as the tie-breaker.
  - Windows-1252 fallback: when strict UTF-8 decoding fails, parse the file again as Windows-1252. This removes the unsupported-encoding error and its snackbar.
