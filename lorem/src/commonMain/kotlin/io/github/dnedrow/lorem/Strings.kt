package io.github.dnedrow.lorem

/**
 * Splits on runs of whitespace, discarding empty results.
 *
 * Deliberately hand-rolled on [Char.isWhitespace] rather than built on `Regex("\\s+")`: regular
 * expressions compile to the host platform's engine, whose whitespace class differs between the
 * JVM, JavaScript, Wasm, and Native. Using one here would let identical input tokenize differently
 * across targets.
 */
internal fun String.splitOnWhitespace(): List<String> {
    val words = mutableListOf<String>()
    val builder = StringBuilder()
    for (character in this) {
        if (character.isWhitespace()) {
            if (builder.isNotEmpty()) {
                words.add(builder.toString())
                builder.clear()
            }
        } else {
            builder.append(character)
        }
    }
    if (builder.isNotEmpty()) words.add(builder.toString())
    return words
}

/** The number of whitespace-separated words. */
internal fun String.loremWordCount(): Int = splitOnWhitespace().size

/** Trims the string and collapses every run of whitespace to a single space. */
internal fun String.normalizeWhitespace(): String = splitOnWhitespace().joinToString(" ")

/**
 * Returns the string with only its first alphabetic character uppercased.
 *
 * Unlike a title-casing helper, this leaves the rest of the sentence untouched.
 */
internal fun String.sentenceCased(): String = mapFirstLetter(Char::uppercaseChar)

/**
 * Returns the string with only its first alphabetic character lowercased.
 *
 * Used when a transition phrase is prepended and the following clause should no longer start a
 * sentence.
 */
internal fun String.sentenceUncased(): String = mapFirstLetter(Char::lowercaseChar)

private inline fun String.mapFirstLetter(transform: (Char) -> Char): String {
    val index = indexOfFirst { it.isLetter() }
    if (index < 0) return this
    val replacement = transform(this[index])
    if (replacement == this[index]) return this
    return substring(0, index) + replacement + substring(index + 1)
}
