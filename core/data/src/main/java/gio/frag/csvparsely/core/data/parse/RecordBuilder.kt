package gio.frag.csvparsely.core.data.parse

/**
 * Created by Giorgos Fragkis on 18-11-2026
 *
 * Collects the fields of the record being read. One instance is reused for every record of the file, and one
 * [StringBuilder] for every field.
 */
internal class RecordBuilder {
    // The values of the current record read so far
    private val fields = mutableListOf<String>()

    // The current value
    private val field = StringBuilder()

    /** Whether the current record has any character, so an empty line can be told apart from `""`. */
    var started = false
        private set

    /** Marks the record as started without adding a character, for an opening quote. */
    fun start() {
        started = true
    }

    /** Adds [char] to the current field. */
    fun append(char: Char) {
        started = true
        field.append(char)
    }

    /** Ends the current field, for a comma, and starts the next one. */
    fun endField() {
        started = true
        fields.add(field.toString())
        field.setLength(0)
    }

    /** Returns the finished record, or an empty list for an empty line, and starts a new one. */
    fun endRecord(): List<String> {
        if (!started) return emptyList()
        endField()
        val record = fields.toList()
        fields.clear()
        started = false
        return record
    }
}
