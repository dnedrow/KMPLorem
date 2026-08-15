package io.github.dnedrow.lorem

import kotlin.math.roundToInt

/**
 * Rescales per-token word counts so the rendered sentence hits [requestedBudget] words, keeping
 * every token at one word or more.
 *
 * The budget is raised to the token count when it is smaller, because a token can never expand to
 * fewer than one word.
 */
internal fun distribute(counts: List<Int>, requestedBudget: Int): List<Int> {
    val tokenCount = counts.size
    if (tokenCount == 0) return emptyList()
    val total = counts.sum()
    if (total <= 0) return List(tokenCount) { 1 }
    val budget = maxOf(requestedBudget, tokenCount)

    val result = IntArray(tokenCount) { index ->
        val scaled = counts[index].toDouble() * budget.toDouble() / total.toDouble()
        // Swift rounds half away from zero; every value here is positive, where `roundToInt`
        // (half up) agrees with it exactly.
        maxOf(1, scaled.roundToInt())
    }

    var difference = budget - result.sum()
    var index = 0
    while (difference > 0) {
        result[index % tokenCount]++
        difference--
        index++
    }
    var attempts = 0
    val attemptLimit = tokenCount * (budget + 1) + tokenCount
    while (difference < 0 && attempts < attemptLimit) {
        val position = index % tokenCount
        if (result[position] > 1) {
            result[position]--
            difference++
        }
        index++
        attempts++
    }
    return result.toList()
}

/**
 * Moves token capacity into the first token so an injected opening phrase stays contiguous instead
 * of being split by the template's literal text.
 *
 * Every other token keeps at least one word and the total budget is unchanged, so sentence word
 * bounds still hold.
 */
internal fun reserveLeadingCapacity(counts: List<Int>, atLeast: Int): List<Int> {
    val first = counts.firstOrNull() ?: return counts
    if (counts.size <= 1 || first >= atLeast) return counts

    val result = counts.toIntArray()
    var needed = atLeast - first
    var index = result.size - 1
    while (needed > 0 && index > 0) {
        val movable = minOf(needed, result[index] - 1)
        if (movable > 0) {
            result[index] -= movable
            result[0] += movable
            needed -= movable
        }
        index--
    }
    return result.toList()
}
