package io.github.dnedrow.lorem

import io.github.dnedrow.lorem.LoremGeneratorTest.Companion.sentencesIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalOpeningTest {

    private val dictionary = LoremDictionary.BuiltIn
    private val opening = LoremGenerator.CANONICAL_OPENING

    private fun opened(
        seed: Long = 42L,
        minimum: Int = 6,
        maximum: Int = 20,
        style: ParagraphStyle = ParagraphStyle.Classic,
    ) = LoremGenerator(
        dictionary,
        seed,
        GeneratorConfiguration(
            startsWithCanonicalOpening = true,
            minimumWordsPerSentence = minimum,
            maximumWordsPerSentence = maximum,
        ),
    )

    @Test
    fun theConstantsAreTheCanonicalPhrase() {
        assertEquals("Lorem ipsum dolor sit amet", LoremGenerator.CANONICAL_OPENING)
        assertEquals(
            listOf("Lorem", "ipsum", "dolor", "sit", "amet"),
            LoremGenerator.canonicalOpeningWords,
        )
    }

    @Test
    fun theOptionIsDisabledByDefault() {
        assertEquals(false, GeneratorConfiguration.Default.startsWithCanonicalOpening)
        for (seed in 1L..30L) {
            val paragraph = LoremGenerator(dictionary, seed).generateParagraph()
            assertTrue(!paragraph.startsWith(opening), "seed $seed opened canonically: $paragraph")
        }
    }

    @Test
    fun theFirstSentenceBeginsWithTheCanonicalPhrase() {
        for (style in ParagraphStyle.presets) {
            for (seed in 1L..30L) {
                val paragraph = opened(seed).generateParagraph(style)
                assertTrue(
                    paragraph.startsWith(opening),
                    "style ${style.name} seed $seed produced: $paragraph",
                )
            }
        }
    }

    @Test
    fun theSingleSentenceOperationOpensWithThePhrase() {
        for (seed in 1L..30L) {
            assertTrue(opened(seed).generateSentence().startsWith(opening))
        }
    }

    @Test
    fun thePhraseIsAPrefixNotTheWholeSentence() {
        for (seed in 1L..30L) {
            val sentence = opened(seed).generateSentence()
            assertTrue(sentence.length > opening.length, "phrase was the whole sentence: $sentence")
            assertTrue(
                sentence.loremWordCount() > LoremGenerator.canonicalOpeningWords.size,
                "no words followed the phrase: $sentence",
            )
            val terminator = sentence.last()
            assertTrue(
                terminator == '.' || terminator == '!' || terminator == '?',
                "sentence lost its punctuation: $sentence",
            )
        }
    }

    @Test
    fun laterSentencesDoNotRepeatThePhrase() {
        for (seed in 1L..30L) {
            val sentences = sentencesIn(opened(seed).generateParagraph(ParagraphStyle.Academic))
            for (sentence in sentences.drop(1)) {
                assertTrue(
                    !sentence.startsWith(opening),
                    "seed $seed repeated the phrase: $sentence",
                )
            }
        }
    }

    @Test
    fun thePhraseNeverAppearsTwiceInOneCall() {
        for (seed in 1L..30L) {
            val paragraph = opened(seed).generateParagraph(ParagraphStyle.Academic)
            assertEquals(
                1,
                occurrencesOf(opening, paragraph),
                "seed $seed emitted the phrase more than once: $paragraph",
            )
        }
    }

    @Test
    fun onlyTheFirstParagraphOfABatchOpensWithThePhrase() {
        for (seed in 1L..20L) {
            val paragraphs = opened(seed).generateParagraphs(4)
            assertTrue(paragraphs.first().startsWith(opening), "seed $seed: ${paragraphs.first()}")
            for (paragraph in paragraphs.drop(1)) {
                assertTrue(
                    !paragraph.contains(opening),
                    "seed $seed repeated the phrase in a later paragraph: $paragraph",
                )
            }
        }
    }

    @Test
    fun enabledGenerationStaysDeterministic() {
        for (style in ParagraphStyle.presets) {
            assertEquals(
                opened(9L).generateParagraph(style),
                opened(9L).generateParagraph(style),
            )
        }
        assertEquals(opened(9L).generateParagraphs(3), opened(9L).generateParagraphs(3))
    }

    @Test
    fun enablingTheOptionShiftsTheWholeText() {
        val plain = LoremGenerator(dictionary, 42L).generateParagraph()
        val withOpening = opened(42L).generateParagraph()
        assertTrue(plain != withOpening)
    }

    @Test
    fun theOpeningCountsTowardWordBounds() {
        for (seed in 1L..30L) {
            for (sentence in sentencesIn(opened(seed, minimum = 6, maximum = 12).generateParagraph())) {
                val words = sentence.loremWordCount()
                assertTrue(words in 6..12, "seed $seed sentence '$sentence' had $words words")
            }
        }
    }

    @Test
    fun thePhraseIsTruncatedWhenTheBoundsCannotHoldIt() {
        // The phrase stays contiguous, so it is truncated to what the selected template's first
        // token can hold — never more than the configured maximum, and never fewer than one word.
        for (seed in 1L..30L) {
            val sentence = opened(seed, minimum = 3, maximum = 3).generateSentence()
            val kept = leadingPhraseWords(sentence)
            assertTrue(kept in 1..3, "seed $seed kept $kept phrase words: $sentence")
            assertTrue(kept < LoremGenerator.canonicalOpeningWords.size, "seed $seed: $sentence")
            assertTrue(
                sentence.loremWordCount() <= 3,
                "seed $seed exceeded the maximum: $sentence",
            )
        }
    }

    @Test
    fun truncationHoldsAtEveryBoundaryBelowThePhraseLength() {
        for (maximum in 1..5) {
            for (seed in 1L..15L) {
                val sentence = opened(seed, minimum = 1, maximum = maximum).generateSentence()
                val kept = leadingPhraseWords(sentence)
                assertTrue(
                    kept in 1..maximum,
                    "maximum $maximum seed $seed kept $kept phrase words: $sentence",
                )
                assertTrue(
                    sentence.loremWordCount() <= maximum,
                    "maximum $maximum seed $seed exceeded the bound: $sentence",
                )
            }
        }
    }

    @Test
    fun theFullPhraseSurvivesWhenASingleTokenTemplateCanHoldIt() {
        // One token and no literal words means the first token owns the whole budget, so five
        // words is exactly enough for the phrase.
        val configuration = GeneratorConfiguration(
            startsWithCanonicalOpening = true,
            minimumWordsPerSentence = 5,
            maximumWordsPerSentence = 5,
            customTemplates = listOf(
                SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 1, format = "{w6}."),
            ),
        )
        for (seed in 1L..15L) {
            val sentence = LoremGenerator(dictionary, seed, configuration).generateSentence()
            assertEquals("$opening.", sentence, "seed $seed")
        }
    }

    @Test
    fun theFullPhraseSurvivesOnceTheDefaultTemplatesHaveRoom() {
        // Nine words is the widest any default template needs to keep the phrase contiguous.
        for (style in ParagraphStyle.presets) {
            for (seed in 1L..20L) {
                val sentence = opened(seed, minimum = 9, maximum = 9, style = style).generateSentence(style)
                assertTrue(
                    sentence.startsWith(opening),
                    "style ${style.name} seed $seed truncated with room to spare: $sentence",
                )
            }
        }
    }

    @Test
    fun noTransitionPrecedesTheOpening() {
        // Academic inserts transitions most often, so it is the strongest check.
        for (seed in 1L..40L) {
            val paragraph = opened(seed).generateParagraph(ParagraphStyle.Academic)
            assertTrue(paragraph.startsWith(opening), "seed $seed: $paragraph")
            for (transition in TransitionLibrary.defaultTransitions.toSet()) {
                assertTrue(
                    !paragraph.startsWith(transition),
                    "seed $seed opened with transition '$transition'",
                )
            }
        }
    }

    @Test
    fun theOpeningSurvivesACustomTemplateThatBeginsWithALiteral() {
        // No candidate begins with a token, so the prepend fallback runs.
        val generator = LoremGenerator(
            dictionary,
            seed = 12L,
            configuration = GeneratorConfiguration(
                startsWithCanonicalOpening = true,
                minimumWordsPerSentence = 6,
                maximumWordsPerSentence = 20,
                customTemplates = listOf(
                    SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 1, format = "Ecce {w4}."),
                ),
            ),
        )
        val sentence = generator.generateSentence()
        assertTrue(sentence.startsWith(opening), "prepend fallback failed: $sentence")
        assertTrue(sentence.contains("ecce"), "template literal was lost: $sentence")
        assertTrue(sentence.loremWordCount() <= 20, "bound exceeded: $sentence")
    }

    /** The number of leading canonical-phrase words the sentence opens with. */
    private fun leadingPhraseWords(sentence: String): Int {
        val words = LoremGenerator.canonicalOpeningWords
        var kept = 0
        for (count in words.indices) {
            if (sentence.startsWith(words.take(count + 1).joinToString(" "))) kept = count + 1
        }
        return kept
    }

    private fun occurrencesOf(needle: String, haystack: String): Int {
        var count = 0
        var index = haystack.indexOf(needle)
        while (index >= 0) {
            count++
            index = haystack.indexOf(needle, index + needle.length)
        }
        return count
    }
}
