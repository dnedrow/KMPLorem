package io.github.dnedrow.lorem

/**
 * The transition phrases used when a configuration supplies none.
 *
 * They are Latin connectors so that an inserted transition matches the Latin word pool.
 */
public object TransitionLibrary {

    /**
     * The eight default transition phrases, each already punctuated.
     *
     * The reference implementation lists `"Tamen,"` twice, which quietly doubles that phrase's odds
     * and leaves only seven distinct connectors. `"Nihilominus,"` takes the second slot here so all
     * eight are unique and evenly weighted.
     */
    public val defaultTransitions: List<String> = listOf(
        "Autem,",
        "Tamen,",
        "Praeterea,",
        "Ergo,",
        "Insuper,",
        "Consequenter,",
        "Nihilominus,",
        "Ut consequens,",
    )
}
