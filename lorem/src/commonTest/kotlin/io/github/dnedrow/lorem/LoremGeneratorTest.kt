package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoremGeneratorTest {

    private val dictionary = LoremDictionary.BuiltIn

    private fun generator(
        seed: Long = 42L,
        configuration: GeneratorConfiguration = GeneratorConfiguration.Default,
    ) = LoremGenerator(dictionary, seed, configuration)

    // MARK: Determinism

    @Test
    fun sameSeedProducesSameParagraph() {
        assertEquals(
            generator(seed = 42L).generateParagraph(),
            generator(seed = 42L).generateParagraph(),
        )
    }

    @Test
    fun differentSeedsProduceDifferentParagraphs() {
        assertTrue(generator(seed = 1L).generateParagraph() != generator(seed = 2L).generateParagraph())
    }

    @Test
    fun repeatedCallsOnOneInstanceAreIdempotent() {
        val generator = generator()
        assertEquals(generator.generateParagraph(), generator.generateParagraph())
        assertEquals(generator.generateSentence(), generator.generateSentence())
        assertEquals(generator.generateParagraphs(3), generator.generateParagraphs(3))
    }

    @Test
    fun everyStyleIsDeterministic() {
        for (style in ParagraphStyle.presets) {
            assertEquals(
                generator(seed = 7L).generateParagraph(style),
                generator(seed = 7L).generateParagraph(style),
                "style ${style.name} was not deterministic",
            )
        }
    }

    // MARK: Batch

    @Test
    fun batchParagraphsDifferFromOneAnother() {
        val paragraphs = generator(seed = 5L).generateParagraphs(4)
        assertEquals(4, paragraphs.size)
        assertEquals(4, paragraphs.toSet().size, "batch repeated a paragraph: $paragraphs")
    }

    @Test
    fun batchCallsAreReproducible() {
        assertEquals(
            generator(seed = 5L).generateParagraphs(4, ParagraphStyle.Technical),
            generator(seed = 5L).generateParagraphs(4, ParagraphStyle.Technical),
        )
    }

    @Test
    fun aNonPositiveBatchCountReturnsAnEmptyList() {
        assertTrue(generator().generateParagraphs(0).isEmpty())
        assertTrue(generator().generateParagraphs(-3).isEmpty())
    }

    @Test
    fun aSingleParagraphBatchMatchesTheParagraphOperation() {
        assertEquals(
            listOf(generator(seed = 8L).generateParagraph()),
            generator(seed = 8L).generateParagraphs(1),
        )
    }

    // MARK: Bounds

    @Test
    fun sentenceCountsFallInsideTheStyleRange() {
        for (style in ParagraphStyle.presets) {
            for (seed in 1L..40L) {
                val count = sentencesIn(generator(seed).generateParagraph(style)).size
                assertTrue(
                    count in style.sentenceCountRange,
                    "style ${style.name} seed $seed produced $count sentences, " +
                        "outside ${style.sentenceCountRange}",
                )
            }
        }
    }

    @Test
    fun sentenceWordCountsRespectTheConfiguredBounds() {
        val configuration = GeneratorConfiguration(
            minimumWordsPerSentence = 5,
            maximumWordsPerSentence = 12,
        )
        for (style in ParagraphStyle.presets) {
            for (seed in 1L..40L) {
                val paragraph = generator(seed, configuration).generateParagraph(style)
                for (sentence in sentencesIn(paragraph)) {
                    val words = sentence.loremWordCount()
                    assertTrue(
                        words in 5..12,
                        "style ${style.name} seed $seed sentence '$sentence' had $words words",
                    )
                }
            }
        }
    }

    @Test
    fun narrowBoundsStillRespectTheMaximum() {
        val configuration = GeneratorConfiguration(
            minimumWordsPerSentence = 3,
            maximumWordsPerSentence = 4,
        )
        for (seed in 1L..30L) {
            for (sentence in sentencesIn(generator(seed, configuration).generateParagraph())) {
                assertTrue(
                    sentence.loremWordCount() <= 4,
                    "seed $seed sentence '$sentence' exceeded the maximum",
                )
            }
        }
    }

    @Test
    fun aTemplateTooLargeForTheMaximumIsDropped() {
        // The default LIST template needs at least five words, so a maximum of four excludes it.
        val configuration = GeneratorConfiguration(
            transitionProbability = 0.0,
            minimumWordsPerSentence = 3,
            maximumWordsPerSentence = 4,
        )
        for (seed in 1L..30L) {
            for (sentence in sentencesIn(generator(seed, configuration).generateParagraph())) {
                // Only the list template pairs a colon with an `et`-joined final item.
                assertTrue(
                    !(sentence.contains(":") && sentence.contains(" et ")),
                    "list template survived: $sentence",
                )
            }
        }
    }

    @Test
    fun aMaximumTooSmallForEveryTemplateRaisesInvalidTemplate() {
        val configuration = GeneratorConfiguration(
            minimumWordsPerSentence = 1,
            maximumWordsPerSentence = 1,
            customTemplates = listOf(
                SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 1, format = "{w2}, et {w2}."),
            ),
        )
        assertFailsWith<LoremException.InvalidTemplate> {
            generator(1L, configuration).generateParagraph()
        }
    }

    @Test
    fun disallowingShortSentencesRaisesTheFloor() {
        val configuration = GeneratorConfiguration(
            allowShortSentences = false,
            minimumWordsPerSentence = 6,
            maximumWordsPerSentence = 20,
        )
        for (seed in 1L..30L) {
            for (sentence in sentencesIn(generator(seed, configuration).generateParagraph())) {
                assertTrue(
                    sentence.loremWordCount() >= 13,
                    "seed $seed sentence '$sentence' fell below the raised floor",
                )
            }
        }
    }

    // MARK: Templates and styles

    @Test
    fun customTemplatesAreUsedExclusively() {
        val configuration = GeneratorConfiguration(
            transitionProbability = 0.0,
            customTemplates = listOf(
                SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 1, format = "Ecce {w4}!"),
            ),
        )
        for (sentence in sentencesIn(generator(3L, configuration).generateParagraph())) {
            assertTrue(sentence.startsWith("Ecce "), "unexpected sentence: $sentence")
            assertTrue(sentence.endsWith("!"), "unexpected sentence: $sentence")
        }
    }

    @Test
    fun customTransitionsAreUsedExclusively() {
        val configuration = GeneratorConfiguration(
            customTransitions = listOf("Itaque,", "Denique,"),
        )
        val paragraph = generator(4L, configuration).generateParagraph(ParagraphStyle.Academic)
        for (transition in TransitionLibrary.defaultTransitions.toSet()) {
            assertTrue(!paragraph.contains(transition), "default transition '$transition' leaked in")
        }
    }

    @Test
    fun questionsAreAbsentWhenDisallowedByConfiguration() {
        val configuration = GeneratorConfiguration(allowQuestions = false)
        for (seed in 1L..30L) {
            val paragraph = generator(seed, configuration).generateParagraph(ParagraphStyle.Mixed)
            assertTrue(!paragraph.contains("?"), "seed $seed produced a question: $paragraph")
        }
    }

    @Test
    fun questionsAreAbsentWhenTheStyleSuppressesThem() {
        for (seed in 1L..30L) {
            val paragraph = generator(seed).generateParagraph(ParagraphStyle.Legal)
            assertTrue(!paragraph.contains("?"), "seed $seed produced a question: $paragraph")
        }
    }

    @Test
    fun consecutivePatternsDifferWhenAlternativesExist() {
        // The default templates give each pattern a distinct terminal shape, so a repeated
        // shape across neighbouring sentences signals a repeated pattern.
        for (seed in 1L..30L) {
            val sentences = sentencesIn(generator(seed).generateParagraph(ParagraphStyle.Mixed))
            sentences.zipWithNext { first, second ->
                if (first.endsWith("?") && second.endsWith("?")) {
                    throw AssertionError("seed $seed repeated the question pattern")
                }
                if (first.endsWith("!") && second.endsWith("!")) {
                    throw AssertionError("seed $seed repeated the emphasis pattern")
                }
            }
        }
    }

    @Test
    fun exhaustedTemplatesRaiseInvalidTemplate() {
        val configuration = GeneratorConfiguration(
            customTemplates = listOf(
                SentenceTemplate(SentenceTemplate.Pattern.QUESTION, weight = 1, format = "{w6}?"),
            ),
        )
        // The legal style zeroes question frequency, so the only template is dropped.
        assertFailsWith<LoremException.InvalidTemplate> {
            generator(1L, configuration).generateParagraph(ParagraphStyle.Legal)
        }
        assertFailsWith<LoremException.InvalidTemplate> {
            generator(1L, configuration).generateSentence(ParagraphStyle.Legal)
        }
        assertFailsWith<LoremException.InvalidTemplate> {
            generator(1L, configuration).generateParagraphs(2, ParagraphStyle.Legal)
        }
    }

    @Test
    fun disallowingQuestionsAlsoExhaustsAQuestionOnlyTemplateSet() {
        val configuration = GeneratorConfiguration(
            allowQuestions = false,
            customTemplates = listOf(
                SentenceTemplate(SentenceTemplate.Pattern.QUESTION, weight = 1, format = "{w6}?"),
            ),
        )
        assertFailsWith<LoremException.InvalidTemplate> {
            generator(1L, configuration).generateParagraph()
        }
    }

    // MARK: Transitions

    @Test
    fun zeroProbabilityYieldsNoTransitions() {
        val configuration = GeneratorConfiguration(transitionProbability = 0.0)
        for (seed in 1L..40L) {
            val paragraph = generator(seed, configuration).generateParagraph(ParagraphStyle.Academic)
            for (transition in TransitionLibrary.defaultTransitions.toSet()) {
                assertTrue(
                    !paragraph.contains(transition),
                    "seed $seed emitted transition '$transition': $paragraph",
                )
            }
        }
    }

    @Test
    fun transitionsAppearWhenProbabilityIsHigh() {
        // Academic pairs a 0.6 style probability with long paragraphs, so a transition is
        // near-certain across forty seeds.
        val seen = (1L..40L).any { seed ->
            val paragraph = generator(seed).generateParagraph(ParagraphStyle.Academic)
            TransitionLibrary.defaultTransitions.any { paragraph.contains(it) }
        }
        assertTrue(seen, "no transition appeared across forty academic paragraphs")
    }

    @Test
    fun theSameTransitionNeverAppearsOnConsecutiveSentences() {
        val transitions = TransitionLibrary.defaultTransitions.toSet()
        for (seed in 1L..60L) {
            val sentences = sentencesIn(generator(seed).generateParagraph(ParagraphStyle.Academic))
            val leading = sentences.map { sentence -> transitions.firstOrNull { sentence.startsWith(it) } }
            leading.zipWithNext { first, second ->
                if (first != null && first == second) {
                    throw AssertionError("seed $seed repeated transition '$first'")
                }
            }
        }
    }

    @Test
    fun theFirstSentenceNeverCarriesATransition() {
        val transitions = TransitionLibrary.defaultTransitions.toSet()
        for (seed in 1L..40L) {
            val first = sentencesIn(generator(seed).generateParagraph(ParagraphStyle.Academic)).first()
            assertTrue(
                transitions.none { first.startsWith(it) },
                "seed $seed opened with a transition: $first",
            )
        }
    }

    // MARK: Shape

    @Test
    fun generatedTextIsTrimmedAndSingleSpaced() {
        for (seed in 1L..30L) {
            val paragraph = generator(seed).generateParagraph(ParagraphStyle.Mixed)
            assertEquals(paragraph.trim(), paragraph)
            assertTrue(!paragraph.contains("  "), "double space in: $paragraph")
            assertTrue(!paragraph.contains("\n"), "newline in: $paragraph")
        }
    }

    @Test
    fun everySentenceStartsWithAnUppercaseLetter() {
        for (seed in 1L..30L) {
            for (sentence in sentencesIn(generator(seed).generateParagraph(ParagraphStyle.Mixed))) {
                val first = sentence.firstOrNull { it.isLetter() }
                assertTrue(
                    first != null && first.isUpperCase(),
                    "sentence did not start uppercase: $sentence",
                )
            }
        }
    }

    @Test
    fun aGeneratedSentenceIsASingleSentence() {
        for (seed in 1L..30L) {
            val sentence = generator(seed).generateSentence(ParagraphStyle.Mixed)
            assertEquals(1, sentencesIn(sentence).size, "not a single sentence: $sentence")
        }
    }

    companion object {
        /** Splits a paragraph on terminal punctuation, keeping the punctuation attached. */
        fun sentencesIn(paragraph: String): List<String> {
            val sentences = mutableListOf<String>()
            val builder = StringBuilder()
            for (character in paragraph) {
                builder.append(character)
                if (character == '.' || character == '!' || character == '?') {
                    sentences.add(builder.toString().trim())
                    builder.clear()
                }
            }
            val tail = builder.toString().trim()
            if (tail.isNotEmpty()) sentences.add(tail)
            return sentences
        }
    }
}
