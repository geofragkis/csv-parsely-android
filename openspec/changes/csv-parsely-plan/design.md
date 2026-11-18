# Design

## Context

The project is the Android Studio template: one `:app` module with Compose + Material3, AGP 9.5 (Kotlin built in), Gradle 9.6, a version catalog, `minSdk` 24. There is no DI, networking or architecture yet. Requirements are in `specs/`, motivation in `proposal.md`.

The sample file is 349 bytes with CRLF line endings, no final newline, quoted strings, unquoted integers and quoted ISO local date-times. The real constraint is the unknown validation files, so nothing in the code depends on the sample's columns.

## Goals / Non-Goals

**Goals:**
- Each layer is testable on its own: the parser and type inference as plain JVM unit tests, the download against a fake server, the UI against a fixed state.
- The parser streams its input, so the file is never loaded into memory as a whole (memory of the parsed rows: see Risks / Trade-offs).
- Each module has one clear responsibility, and the dependency graph has no cycles.

**Non-Goals:**
- Files larger than available memory (no pagination, no database).
- Delimiters other than comma. Encodings other than UTF-8 and Windows-1252: such a file loads, but its non-ASCII letters show wrong.
- Date formats other than ISO-8601.
- Caching or showing a previous download when offline.

## Decisions

### Modules and dependency direction

```
            :app  (Hilt application, MainActivity, global Scaffold + SnackbarHost)
           /    \
          v      v
:feature:csv-viewer --> :core:design-system
          |
          v
     :core:domain  <--  :core:data  <-- (:app wires data into the graph)
```

| Module | Type | Holds |
|---|---|---|
| `:build-logic` | included build | convention plugins: Android application, Android library, Compose, Hilt, JVM library |
| `:core:domain` | pure Kotlin/JVM | models (`CsvTable`, `Column`, `ColumnType`, `CellValue`, `RawCsv`), `CsvError`, `CsvRepository` interface, `RawCsv.toCsvTable()`, `LoadCsvTableUseCase`, dispatcher qualifiers |
| `:core:data` | Android library | `CsvDownloader` (OkHttp), `CsvParser`, `CsvRepositoryImpl`, Hilt modules |
| `:core:design-system` | Android library | theme (moved from `:app`) |
| `:feature:csv-viewer` | Android library | `CsvViewerViewModel`, UI state/event/effect types, screen composables, string resources |
| `:app` | application | `@HiltAndroidApp`, `MainActivity`, app-level `Scaffold` with the `SnackbarHost` |

`:feature` never depends on `:core:data`. Only `:app` sees both, so Hilt can bind the repository implementation. `:core:domain` uses only `javax.inject` annotations (no Android, no Hilt plugin), so the domain stays a plain JVM module with fast tests.

*Alternative:* a single module with packages. It's simpler, but the brief asks for recognised architecture, and the module boundaries make the layer rules enforced by the build, not just a convention. The android-project-structure skill sets up `build-logic` and checks the graph.

### Download to a file, then parse

`CsvDownloader` makes a GET request with OkHttp and copies the body into `cacheDir/csv/download.csv` with `InputStream.copyTo`, replacing the previous file. Steps:

1. A non-2xx status gives `CsvError.Http(code)`. An `IOException` from the call or the copy gives `CsvError.Network`; any other exception (such as running out of memory) is passed on for the use case to map. A full disk is reported as a network error too: it is rare for a small file in the cache directory, and telling it apart relies on an Android implementation detail (the `ENOSPC` errno inside the `IOException`).
2. `download()` runs the blocking work on the IO dispatcher itself, so it is safe to call from any thread. `execute()` and the body reads block and don't notice coroutine cancellation, so on cancellation the OkHttp `Call` is cancelled, which makes a blocked read fail at once, so a cancelled download stops right away.
3. A failed or cancelled download can leave a partial file. It is never parsed, because `download()` throws, and the next download overwrites it. There is no offline cache, and only one load runs at a time (Retry is accepted only in `Failed`, after the previous load has ended, and a configuration change keeps the ViewModel), so a canceled download can never write into the file of a newer one, and no temporary file or cleanup is needed.

