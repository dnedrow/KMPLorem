package io.github.dnedrow.lorem

/**
 * Every failure this library can report.
 *
 * All failure conditions are argument errors detectable at development time, which is why the
 * hierarchy is rooted in [IllegalArgumentException]. Sealing it lets callers discriminate
 * exhaustively with a `when` expression.
 *
 * The library never terminates the process on a documented failure. Public functions that can
 * fail are annotated with `@Throws(LoremException::class)` so Kotlin/Native consumers receive a
 * catchable error rather than a crash.
 */
public sealed class LoremException(message: String) : IllegalArgumentException(message) {

    /** A dictionary was constructed from an empty or whitespace-only word list. */
    public class EmptyDictionary : LoremException(
        "A Lorem dictionary must contain at least one word.",
    )

    /**
     * A sentence template contained a malformed `{wN}` token or an unmatched brace, or no usable
     * templates remained for generation.
     */
    public class InvalidTemplate : LoremException(
        "The sentence template is malformed or no templates are available.",
    )

    /**
     * A weight or word-count bound was outside its allowed range, for example a non-positive
     * template weight or a minimum word count above the maximum.
     */
    public class InvalidWeight : LoremException(
        "A weight or word-count bound was outside its allowed range.",
    )
}
