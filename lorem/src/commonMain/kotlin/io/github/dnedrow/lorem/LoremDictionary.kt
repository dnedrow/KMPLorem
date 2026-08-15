package io.github.dnedrow.lorem

/**
 * The pool of words used to render sentences.
 *
 * Words are trimmed, de-duplicated preserving first-seen order (which keeps generation
 * deterministic), and validated to be non-empty.
 */
public class LoremDictionary
@Throws(LoremException::class)
constructor(words: List<String>) {

    /** The unique words available to the renderer, in first-seen order. */
    public val words: List<String>

    init {
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<String>()
        for (word in words) {
            val trimmed = word.trim()
            if (trimmed.isEmpty()) continue
            if (seen.add(trimmed)) unique.add(trimmed)
        }
        if (unique.isEmpty()) throw LoremException.EmptyDictionary()
        this.words = unique
    }

    /**
     * Returns the word at [index], wrapping around the word list.
     *
     * [words] is never empty, so the modulo is always safe.
     */
    internal fun wordAt(index: Int): String = words[index % words.size]

    override fun equals(other: Any?): Boolean =
        this === other || (other is LoremDictionary && words == other.words)

    override fun hashCode(): Int = words.hashCode()

    override fun toString(): String = "LoremDictionary(${words.size} words)"

    public companion object {
        /**
         * The dictionary built into the library: roughly 210 unique Latin words.
         *
         * Available on every target without file, bundle, or network access, so it cannot fail.
         */
        public val BuiltIn: LoremDictionary = LoremDictionary(BUILT_IN_WORDS)
    }
}
