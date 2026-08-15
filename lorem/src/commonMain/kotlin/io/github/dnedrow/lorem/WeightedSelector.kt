package io.github.dnedrow.lorem

/**
 * An element paired with the relative weight used when selecting it.
 *
 * @property element the value returned when this item is selected.
 * @property weight the relative likelihood of selection. Must be greater than zero.
 */
public data class WeightedItem<out T>(
    public val element: T,
    public val weight: Int,
)

/**
 * Selects elements in proportion to their weights using an injected deterministic generator.
 *
 * Selection uses a cumulative-weight table and a bounded draw, so it runs in `O(log n)` and
 * reproduces exactly for a given generator state.
 */
public class WeightedSelector<out T>
@Throws(LoremException::class)
constructor(items: List<WeightedItem<T>>) {

    private val elements: List<T>
    private val cumulativeWeights: IntArray

    init {
        if (items.isEmpty()) throw LoremException.InvalidTemplate()
        var running = 0
        val cumulative = IntArray(items.size)
        items.forEachIndexed { index, item ->
            if (item.weight <= 0) throw LoremException.InvalidWeight()
            running += item.weight
            cumulative[index] = running
        }
        elements = items.map { it.element }
        cumulativeWeights = cumulative
    }

    /** The total of all item weights. */
    public val totalWeight: Int get() = cumulativeWeights.last()

    /** The number of selectable elements. */
    public val count: Int get() = elements.size

    /**
     * Selects one element in proportion to its weight.
     *
     * [totalWeight] is always positive, because construction rejects an empty item list and any
     * non-positive weight.
     */
    public fun select(random: SeededRandom): T {
        val draw = random.nextInt(totalWeight)
        var low = 0
        var high = cumulativeWeights.size - 1
        while (low < high) {
            val mid = (low + high) / 2
            if (draw < cumulativeWeights[mid]) high = mid else low = mid + 1
        }
        return elements[low]
    }
}
