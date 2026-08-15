package io.github.dnedrow.lorem

/**
 * Carries word-repetition state across the sentences of a single generation pass.
 *
 * Held per call and never stored on a generator, which is what keeps generators immutable.
 */
internal class WordState {
    var lastWord: String? = null
}

/**
 * Expands a sentence template into text.
 *
 * A template format mixes literal text with `{wN}` tokens, where `N` is a positive count of words
 * to draw from the dictionary. For example `"{w4}, et {w3}."` renders as four words, a comma, the
 * literal `et`, three more words, and a period.
 *
 * Rendering trims the result, collapses runs of whitespace to single spaces, and uppercases the
 * first alphabetic character.
 */
public class TemplateRenderer
@Throws(LoremException::class)
constructor(format: String) {

    internal sealed interface Segment {
        data class Literal(val text: String) : Segment
        data class Token(val count: Int) : Segment
    }

    internal val segments: List<Segment>

    /** The word count requested by each `{wN}` token, in template order. */
    public val tokenCounts: List<Int>

    /**
     * The number of words contributed by literal text, independent of the tokens.
     *
     * Punctuation attached to a neighbouring word is not counted.
     */
    public val fixedWordCount: Int

    /**
     * Whether the template opens with a `{wN}` token rather than literal text.
     *
     * Only such templates can carry a leading phrase at the very start of the rendered sentence.
     */
    public val beginsWithToken: Boolean

    /**
     * The smallest number of words this template can render, reached when every token expands to a
     * single word.
     */
    public val minimumWordCount: Int get() = fixedWordCount + tokenCounts.size

    init {
        val parsed = mutableListOf<Segment>()
        val counts = mutableListOf<Int>()
        val literal = StringBuilder()
        var index = 0

        while (index < format.length) {
            val character = format[index]
            if (character == '}') throw LoremException.InvalidTemplate()
            if (character != '{') {
                literal.append(character)
                index++
                continue
            }

            var cursor = index + 1
            if (cursor >= format.length || format[cursor] != 'w') throw LoremException.InvalidTemplate()
            cursor++
            val digits = StringBuilder()
            while (cursor < format.length && format[cursor].isDigit()) {
                digits.append(format[cursor])
                cursor++
            }
            if (cursor >= format.length || format[cursor] != '}') throw LoremException.InvalidTemplate()
            val count = digits.toString().toIntOrNull() ?: throw LoremException.InvalidTemplate()
            if (count <= 0) throw LoremException.InvalidTemplate()

            if (literal.isNotEmpty()) {
                parsed.add(Segment.Literal(literal.toString()))
                literal.clear()
            }
            parsed.add(Segment.Token(count))
            counts.add(count)
            index = cursor + 1
        }

        if (literal.isNotEmpty()) parsed.add(Segment.Literal(literal.toString()))

        segments = parsed
        tokenCounts = counts
        beginsWithToken = parsed.firstOrNull() is Segment.Token
        // Render every token as a single placeholder word, then subtract the tokens themselves;
        // what remains is the word count contributed by the literals alone.
        fixedWordCount = assemble(parsed, List(counts.size) { listOf("x") }).loremWordCount() - counts.size
    }

    /**
     * Renders the template using words drawn from [dictionary].
     *
     * @param dictionary the word pool to draw from.
     * @param random the deterministic generator to draw with.
     * @param tokenCounts optional replacement word counts, one per token, used to fit a sentence to
     *   configured bounds. Defaults to the template's own counts.
     * @param leadingWords words emitted, in order, before any dictionary draw. They fill the first
     *   word slots of the template, spilling into later tokens when the first token is shorter than
     *   the list. Words that do not fit within the token budget are dropped.
     */
    public fun render(
        dictionary: LoremDictionary,
        random: SeededRandom,
        tokenCounts: List<Int>? = null,
        leadingWords: List<String> = emptyList(),
    ): String = render(dictionary, random, tokenCounts, leadingWords, WordState())

    internal fun render(
        dictionary: LoremDictionary,
        random: SeededRandom,
        overrideCounts: List<Int>?,
        leadingWords: List<String>,
        state: WordState,
    ): String {
        val counts = if (overrideCounts != null && overrideCounts.size == tokenCounts.size) {
            overrideCounts
        } else {
            tokenCounts
        }

        var pending = 0
        val expansions = counts.map { count ->
            List(count) {
                if (pending < leadingWords.size) {
                    val leading = leadingWords[pending]
                    pending++
                    state.lastWord = leading
                    leading
                } else {
                    nextWord(dictionary, random, state)
                }
            }
        }

        return assemble(segments, expansions).normalizeWhitespace().sentenceCased()
    }

    /** The total number of words produced when the tokens expand to [counts]. */
    internal fun wordCountForTokenCounts(counts: List<Int>): Int = fixedWordCount + counts.sum()

    private fun nextWord(
        dictionary: LoremDictionary,
        random: SeededRandom,
        state: WordState,
    ): String {
        val total = dictionary.words.size
        var index = random.nextInt(total)
        if (total > 1 && dictionary.words[index] == state.lastWord) {
            index = (index + 1) % total
        }
        val word = dictionary.words[index]
        state.lastWord = word
        return word
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is TemplateRenderer && segments == other.segments)

    override fun hashCode(): Int = segments.hashCode()

    private companion object {
        private fun assemble(segments: List<Segment>, words: List<List<String>>): String {
            val result = StringBuilder()
            var tokenIndex = 0
            for (segment in segments) {
                when (segment) {
                    is Segment.Literal -> result.append(segment.text)
                    is Segment.Token -> {
                        if (tokenIndex < words.size) result.append(words[tokenIndex].joinToString(" "))
                        tokenIndex++
                    }
                }
            }
            return result.toString()
        }
    }
}
