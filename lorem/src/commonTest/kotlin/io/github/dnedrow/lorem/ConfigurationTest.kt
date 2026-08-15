package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeneratorConfigurationTest {

    @Test
    fun defaultConfigurationIsConstructible() {
        val config = GeneratorConfiguration.Default
        assertEquals(1.0, config.transitionProbability)
        assertTrue(config.allowQuestions)
        assertTrue(config.allowShortSentences)
        assertTrue(config.avoidImmediatePatternRepeats)
        assertEquals(false, config.startsWithCanonicalOpening)
        assertEquals(6, config.minimumWordsPerSentence)
        assertEquals(20, config.maximumWordsPerSentence)
        assertTrue(config.customTransitions.isEmpty())
        assertTrue(config.customTemplates.isEmpty())
    }

    @Test
    fun nonPositiveWordBoundsAreRejected() {
        assertFailsWith<LoremException.InvalidWeight> {
            GeneratorConfiguration(minimumWordsPerSentence = 0, maximumWordsPerSentence = 20)
        }
        assertFailsWith<LoremException.InvalidWeight> {
            GeneratorConfiguration(minimumWordsPerSentence = -3, maximumWordsPerSentence = 20)
        }
        assertFailsWith<LoremException.InvalidWeight> {
            GeneratorConfiguration(minimumWordsPerSentence = 6, maximumWordsPerSentence = 0)
        }
        assertFailsWith<LoremException.InvalidWeight> {
            GeneratorConfiguration(minimumWordsPerSentence = 6, maximumWordsPerSentence = -1)
        }
    }

    @Test
    fun aMinimumAboveTheMaximumIsRejected() {
        assertFailsWith<LoremException.InvalidWeight> {
            GeneratorConfiguration(minimumWordsPerSentence = 12, maximumWordsPerSentence = 11)
        }
    }

    @Test
    fun equalBoundsAreAccepted() {
        val config = GeneratorConfiguration(
            minimumWordsPerSentence = 8,
            maximumWordsPerSentence = 8,
        )
        assertEquals(8, config.minimumWordsPerSentence)
        assertEquals(8, config.maximumWordsPerSentence)
    }

    @Test
    fun transitionProbabilityClampsAtBothEnds() {
        assertEquals(1.0, GeneratorConfiguration(transitionProbability = 4.5).transitionProbability)
        assertEquals(0.0, GeneratorConfiguration(transitionProbability = -2.0).transitionProbability)
        assertEquals(0.25, GeneratorConfiguration(transitionProbability = 0.25).transitionProbability)
    }

    @Test
    fun copyStillValidates() {
        assertFailsWith<LoremException.InvalidWeight> {
            GeneratorConfiguration.Default.copy(minimumWordsPerSentence = 0)
        }
        assertFailsWith<LoremException.InvalidWeight> {
            GeneratorConfiguration.Default.copy(minimumWordsPerSentence = 25)
        }
    }

    @Test
    fun copyStillClamps() {
        assertEquals(
            1.0,
            GeneratorConfiguration.Default.copy(transitionProbability = 9.0).transitionProbability,
        )
        assertEquals(
            0.0,
            GeneratorConfiguration.Default.copy(transitionProbability = -9.0).transitionProbability,
        )
    }

    @Test
    fun copyCarriesUnchangedProperties() {
        val original = GeneratorConfiguration(
            transitionProbability = 0.5,
            allowQuestions = false,
            minimumWordsPerSentence = 4,
            maximumWordsPerSentence = 9,
        )
        val copy = original.copy(allowShortSentences = false)
        assertEquals(0.5, copy.transitionProbability)
        assertEquals(false, copy.allowQuestions)
        assertEquals(4, copy.minimumWordsPerSentence)
        assertEquals(9, copy.maximumWordsPerSentence)
        assertEquals(false, copy.allowShortSentences)
        assertEquals(original, original.copy())
    }

    @Test
    fun effectiveMinimumWordsRisesWhenShortSentencesAreDisallowed() {
        val short = GeneratorConfiguration(
            allowShortSentences = true,
            minimumWordsPerSentence = 6,
            maximumWordsPerSentence = 20,
        )
        assertEquals(6, short.effectiveMinimumWords)

        val long = short.copy(allowShortSentences = false)
        assertEquals(13, long.effectiveMinimumWords)
        assertTrue(long.effectiveMinimumWords <= long.maximumWordsPerSentence)
    }

    @Test
    fun effectiveMinimumWordsNeverExceedsTheMaximum() {
        for (minimum in 1..20) {
            for (maximum in minimum..20) {
                val config = GeneratorConfiguration(
                    allowShortSentences = false,
                    minimumWordsPerSentence = minimum,
                    maximumWordsPerSentence = maximum,
                )
                assertTrue(
                    config.effectiveMinimumWords in minimum..maximum,
                    "min=$minimum max=$maximum gave ${config.effectiveMinimumWords}",
                )
            }
        }
    }

    @Test
    fun equalityComparesEveryProperty() {
        assertEquals(GeneratorConfiguration(), GeneratorConfiguration())
        assertEquals(GeneratorConfiguration().hashCode(), GeneratorConfiguration().hashCode())
        assertTrue(GeneratorConfiguration() != GeneratorConfiguration(allowQuestions = false))
    }
}

class ParagraphStyleTest {

    @Test
    fun allPresetsResolve() {
        assertEquals(5, ParagraphStyle.presets.size)
        assertEquals(
            listOf("classic", "technical", "academic", "legal", "mixed"),
            ParagraphStyle.presets.map { it.name },
        )
        assertEquals(
            listOf(
                ParagraphStyle.Classic,
                ParagraphStyle.Technical,
                ParagraphStyle.Academic,
                ParagraphStyle.Legal,
                ParagraphStyle.Mixed,
            ),
            ParagraphStyle.presets,
        )
    }

