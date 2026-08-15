package io.github.dnedrow.lorem

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Concurrency coverage for the targets where real threads exist.
 *
 * The generator holds no mutable state — every pass builds its own [SeededRandom] and pass state —
 * so both a shared instance and independently seeded instances must be safe. This lives in
 * `jvmTest` rather than `commonTest` because `wasmJs` and `js` are single-threaded and Kotlin has
 * no common threading primitive; the library itself stays entirely in `commonMain`.
 */
class ConcurrencyTest {

    private val dictionary = LoremDictionary.BuiltIn

    @Test
    fun oneGeneratorSharedAcrossThreadsProducesIdenticalOutput() {
        val generator = LoremGenerator(dictionary, 42L)
        val expected = generator.generateParagraph(ParagraphStyle.Technical)

        val results = runConcurrently(threads = 8, repeats = 25) {
            generator.generateParagraph(ParagraphStyle.Technical)
        }
        assertEquals(setOf(expected), results.toSet())
    }

    @Test
    fun independentlySeededGeneratorsDoNotInterfere() {
        val seeds = (1L..8L).toList()
        val expected = seeds.associateWith { LoremGenerator(dictionary, it).generateParagraph() }

        val executor = Executors.newFixedThreadPool(seeds.size)
        try {
            val tasks = seeds.map { seed ->
                Callable {
                    val generator = LoremGenerator(dictionary, seed)
                    List(25) { seed to generator.generateParagraph() }
                }
            }
            val observed = executor.invokeAll(tasks).flatMap { it.get() }
            for ((seed, paragraph) in observed) {
                assertEquals(expected.getValue(seed), paragraph, "seed $seed diverged under load")
            }
        } finally {
            executor.shutdown()
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))
        }
    }

    @Test
    fun aSharedGeneratorIsSafeForBatchCalls() {
        val generator = LoremGenerator(
            dictionary,
            11L,
            GeneratorConfiguration(startsWithCanonicalOpening = true),
        )
        val expected = generator.generateParagraphs(4)

        val results = runConcurrently(threads = 8, repeats = 15) { generator.generateParagraphs(4) }
        assertEquals(setOf(expected), results.toSet())
    }

    private fun <T> runConcurrently(threads: Int, repeats: Int, block: () -> T): List<T> {
        val executor = Executors.newFixedThreadPool(threads)
        try {
            val tasks = List(threads) { Callable { List(repeats) { block() } } }
            return executor.invokeAll(tasks).flatMap { it.get() }
        } finally {
            executor.shutdown()
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))
        }
    }
}
