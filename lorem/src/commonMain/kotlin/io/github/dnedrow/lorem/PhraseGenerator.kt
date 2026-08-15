package io.github.dnedrow.lorem

/**
 * Chooses transition phrases for sentences, avoiding immediate reuse.
 *
 * @param transitions the available phrases. Blank entries are ignored.
 */
public class PhraseGenerator(transitions: List<String>) {

    /** The phrases available for selection, in order. */
    public val transitions: List<String> = transitions.filter { it.isNotBlank() }

    /**
     * Selects a transition phrase, skipping [previous] when an alternative exists.
     *
     * @param previous the phrase used on the preceding sentence, if any.
     * @param random the generator to draw from.
     * @return the chosen phrase, or `null` when no phrase is available.
     */
    public fun nextTransition(previous: String?, random: SeededRandom): String? {
        if (transitions.isEmpty()) return null
        val candidates = if (transitions.size > 1) {
            transitions.filter { it != previous }
        } else {
            transitions
        }
        if (candidates.isEmpty()) return null
        return candidates[random.nextInt(candidates.size)]
    }

    public companion object {
        /** The number of whitespace-separated words in [transition]. */
        public fun wordCount(transition: String): Int = transition.loremWordCount()

        /** Prepends [transition] to [sentence], lowercasing the sentence's first letter. */
        public fun apply(transition: String, sentence: String): String =
            transition + " " + sentence.sentenceUncased()
    }
}
