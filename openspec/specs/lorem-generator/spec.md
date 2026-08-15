# lorem-generator Specification

## Purpose

Provides deterministic, seeded Lorem Ipsum generation for Kotlin Multiplatform previews, tests, and documentation — turning a seed, a word dictionary, and weighted sentence templates into reproducible paragraphs with configurable styles, optional transitions, an opt-in canonical opening, and anti-repetition, without shared mutable state or platform-specific code.

## Requirements

### Requirement: Deterministic seeded generation

The generator SHALL produce identical output for identical inputs (same dictionary, seed, configuration, and requested operation). It SHALL derive all randomness from a seedable pseudo-random generator implemented with the SplitMix64 algorithm and constructed from a `Long` seed. Two generators created with the same seed SHALL yield character-for-character identical text; generators created with different seeds SHALL yield different text for a non-trivial request. Output SHALL be identical across every supported target for a given seed.

#### Scenario: Same seed produces same output

- **WHEN** two generators are created with the same dictionary and seed `42` and each generates a paragraph with the same style
- **THEN** both produce the exact same string

#### Scenario: Different seeds produce different output

- **WHEN** two generators share a dictionary but use seeds `1` and `2` and each generates a paragraph with the same style
- **THEN** the two produced strings are not equal

#### Scenario: Seeded value generator is reproducible

- **WHEN** two seeded generators are initialized with the same seed and each is asked for the same count of values
- **THEN** the two produced sequences are equal

#### Scenario: Repeated calls on one generator are idempotent

- **WHEN** the same generation call is made twice on a single generator instance
- **THEN** both calls return the exact same string

### Requirement: Paragraph generation honors style

The generator SHALL expose paragraph generation parameterized by a `ParagraphStyle` with the presets `classic`, `technical`, `academic`, `legal`, and `mixed`. A style SHALL define a sentence-count range, a transition probability, a complexity weighting, a question frequency, and a preferred template mix. The number of sentences in a generated paragraph SHALL fall within the style's configured sentence-count range (inclusive).

#### Scenario: Paragraph sentence count is within the style range

- **WHEN** a paragraph is generated with a style whose sentence-count range is `3..6`
- **THEN** the paragraph contains between 3 and 6 sentences inclusive

#### Scenario: All five presets are available

- **WHEN** a caller references the `classic`, `technical`, `academic`, `legal`, and `mixed` presets
- **THEN** each preset resolves to a valid configuration usable for generation

### Requirement: Batch generation varies within one reproducible call

The generator SHALL expose a batch paragraph operation that produces several paragraphs which differ from one another while the call as a whole remains reproducible for the generator's seed. Requesting a count below one SHALL return an empty list rather than failing.

#### Scenario: Batch paragraphs differ from one another

- **WHEN** four paragraphs are generated in a single batch call
- **THEN** the paragraphs are not all identical

#### Scenario: Batch generation is reproducible

- **WHEN** two generators sharing a dictionary, seed, and configuration each generate four paragraphs in one batch call with the same style
- **THEN** the two lists are equal element for element

#### Scenario: Non-positive count yields an empty result

- **WHEN** a batch call requests zero or fewer paragraphs
- **THEN** an empty list is returned and no error is raised

### Requirement: Sentences are built from weighted templates

The generator SHALL construct sentences from `SentenceTemplate` values, each carrying a `Pattern` (`SIMPLE`, `COMPOUND`, `COMPLEX`, `LIST`, `QUESTION`, `EMPHASIS`), a positive integer weight, and a format string. Template selection SHALL be weighted so that higher-weight templates are chosen proportionally more often across a large sample, using the deterministic value generator.

#### Scenario: Higher-weight templates are selected proportionally

- **WHEN** a weighted set contains one template with weight 9 and one with weight 1 and many selections are drawn with a fixed seed
- **THEN** the weight-9 template is selected substantially more often than the weight-1 template

#### Scenario: Question templates are suppressed when disallowed

- **WHEN** generation runs with a configuration that disallows questions
- **THEN** no generated sentence ends with a question mark

### Requirement: Template rendering expands word tokens

The rendering engine SHALL expand `{wN}` tokens into exactly `N` words drawn from the active dictionary, preserve literal text and punctuation in the template format, capitalize the first letter of each rendered sentence, and produce trimmed, single-spaced output. Rendering an invalid template (for example a malformed token or a non-positive word count) SHALL raise an invalid-template failure rather than crash. Whitespace handling SHALL be identical on every supported target.

