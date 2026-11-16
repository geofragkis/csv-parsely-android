# Design

## Context

The project is the Android Studio template: one `:app` module with Compose + Material3, AGP 9.5 (Kotlin built in), Gradle 9.6, a version catalog, `minSdk` 24. There is no DI, networking or architecture yet. Requirements are in `specs/`, motivation in `proposal.md`.

The sample file is 349 bytes with CRLF line endings, no final newline, quoted strings, unquoted integers and quoted ISO local date-times. The real constraint is the unknown validation files, so nothing in the code depends on the sample's columns.

## Goals / Non-Goals

**Goals:**
- Each layer is testable on its own: the parser and type inference as plain JVM unit tests, the download against a fake server, the UI against a fixed state.
- The parser streams its input, so the file is never loaded into memory as a whole (memory of the parsed rows: see Open Questions).
- Each module has one clear responsibility, and the dependency graph has no cycles.

**Non-Goals:**
- Files larger than available memory (no pagination, no database).
- Delimiters other than comma. Encodings other than UTF-8 are detected and rejected, not decoded.
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

`CsvDownloader` makes a GET request with OkHttp and writes the body to `cacheDir/csv/download.csv.tmp`, then renames it to `download.csv` once the body has been read completely. Steps:

1. A non-2xx status gives `CsvError.Http(code)`. An `IOException` from the call gives `CsvError.Network`.
2. If `Content-Length` is known, compare it with `StorageManager.getAllocatableBytes(...)` for the cache volume. If it's too large, fail with `CsvError.InsufficientStorage` before creating the file.
3. Copy in a loop: read a chunk from the response source, then write it to the file sink. An exception on the read side is a network error. An exception on the write side is `InsufficientStorage` only when its cause chain contains an `ErrnoException` with `errno == OsConstants.ENOSPC`; any other write exception becomes `CsvError.Unknown`. Keeping the read and the write separate is what lets us tell "disk full" from "connection lost". `ensureActive()` runs between chunks so cancellation stops the copy.
4. On any failure or cancellation, delete the `.tmp` file. On success, rename it over the final file.
5. Only one download runs at a time, because the ViewModel accepts Retry only in the `Failed` state (see UI). So a fixed `.tmp` name is safe.

*Alternatives:* stream straight into the parser (no disk, but a literal reading of "the application must download the file" asks for a file, and the parser would be coupled to the network); Retrofit (adds nothing for one GET of a raw file).

### Parser: a character state machine over a `Reader`

`CsvParser.parse(reader: Reader): RawCsv` reads a `BufferedReader` character by character through a small state machine:

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
- Records are produced one at a time (an internal `Sequence`). The header is the first record. Rows are padded or kept wider as the spec describes, and `irregularRowCount` is counted along the way.
- One `StringBuilder` is reused per field, so no per-character allocation.
- The repository opens the file with a UTF-8 decoder set to `CodingErrorAction.REPORT` (not the default `REPLACE`). Invalid bytes throw a `MalformedInputException` during parsing, which becomes `CsvError.UnsupportedEncoding`. The check happens during the one parsing pass, so there's no extra pass over the file. *Alternative:* fall back to Windows-1252. This is kept as a possible follow-up; for now we prefer a clear error to guessing.

*Alternative:* `readLine()` + `split(',')`. That fails on quoted commas and on line breaks inside quotes, which is exactly what the reviewers' files are likely to test.

### Type inference is domain logic in the use case

`CsvRepository.load(): RawCsv` (download + parse, on the IO dispatcher). `LoadCsvTableUseCase` calls it, then `RawCsv.toCsvTable()` (on the default dispatcher) does one pass per column to pick the type and a second pass to convert. The result is `Result<CsvTable>`.

- INTEGER: trimmed, `^[+-]?(0|[1-9][0-9]*)$` and `toLongOrNull() != null`.
- DATE: trimmed, `LocalDate.parse` or `LocalDateTime.parse` with strict ISO formatters: a four-digit year from 0001 with no sign, an uppercase `T` before the time, and dates or times that don't exist (`2023-02-30`, `24:00`) rejected instead of adjusted. A cell keeps what the file gave: a date-time stays a date-time (midnight included), and a date-only value has no time.
- Otherwise TEXT, keeping the raw string.

