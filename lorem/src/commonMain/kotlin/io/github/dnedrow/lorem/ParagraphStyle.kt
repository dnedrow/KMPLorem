package io.github.dnedrow.lorem

/**
 * The rhetorical shape of a generated paragraph.
 *
 * A style controls how many sentences a paragraph contains, how often transitions appear, how
 * strongly longer sentence patterns are favoured, how often questions appear, and the preferred mix
 * of sentence patterns.
 *
 * @property name the style's identifier, useful for debugging and logging.
 * @property sentenceCountRange the inclusive range of sentences a paragraph may contain.
 * @property transitionProbability the chance, from `0` to `1`, that a sentence is prefixed with a
 *   transition. Values outside that range are clamped.
 * @property complexityWeighting a multiplier applied to compound and complex template weights.
 *   Negative values are clamped to zero.
 * @property questionFrequency a multiplier applied to question template weights. `0` removes
 *   questions. Negative values are clamped to zero.
 * @property patternWeightMultipliers per-pattern multipliers defining the preferred template mix.
 */
public class ParagraphStyle(
    public val name: String,
    public val sentenceCountRange: IntRange,
    transitionProbability: Double,
    complexityWeighting: Double,
    questionFrequency: Double,
    public val patternWeightMultipliers: Map<SentenceTemplate.Pattern, Double> = emptyMap(),
) {

    public val transitionProbability: Double = transitionProbability.coerceIn(0.0, 1.0)
    public val complexityWeighting: Double = maxOf(complexityWeighting, 0.0)
    public val questionFrequency: Double = maxOf(questionFrequency, 0.0)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is ParagraphStyle &&
                    name == other.name &&
                    sentenceCountRange == other.sentenceCountRange &&
                    transitionProbability == other.transitionProbability &&
                    complexityWeighting == other.complexityWeighting &&
                    questionFrequency == other.questionFrequency &&
                    patternWeightMultipliers == other.patternWeightMultipliers
                )

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + sentenceCountRange.hashCode()
        result = 31 * result + transitionProbability.hashCode()
        result = 31 * result + complexityWeighting.hashCode()
        result = 31 * result + questionFrequency.hashCode()
        result = 31 * result + patternWeightMultipliers.hashCode()
        return result
    }

    override fun toString(): String = "ParagraphStyle(name=$name)"

    public companion object {
        /** Balanced prose with a moderate mix of every pattern. */
        public val Classic: ParagraphStyle = ParagraphStyle(
            name = "classic",
            sentenceCountRange = 3..6,
            transitionProbability = 0.3,
            complexityWeighting = 1.0,
            questionFrequency = 1.0,
        )

        /** Documentation-like prose favouring lists and complex sentences. */
        public val Technical: ParagraphStyle = ParagraphStyle(
            name = "technical",
            sentenceCountRange = 4..7,
            transitionProbability = 0.4,
            complexityWeighting = 1.4,
            questionFrequency = 0.4,
            patternWeightMultipliers = mapOf(
                SentenceTemplate.Pattern.LIST to 2.0,
                SentenceTemplate.Pattern.EMPHASIS to 0.5,
            ),
        )

        /** Scholarly prose with frequent transitions and long sentences. */
        public val Academic: ParagraphStyle = ParagraphStyle(
            name = "academic",
            sentenceCountRange = 4..8,
            transitionProbability = 0.6,
            complexityWeighting = 1.8,
            questionFrequency = 0.3,
            patternWeightMultipliers = mapOf(
                SentenceTemplate.Pattern.SIMPLE to 0.5,
                SentenceTemplate.Pattern.EMPHASIS to 0.4,
            ),
        )

        /** Dense contractual prose: long sentences, few transitions, no questions. */
        public val Legal: ParagraphStyle = ParagraphStyle(
            name = "legal",
            sentenceCountRange = 3..5,
            transitionProbability = 0.2,
            complexityWeighting = 2.2,
            questionFrequency = 0.0,
            patternWeightMultipliers = mapOf(
                SentenceTemplate.Pattern.SIMPLE to 0.3,
                SentenceTemplate.Pattern.EMPHASIS to 0.2,
                SentenceTemplate.Pattern.LIST to 1.5,
            ),
        )

        /** Varied prose that draws evenly from the full pattern set. */
        public val Mixed: ParagraphStyle = ParagraphStyle(
            name = "mixed",
            sentenceCountRange = 2..7,
            transitionProbability = 0.35,
            complexityWeighting = 1.0,
            questionFrequency = 1.6,
            patternWeightMultipliers = mapOf(SentenceTemplate.Pattern.EMPHASIS to 1.5),
        )

        /** Every bundled preset, in declaration order. */
        public val presets: List<ParagraphStyle> =
            listOf(Classic, Technical, Academic, Legal, Mixed)
    }
}
