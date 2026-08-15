package io.github.dnedrow.lorem

/**
 * A weighted sentence shape used to build generated text.
 *
 * @property pattern the grammatical shape this template produces.
 * @property weight the relative likelihood of selection. Always positive.
 * @property format the template text, mixing literals with `{wN}` word tokens.
 */
public class SentenceTemplate
@Throws(LoremException::class)
constructor(
    public val pattern: Pattern,
    public val weight: Int,
    public val format: String,
) {

    /** The grammatical shape of a sentence. */
    public enum class Pattern {
        /** A single short clause. */
        SIMPLE,

        /** Two clauses joined by a conjunction. */
        COMPOUND,

        /** A main clause with a subordinate clause. */
        COMPLEX,

        /** An enumeration of items. */
        LIST,

        /** An interrogative sentence. */
        QUESTION,

        /** A short, emphatic statement. */
        EMPHASIS,
    }

    init {
        if (weight <= 0) throw LoremException.InvalidWeight()
        // Parsing throws InvalidTemplate for a malformed format, so an instance is always
        // renderable. The generator re-parses once per pass rather than holding the renderer here,
        // keeping this type a plain value.
        TemplateRenderer(format)
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is SentenceTemplate &&
                    pattern == other.pattern &&
                    weight == other.weight &&
                    format == other.format
                )

    override fun hashCode(): Int {
        var result = pattern.hashCode()
        result = 31 * result + weight
        result = 31 * result + format.hashCode()
        return result
    }

    override fun toString(): String = "SentenceTemplate(pattern=$pattern, weight=$weight, format=$format)"
}
