package io.github.dnedrow.lorem

import kotlin.math.roundToInt

/**
 * Produces deterministic Lorem Ipsum text for previews, tests, and documentation.
 *
 * A generator is an immutable value: the same dictionary, seed, configuration, and request always
 * produce identical output, and separate instances are safe to use concurrently.
 *
 * ```kotlin
 * val generator = LoremGenerator(dictionary = LoremDictionary.BuiltIn, seed = 42L)
 * val paragraph = generator.generateParagraph(style = ParagraphStyle.Technical)
 * ```
 *
 * Because generation is deterministic, calling [generateParagraph] twice on the same instance
 * returns the same paragraph. Use [generateParagraphs] when several *different* paragraphs are
 * needed from one reproducible call.
 *
 * @property dictionary the word pool sentences are drawn from.
 * @property seed the seed determining every random decision.
 * @property configuration the tuning applied to every generation pass.
 */
public class LoremGenerator(
    public val dictionary: LoremDictionary = LoremDictionary.BuiltIn,
    public val seed: Long,
    public val configuration: GeneratorConfiguration = GeneratorConfiguration.Default,
) {

    /**
     * Generates a single sentence.
     *
     * @param style the style whose template mix is applied.
     * @throws LoremException when no usable template is available.
     */
    @Throws(LoremException::class)
    public fun generateSentence(style: ParagraphStyle = ParagraphStyle.Classic): String {
        val random = SeededRandom(seed)
        val state = PassState()
        val prepared = prepareTemplates(style)
        val phrases = makePhraseGenerator()
        return makeSentence(style, isFirst = true, prepared = prepared, phrases = phrases, state = state, random = random)
    }

    /**
     * Generates one paragraph whose sentence count falls inside the style's range.
     *
     * @param style the style controlling length, transitions, and mix.
     * @throws LoremException when no usable template is available.
     */
    @Throws(LoremException::class)
    public fun generateParagraph(style: ParagraphStyle = ParagraphStyle.Classic): String {
        val random = SeededRandom(seed)
        val state = PassState()
        val prepared = prepareTemplates(style)
        val phrases = makePhraseGenerator()
        return makeParagraph(style, prepared, phrases, state, random)
    }

    /**
     * Generates several paragraphs that differ from one another while the call as a whole stays
     * reproducible for the generator's seed.
     *
     * @param count how many paragraphs to produce. Values below `1` yield an empty list.
     * @param style the style controlling length, transitions, and mix.
     * @throws LoremException when no usable template is available.
     */
    @Throws(LoremException::class)
    public fun generateParagraphs(
        count: Int,
        style: ParagraphStyle = ParagraphStyle.Classic,
    ): List<String> {
        if (count <= 0) return emptyList()
        val random = SeededRandom(seed)
        val state = PassState()
        val prepared = prepareTemplates(style)
        val phrases = makePhraseGenerator()
        return List(count) { makeParagraph(style, prepared, phrases, state, random) }
    }

    // MARK: - Pass state

    /** Per-call state used for anti-repetition. Never stored on the generator. */
    private class PassState {
        var lastPattern: SentenceTemplate.Pattern? = null
        var lastTransition: String? = null

        /** Shared with the renderer so word repetition is avoided across sentence boundaries. */
        val words: WordState = WordState()

        /**
         * Whether the canonical opening has already been emitted in this call.
         *
         * Keeps the phrase on the first sentence of the first paragraph only.
         */
        var hasEmittedOpening: Boolean = false
    }

    /** A template paired with its parsed renderer and style-adjusted weight. */
    private class PreparedTemplate(
        val template: SentenceTemplate,
        val renderer: TemplateRenderer,
        val weight: Int,
    )

    // MARK: - Preparation

    private fun makePhraseGenerator(): PhraseGenerator {
        val transitions = configuration.customTransitions.ifEmpty {
            TransitionLibrary.defaultTransitions
        }
        return PhraseGenerator(transitions)
    }

    private fun prepareTemplates(style: ParagraphStyle): List<PreparedTemplate> {
        val templates = configuration.customTemplates.ifEmpty { TemplateLibrary.defaultTemplates }
        val questionsAllowed = configuration.allowQuestions && style.questionFrequency > 0

        val prepared = templates.mapNotNull { template ->
            if (template.pattern == SentenceTemplate.Pattern.QUESTION && !questionsAllowed) {
                return@mapNotNull null
            }
            val renderer = TemplateRenderer(template.format)
            // A template that cannot render inside the configured maximum is dropped rather than
            // allowed to overrun it, so the bound stays authoritative. The reference implementation
            // lets the template's own minimum win instead.
            if (renderer.minimumWordCount > configuration.maximumWordsPerSentence) {
                return@mapNotNull null
            }
            PreparedTemplate(
                template = template,
                renderer = renderer,
                weight = weightFor(template, style),
            )
        }
        if (prepared.isEmpty()) throw LoremException.InvalidTemplate()
        return prepared
    }

    private fun weightFor(template: SentenceTemplate, style: ParagraphStyle): Int {
        var value = template.weight.toDouble()
        when (template.pattern) {
            SentenceTemplate.Pattern.COMPOUND,
            SentenceTemplate.Pattern.COMPLEX,
            -> value *= style.complexityWeighting

            SentenceTemplate.Pattern.QUESTION -> value *= style.questionFrequency

            SentenceTemplate.Pattern.SIMPLE,
            SentenceTemplate.Pattern.LIST,
            SentenceTemplate.Pattern.EMPHASIS,
            -> Unit
        }
        value *= style.patternWeightMultipliers[template.pattern] ?: 1.0
        return maxOf(1, value.roundToInt())
    }

    // MARK: - Generation

    private fun makeParagraph(
        style: ParagraphStyle,
        prepared: List<PreparedTemplate>,
        phrases: PhraseGenerator,
        state: PassState,
        random: SeededRandom,
    ): String {
        val sentenceCount = random.nextInt(style.sentenceCountRange.first, style.sentenceCountRange.last + 1)
        return (0 until maxOf(sentenceCount, 1)).joinToString(" ") { index ->
            makeSentence(style, index == 0, prepared, phrases, state, random)
        }
    }

    private fun makeSentence(
        style: ParagraphStyle,
        isFirst: Boolean,
        prepared: List<PreparedTemplate>,
        phrases: PhraseGenerator,
        state: PassState,
        random: SeededRandom,
    ): String {
        val opensWithPhrase = configuration.startsWithCanonicalOpening &&
            isFirst &&
            !state.hasEmittedOpening

        var candidates = candidates(prepared, state.lastPattern)
        if (opensWithPhrase) {
            val tokenLeading = candidates.filter { it.renderer.beginsWithToken }
            if (tokenLeading.isNotEmpty()) candidates = tokenLeading
        }
        val selector = WeightedSelector(candidates.map { WeightedItem(it, it.weight) })
        val chosen = selector.select(random)

        val maximum = configuration.maximumWordsPerSentence
        val minimum = minOf(configuration.effectiveMinimumWords, maximum)
        var target = random.nextInt(minimum, maximum + 1)

        val openingWords = canonicalOpeningWords
        // A token-leading template absorbs the phrase into its own word slots; otherwise the phrase
        // has to be prepended and needs its own budget.
        val injectsOpening = opensWithPhrase && chosen.renderer.beginsWithToken
        val prependsOpening = opensWithPhrase && !injectsOpening

        if (opensWithPhrase) {
            // A token-leading template needs room for the phrase in its first token while every
            // other token keeps at least one word.
            val desired = if (injectsOpening) {
                chosen.renderer.fixedWordCount +
                    openingWords.size +
                    maxOf(0, chosen.renderer.tokenCounts.size - 1)
            } else {
                chosen.renderer.minimumWordCount + openingWords.size
            }
            target = maxOf(target, minOf(maximum, desired))
        }

        var transition: String? = null
        if (!isFirst) {
            val probability = style.transitionProbability * configuration.transitionProbability
            if (probability > 0 && random.nextDouble() < probability) {
                transition = phrases.nextTransition(state.lastTransition, random)
            }
        }

        var transitionWords = transition?.let { PhraseGenerator.wordCount(it) } ?: 0
        if (chosen.renderer.minimumWordCount + transitionWords > maximum) {
            transition = null
            transitionWords = 0
        }

        val prefixWords = if (prependsOpening) {
            maxOf(0, minOf(openingWords.size, maximum - chosen.renderer.minimumWordCount))
        } else {
            0
        }
        val reserved = transitionWords + prefixWords

        var bodyTarget = target - reserved
        bodyTarget = maxOf(bodyTarget, chosen.renderer.minimumWordCount)
        bodyTarget = minOf(bodyTarget, maxOf(chosen.renderer.minimumWordCount, maximum - reserved))

        val budget = bodyTarget - chosen.renderer.fixedWordCount
        var counts = distribute(chosen.renderer.tokenCounts, budget)
        if (injectsOpening) {
            counts = reserveLeadingCapacity(counts, atLeast = openingWords.size)
        }
        // Trailing phrase words that exceed the first token's capacity are dropped so the sentence
        // still respects `maximumWordsPerSentence`.
        val leadingWords = if (injectsOpening) {
            openingWords.take(counts.firstOrNull() ?: 0)
        } else {
            emptyList()
        }

        val body = chosen.renderer.render(
            dictionary = dictionary,
            random = random,
            overrideCounts = counts,
            leadingWords = leadingWords,
            state = state.words,
        )

        state.lastPattern = chosen.template.pattern
        if (opensWithPhrase) state.hasEmittedOpening = true

        if (prependsOpening && prefixWords > 0) {
            val phrase = openingWords.take(prefixWords).joinToString(" ")
            return phrase + " " + body.sentenceUncased()
        }
        val chosenTransition = transition ?: return body
        state.lastTransition = chosenTransition
        return PhraseGenerator.apply(chosenTransition, body)
    }

    private fun candidates(
        prepared: List<PreparedTemplate>,
        lastPattern: SentenceTemplate.Pattern?,
    ): List<PreparedTemplate> {
        if (!configuration.avoidImmediatePatternRepeats || lastPattern == null) return prepared
        val filtered = prepared.filter { it.template.pattern != lastPattern }
        return filtered.ifEmpty { prepared }
    }

    public companion object {
        /**
         * The canonical Lorem Ipsum opening phrase.
         *
         * Enable [GeneratorConfiguration.startsWithCanonicalOpening] to have generated text begin
         * with this phrase.
         */
        public const val CANONICAL_OPENING: String = "Lorem ipsum dolor sit amet"

        /**
         * The words of [CANONICAL_OPENING], in order.
         *
         * When the configured maximum word count cannot hold the whole phrase, only a leading slice
         * of these words is used.
         */
        public val canonicalOpeningWords: List<String> = CANONICAL_OPENING.splitOnWhitespace()
    }
}