*Alternatives:* stream straight into the parser (no disk, but a literal reading of "the application must download the file" asks for a file, and the parser would be coupled to the network); Retrofit (adds nothing for one GET of a raw file).

### Parser: a character state machine over a `Reader`

`CsvParser.parse(open: () -> InputStream): RawCsv` reads a `BufferedReader` character by character through a small state machine:

```
           ,  -> end field                     "" -> literal "
 [FIELD_START] --"--> [IN_QUOTES] --"--> [QUOTE_IN_QUOTES]
      |  other              ^  any char        |  , / EOL -> end field/record
      v                     +------------------+  other   -> append (lenient)
 [UNQUOTED] -- , / CR / LF / CRLF / EOF --> end field / record
```

- CR followed by LF counts as one terminator. CR alone and LF alone are terminators too.
- A BOM is removed if it's the first character. A completely empty line (no characters, not even `""`) is skipped when the header has more than one column. In a single-column file it is a row with one empty value. The state machine tracks whether the record contained any character, so `""` is never mistaken for an empty line.
- EOF while in `IN_QUOTES` gives `CsvError.Malformed(line)`, using the line where the quote opened.
- No records at all gives `CsvError.EmptyFile`.
- `records()` returns all records as a list, and `parse()` then picks the header (the first record that is not an empty line) and the rows. Rows are padded or kept wider as the spec describes. *Alternative:* produce the records one at a time with a `Sequence`; it avoids the list of all records (one reference per record), but plain lists are easier to read and explain.
- One `StringBuilder` is reused per field, so no per-character allocation.
- `parse()` reads the file as UTF-8 with a decoder set to `CodingErrorAction.REPORT` (not the default `REPLACE`), so invalid bytes throw a `MalformedInputException` instead of silently becoming `�`. On that exception it reads the file again as Windows-1252 (the default of Excel on Windows) with the default lenient decoder, which never fails. The second pass only happens for a non-UTF-8 file. `parse()` takes a function that opens the file (`open: () -> InputStream`), because the bytes may be read twice; the repository passes `{ file.inputStream() }` and the tests pass their bytes. The whole encoding rule lives in the parser. *Alternative:* report an unsupported-encoding error. That stops a common Excel export from loading, for no benefit, since decoding never crashes.

*Alternative:* `readLine()` + `split(',')`. That fails on quoted commas and on line breaks inside quotes, which is exactly what the reviewers' files are likely to test.

### Type inference is domain logic in the use case

`CsvRepository.load(): RawCsv` (download + parse, on the IO dispatcher). `LoadCsvTableUseCase` calls it, then `RawCsv.toCsvTable()` (on the default dispatcher) does one pass per column to pick the type. The result is `Result<CsvTable>`.

`CsvTable` keeps each cell's raw text plus the column types, and converts a row only when it is read: `class CsvTable(val columns: List<Column>, private val rows: List<List<String>>)` with `rowCount` and `row(index): List<CellValue>`. `toCsvTable()` reuses the parsed rows, so there is no second, typed copy of the table, and the conversion runs only for the rows the screen shows. The type rules stay in the domain: `toCellValue()` is `internal` to `:core:domain`, and the UI only asks the table for a row. *Alternative:* convert every cell up front. On a Pixel 8 (256 MB heap) that ran out of memory at 1,000,000 rows, because the raw and the typed table existed at the same time.

- INTEGER: trimmed, `^[+-]?(0|[1-9][0-9]*)$` and `toLongOrNull() != null`.
- DATE: trimmed, then `java.time`'s built-in ISO parsers, tried in order: `LocalDate.parse`, `LocalDateTime.parse`, `OffsetDateTime.parse`. They are strict by default: dates, times and offsets that don't exist (`2023-02-30`, `24:00`, `+25:00`) are rejected instead of adjusted. Only years 1 to 9999 count as dates: outside them a year can't be shown correctly (year 0000 would show as year 1) or fails when moved to another time zone. A cell keeps what the file gave: a date-time stays a date-time (midnight included), a date-only value has no time, and a value with an offset keeps it.
- Otherwise TEXT, keeping the raw string.

