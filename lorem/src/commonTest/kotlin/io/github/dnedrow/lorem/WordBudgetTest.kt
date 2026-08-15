package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordBudgetTest {

    // MARK: distribute

    @Test
    fun distributeSumsToTheBudget() {
        for (counts in SHAPES) {
            for (budget in 1..40) {
                val result = distribute(counts, budget)
                val expected = maxOf(budget, counts.size)
                assertEquals(
                    expected,
                    result.sum(),
                    "counts=$counts budget=$budget produced $result",
                )
            }
        }
    }

    @Test
    fun distributeKeepsEveryTokenAtOneOrMore() {
        for (counts in SHAPES) {
            for (budget in 1..40) {
                val result = distribute(counts, budget)
                assertTrue(
                    result.all { it >= 1 },
                    "counts=$counts budget=$budget produced $result",
                )
            }
        }
    }

    @Test
    fun distributePreservesTokenCount() {
        for (counts in SHAPES) {
            for (budget in 1..40) {
                assertEquals(counts.size, distribute(counts, budget).size)
            }
        }
    }

    @Test
    fun distributeClampsABudgetBelowTheTokenCount() {
        val result = distribute(listOf(3, 2, 2, 2), requestedBudget = 1)
        assertContentEquals(listOf(1, 1, 1, 1), result)
    }

    @Test
    fun distributeHandlesAnEmptyTokenList() {
        assertContentEquals(emptyList(), distribute(emptyList(), requestedBudget = 10))
    }

    @Test
    fun distributeHandlesAZeroTotal() {
        assertContentEquals(listOf(1, 1, 1), distribute(listOf(0, 0, 0), requestedBudget = 9))
    }

    @Test
    fun distributeRoughlyPreservesProportions() {
        // 8:2 split of a 20-word budget should land near 16:4.
        val result = distribute(listOf(8, 2), requestedBudget = 20)
        assertEquals(20, result.sum())
        assertTrue(result[0] > result[1], "expected the heavier token to stay heavier: $result")
        assertTrue(result[0] in 15..17, "expected roughly 16, got ${result[0]}")
    }

    @Test
    fun distributeIsStableForRepeatedCalls() {
        assertContentEquals(
            distribute(listOf(5, 5), requestedBudget = 13),
            distribute(listOf(5, 5), requestedBudget = 13),
        )
    }

    @Test
    fun distributeMatchesTheBudgetExactlyForSingleTokens() {
        for (budget in 1..30) {
            assertContentEquals(listOf(budget), distribute(listOf(6), budget))
        }
    }

    // MARK: reserveLeadingCapacity

    @Test
    fun reserveMovesCapacityIntoTheFirstToken() {
        val result = reserveLeadingCapacity(listOf(2, 4, 4), atLeast = 5)
        assertEquals(5, result[0])
        assertEquals(10, result.sum())
        assertTrue(result.all { it >= 1 })
    }

    @Test
    fun reservePreservesTheTotal() {
        for (counts in SHAPES.filter { it.size > 1 }) {
            for (requested in 1..15) {
                val result = reserveLeadingCapacity(counts, requested)
                assertEquals(
                    counts.sum(),
                    result.sum(),
                    "counts=$counts requested=$requested produced $result",
                )
            }
        }
    }

    @Test
    fun reserveKeepsEveryOtherTokenAtOneOrMore() {
        for (counts in SHAPES.filter { it.size > 1 }) {
            for (requested in 1..15) {
                val result = reserveLeadingCapacity(counts, requested)
                assertTrue(
                    result.drop(1).all { it >= 1 },
                    "counts=$counts requested=$requested produced $result",
                )
            }
        }
    }

    @Test
    fun reserveTakesWhatItCanWhenCapacityIsShort() {
        // Only 3 movable words exist (each later token must keep one), so the first token
        // reaches 4 rather than the requested 9.
        val result = reserveLeadingCapacity(listOf(1, 2, 3), atLeast = 9)
        assertEquals(6, result.sum())
        assertEquals(4, result[0])
        assertTrue(result.drop(1).all { it >= 1 })
    }

    @Test
    fun reserveLeavesCountsAloneWhenTheFirstTokenAlreadyFits() {
        assertContentEquals(listOf(6, 2), reserveLeadingCapacity(listOf(6, 2), atLeast = 5))
        assertContentEquals(listOf(5, 2), reserveLeadingCapacity(listOf(5, 2), atLeast = 5))
    }

    @Test
    fun reserveLeavesSingleTokenCountsAlone() {
        assertContentEquals(listOf(3), reserveLeadingCapacity(listOf(3), atLeast = 9))
    }

    @Test
    fun reserveHandlesAnEmptyTokenList() {
        assertContentEquals(emptyList(), reserveLeadingCapacity(emptyList(), atLeast = 5))
    }

    @Test
    fun reserveDrawsFromTheTailFirst() {
        // Pulling from the end keeps earlier tokens closer to their intended share.
        val result = reserveLeadingCapacity(listOf(1, 5, 5), atLeast = 3)
        assertEquals(3, result[0])
        assertEquals(5, result[1])
        assertEquals(3, result[2])
    }

    private companion object {
        private val SHAPES = listOf(
            listOf(6),
            listOf(5, 5),
            listOf(4, 6),
            listOf(3, 2, 2, 2),
            listOf(7),
            listOf(4, 3),
            listOf(1, 1, 1),
            listOf(8, 2),
            listOf(2, 4, 4),
        )
    }
}
