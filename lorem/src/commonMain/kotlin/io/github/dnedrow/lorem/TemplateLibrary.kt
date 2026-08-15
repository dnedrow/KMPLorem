package io.github.dnedrow.lorem

/**
 * The sentence templates used when a configuration supplies none.
 *
 * The literals are Latin — clauses and list items join with `et`, and the complex template opens
 * with `Quamvis` — so generated text reads as cohesive Latin rather than mixing Latin dictionary
 * words with English connectors.
 */
public object TemplateLibrary {

    /** The default weighted templates, one or more per [SentenceTemplate.Pattern]. */
    public val defaultTemplates: List<SentenceTemplate> = listOf(
        SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 30, format = "{w6}."),
        SentenceTemplate(SentenceTemplate.Pattern.COMPOUND, weight = 20, format = "{w5}, et {w5}."),
        SentenceTemplate(SentenceTemplate.Pattern.COMPLEX, weight = 18, format = "Quamvis {w4}, {w6}."),
        SentenceTemplate(SentenceTemplate.Pattern.LIST, weight = 14, format = "{w3}: {w2}, {w2}, et {w2}."),
        SentenceTemplate(SentenceTemplate.Pattern.QUESTION, weight = 10, format = "{w7}?"),
        SentenceTemplate(SentenceTemplate.Pattern.EMPHASIS, weight = 8, format = "{w4}: {w3}!"),
    )
}
