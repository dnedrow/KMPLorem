## 1. Project scaffolding

- [x] 1.1 Copy the multiplatform-library-template layout into the repository root: `gradlew`, `gradlew.bat`, `gradle/wrapper/`, `gradle/libs.versions.toml`, `.gitignore`, and the root `build.gradle.kts`
- [x] 1.2 Set `rootProject.name = "lorem"` in `settings.gradle.kts` and rename the template's `library` module directory to `lorem`, updating `include(":lorem")`
- [x] 1.3 In `lorem/build.gradle.kts`, declare all seven targets — `jvm`, `androidLibrary`, `iosArm64`, `iosSimulatorArm64`, `iosX64`, `linuxX64`, `wasmJs` — and set the Android namespace to `io.github.dnedrow.lorem`
- [x] 1.4 Enable `explicitApi()` on the `:lorem` module
- [x] 1.5 Confirm `commonTest` depends on `kotlin-test` and that no dependency is declared in `commonMain`
- [x] 1.6 Update the `mavenPublishing` block with placeholder coordinates: group `io.github.dnedrow`, artifact `lorem`, and a POM name/description reflecting this library; leave licence, developer, and SCM blocks as explicit placeholders pending the publishing decision
- [x] 1.7 Extend `.github/workflows/gradle.yml` with matrix entries for `iosX64Test` and `wasmJsTest`, plus a compile-only entry for `iosArm64` (the template declares that target but never builds it in CI)
- [x] 1.8 Verify `./gradlew build` succeeds on an empty source tree before any library code is written

## 2. Foundations

- [x] 2.1 Create the `io.github.dnedrow.lorem` package in `commonMain` and add `SeededRandom`, extending `kotlin.random.Random` with a `ULong` SplitMix64 state, a public `Long` seed parameter, and an overridden `nextBits(bitCount: Int)`
- [x] 2.2 Add the sealed `LoremException` hierarchy rooted in `IllegalArgumentException` with the `EmptyDictionary`, `InvalidTemplate`, and `InvalidWeight` cases, each carrying a human-readable message
- [x] 2.3 Add internal string helpers: a whitespace tokenizer built on `Char.isWhitespace()` (no `Regex`), a word-count function, a whitespace-normalizing function, and first-letter uppercase/lowercase helpers that touch only the first alphabetic character
- [x] 2.4 Test `SeededRandom`: identical seeds produce identical sequences, different seeds diverge, and `nextInt`, `nextInt(bound)`, and `nextDouble()` are all reproducible for a fixed seed
- [x] 2.5 Test the string helpers against tabs, newlines, runs of spaces, leading and trailing whitespace, and strings whose first character is punctuation

## 3. Dictionary and templates

- [x] 3.1 Add the 210-word Latin dictionary as a compiled-in list in `commonMain`, sourced from DSKit's `Sources/DSLorem/Resources/lorem_words.csv` in file order
- [x] 3.2 Implement `LoremDictionary` with a custom-words constructor that trims entries, drops empties, deduplicates preserving first-seen order, and raises `LoremException.EmptyDictionary` when nothing usable remains
- [x] 3.3 Add the built-in dictionary accessor and an internal wrapping index lookup
- [x] 3.4 Implement `SentenceTemplate` with its `Pattern` enum (`SIMPLE`, `COMPOUND`, `COMPLEX`, `LIST`, `QUESTION`, `EMPHASIS`), a positive-weight check raising `LoremException.InvalidWeight`, and format validation at construction
- [x] 3.5 Test dictionary construction: deduplication, first-seen order retention, trimming, empty rejection, and that the built-in dictionary is non-empty and unique

## 4. Weighted selection and default libraries

