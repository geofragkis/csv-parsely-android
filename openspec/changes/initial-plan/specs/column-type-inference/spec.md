# Spec Delta

## Purpose

Converts the raw text of every parsed column into a string, an integer or a date, decided from the column's content so that any CSV layout is typed correctly.

## ADDED Requirements

### Requirement: One type per column
The system SHALL give each column exactly one type: INTEGER, DATE or TEXT. The type SHALL be decided from all non-empty values in that column. Empty values SHALL NOT affect the decision and SHALL stay empty after conversion.

#### Scenario: Sample file columns
- **WHEN** the sample file is parsed
- **THEN** `First name`, `Sur name` and `avatar` are TEXT, `Issue count` is INTEGER and `Date of birth` is DATE

#### Scenario: Empty values in an integer column
- **WHEN** a column holds `5`, an empty value and `7`
- **THEN** the column is INTEGER and the second value stays empty

#### Scenario: Column with only empty values
- **WHEN** every value in a column is empty
- **THEN** the column is TEXT

### Requirement: Integer columns
The system SHALL type a column as INTEGER when every non-empty value, ignoring surrounding whitespace, is an optionally signed whole number that fits in a 64-bit signed integer and has no leading zeros (other than the value `0` itself).

#### Scenario: Negative numbers
- **WHEN** a column holds `-3`, `0` and `+12`
- **THEN** the column is INTEGER with values -3, 0 and 12

#### Scenario: Number too large
- **WHEN** a column holds `99999999999999999999`
- **THEN** the column is TEXT

#### Scenario: Leading zeros are kept as text
- **WHEN** a column holds postal-code-like values such as `0123`
- **THEN** the column is TEXT and the values keep their leading zeros

#### Scenario: Decimal numbers
- **WHEN** a column holds `1.5`
- **THEN** the column is TEXT

### Requirement: Date columns
The system SHALL type a column as DATE when every non-empty value, ignoring surrounding whitespace, is an ISO-8601 calendar date (`yyyy-MM-dd`) or an ISO-8601 local date-time (`yyyy-MM-ddTHH:mm[:ss[.fraction]]`). A date value SHALL keep its time of day when one is given.

#### Scenario: Date-time values
- **WHEN** a column holds `1978-01-02T00:00:00`
- **THEN** the column is DATE with the value 2 January 1978, midnight

#### Scenario: Date-only values
- **WHEN** a column holds `2001-04-20`
- **THEN** the column is DATE with the value 20 April 2001

#### Scenario: Invalid calendar date
- **WHEN** a column holds `2023-02-30`
- **THEN** the column is TEXT

### Requirement: Text is the fallback
The system SHALL type a column as TEXT when it is not INTEGER or DATE. Text values SHALL be kept exactly as parsed, including surrounding whitespace.

#### Scenario: Mixed column
- **WHEN** a column holds `5` and `five`
- **THEN** the column is TEXT and both values are kept unchanged