#### Scenario: Word token expands to the requested count

- **WHEN** the template `"{w8}."` is rendered
- **THEN** the result contains exactly 8 words followed by a period

#### Scenario: First letter is capitalized

- **WHEN** any sentence is rendered
- **THEN** its first alphabetic character is uppercase

#### Scenario: Malformed template raises a typed failure

- **WHEN** a template with a malformed token such as `"{w}"` or `"{w0}"` is rendered
- **THEN** an invalid-template failure is raised

#### Scenario: Whitespace normalization is platform-independent

- **WHEN** the same template is rendered with the same seed on each supported target
- **THEN** every target produces the same trimmed, single-spaced string

### Requirement: Dictionary provisioning and validation

The library SHALL provide a built-in dictionary of approximately 210 unique Latin words, available without file, bundle, or network access on every supported target. It SHALL also accept custom word collections. Dictionary construction SHALL trim entries, discard empty entries, and deduplicate words while preserving first-seen order so that generation stays deterministic. Constructing a dictionary that yields no usable words SHALL raise an empty-dictionary failure.

#### Scenario: Built-in dictionary is available

- **WHEN** the built-in dictionary is requested on any supported target
- **THEN** it returns a non-empty dictionary of unique Latin words

#### Scenario: Duplicate words are removed

- **WHEN** a custom dictionary is created from a list containing duplicate words
- **THEN** the resulting dictionary contains each word at most once

#### Scenario: First-seen order is preserved

- **WHEN** a custom dictionary is created from a list whose entries include duplicates
- **THEN** the retained words appear in the order of their first occurrence in the input

#### Scenario: Empty dictionary is rejected

- **WHEN** a dictionary is constructed from an empty or whitespace-only word list
- **THEN** an empty-dictionary failure is raised

### Requirement: Transition insertion is style-controlled and bounded

The generator SHALL optionally prepend transition phrases (for example `Autem`, `Tamen`, `Praeterea`) to sentences, governed by the active style's transition probability. The built-in default transitions SHALL be Latin phrases so inserted connectors match the Latin word pool, and SHALL be distinct from one another so no phrase is silently weighted more heavily than its peers. When probability is zero no transitions SHALL appear; when positive, transition insertion SHALL be deterministic for a given seed and SHALL avoid immediate reuse of the same transition.

#### Scenario: Zero probability yields no transitions

- **WHEN** a paragraph is generated with a style whose transition probability is 0
- **THEN** no sentence begins with a transition phrase

#### Scenario: Transitions do not repeat immediately

- **WHEN** transitions are inserted across a multi-sentence paragraph
- **THEN** the same transition phrase does not appear on two consecutive sentences

#### Scenario: Default transitions are Latin

- **WHEN** the default transition library prepends a transition to a sentence
- **THEN** the inserted phrase is a Latin connector such as `Autem`, `Tamen`, or `Praeterea` and contains no English connector words

#### Scenario: Default transitions are distinct

- **WHEN** the default transition library is inspected
- **THEN** no phrase appears more than once

### Requirement: Default libraries render cohesive Latin

The built-in default sentence templates and transition phrases SHALL use Latin connectives and transition words so that generated output reads as cohesive Latin rather than mixing Latin dictionary words with English connectors. Default template literals SHALL join clauses and list items with the Latin conjunction `et`, and the complex template SHALL open with the Latin subordinator `Quamvis`. Supplying custom templates or custom transitions SHALL override these defaults, so callers MAY provide connectors in any language.

#### Scenario: The default compound template joins clauses with a Latin conjunction

- **WHEN** the default compound template renders a sentence
- **THEN** its two clauses are joined by the Latin conjunction `et` rather than the English `and`

#### Scenario: The default complex template opens with a Latin subordinator

- **WHEN** the default complex template renders a sentence
- **THEN** it begins with the Latin word `Quamvis` rather than the English `Although`

### Requirement: Anti-repetition preserves variety

The generator SHALL avoid emitting the same sentence pattern on two consecutive sentences when the configuration enables immediate-pattern-repeat avoidance and at least one alternative pattern is available. It SHALL reduce immediate word repetition when alternatives exist.

#### Scenario: Consecutive patterns differ

