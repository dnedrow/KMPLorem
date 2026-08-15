package io.github.dnedrow.lorem

import kotlin.random.Random

/**
 * A deterministic, seedable pseudo-random generator based on the SplitMix64 algorithm.
 *
 * Two generators created with the same seed produce identical sequences, which is what makes
 * [LoremGenerator] output reproducible across runs, machines, and targets.
 *
 * Because this type owns its core algorithm rather than delegating to the standard library's
 * default generator, its sequence is stable across Kotlin toolchain upgrades. Every derived
 * draw — [nextInt], [nextDouble], and friends — is supplied by [Random] on top of [nextBits].
 *
 * ```kotlin
 * val random = SeededRandom(seed = 42L)
 * val value = random.nextInt(0, 10)
 * ```
 */
public class SeededRandom(seed: Long) : Random() {

    private var state: ULong = seed.toULong()

    /** Returns the next value in the deterministic sequence. */
    public fun nextULong(): ULong {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z shr 30)) * MIX_1
        z = (z xor (z shr 27)) * MIX_2
        return z xor (z shr 31)
    }

    override fun nextBits(bitCount: Int): Int {
        if (bitCount == 0) return 0
        // Take the high bits, which are the best-mixed output of the SplitMix64 finalizer.
        return (nextULong() shr (ULong.SIZE_BITS - bitCount)).toInt()
    }

    private companion object {
        private const val GOLDEN_GAMMA: ULong = 0x9E3779B97F4A7C15uL
        private const val MIX_1: ULong = 0xBF58476D1CE4E5B9uL
        private const val MIX_2: ULong = 0x94D049BB133111EBuL
    }
}
