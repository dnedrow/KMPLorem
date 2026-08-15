package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WeightedSelectorTest {

    @Test
    fun weightsDriveSelectionFrequency() {
        val selector = WeightedSelector(
            listOf(WeightedItem("heavy", 9), WeightedItem("light", 1)),
        )
        val random = SeededRandom(42L)
        var heavy = 0
        var light = 0
        repeat(10_000) {
            when (selector.select(random)) {
                "heavy" -> heavy++
                else -> light++
            }
        }
        assertTrue(heavy > light * 5, "expected heavy ($heavy) to dominate light ($light)")
        val ratio = heavy.toDouble() / (heavy + light)
        assertTrue(ratio in 0.85..0.95, "expected roughly 0.9, got $ratio")
    }

    @Test
    fun everyElementIsReachable() {
        val selector = WeightedSelector(
            listOf(WeightedItem("a", 1), WeightedItem("b", 1), WeightedItem("c", 1)),
        )
        val random = SeededRandom(7L)
        val seen = mutableSetOf<String>()
        repeat(500) { seen.add(selector.select(random)) }
        assertEquals(setOf("a", "b", "c"), seen)
    }

    @Test
    fun singleItemIsAlwaysSelected() {
        val selector = WeightedSelector(listOf(WeightedItem("only", 3)))
        val random = SeededRandom(1L)
        repeat(50) { assertEquals("only", selector.select(random)) }
    }

    @Test
    fun selectionIsDeterministic() {
        val items = listOf(WeightedItem("a", 2), WeightedItem("b", 3), WeightedItem("c", 5))
        val first = WeightedSelector(items).let { s -> SeededRandom(11L).let { r -> List(100) { s.select(r) } } }
        val second = WeightedSelector(items).let { s -> SeededRandom(11L).let { r -> List(100) { s.select(r) } } }
        assertEquals(first, second)
    }

    @Test
    fun totalWeightAndCountAreReported() {
        val selector = WeightedSelector(
            listOf(WeightedItem("a", 2), WeightedItem("b", 3)),
        )
        assertEquals(5, selector.totalWeight)
        assertEquals(2, selector.count)
    }

    @Test
    fun emptyItemListIsRejected() {
        assertFailsWith<LoremException.InvalidTemplate> {
            WeightedSelector(emptyList<WeightedItem<String>>())
        }
    }

    @Test
    fun nonPositiveWeightsAreRejected() {
        assertFailsWith<LoremException.InvalidWeight> {
            WeightedSelector(listOf(WeightedItem("a", 0)))
        }
        assertFailsWith<LoremException.InvalidWeight> {
            WeightedSelector(listOf(WeightedItem("a", 1), WeightedItem("b", -3)))
        }
    }
}
