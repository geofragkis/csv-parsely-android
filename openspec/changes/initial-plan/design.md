# Design

## Context

The project is the Android Studio template: one `:app` module with Compose + Material3, AGP 9.5 (Kotlin built in), Gradle 9.6, a version catalog, `minSdk` 24. There is no DI, networking or architecture yet. Requirements are in `specs/`, motivation in `proposal.md`.

The sample file is 349 bytes with CRLF line endings, no final newline, quoted strings, unquoted integers and quoted ISO local date-times. The real constraint is the unknown validation files, so nothing in the code depends on the sample's columns.

## Goals / Non-Goals

**Goals:**
- Each layer is testable on its own: the parser and type inference as plain JVM unit tests, the download against a fake server, the UI against a fixed state.
- The parser streams its input. The whole table is held in memory only once it has been typed.
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
| `:core:domain` | pure Kotlin/JVM | models (`CsvTable`, `Column`, `ColumnType`, `CellValue`, `RawCsv`), `CsvError`, `CsvRepository` interface, `ColumnTypeInference`, `LoadCsvTableUseCase`, dispatcher qualifiers |
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
3. Copy in a loop: read a chunk from the response source, then write it to the file sink. An exception on the read side is a network error; an exception on the write side is a storage error. Keeping the read and the write separate is what lets us tell "disk full" from "connection lost". `ensureActive()` runs between chunks so cancellation stops the copy.
4. On any failure or cancellation, delete the `.tmp` file. On success, rename it over the final file.

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
- A BOM is removed if it's the first character. A record that is a single empty field from an empty line is skipped.
- EOF while in `IN_QUOTES` gives `CsvError.Malformed(line)`, using the line where the quote opened.
- No records at all gives `CsvError.EmptyFile`.
- Records are produced one at a time (an internal `Sequence`). The header is the first record. Rows are padded or kept wider as the spec describes, and `irregularRowCount` is counted along the way.
- One `StringBuilder` is reused per field, so no per-character allocation.
- The repository opens the file with a UTF-8 decoder set to `CodingErrorAction.REPORT` (not the default `REPLACE`). Invalid bytes throw a `MalformedInputException` during parsing, which becomes `CsvError.UnsupportedEncoding`. The check happens during the one parsing pass, so there's no extra pass over the file. *Alternative:* fall back to Windows-1252. This is kept as a possible follow-up; for now we prefer a clear error to guessing.

*Alternative:* `readLine()` + `split(',')`. That fails on quoted commas and on line breaks inside quotes, which is exactly what the reviewers' files are likely to test.

### Type inference is domain logic in the use case

`CsvRepository.load(): RawCsv` (download + parse, on the IO dispatcher). `LoadCsvTableUseCase` calls it, then `ColumnTypeInference` (on the default dispatcher) does one pass per column to pick the type and a second pass to convert. The result is `Result<CsvTable>`.

- INTEGER: trimmed, `^[+-]?(0|[1-9][0-9]*)$` and `toLongOrNull() != null`.
- DATE: trimmed, `LocalDate.parse` or `LocalDateTime.parse` (ISO). Every cell is stored as a `LocalDateTime`, with date-only values at midnight.
- Otherwise TEXT, keeping the raw string.

`CellValue` is a sealed interface: `Text(String)`, `Integer(Long)`, `Date(LocalDateTime)`, `Empty`.

The use case contains real logic (the conversion the brief asks for), so it's more than a call passed through to the repository. *Alternative:* infer inside the parser. That mixes tokenising with a business rule and makes both harder to test.

### Errors

`CsvError` is a sealed class (extending `Exception`): `Network`, `Http(code)`, `InsufficientStorage`, `EmptyFile`, `Malformed(line)`, `UnsupportedEncoding`. It travels inside `Result`, and the ViewModel maps it to a string resource. Any other exception is turned into a generic error message, never a crash.

### UI: MVVM with unidirectional data flow, no reducers

```
 CsvViewerScreen --onEvent(CsvEvent.Retry)--> CsvViewerViewModel --> LoadCsvTableUseCase
       ^                                          |
       +---- state: StateFlow<CsvUiState> --------+
       +---- effects: Flow<CsvEffect> (Channel) --+--> SnackbarHostState (app Scaffold)
```

- `CsvUiState`: `Loading`, `Loaded(columns, rows)`, `NoRows`, `Failed` (an empty screen with a short hint; the message itself is in the snackbar).
- `CsvEvent`: `Retry`.
- `CsvEffect`: `ShowError(kind)` (indefinite, with Retry except for `UnsupportedEncoding`, because the same file would fail again), `ShowIrregularRows(count)` (short).
- The ViewModel loads in `init`, so a rotation doesn't reload. Effects go through a `Channel`, so a message is delivered once and not replayed after rotation.
- The screen collects state with `collectAsStateWithLifecycle` and effects in a `LaunchedEffect`. A stateless `CsvViewerContent(state)` holds all rendering so UI tests don't need Hilt.
- `LazyColumn(items(rows, key = { it.index }, contentType = { "row" }))`. The row index is the key because it's unique and stable, while row content can repeat.
- Values are formatted in the card composable with a `DateTimeFormatter.ofLocalizedDate(MEDIUM)` remembered per locale. Integers are shown with `toString()` (no grouping, so IDs and years read correctly).
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
| `CsvParser` | JVM unit | every scenario in `csv-parsing` (including UTF-8 vs Windows-1252 bytes), plus adversarial input: unclosed quote, quote in an unquoted field, text after a closing quote, CR only, mixed line endings, BOM, blank lines, header only, empty, Unicode/emoji, very long field, 100k generated rows |
| `ColumnTypeInference` | JVM unit | every scenario in `column-type-inference`, plus Long min/max edges, whitespace, leap day |
| `CsvDownloader` | JVM unit (MockWebServer, temp dir) | success, replacement, HTTP error, dropped connection leaves no file, declared size too big |
| Repository + use case | JVM integration (MockWebServer) | sample file bytes → typed `CsvTable` end to end |
| `CsvViewerViewModel` | JVM unit (coroutines-test, fake repository) | Loading → Loaded/NoRows/Failed, error effect, irregular-row effect, Retry |
| `CsvViewerContent` | instrumented Compose UI | loading indicator, cards with labels and formatted values, `-` for empty, unnamed column label, no-rows message |

## Risks / Trade-offs

- [A validation file is larger than memory] → The parser streams, so only the typed table needs to fit. This is noted as a known limit; pagination or a database would be the next step.
- [A validation file uses `;` or dates in another format] → Its columns are shown as TEXT, so data still displays but less precisely. The decision is written down in the specs and can be extended in one place (multi-format date detection is a listed follow-up).
- [A validation file is a Windows-1252 Excel export] → It's rejected with a clear unsupported-encoding message, not shown with corrupted characters. A Windows-1252 fallback is a listed follow-up.
- [`getAllocatableBytes` may free cache to make room, or the server sends no `Content-Length`] → The write-side check (a disk-full exception gives `InsufficientStorage` and the partial file is deleted) covers both.
- [Five modules for one screen] → Each module has a distinct responsibility, and the build-logic plugins keep each module's build file to a few lines.
- [Raising `minSdk` from 24 to 26] → Drops Android 7.x devices, which are negligible today. It avoids enabling desugaring just for `java.time`.
