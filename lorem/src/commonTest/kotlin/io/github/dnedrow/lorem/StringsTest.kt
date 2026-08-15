package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {

    @Test
    fun splitsOnEveryKindOfWhitespace() {
        assertEquals(listOf("a", "b", "c"), "a\tb\nc".splitOnWhitespace())
        assertEquals(listOf("a", "b"), "a    b".splitOnWhitespace())
        assertEquals(listOf("a", "b"), "  a \r\n b  ".splitOnWhitespace())
    }

    @Test
    fun splittingDiscardsEmptyResults() {
        assertEquals(emptyList(), "".splitOnWhitespace())
        assertEquals(emptyList(), "   \t\n ".splitOnWhitespace())
    }

    @Test
    fun countsWords() {
        assertEquals(0, "".loremWordCount())
        assertEquals(1, "  lorem  ".loremWordCount())
        assertEquals(3, "lorem\tipsum\ndolor".loremWordCount())
    }

    @Test
    fun normalizationTrimsAndCollapses() {
        assertEquals("lorem ipsum dolor", "  lorem   ipsum \n dolor  ".normalizeWhitespace())
        assertEquals("", "   ".normalizeWhitespace())
    }

    @Test
    fun sentenceCasingTouchesOnlyTheFirstLetter() {
        assertEquals("Lorem ipsum", "lorem ipsum".sentenceCased())
        assertEquals("Lorem IPSUM", "lorem IPSUM".sentenceCased())
    }

    @Test
    fun sentenceCasingSkipsLeadingNonLetters() {
        assertEquals("\"Lorem ipsum\"", "\"lorem ipsum\"".sentenceCased())
        assertEquals("  Lorem", "  lorem".sentenceCased())
    }

    @Test
    fun sentenceCasingLeavesLetterlessStringsAlone() {
        assertEquals("123 -- !", "123 -- !".sentenceCased())
        assertEquals("", "".sentenceCased())
    }

    @Test
    fun sentenceUncasingTouchesOnlyTheFirstLetter() {
        assertEquals("lorem Ipsum", "Lorem Ipsum".sentenceUncased())
        assertEquals("\"lorem\"", "\"Lorem\"".sentenceUncased())
        assertEquals("", "".sentenceUncased())
    }

    @Test
    fun casingRoundTripsForSimpleSentences() {
        val original = "Lorem ipsum dolor sit amet."
        assertEquals(original, original.sentenceUncased().sentenceCased())
    }
}