`CellValue` is a sealed interface: `Text(String)`, `Integer(Long)`, `Date(date: LocalDate, time: LocalTime?)`, `Empty`. `time == null` means the file gave only a date, which keeps a date-only value apart from a real midnight.

Dates are shown as written and never converted to the user's time zone. The file does not say whether a value is a calendar date (such as a date of birth) or a moment in time, and converting a calendar date can shift it by a day. Date-times with an offset are typed as TEXT.

The use case contains real logic (the conversion the brief asks for), so it's more than a call passed through to the repository. *Alternative:* infer inside the parser. That mixes tokenising with a business rule and makes both harder to test.

### Errors

`CsvError` is a sealed class (extending `Exception`): `Network`, `Http(code)`, `InsufficientStorage`, `EmptyFile`, `Malformed(line)`, `UnsupportedEncoding`, `Unknown(cause)`. It travels inside `Result`, and the ViewModel maps it to a string resource.

The use case builds the `Result` with a `try`/`catch`, not with `runCatching`: `CancellationException` is always rethrown, so a cancelled load (ViewModel cleared) never turns into a failure or an error message. A `CsvError` is passed on as is, and any other exception is wrapped in `CsvError.Unknown`, so the UI always gets a known kind and the app never crashes on an unexpected error.

### UI: MVVM with unidirectional data flow, no reducers

```
 CsvViewerScreen --onEvent(CsvEvent.Retry)--> CsvViewerViewModel --> LoadCsvTableUseCase
       ^                                          |
       +---- state: StateFlow<CsvUiState> --------+
       +---- effects: Flow<CsvEffect> (Channel) --+--> SnackbarHostState (app Scaffold)
```

- `CsvUiState`: `Loading`, `Loaded(columns, rows)`, `NoRows`, `Failed(error, canRetry)`. The error message and the Retry button are part of the state, so they survive any configuration change (the Activity, and with it the `SnackbarHostState`, is recreated, but the ViewModel is not).
- `canRetry` is true only for network-related and authentication failures (`Network`, `Http` 401/403/408/429/5xx) and false for everything else (`InsufficientStorage`, `Malformed`, `EmptyFile`, `UnsupportedEncoding`, other 4xx, `Unknown`). A single property `CsvError.canRetry` holds this rule.
- `CsvEvent`: `Retry`. The ViewModel ignores it unless the state is `Failed`, so a second download can never start while one is running.
- `CsvEffect`: `ShowError(kind)` (short, no action: it announces the failure, while the screen holds the message and the Retry button), `ShowIrregularRows(count)` (short).
- The ViewModel loads in `init`, so a configuration change doesn't reload. Effects go through a `Channel`, so a snackbar is delivered once and not replayed.
- The app is locked to portrait (`android:screenOrientation="portrait"`). Since Android 16 (we target 37) the platform ignores this on large screens (smallest width ≥ 600dp), so the layout must still work in landscape there. Other configuration changes (dark mode, locale, font size) can happen on any device, which is why the state-based error screen is needed anyway.
- The screen collects state with `collectAsStateWithLifecycle` and effects in a `LaunchedEffect`. A stateless `CsvViewerContent(state)` holds all rendering so UI tests don't need Hilt.
- `LazyColumn(items(rows, key = { it.index }, contentType = { "row" }))`. The row index is the key because it's unique and stable, while row content can repeat.
- Values are formatted in the card composable with two formatters remembered per locale: `ofLocalizedDateTime(MEDIUM, SHORT)` for dates with a time (always shown, midnight included) and `ofLocalizedDate(MEDIUM)` for date-only values. Integers are shown with `toString()` (no grouping, so IDs and years read correctly).
- The global snackbar: `:app`'s `Scaffold` owns the `SnackbarHostState` and passes it to the screen.
- Compose strong skipping (on by default with the Kotlin 2.x Compose compiler) means we don't need immutable collection libraries. The UI models are `@Immutable` data classes.