- [x] 4.1 Implement `WeightedSelector` with a cumulative-weight table and binary-search selection, raising `LoremException.InvalidTemplate` on an empty item list and `LoremException.InvalidWeight` on a non-positive weight
- [x] 4.2 Implement `TemplateLibrary` with the six default weighted templates, preserving the Latin literals exactly: `{w6}.`, `{w5}, et {w5}.`, `Quamvis {w4}, {w6}.`, `{w3}: {w2}, {w2}, et {w2}.`, `{w7}?`, and `{w4}: {w3}!`
- [x] 4.3 Implement `TransitionLibrary` with the eight default Latin transition phrases, each already punctuated
- [x] 4.4 Test weighted selection: a weight-9 versus weight-1 pair skews proportionally over a large fixed-seed sample, an empty item list is rejected, and a non-positive weight is rejected
- [x] 4.5 Test that the default templates join clauses and list items with `et` and that the complex template opens with `Quamvis`, and that every default transition is a Latin connector

## 5. Template rendering

- [x] 5.1 Implement the `{wN}` format scanner in `TemplateRenderer`: literals and tokens as parsed segments, rejecting `{w}`, `{w0}`, non-numeric counts, unmatched `{`, and a stray `}` with `LoremException.InvalidTemplate`
- [x] 5.2 Derive and expose `tokenCounts`, `fixedWordCount`, `minimumWordCount`, and whether the template begins with a token rather than a literal
- [x] 5.3 Implement rendering: expand each token to its word count, consume any supplied leading words first (spilling across token boundaries), avoid drawing the immediately preceding word when an alternative exists, assemble against the literals, normalize whitespace, and uppercase the first alphabetic character
- [x] 5.4 Test parsing: valid formats produce the expected token counts and fixed word counts, and each malformed-format case raises `LoremException.InvalidTemplate`
- [x] 5.5 Test rendering: `{w8}.` yields exactly eight words and a period, the first alphabetic character is uppercase, output is trimmed and single-spaced, and literals and punctuation land in their template positions

## 6. Word-budget arithmetic

- [x] 6.1 Implement `distribute(counts, budget)`: rescale per-token word counts to hit the budget, keeping every token at one word or more, with a bounded correction loop
- [x] 6.2 Implement `reserveLeadingCapacity(counts, atLeast)`: move capacity from later tokens into the first so an injected opening phrase stays contiguous, leaving every other token at one word or more and the total unchanged
- [x] 6.3 Test `distribute` directly over its contract: the result sums to the budget, no element falls below one, a budget below the token count is clamped upward, and a zero-token input returns empty
- [x] 6.4 Test `reserveLeadingCapacity` directly: the first element reaches the requested capacity when the total allows, the total is preserved, no element falls below one, and a single-token input is returned unchanged

## 7. Styles, configuration, and phrases

- [x] 7.1 Implement `ParagraphStyle` with a name, an `IntRange` sentence count, a transition probability clamped to `0.0..1.0`, a complexity weighting, a question frequency, and per-pattern weight multipliers
- [x] 7.2 Add the five presets — `Classic`, `Technical`, `Academic`, `Legal`, `Mixed` — with the sentence ranges, probabilities, weightings, and multipliers from the reference implementation, plus a list of all presets
- [x] 7.3 Implement `GeneratorConfiguration` as a `data class` with a validating `init` block, a `Default` companion value using the normal validated path, and an internal effective-minimum-words derivation for when short sentences are disallowed
- [x] 7.4 Implement `PhraseGenerator`: filter blank transitions at construction, select a transition avoiding the previous one when an alternative exists, measure a phrase's word count, and prepend a transition while lowercasing the following clause
- [x] 7.5 Test configuration validation: non-positive bounds and a minimum above the maximum are rejected, the transition probability clamps at both ends, the default configuration is constructible, and `copy()` still validates
- [x] 7.6 Test that all five style presets resolve and that their sentence-count ranges are the expected values

## 8. Generator core

