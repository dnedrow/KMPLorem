package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplateRendererTest {

    private val dictionary = LoremDictionary.BuiltIn

    // MARK: parsing

    @Test
    fun parsesTokenCounts() {
        assertContentEquals(listOf(6), TemplateRenderer("{w6}.").tokenCounts)
        assertContentEquals(listOf(5, 5), TemplateRenderer("{w5}, et {w5}.").tokenCounts)
        assertContentEquals(listOf(3, 2, 2, 2), TemplateRenderer("{w3}: {w2}, {w2}, et {w2}.").tokenCounts)
    }

    @Test
    fun computesFixedWordCountFromLiteralsOnly() {
        // Punctuation attached to a neighbouring word is not counted as a word.
        assertEquals(0, TemplateRenderer("{w6}.").fixedWordCount)
        assertEquals(1, TemplateRenderer("{w5}, et {w5}.").fixedWordCount)
        assertEquals(1, TemplateRenderer("Quamvis {w4}, {w6}.").fixedWordCount)
        assertEquals(1, TemplateRenderer("{w3}: {w2}, {w2}, et {w2}.").fixedWordCount)
    }

    @Test
    fun computesMinimumWordCount() {
        assertEquals(1, TemplateRenderer("{w6}.").minimumWordCount)
        assertEquals(3, TemplateRenderer("{w5}, et {w5}.").minimumWordCount)
        assertEquals(3, TemplateRenderer("Quamvis {w4}, {w6}.").minimumWordCount)
    }

    @Test
    fun detectsWhetherTheTemplateOpensWithAToken() {
        assertTrue(TemplateRenderer("{w6}.").beginsWithToken)
        assertFalse(TemplateRenderer("Quamvis {w4}, {w6}.").beginsWithToken)
    }

    @Test
    fun rejectsMalformedTokens() {
        for (format in listOf("{w}", "{w0}", "{wx}", "{x4}", "{", "{w4", "}", "a } b", "{w4 }")) {
            assertFailsWith<LoremException.InvalidTemplate>("expected $format to be rejected") {
                TemplateRenderer(format)
            }
        }
    }

    @Test
    fun acceptsLiteralOnlyTemplates() {
        val renderer = TemplateRenderer("Lorem ipsum.")
        assertTrue(renderer.tokenCounts.isEmpty())
        assertEquals(2, renderer.fixedWordCount)
    }

    // MARK: rendering

    @Test
    fun wordTokenExpandsToTheRequestedCount() {
        val rendered = TemplateRenderer("{w8}.").render(dictionary, SeededRandom(1L))
        assertEquals(8, rendered.loremWordCount())
        assertTrue(rendered.endsWith("."))
    }

    @Test
    fun firstLetterIsCapitalized() {
        repeat(25) { seed ->
            val rendered = TemplateRenderer("{w6}.").render(dictionary, SeededRandom(seed.toLong()))
            val firstLetter = rendered.first { it.isLetter() }
            assertTrue(firstLetter.isUpperCase(), "expected uppercase, got '$firstLetter' in $rendered")
        }
    }

    @Test
    fun outputIsTrimmedAndSingleSpaced() {
        val rendered = TemplateRenderer("  {w3}   {w3}  ").render(dictionary, SeededRandom(3L))
        assertEquals(rendered.trim(), rendered)
        assertFalse(rendered.contains("  "))
    }

    @Test
    fun literalsAndPunctuationKeepTheirPositions() {
        val rendered = TemplateRenderer("Quamvis {w4}, {w6}.").render(dictionary, SeededRandom(9L))
        assertTrue(rendered.startsWith("Quamvis "))
        assertTrue(rendered.contains(","))
        assertTrue(rendered.endsWith("."))
        assertEquals(11, rendered.loremWordCount())
    }

    @Test
    fun renderingIsDeterministicForAGivenSeed() {
        val renderer = TemplateRenderer("{w5}, et {w5}.")
        assertEquals(
            renderer.render(dictionary, SeededRandom(42L)),
            renderer.render(dictionary, SeededRandom(42L)),
        )
    }

    @Test
    fun overrideTokenCountsChangeTheWordTotal() {
        val renderer = TemplateRenderer("{w5}, et {w5}.")
        val rendered = renderer.render(dictionary, SeededRandom(4L), tokenCounts = listOf(2, 3))
        assertEquals(6, rendered.loremWordCount())
    }

    @Test
    fun overrideTokenCountsOfTheWrongSizeAreIgnored() {
        val renderer = TemplateRenderer("{w5}, et {w5}.")
        val rendered = renderer.render(dictionary, SeededRandom(4L), tokenCounts = listOf(2))
        assertEquals(11, rendered.loremWordCount())
    }

    @Test
    fun leadingWordsFillTheFirstSlots() {
        val renderer = TemplateRenderer("{w6}.")
        val rendered = renderer.render(
            dictionary,
            SeededRandom(4L),
            leadingWords = listOf("Lorem", "ipsum", "dolor"),
        )
        assertTrue(rendered.startsWith("Lorem ipsum dolor "))
        assertEquals(6, rendered.loremWordCount())
    }

    @Test
    fun leadingWordsSpillAcrossTokenBoundaries() {
        val renderer = TemplateRenderer("{w2}, et {w4}.")
        val rendered = renderer.render(
            dictionary,
            SeededRandom(4L),
            leadingWords = listOf("alpha", "beta", "gamma"),
        )
        assertTrue(rendered.startsWith("Alpha beta, et gamma "))
    }

    @Test
    fun leadingWordsBeyondTheBudgetAreDropped() {
        val renderer = TemplateRenderer("{w2}.")
        val rendered = renderer.render(
            dictionary,
            SeededRandom(4L),
            leadingWords = listOf("alpha", "beta", "gamma", "delta"),
        )
        assertEquals("Alpha beta.", rendered)
    }

    @Test
    fun consecutiveWordsAreNotRepeated() {
        val renderer = TemplateRenderer("{w40}.")
        repeat(20) { seed ->
            val words = renderer.render(dictionary, SeededRandom(seed.toLong())).removeSuffix(".").splitOnWhitespace()
            for (index in 1 until words.size) {
                assertFalse(
                    words[index].equals(words[index - 1], ignoreCase = true),
                    "repeated '${words[index]}' at $index for seed $seed",
                )
            }
        }
    }

    @Test
    fun wordStateCarriesAcrossRenders() {
        val renderer = TemplateRenderer("{w1}.")
        val state = WordState()
        val random = SeededRandom(12L)
        val first = renderer.render(dictionary, random, null, emptyList(), state)
        val second = renderer.render(dictionary, random, null, emptyList(), state)
        assertFalse(first.equals(second, ignoreCase = true))
    }
}
