package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultLibrariesTest {

    private val dictionary = LoremDictionary.BuiltIn

    @Test
    fun everyPatternHasADefaultTemplate() {
        val covered = TemplateLibrary.defaultTemplates.map { it.pattern }.toSet()
        assertEquals(SentenceTemplate.Pattern.entries.toSet(), covered)
    }

    @Test
    fun defaultTemplateWeightsArePositive() {
        assertTrue(TemplateLibrary.defaultTemplates.all { it.weight > 0 })
    }

    @Test
    fun compoundTemplateJoinsClausesWithLatinEt() {
        val compound = TemplateLibrary.defaultTemplates.single {
            it.pattern == SentenceTemplate.Pattern.COMPOUND
        }
        assertTrue(compound.format.contains(" et "), "compound format was ${compound.format}")
        assertFalse(compound.format.contains(" and "))

        val rendered = TemplateRenderer(compound.format).render(dictionary, SeededRandom(3L))
        assertTrue(rendered.contains(" et "), "rendered compound was $rendered")
        assertFalse(rendered.contains(" and "))
    }

    @Test
    fun listTemplateJoinsItemsWithLatinEt() {
        val list = TemplateLibrary.defaultTemplates.single {
            it.pattern == SentenceTemplate.Pattern.LIST
        }
        assertTrue(list.format.contains(" et "), "list format was ${list.format}")
        assertFalse(list.format.contains(" and "))
    }

    @Test
    fun complexTemplateOpensWithLatinSubordinator() {
        val complex = TemplateLibrary.defaultTemplates.single {
            it.pattern == SentenceTemplate.Pattern.COMPLEX
        }
        assertTrue(complex.format.startsWith("Quamvis "), "complex format was ${complex.format}")
        assertFalse(complex.format.contains("Although"))

        val rendered = TemplateRenderer(complex.format).render(dictionary, SeededRandom(3L))
        assertTrue(rendered.startsWith("Quamvis "), "rendered complex was $rendered")
    }

    @Test
    fun noDefaultTemplateContainsEnglishConnectors() {
        val english = listOf(" and ", " or ", " but ", "Although", "However", "Therefore")
        for (template in TemplateLibrary.defaultTemplates) {
            for (word in english) {
                assertFalse(
                    template.format.contains(word),
                    "template '${template.format}' contains English connector '$word'",
                )
            }
        }
    }

    @Test
    fun defaultTransitionsAreLatinConnectors() {
        val expected = listOf(
            "Autem,", "Tamen,", "Praeterea,", "Ergo,",
            "Insuper,", "Consequenter,", "Nihilominus,", "Ut consequens,",
        )
        assertEquals(expected, TransitionLibrary.defaultTransitions)
    }

    @Test
    fun defaultTransitionsAreDistinct() {
        assertEquals(
            TransitionLibrary.defaultTransitions.size,
            TransitionLibrary.defaultTransitions.toSet().size,
        )
    }

    @Test
    fun defaultTransitionsContainNoEnglishConnectors() {
        val english = listOf("However", "Moreover", "Therefore", "Furthermore", "Consequently", "Yet")
        for (transition in TransitionLibrary.defaultTransitions) {
            for (word in english) {
                assertFalse(
                    transition.contains(word, ignoreCase = true),
                    "transition '$transition' contains English connector '$word'",
                )
            }
        }
    }

    @Test
    fun defaultTransitionsArePunctuated() {
        assertTrue(TransitionLibrary.defaultTransitions.all { it.endsWith(",") })
    }

    @Test
    fun everyDefaultTemplateRendersSuccessfully() {
        for (template in TemplateLibrary.defaultTemplates) {
            val rendered = TemplateRenderer(template.format).render(dictionary, SeededRandom(1L))
            assertTrue(rendered.isNotBlank(), "template '${template.format}' rendered blank")
        }
    }

    @Test
    fun sentenceTemplateRejectsNonPositiveWeight() {
        assertFailsWith<LoremException.InvalidWeight> {
            SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 0, format = "{w4}.")
        }
        assertFailsWith<LoremException.InvalidWeight> {
            SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = -1, format = "{w4}.")
        }
    }

    @Test
    fun sentenceTemplateRejectsMalformedFormat() {
        assertFailsWith<LoremException.InvalidTemplate> {
            SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 1, format = "{w0}.")
        }
    }
}