- **WHEN** a multi-sentence paragraph is generated with pattern-repeat avoidance enabled and multiple templates available
- **THEN** no two consecutive sentences use the same pattern

#### Scenario: Repetition avoidance yields when no alternative exists

- **WHEN** a configuration supplies a single template and a multi-sentence paragraph is generated with pattern-repeat avoidance enabled
- **THEN** generation succeeds using that template for every sentence

### Requirement: Configurable generation parameters

The generator SHALL accept a `GeneratorConfiguration` exposing a transition-probability scale, an allow-questions flag, an allow-short-sentences flag, an avoid-immediate-pattern-repeats flag, minimum and maximum words per sentence, custom transitions, custom templates, and an option controlling the canonical opening phrase. Generated sentence word counts SHALL respect the minimum and maximum bounds. The maximum SHALL be authoritative over template shape: a template whose smallest renderable word count exceeds the configured maximum SHALL be excluded from selection rather than permitted to overrun the bound, and generation SHALL raise an invalid-template failure when that exclusion leaves no candidate template. A transition-probability scale outside `0.0..1.0` SHALL be clamped into that range. A configuration whose word-count bounds are not positive, or whose minimum exceeds its maximum, SHALL be rejected with an invalid-weight failure.

#### Scenario: Word counts respect configured bounds

- **WHEN** generation runs with a minimum of 4 and a maximum of 10 words per sentence
- **THEN** every generated sentence contains between 4 and 10 words inclusive

#### Scenario: Custom templates are used

- **WHEN** a configuration supplies a non-empty custom template set
- **THEN** generated sentences are rendered only from the supplied templates

#### Scenario: A template that cannot fit the maximum is excluded

- **WHEN** generation runs with a maximum word count smaller than the smallest number of words a given template can render
- **THEN** that template is never selected and no generated sentence exceeds the configured maximum

#### Scenario: An unsatisfiable maximum is reported

- **WHEN** generation runs with a maximum word count smaller than the smallest number of words any available template can render
- **THEN** an invalid-template failure is raised

#### Scenario: Invalid bounds are rejected

- **WHEN** a configuration is constructed with a minimum word count above its maximum, or with a non-positive bound
- **THEN** an invalid-weight failure is raised

#### Scenario: Transition probability is clamped

- **WHEN** a configuration is constructed with a transition-probability scale of `-0.5` or `2.0`
- **THEN** the stored value is `0.0` or `1.0` respectively

#### Scenario: Canonical opening is exposed as configuration

- **WHEN** a caller inspects a configuration
- **THEN** the canonical opening option is readable and defaults to disabled

### Requirement: Opt-in canonical opening phrase

The generator SHALL provide an opt-in option that forces the first sentence of a generation call to begin with the canonical opening phrase `Lorem ipsum dolor sit amet`. The option SHALL default to disabled. When enabled, the phrase SHALL act as a prefix: the remainder of the first sentence SHALL continue with generated words together with the selected template's literal text and punctuation. The phrase SHALL appear only on the first sentence of a call — the single-sentence operation, the paragraph operation, and the first paragraph of the batch operation — and never on a later sentence or a later paragraph. Configured word-count bounds SHALL remain authoritative: the phrase SHALL be emitted contiguously, so when the leading word slot available under the configured maximum cannot accommodate the full phrase, the phrase SHALL be truncated from the end, keeping as many leading words as fit and never fewer than one. Output SHALL remain deterministic: with the option enabled, the same dictionary, seed, configuration, and request SHALL produce character-for-character identical text.

#### Scenario: First sentence begins with the canonical phrase

- **WHEN** a paragraph is generated with the canonical opening enabled
- **THEN** the paragraph begins with `Lorem ipsum dolor sit amet`

#### Scenario: The phrase is a prefix, not the whole sentence

- **WHEN** a sentence is generated with the canonical opening enabled and the configured maximum word count allows additional words
- **THEN** the sentence contains the canonical phrase followed by further generated words and the template's own punctuation

#### Scenario: Later sentences do not repeat the phrase

- **WHEN** a multi-sentence paragraph is generated with the canonical opening enabled
- **THEN** no sentence after the first begins with the canonical phrase

#### Scenario: Only the first paragraph of a batch opens with the phrase

- **WHEN** several paragraphs are generated in one batch call with the canonical opening enabled
- **THEN** the first paragraph begins with the canonical phrase and no later paragraph does

