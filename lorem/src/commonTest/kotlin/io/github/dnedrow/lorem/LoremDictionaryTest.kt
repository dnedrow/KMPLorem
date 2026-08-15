package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoremDictionaryTest {

    @Test
    fun builtInDictionaryIsNonEmptyAndUnique() {
        val dictionary = LoremDictionary.BuiltIn
        assertTrue(dictionary.words.isNotEmpty())
        assertEquals(dictionary.words.size, dictionary.words.toSet().size)
    }

    @Test
    fun builtInDictionaryHasTheExpectedSize() {
        assertEquals(210, LoremDictionary.BuiltIn.words.size)
    }

    @Test
    fun builtInDictionaryIsLowercaseLatinLetters() {
        assertTrue(LoremDictionary.BuiltIn.words.all { word -> word.all { it in 'a'..'z' } })
    }

    @Test
    fun duplicatesAreRemoved() {
        val dictionary = LoremDictionary(listOf("lorem", "ipsum", "lorem", "dolor", "ipsum"))
        assertEquals(listOf("lorem", "ipsum", "dolor"), dictionary.words)
    }

    @Test
    fun firstSeenOrderIsPreserved() {
        val dictionary = LoremDictionary(listOf("gamma", "alpha", "gamma", "beta"))
        assertEquals(listOf("gamma", "alpha", "beta"), dictionary.words)
    }

    @Test
    fun entriesAreTrimmedAndBlanksDropped() {
        val dictionary = LoremDictionary(listOf("  lorem  ", "", "   ", "\tipsum\n"))
        assertEquals(listOf("lorem", "ipsum"), dictionary.words)
    }

    @Test
    fun trimmingCanRevealDuplicates() {
        val dictionary = LoremDictionary(listOf("lorem", " lorem "))
        assertEquals(listOf("lorem"), dictionary.words)
    }

    @Test
    fun emptyWordListIsRejected() {
        assertFailsWith<LoremException.EmptyDictionary> { LoremDictionary(emptyList()) }
    }

    @Test
    fun whitespaceOnlyWordListIsRejected() {
        assertFailsWith<LoremException.EmptyDictionary> { LoremDictionary(listOf("  ", "\t", "\n")) }
    }

    @Test
    fun wordLookupWrapsAround() {
        val dictionary = LoremDictionary(listOf("a", "b", "c"))
        assertEquals("a", dictionary.wordAt(0))
        assertEquals("c", dictionary.wordAt(2))
        assertEquals("a", dictionary.wordAt(3))
        assertEquals("b", dictionary.wordAt(7))
    }

    @Test
    fun dictionariesCompareByWords() {
        assertEquals(LoremDictionary(listOf("a", "b")), LoremDictionary(listOf("a", "b")))
        assertEquals(
            LoremDictionary(listOf("a", "b")).hashCode(),
            LoremDictionary(listOf("a", "b")).hashCode(),
        )
    }
}