`CellValue` is a sealed interface: `Text(String)`, `Integer(Long)`, `Date(date: LocalDate, time: LocalTime?, offset: ZoneOffset?)`, `Empty`. `time == null` means the file gave only a date, which keeps a date-only value apart from a real midnight. `offset` is set only together with a time.

A value with an offset is a moment in time, so the UI shows it in the user's time zone. A value without an offset is shown as written and never converted: the file does not say whether it is a calendar date (such as a date of birth) or a moment, and converting a calendar date can shift it by a day. The domain keeps the offset as read and does not know the device's time zone; the UI converts at display time, so a time-zone change on the device is picked up.

The use case contains real logic (the conversion the brief asks for), so it's more than a call passed through to the repository. *Alternative:* infer inside the parser. That mixes tokenising with a business rule and makes both harder to test.

### Errors

`CsvError` is a sealed class (extending `Exception`): `Network`, `Http(code)`, `EmptyFile`, `Malformed(line)`, `FileTooLarge`, `Unknown(cause)`. It travels inside `Result`, and the ViewModel maps it to a string resource.

The use case builds the `Result` with `runCatching` and `fold`. `runCatching` also catches `CancellationException`, so `fold` always rethrows it: a cancelled load (ViewModel cleared) never turns into a failure or an error message. A `CsvError` is passed on as is, and any other exception is wrapped in `CsvError.Unknown`, so the UI always gets a known kind and the app never crashes on an unexpected error. `runCatching` also catches JVM errors, so an `OutOfMemoryError` becomes `CsvError.FileTooLarge` instead of a crash. Loading the file is by far the largest use of memory in the app, so a file too large to hold is the likely cause. The memory taken by the failed load is freed once the load fails, so the app can keep running.

### UI: MVVM with unidirectional data flow, no reducers

```
 CsvViewerScreen --onEvent(CsvEvent.Retry)--> CsvViewerViewModel --> LoadCsvTableUseCase
       ^                                          |
       +---- state: StateFlow<CsvUiState> --------+
       +---- effects: Flow<CsvEffect> (Channel) --+--> SnackbarHostState (app Scaffold)
```

- `CsvUiState`: `Loading`, `Loaded(table)`, `NoRows`, `Failed(error, canRetry)`. The error message and the Retry button are part of the state, so they survive any configuration change (the Activity, and with it the `SnackbarHostState`, is recreated, but the ViewModel is not).
- `canRetry` is true only for network-related and authentication failures (`Network`, `Http` 401/403/408/429/5xx) and false for everything else (`Malformed`, `EmptyFile`, `FileTooLarge`, other 4xx, `Unknown`). A single property `CsvError.canRetry` holds this rule.
- `CsvEvent`: `Retry`. The ViewModel ignores it unless the state is `Failed`, so a second download can never start while one is running.
- `CsvEffect`: `ShowError(kind)` (short, no action: it announces the failure, while the screen holds the message and the Retry button).
- The ViewModel loads in `init`, so a configuration change doesn't reload. Effects go through a `Channel`, so a snackbar is delivered once and not replayed.
- The app is locked to portrait (`android:screenOrientation="portrait"`). Since Android 16 (we target 37) the platform ignores this on large screens (smallest width ≥ 600dp), so the layout must still work in landscape there. Other configuration changes (dark mode, locale, font size) can happen on any device, which is why the state-based error screen is needed anyway.
- The screen collects state with `collectAsStateWithLifecycle` and effects in a `LaunchedEffect`. A stateless `CsvViewerContent(state)` holds all rendering so UI tests don't need Hilt.
- `LazyColumn(items(count = table.rowCount, key = { index -> index }, contentType = { "row" }))`, and each item reads its cells with `remember(table, index) { table.row(index) }`, so only the visible rows are converted, once each. The row index is the key because it's unique and stable, while row content can repeat.
- Values are formatted in the card composable: the date with `DateTimeFormatter.ofLocalizedDate(MEDIUM)`, remembered per locale, and a time, when there is one, with `DateTimeFormatter.ofPattern("HH:mm")`, joined as `<date>, <time>`, so times are always 24-hour (midnight included). A value with an offset is first converted with `OffsetDateTime.of(date, time, offset).atZoneSameInstant(ZoneId.systemDefault())`. Integers are shown with `toString()` (no grouping, so IDs and years read correctly).
- The global snackbar: `:app`'s `Scaffold` owns the `SnackbarHostState` and passes it to the screen.
- Compose strong skipping (on by default with the Kotlin 2.x Compose compiler) means we don't need immutable collection libraries. The UI models are `@Immutable` data classes.

