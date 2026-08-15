package io.github.dnedrow.lorem

/**
 * Tuning knobs applied to every generation pass, independent of paragraph style.
 *
 * @property transitionProbability a scale applied to the active style's transition probability,
 *   clamped to `0.0..1.0`. Use `0.0` to suppress transitions regardless of style.
 * @property allowQuestions whether question templates may be selected.
 * @property allowShortSentences whether sentences may use the lower half of the word-count range.
 * @property avoidImmediatePatternRepeats whether the same [SentenceTemplate.Pattern] may appear on
 *   two consecutive sentences when alternatives exist.
 * @property startsWithCanonicalOpening whether generated text begins with
 *   [LoremGenerator.CANONICAL_OPENING].
 *
 *   The phrase acts as a prefix on the **first sentence of a call** only — `generateSentence`,
 *   `generateParagraph`, or the first paragraph of `generateParagraphs` — and the sentence continues
 *   with generated words.
 *
 *   Two behaviors are worth knowing:
 *   - Word-count bounds stay authoritative. When [maximumWordsPerSentence] cannot hold the whole
 *     phrase, the phrase is truncated from the end, keeping as many leading words as fit.
 *   - Enabling this option changes the *entire* generated text for a given seed, not only its first
 *     few words, because the phrase replaces dictionary draws and so shifts the random sequence.
 * @property minimumWordsPerSentence the fewest words a generated sentence may contain.
 * @property maximumWordsPerSentence the most words a generated sentence may contain.
 * @property customTransitions transition phrases replacing the defaults when non-empty.
 * @property customTemplates sentence templates replacing the defaults when non-empty.
 *
 * @throws LoremException.InvalidWeight when the word-count bounds are not positive or the minimum
 *   exceeds the maximum.
 */
public class GeneratorConfiguration
@Throws(LoremException::class)
constructor(
    transitionProbability: Double = 1.0,
    public val allowQuestions: Boolean = true,
    public val allowShortSentences: Boolean = true,
    public val avoidImmediatePatternRepeats: Boolean = true,
    public val startsWithCanonicalOpening: Boolean = false,
    public val minimumWordsPerSentence: Int = 6,
    public val maximumWordsPerSentence: Int = 20,
    public val customTransitions: List<String> = emptyList(),
    public val customTemplates: List<SentenceTemplate> = emptyList(),
) {

    public val transitionProbability: Double = transitionProbability.coerceIn(0.0, 1.0)

    init {
        if (minimumWordsPerSentence <= 0 || maximumWordsPerSentence <= 0) {
            throw LoremException.InvalidWeight()
        }
        if (minimumWordsPerSentence > maximumWordsPerSentence) {
            throw LoremException.InvalidWeight()
        }
    }

    /**
     * The lowest word count a sentence may target, raised above [minimumWordsPerSentence] when
     * short sentences are disallowed.
     */
    internal val effectiveMinimumWords: Int
        get() = if (allowShortSentences) {
            minimumWordsPerSentence
        } else {
            (minimumWordsPerSentence + maximumWordsPerSentence + 1) / 2
        }

    /**
     * Returns a copy with the given properties replaced.
     *
     * Written by hand rather than generated: a `data class` cannot clamp a constructor property in
     * its `init` block, and clamping [transitionProbability] is required behavior. Routing through
     * the primary constructor keeps both the clamp and the word-bound validation on every copy.
     */
    @Throws(LoremException::class)
    public fun copy(
        transitionProbability: Double = this.transitionProbability,
        allowQuestions: Boolean = this.allowQuestions,
        allowShortSentences: Boolean = this.allowShortSentences,
        avoidImmediatePatternRepeats: Boolean = this.avoidImmediatePatternRepeats,
        startsWithCanonicalOpening: Boolean = this.startsWithCanonicalOpening,
        minimumWordsPerSentence: Int = this.minimumWordsPerSentence,
        maximumWordsPerSentence: Int = this.maximumWordsPerSentence,
        customTransitions: List<String> = this.customTransitions,
        customTemplates: List<SentenceTemplate> = this.customTemplates,
    ): GeneratorConfiguration = GeneratorConfiguration(
        transitionProbability = transitionProbability,
        allowQuestions = allowQuestions,
        allowShortSentences = allowShortSentences,
        avoidImmediatePatternRepeats = avoidImmediatePatternRepeats,
        startsWithCanonicalOpening = startsWithCanonicalOpening,
        minimumWordsPerSentence = minimumWordsPerSentence,
        maximumWordsPerSentence = maximumWordsPerSentence,
        customTransitions = customTransitions,
        customTemplates = customTemplates,
    )

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is GeneratorConfiguration &&
                    transitionProbability == other.transitionProbability &&
                    allowQuestions == other.allowQuestions &&
                    allowShortSentences == other.allowShortSentences &&
                    avoidImmediatePatternRepeats == other.avoidImmediatePatternRepeats &&
                    startsWithCanonicalOpening == other.startsWithCanonicalOpening &&
                    minimumWordsPerSentence == other.minimumWordsPerSentence &&
                    maximumWordsPerSentence == other.maximumWordsPerSentence &&
                    customTransitions == other.customTransitions &&
                    customTemplates == other.customTemplates
                )

    override fun hashCode(): Int {
        var result = transitionProbability.hashCode()
        result = 31 * result + allowQuestions.hashCode()
        result = 31 * result + allowShortSentences.hashCode()
        result = 31 * result + avoidImmediatePatternRepeats.hashCode()
        result = 31 * result + startsWithCanonicalOpening.hashCode()
        result = 31 * result + minimumWordsPerSentence
        result = 31 * result + maximumWordsPerSentence
        result = 31 * result + customTransitions.hashCode()
        result = 31 * result + customTemplates.hashCode()
        return result
    }

    override fun toString(): String =
        "GeneratorConfiguration(transitionProbability=$transitionProbability, " +
            "allowQuestions=$allowQuestions, allowShortSentences=$allowShortSentences, " +
            "avoidImmediatePatternRepeats=$avoidImmediatePatternRepeats, " +
            "startsWithCanonicalOpening=$startsWithCanonicalOpening, " +
            "minimumWordsPerSentence=$minimumWordsPerSentence, " +
            "maximumWordsPerSentence=$maximumWordsPerSentence, " +
            "customTransitions=$customTransitions, customTemplates=$customTemplates)"

    public companion object {
        /**
         * The configuration used when none is supplied.
         *
         * Built through the normal validated path; the reference implementation's private
         * unvalidated initializer has no Kotlin counterpart because property initializers here are
         * free to throw.
         */
        public val Default: GeneratorConfiguration = GeneratorConfiguration()
    }
}
