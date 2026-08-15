package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SeededRandomTest {

    @Test
    fun sameSeedProducesSameSequence() {
        val a = SeededRandom(seed = 42L)
        val b = SeededRandom(seed = 42L)
        val first = List(64) { a.nextULong() }
        val second = List(64) { b.nextULong() }
        assertEquals(first, second)
    }

    @Test
    fun differentSeedsDiverge() {
        val a = List(64) { SeededRandom(seed = 1L).nextULong() }
        val b = List(64) { SeededRandom(seed = 2L).nextULong() }
        assertNotEquals(a, b)
    }

    @Test
    fun zeroSeedIsValidAndProducesNonZeroOutput() {
        val random = SeededRandom(seed = 0L)
        val values = List(16) { random.nextULong() }
        assertTrue(values.any { it != 0uL }, "SplitMix64 seeded with 0 must still mix")
    }

    @Test
    fun negativeSeedsAreAccepted() {
        val a = List(16) { SeededRandom(seed = -7L).nextULong() }
        val b = List(16) { SeededRandom(seed = -7L).nextULong() }
        assertEquals(a, b)
        assertNotEquals(a, List(16) { SeededRandom(seed = 7L).nextULong() })
    }

    @Test
    fun derivedIntDrawsAreReproducible() {
        val a = SeededRandom(seed = 99L)
        val b = SeededRandom(seed = 99L)
        repeat(200) {
            assertEquals(a.nextInt(3, 7), b.nextInt(3, 7))
        }
    }

    @Test
    fun derivedIntDrawsRespectBounds() {
        val random = SeededRandom(seed = 5L)
        repeat(1_000) {
            val value = random.nextInt(3, 7)
            assertTrue(value in 3..6, "nextInt(3, 7) produced $value")
        }
    }

    @Test
    fun derivedBoundedDrawsRespectBounds() {
        val random = SeededRandom(seed = 11L)
        repeat(1_000) {
            val value = random.nextInt(10)
            assertTrue(value in 0..9, "nextInt(10) produced $value")
        }
    }

    @Test
    fun derivedDoubleDrawsAreReproducibleAndInRange() {
        val a = SeededRandom(seed = 77L)
        val b = SeededRandom(seed = 77L)
        repeat(500) {
            val value = a.nextDouble()
            assertEquals(value, b.nextDouble())
            assertTrue(value >= 0.0 && value < 1.0, "nextDouble() produced $value")
        }
    }

    @Test
    fun derivedDrawsCoverTheirRange() {
        val random = SeededRandom(seed = 2024L)
        val seen = mutableSetOf<Int>()
        repeat(2_000) { seen.add(random.nextInt(3, 7)) }
        assertEquals(setOf(3, 4, 5, 6), seen)
    }

    @Test
    fun nextBitsHonorsRequestedWidth() {
        val random = SeededRandom(seed = 31L)
        assertEquals(0, random.nextBits(0))
        for (bitCount in 1..32) {
            val limit = if (bitCount == 32) Int.MAX_VALUE.toLong() * 2 + 1 else (1L shl bitCount) - 1
            repeat(50) {
                val value = random.nextBits(bitCount).toLong() and limit
                assertTrue(value <= limit, "nextBits($bitCount) exceeded its width")
            }
        }
    }
}
