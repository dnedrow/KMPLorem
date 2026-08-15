package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Keeps the README's snippets honest by compiling and running them. */
class ReadmeExamplesTest {

    @Test
    fun quickStartCompiles() {
        val generator = LoremGenerator(dictionary = LoremDictionary.BuiltIn, seed = 42L)
        assertTrue(generator.generateSentence().isNotBlank())
        assertTrue(generator.generateParagraph(ParagraphStyle.Technical).isNotBlank())
        assertEquals(3, generator.generateParagraphs(count = 3, style = ParagraphStyle.Academic).size)
    }

    @Test
    fun aCustomDictionaryCompiles() {
        val dictionary = LoremDictionary(listOf("alpha", "beta", "gamma", "delta"))
        assertTrue(LoremGenerator(dictionary, seed = 7L).generateParagraph().isNotBlank())
    }

    @Test
    fun theIdempotenceExampleHolds() {
        val generator = LoremGenerator(seed = 42L)
        assertEquals(generator.generateParagraph(), generator.generateParagraph())
        assertEquals(3, generator.generateParagraphs(count = 3).toSet().size)
    }

    @Test
    fun theConfigurationExampleCompiles() {
        val configuration = GeneratorConfiguration(
            transitionProbability = 0.0,
            allowQuestions = false,
            allowShortSentences = false,
            avoidImmediatePatternRepeats = true,
            minimumWordsPerSentence = 8,
            maximumWordsPerSentence = 16,
            customTransitions = listOf("Itaque,", "Denique,"),
            customTemplates = listOf(
                SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 1, format = "Ecce {w4}."),
            ),
        )
        assertTrue(LoremGenerator(seed = 42L, configuration = configuration).generateParagraph().isNotBlank())
    }

    @Test
    fun theCanonicalOpeningExampleMatchesTheReadme() {
        val generator = LoremGenerator(
            seed = 42L,
            configuration = GeneratorConfiguration(startsWithCanonicalOpening = true),
        )
        assertTrue(
            generator.generateParagraph().startsWith(
                "Lorem ipsum dolor sit amet adipisci quisquam asperiores aut proident asperiores doloribus.",
            ),
        )
    }

    @Test
    fun theFailureHandlingExampleCompiles() {
        val generator = LoremGenerator(
            seed = 1L,
            configuration = GeneratorConfiguration(
                customTemplates = listOf(
                    SentenceTemplate(SentenceTemplate.Pattern.QUESTION, weight = 1, format = "{w6}?"),
                ),
            ),
        )
        val label = try {
            generator.generateParagraph(ParagraphStyle.Legal)
            "none"
        } catch (exception: LoremException) {
            when (exception) {
                is LoremException.EmptyDictionary -> "empty"
                is LoremException.InvalidTemplate -> "template"
                is LoremException.InvalidWeight -> "weight"
            }
        }
        assertEquals("template", label)
    }
}
