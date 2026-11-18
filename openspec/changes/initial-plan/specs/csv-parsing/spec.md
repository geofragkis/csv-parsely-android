# Spec Delta

## Purpose

Turns any comma-separated, UTF-8 CSV text into a header and rows of raw text fields, tolerating the common variations found in real-world files.

## ADDED Requirements

### Requirement: First row is the header
The system SHALL treat the first record of the file as the header that names the columns. Every following record SHALL be a data row.

#### Scenario: Sample file
- **WHEN** the file `"First name","Sur name","Issue count","Date of birth","avatar"` is followed by three records
- **THEN** the result has five column names and three rows

#### Scenario: Header only
- **WHEN** the file contains only a header record
- **THEN** the result has the column names and zero rows

### Requirement: Fields separated by commas, with optional quoting
The system SHALL split records on commas. A field enclosed in double quotes SHALL be allowed to contain commas, line breaks and doubled double quotes (`""`), which stand for one literal double quote. The enclosing quotes SHALL NOT be part of the value.

#### Scenario: Comma inside quotes
- **WHEN** a record is `"Jansen, Theo",5`
- **THEN** the fields are `Jansen, Theo` and `5`

#### Scenario: Escaped quote
- **WHEN** a record is `"He said ""hi""",x`
- **THEN** the first field is `He said "hi"`

#### Scenario: Line break inside quotes
- **WHEN** a quoted field contains a line break
- **THEN** the line break is part of the field and does not end the record

#### Scenario: Empty fields
- **WHEN** a record is `a,,""`
- **THEN** the fields are `a`, an empty value and an empty value

### Requirement: Any common line ending
The system SHALL accept CRLF, LF and CR as record terminators, and a final record with no terminator. A line ending SHALL NOT appear in any unquoted field value.

#### Scenario: CRLF without final newline
- **WHEN** records end with CRLF and the last record has no terminator
- **THEN** every record is read and no field contains a carriage return

#### Scenario: CR-only line endings
- **WHEN** records are separated only by CR
- **THEN** each record is read separately

### Requirement: Ignore byte order mark and blank lines
The system SHALL ignore a UTF-8 byte order mark at the start of the file. It SHALL skip completely empty lines before the header, and after it when the header has more than one column. When the header has one column, an empty line SHALL be a row with an empty value. A line containing only `""` SHALL always be a row.

#### Scenario: File starts with a BOM
- **WHEN** the file starts with a UTF-8 BOM
- **THEN** the first column name does not contain the BOM

#### Scenario: Blank lines before the header
- **WHEN** a file starts with empty lines, followed by the header `note` and the row `a`
- **THEN** the header is `note` and the result has one row

#### Scenario: Trailing blank lines
- **WHEN** a file with several columns ends with several empty lines
- **THEN** no empty rows are produced

#### Scenario: Blank line in a single-column file
- **WHEN** the header is `note` and the rows are `a`, an empty line and `b`
- **THEN** the result has three rows, the second with an empty value

#### Scenario: Quoted empty value on its own line
- **WHEN** a file with several columns contains a line with only `""`
- **THEN** that line is a row whose first value is empty and whose other values are padded

### Requirement: Keep rows with an irregular field count
The system SHALL pad a row with fewer fields than the header with empty values. A row with more fields SHALL keep the extra values, and the table SHALL get extra unnamed columns to hold them.

#### Scenario: Short row
- **WHEN** the header has three columns and a row has two fields
- **THEN** the row has three values, the last one empty

#### Scenario: Long row
- **WHEN** the header has two columns and a row has three fields
- **THEN** the table has three columns, the third without a name, and every other row has an empty third value

### Requirement: Only UTF-8 encoded files
The system SHALL decode the file as UTF-8. It SHALL fail with an unsupported-encoding error when the file contains a byte sequence that is not valid UTF-8, and SHALL NOT replace invalid bytes with substitute characters.

#### Scenario: Non-ASCII UTF-8 text
- **WHEN** a field contains `Daniëlle` encoded as UTF-8
- **THEN** the value is `Daniëlle`

#### Scenario: Windows-1252 file
- **WHEN** a field contains `Daniëlle` encoded as Windows-1252
- **THEN** parsing fails with an unsupported-encoding error

### Requirement: Reject files that cannot be parsed
The system SHALL fail with a malformed-file error that names the line where the problem starts when a quoted field is never closed. It SHALL fail with an empty-file error when the file has no records.

#### Scenario: Unclosed quote
- **WHEN** a quoted field opened on line 4 is still open at the end of the file
- **THEN** parsing fails with a malformed-file error that mentions line 4

#### Scenario: Empty file
- **WHEN** the file is empty or contains only blank lines and a BOM
- **THEN** parsing fails with an empty-file error

### Requirement: Stream the file while parsing
The system SHALL read the file as a stream, one character at a time, and SHALL NOT load the whole file into memory at once. While parsing, it SHALL hold only the records already read and the record being read.

#### Scenario: Large file
- **WHEN** a generated file with 100,000 rows is parsed
- **THEN** all rows are produced and the file is never loaded into memory as a whole