### Dependency injection

Hilt with one `NetworkModule` (`OkHttpClient` with timeouts), one `DataModule` (`@Binds CsvRepository`, the CSV URL, the cache directory) and one `DispatchersModule` (`@IoDispatcher`, `@DefaultDispatcher`). KSP is used for annotation processing.

### Build changes

- `minSdk` 26 so `java.time` and `StorageManager.getAllocatableBytes` are available without desugaring. That still covers practically all active devices.
- Remove unused template dependencies (e.g. espresso if no test uses it), in line with "only necessary dependencies".

### Testing

| Layer | Test type | What |
|---|---|---|
| `CsvParser` | JVM unit | every scenario in `csv-parsing` (including UTF-8 vs Windows-1252 bytes), plus adversarial input: unclosed quote, quote in an unquoted field, text after a closing quote, CR only, mixed line endings, BOM, blank lines (single- and multi-column), a `""`-only line, header only, empty, Unicode/emoji, very long field, 100k generated rows |
| `RawCsv.toCsvTable()` | JVM unit | every scenario in `column-type-inference`, plus Long min/max edges, whitespace, leap day |
| `CsvDownloader` | JVM unit (MockWebServer, temp dir) | success, replacement, HTTP error, dropped connection leaves no file, declared size too big, cancellation mid-copy leaves no file, disk full while writing (ENOSPC from a failing sink) gives `InsufficientStorage`, other write failure gives `Unknown` |
| Repository + use case | JVM integration (MockWebServer) | sample file bytes → typed `CsvTable` end to end |
| `LoadCsvTableUseCase` | JVM unit (fake repository) | success, `CsvError` passed on, other exception → `Unknown`, cancellation is rethrown and not turned into a failure |
| `CsvViewerViewModel` | JVM unit (coroutines-test, fake repository) | Loading → Loaded/NoRows/Failed, `canRetry` per error kind, error effect, irregular-row effect, Retry from `Failed`, Retry ignored while loading |
| `CsvViewerContent` | instrumented Compose UI | loading indicator, cards with labels and formatted values, `-` for empty, unnamed column label, no-rows message, error message with and without Retry button |

## Risks / Trade-offs

- [A validation file is larger than memory] → The parser streams the file, so the file itself is never loaded as a whole. The parsed rows are kept in memory, though: while `RawCsv.toCsvTable()` converts them, the raw rows (`RawCsv`) and the typed rows (`CsvTable`) both exist for a moment, so peak memory is roughly twice the table. This is noted as a known limit; pagination or a database would be the next step.

## Open Questions

- Memory use of parsing and type inference (see the first risk above). To be discussed again with the user before implementing the parser (task 5.1): whether the brief raw + typed peak is acceptable, or whether rows should be converted in a way that avoids holding both copies. The specs don't change either way.
- Snackbar for errors: keep the `ShowError` effect (a short snackbar that repeats the error already shown in the `Failed` state), or use the snackbar only for the irregular-rows warning (`ShowIrregularRows`). Also reword "error screen" in the specs to "the error state of the screen", since it is not a separate screen. To be settled with the user before task 6.1. The specs and tasks keep `ShowError` until then.
- [A validation file uses `;` or dates in another format] → Its columns are shown as TEXT, so data still displays but less precisely. The decision is written down in the specs and can be extended in one place (multi-format date detection is a listed follow-up).
- [A validation file is a Windows-1252 Excel export] → It's rejected with a clear unsupported-encoding message, not shown with corrupted characters. A Windows-1252 fallback is a listed follow-up.
- [`getAllocatableBytes` may free cache to make room, or the server sends no `Content-Length`] → The write-side check (a disk-full exception gives `InsufficientStorage` and the partial file is deleted) covers both.
- [Five modules for one screen] → Each module has a distinct responsibility, and the build-logic plugins keep each module's build file to a few lines.
- [Raising `minSdk` from 24 to 26] → Drops Android 7.x devices, which are negligible today. It avoids enabling desugaring just for `java.time`.