#### Scenario: The option is disabled by default

- **WHEN** a paragraph is generated with a default configuration
- **THEN** the output does not begin with the canonical phrase

#### Scenario: Enabled generation stays deterministic

- **WHEN** two generators share a dictionary, seed, and a configuration with the canonical opening enabled, and each generates a paragraph with the same style
- **THEN** both produce the exact same string

#### Scenario: The opening counts toward word bounds

- **WHEN** generation runs with the canonical opening enabled and configured word-count bounds that can accommodate the phrase
- **THEN** the words of the canonical phrase count toward the first sentence's word total, and that total respects the configured bounds

#### Scenario: The phrase is truncated when the bounds cannot fit it

- **WHEN** generation runs with the canonical opening enabled and a maximum word count too small to hold the full phrase
- **THEN** the first sentence begins with a contiguous leading run of the phrase's words, dropping trailing words, and its word total still respects the configured bounds

#### Scenario: The phrase stays contiguous rather than being split

- **WHEN** the selected template's leading word slot cannot hold the whole phrase
- **THEN** the emitted portion of the phrase is an unbroken leading run of its words, with no literal text or punctuation inserted between them

#### Scenario: No transition precedes the opening

- **WHEN** a paragraph is generated with the canonical opening enabled and a style that inserts transitions
- **THEN** the first sentence begins with the canonical phrase rather than a transition phrase

### Requirement: Typed failure handling without crashes

The library SHALL surface every failure condition through a sealed `LoremException` hierarchy with the cases empty dictionary, invalid template, and invalid weight, allowing callers to discriminate exhaustively. Public operations that can fail SHALL be annotated so that Kotlin/Native consumers receive a catchable error rather than an abnormal process termination. The library SHALL NOT terminate the process on any documented failure condition. A non-positive template weight SHALL be reported as an invalid-weight failure. Generation SHALL raise an invalid-template failure when the combination of configuration and style eliminates every candidate template.

#### Scenario: Invalid weight is reported

- **WHEN** a sentence template or weighted selection is created with a non-positive weight
- **THEN** an invalid-weight failure is raised

#### Scenario: Exhausted template set is reported

- **WHEN** generation runs with a custom template set containing only question templates and a style whose question frequency is zero
- **THEN** an invalid-template failure is raised

#### Scenario: Failures are recoverable

- **WHEN** any documented failure condition occurs
- **THEN** the caller receives a `LoremException` and the process does not terminate

#### Scenario: Failures cross the native boundary

- **WHEN** a documented failure condition occurs in a call made from a Kotlin/Native consumer
- **THEN** the consumer receives a catchable error rather than a crash

### Requirement: Thread-safe independent instances

Generator instances SHALL be independent and SHALL NOT share mutable global state. Every public type in the API (generator, dictionary, configuration, template, style, failure) SHALL be immutable after construction, and the library SHALL declare no top-level mutable state. Concurrent use of separate generators on different threads SHALL be free of data races.

#### Scenario: Separate instances generate concurrently

- **WHEN** two independently seeded generators generate paragraphs concurrently on different threads
- **THEN** each result matches the result it would produce when run in isolation with the same seed

#### Scenario: One instance is safe to share

- **WHEN** a single generator instance is used concurrently from multiple threads with the same request
- **THEN** every call returns the same string and no call fails

### Requirement: Multiplatform target coverage

The library SHALL publish artifacts for the JVM, Android, `iosArm64`, `iosSimulatorArm64`, `iosX64`, `linuxX64`, and `wasmJs` targets. All behavior SHALL be implemented in the common source set with no platform-specific declarations, so that every target exhibits identical behavior. The library SHALL declare no runtime dependencies beyond the Kotlin standard library. The public API SHALL be explicit, with every public declaration carrying an explicit visibility modifier and return type.

#### Scenario: Every target builds and passes the same tests

- **WHEN** the common test suite is run against each supported target
- **THEN** every target compiles and every test passes

#### Scenario: No platform-specific declarations exist

- **WHEN** the source tree is inspected
- **THEN** no `expect` or `actual` declaration is present and all library code resides in the common source set

#### Scenario: No runtime dependencies are declared

- **WHEN** a consumer resolves the published artifact
- **THEN** the only transitive runtime dependency is the Kotlin standard library