    @Test
    fun presetSentenceCountRangesAreTheExpectedValues() {
        assertEquals(3..6, ParagraphStyle.Classic.sentenceCountRange)
        assertEquals(4..7, ParagraphStyle.Technical.sentenceCountRange)
        assertEquals(4..8, ParagraphStyle.Academic.sentenceCountRange)
        assertEquals(3..5, ParagraphStyle.Legal.sentenceCountRange)
        assertEquals(2..7, ParagraphStyle.Mixed.sentenceCountRange)
    }

    @Test
    fun presetTuningValuesAreTheExpectedValues() {
        assertEquals(0.3, ParagraphStyle.Classic.transitionProbability)
        assertEquals(1.0, ParagraphStyle.Classic.complexityWeighting)
        assertEquals(1.0, ParagraphStyle.Classic.questionFrequency)
        assertTrue(ParagraphStyle.Classic.patternWeightMultipliers.isEmpty())

        assertEquals(0.4, ParagraphStyle.Technical.transitionProbability)
        assertEquals(1.4, ParagraphStyle.Technical.complexityWeighting)
        assertEquals(0.4, ParagraphStyle.Technical.questionFrequency)
        assertEquals(2.0, ParagraphStyle.Technical.patternWeightMultipliers[SentenceTemplate.Pattern.LIST])
        assertEquals(0.5, ParagraphStyle.Technical.patternWeightMultipliers[SentenceTemplate.Pattern.EMPHASIS])

        assertEquals(0.6, ParagraphStyle.Academic.transitionProbability)
        assertEquals(1.8, ParagraphStyle.Academic.complexityWeighting)
        assertEquals(0.3, ParagraphStyle.Academic.questionFrequency)

        assertEquals(0.2, ParagraphStyle.Legal.transitionProbability)
        assertEquals(2.2, ParagraphStyle.Legal.complexityWeighting)
        assertEquals(0.0, ParagraphStyle.Legal.questionFrequency)

        assertEquals(0.35, ParagraphStyle.Mixed.transitionProbability)
        assertEquals(1.0, ParagraphStyle.Mixed.complexityWeighting)
        assertEquals(1.6, ParagraphStyle.Mixed.questionFrequency)
    }

    @Test
    fun styleTuningValuesAreClamped() {
        val style = ParagraphStyle(
            name = "extreme",
            sentenceCountRange = 1..2,
            transitionProbability = 3.0,
            complexityWeighting = -1.0,
            questionFrequency = -0.5,
        )
        assertEquals(1.0, style.transitionProbability)
        assertEquals(0.0, style.complexityWeighting)
        assertEquals(0.0, style.questionFrequency)

        val floor = ParagraphStyle("floor", 1..2, -4.0, 0.0, 0.0)
        assertEquals(0.0, floor.transitionProbability)
    }
}

class PhraseGeneratorTest {

    @Test
    fun blankTransitionsAreFilteredAtConstruction() {
        val generator = PhraseGenerator(listOf("Autem,", "", "   ", "\t", "Tamen,"))
        assertEquals(listOf("Autem,", "Tamen,"), generator.transitions)
    }

    @Test
    fun anEmptyGeneratorReturnsNull() {
        assertNull(PhraseGenerator(emptyList()).nextTransition(null, SeededRandom(1L)))
        assertNull(PhraseGenerator(listOf("", "  ")).nextTransition(null, SeededRandom(1L)))
    }

    @Test
    fun theOnlyTransitionIsReturnedEvenWhenItIsThePrevious() {
        val generator = PhraseGenerator(listOf("Autem,"))
        assertEquals("Autem,", generator.nextTransition("Autem,", SeededRandom(1L)))
    }

    @Test
    fun thePreviousTransitionIsAvoidedWhenAlternativesExist() {
        val generator = PhraseGenerator(TransitionLibrary.defaultTransitions)
        val random = SeededRandom(7L)
        var previous = generator.nextTransition(null, random)
        assertNotNull(previous)
        repeat(200) {
            val next = generator.nextTransition(previous, random)
            assertNotNull(next)
            assertTrue(next != previous, "transition repeated immediately: $next")
            previous = next
        }
    }

    @Test
    fun selectionIsDeterministicForAGivenSeed() {
        val generator = PhraseGenerator(TransitionLibrary.defaultTransitions)
        val first = List(50) { generator.nextTransition(null, SeededRandom(9L)) }
        val second = List(50) { generator.nextTransition(null, SeededRandom(9L)) }
        assertEquals(first, second)
    }

    @Test
    fun selectionCoversEveryTransition() {
        val generator = PhraseGenerator(TransitionLibrary.defaultTransitions)
        val random = SeededRandom(11L)
        val seen = mutableSetOf<String>()
        repeat(500) { seen.add(generator.nextTransition(null, random)!!) }
        assertEquals(TransitionLibrary.defaultTransitions.toSet(), seen)
    }

    @Test
    fun wordCountMeasuresWhitespaceSeparatedWords() {
        assertEquals(1, PhraseGenerator.wordCount("Autem,"))
        assertEquals(2, PhraseGenerator.wordCount("Ut consequens,"))
        assertEquals(0, PhraseGenerator.wordCount("   "))
        assertEquals(3, PhraseGenerator.wordCount("  one   two \n three  "))
    }

    @Test
    fun applyPrependsATransitionAndLowercasesTheClause() {
        assertEquals(
            "Autem, dolor sit amet.",
            PhraseGenerator.apply("Autem,", "Dolor sit amet."),
        )
    }

    @Test
    fun applyLeavesAClauseWithoutLettersAlone() {
        assertEquals("Autem, 123.", PhraseGenerator.apply("Autem,", "123."))
    }
}