- [x] 8.1 Implement `LoremGenerator` holding an immutable dictionary, `Long` seed, and configuration, with `@Throws(LoremException::class)` on every public generation function
- [x] 8.2 Implement template preparation: select custom templates when supplied otherwise the defaults, drop question templates when disallowed by configuration or style, parse each format once, apply style weighting per pattern, and raise `LoremException.InvalidTemplate` when no candidate survives
- [x] 8.3 Implement the per-call pass state carrying last pattern, last transition, last word, and whether the opening has been emitted — never stored on the generator
- [x] 8.4 Implement sentence construction: filter candidates against the previous pattern when alternatives exist, select a template, draw a target word count, reserve budget for a transition, distribute the remaining budget across tokens, and render
- [x] 8.5 Implement paragraph construction: draw a sentence count from the style's range and join the sentences with single spaces
- [x] 8.6 Implement the single-sentence, paragraph, and batch operations, each constructing a fresh `SeededRandom(seed)` and threading one instance across the whole call; the batch operation returns an empty list for a non-positive count
- [x] 8.7 Test determinism: equal seeds produce equal output, different seeds diverge, repeated calls on one instance are idempotent, and batch paragraphs differ from one another while the batch call as a whole is reproducible
- [x] 8.8 Test bounds and style behavior: sentence counts fall in the style range, every sentence's word count respects the configured bounds, custom templates are used exclusively when supplied, questions are absent when disallowed, and consecutive patterns differ when alternatives exist
- [x] 8.9 Test the exhausted-template path: a question-only custom template set combined with the `Legal` style raises `LoremException.InvalidTemplate`
- [x] 8.10 Test transitions: zero probability yields none, and the same transition never appears on two consecutive sentences

## 9. Canonical opening

- [x] 9.1 Expose the canonical opening phrase `Lorem ipsum dolor sit amet` and its word list as public constants
- [x] 9.2 Add `startsWithCanonicalOpening` to `GeneratorConfiguration`, defaulting to disabled
- [x] 9.3 Implement injection: on the first sentence of a call, prefer a token-leading template, reserve leading capacity so the phrase stays contiguous, and pass the phrase as leading words to the renderer
- [x] 9.4 Implement the prepend fallback for when no candidate template begins with a token, lowercasing the body's first letter as the transition path does
- [x] 9.5 Implement truncation: when the maximum word count cannot hold the whole phrase, keep as many leading words as fit and drop the rest, so the configured maximum still holds
- [x] 9.6 Scope the phrase to the first sentence of a call only, via the pass state, so later sentences and later paragraphs in a batch never carry it
- [x] 9.7 Test every scenario in the spec's canonical-opening requirement, including the truncation boundary, the disabled default, determinism with the option enabled, word-bound accounting, and that no transition precedes the phrase

## 10. Cross-target verification

- [x] 10.1 Author fixed-seed golden tests asserting exact strings for a sentence, a paragraph per style, a batch, and a canonical-opening paragraph — written last, so they pin reviewed behavior
- [x] 10.2 Add a concurrency test covering two independently seeded generators used simultaneously and one generator shared across threads, on the targets where threading is available
- [x] 10.3 Run the full test suite on every target and confirm identical results, paying particular attention to `wasmJs` and `linuxX64` for whitespace and character-class divergence
- [x] 10.4 Confirm the source tree contains no `expect` or `actual` declaration and no source set other than `commonMain` and `commonTest` holds library code
- [x] 10.5 Confirm every public declaration compiles under explicit API mode and every fallible public function carries `@Throws(LoremException::class)`

## 11. Documentation

- [x] 11.1 Write the module README: overview, a quick-start example, the five styles, the canonical opening option, and the supported target list
- [x] 11.2 Document the idempotent single-call model prominently, showing the batch operation as the way to get varied output
- [x] 11.3 Document the known interop limitations for Swift consumers — default arguments do not cross the Objective-C bridge, and `LoremException` cases arrive as `NSError`
- [x] 11.4 Note that custom dictionaries containing combining marks or surrogate pairs may render differently across targets, since character semantics are platform-defined
- [x] 11.5 Replace the template's README, LICENSE placeholder, and images directory with content for this project