### Dependency injection

Hilt with one `NetworkModule` (`OkHttpClient` with timeouts), one `DataModule` (a `@Provides` for `CsvRepository` that builds `CsvRepositoryImpl` with the CSV URL, the cache directory, the downloader and the parser) and one `DispatchersModule` (`@IoDispatcher`, `@DefaultDispatcher`). KSP is used for annotation processing.

### Build changes

- `minSdk` 26 so `java.time` is available without desugaring. That still covers practically all active devices.
- Remove unused template dependencies (e.g. espresso if no test uses it), in line with "only necessary dependencies".

### Testing

| Layer | Test type | What |
|---|---|---|
| `CsvParser` | JVM unit | every scenario in `csv-parsing` (including a Windows-1252 file read through the fallback), plus adversarial input: unclosed quote, quote in an unquoted field, text after a closing quote, CR only, mixed line endings, BOM, blank lines (single- and multi-column), a `""`-only line, header only, empty, Unicode/emoji, very long field, 100k generated rows |
| `RawCsv.toCsvTable()` and `CsvTable.row()` | JVM unit | every scenario in `column-type-inference`, plus Long min/max edges, whitespace, leap day, date-times with an offset (`Z`, positive, negative), an invalid offset |
| `CsvDownloader` | JVM unit (MockWebServer, temp dir) | success, replacement, HTTP error, dropped connection gives a network error, cancellation while a read is blocked stops at once |
| Repository + use case | JVM integration (MockWebServer) | sample file bytes → typed `CsvTable` end to end |
| Parser + type inference on a device | instrumented (emulator or phone) | a generated 100k-row file parsed and typed within a time limit, to check memory and speed where the heap is smaller than on the JVM |
| `LoadCsvTableUseCase` | JVM unit (fake repository) | success, `CsvError` passed on, `OutOfMemoryError` → `FileTooLarge`, other exception → `Unknown`, cancellation is rethrown and not turned into a failure |
| `CsvViewerViewModel` | JVM unit (coroutines-test, fake repository) | Loading → Loaded/NoRows/Failed, `canRetry` per error kind, error effect, Retry from `Failed`, Retry ignored while loading |
| `CsvViewerContent` | instrumented Compose UI | loading indicator, cards with labels and formatted values, `-` for empty, 24-hour times, a date-time with an offset converted to the device time zone, unnamed column label, no-rows message, error message with and without Retry button |

## Risks / Trade-offs

- [A validation file is larger than memory] → The parser streams the file, so the file itself is never loaded as a whole. The parsed rows are kept in memory, though, as raw text; cells are converted only when a row is shown, so there is no second, typed copy of the table. A file too large for memory fails with `FileTooLarge`, and this is noted as a known limit; pagination or a database would be the next step.

## Open Questions

- Snackbar for errors: keep the `ShowError` effect (a short snackbar that repeats the error already shown in the `Failed` state), or drop the snackbar and rely on the `Failed` state alone. Also reword "error screen" in the specs to "the error state of the screen", since it is not a separate screen. To be settled with the user before task 6.1. The specs and tasks keep `ShowError` until then.
- [A validation file uses `;` or dates in another format] → Its columns are shown as TEXT, so data still displays but less precisely. The decision is written down in the specs and can be extended in one place (multi-format date detection is a listed follow-up).
- [A validation file is a Windows-1252 Excel export] → It's read again as Windows-1252 and shown correctly. A file in another code page loads with garbled non-ASCII letters; the README says so.
- [Five modules for one screen] → Each module has a distinct responsibility, and the build-logic plugins keep each module's build file to a few lines.
- [Raising `minSdk` from 24 to 26] → Drops Android 7.x devices, which are negligible today. It avoids enabling desugaring just for `java.time`.
