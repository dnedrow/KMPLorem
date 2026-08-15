# lorem

A Kotlin Multiplatform library that generates deterministic Lorem Ipsum text for previews, tests, and documentation.

Give it a seed and it always produces the same text — on every target, in every process, across toolchain upgrades. The generator is a plain immutable value with no shared mutable state, so instances are safe to hand around and use concurrently.

## Quick start

```kotlin
import io.github.dnedrow.lorem.GeneratorConfiguration
import io.github.dnedrow.lorem.LoremDictionary
import io.github.dnedrow.lorem.LoremGenerator
import io.github.dnedrow.lorem.ParagraphStyle

val generator = LoremGenerator(dictionary = LoremDictionary.BuiltIn, seed = 42L)

val sentence = generator.generateSentence()
val paragraph = generator.generateParagraph(ParagraphStyle.Technical)
val paragraphs = generator.generateParagraphs(count = 3, style = ParagraphStyle.Academic)
```

`LoremDictionary.BuiltIn` holds 210 Latin words and is embedded in the library, so there is nothing to load at runtime and nothing to bundle. Supply your own pool when you want different vocabulary:

```kotlin
val dictionary = LoremDictionary(listOf("alpha", "beta", "gamma", "delta"))
val generator = LoremGenerator(dictionary, seed = 7L)
```

## One call, one result

A generator is idempotent. Calling `generateParagraph()` twice on the same instance returns the *same* paragraph, because every call starts from the generator's seed:

```kotlin
val generator = LoremGenerator(seed = 42L)
generator.generateParagraph() == generator.generateParagraph() // true
```

That is the point — a preview that re-renders should not churn. When you want several *different* paragraphs, ask for them in one call rather than looping:

```kotlin
// Three distinct paragraphs, and the call as a whole is reproducible.
val varied = generator.generateParagraphs(count = 3)

// Three identical paragraphs. Almost certainly not what you wanted.
val repeated = List(3) { generator.generateParagraph() }
```

If you need variation across separate calls, vary the seed instead:

```kotlin
val perRow = rows.mapIndexed { index, _ -> LoremGenerator(seed = index.toLong()).generateSentence() }
```

## Styles

Each `ParagraphStyle` sets a sentence-count range, how often transitions appear, how strongly long sentence patterns are favoured, how often questions appear, and the preferred mix of sentence shapes.

| Style | Sentences | Character |
| --- | --- | --- |
| `ParagraphStyle.Classic` | 3–6 | Balanced prose with a moderate mix of every pattern |
| `ParagraphStyle.Technical` | 4–7 | Documentation-like, favouring lists and complex sentences |
| `ParagraphStyle.Academic` | 4–8 | Scholarly, with frequent transitions and long sentences |
| `ParagraphStyle.Legal` | 3–5 | Dense contractual prose: long sentences, few transitions, no questions |
| `ParagraphStyle.Mixed` | 2–7 | Varied prose drawing evenly from the full pattern set |

`ParagraphStyle.presets` lists all five, and the constructor is public if you want your own.

## Configuration

`GeneratorConfiguration` tunes every pass, independent of style:

```kotlin
val configuration = GeneratorConfiguration(
    transitionProbability = 0.0,       // scales the style's own probability; 0.0 suppresses transitions
    allowQuestions = false,
    allowShortSentences = false,       // raises the floor to the midpoint of the word range
    avoidImmediatePatternRepeats = true,
    minimumWordsPerSentence = 8,
    maximumWordsPerSentence = 16,
    customTransitions = listOf("Itaque,", "Denique,"),
    customTemplates = listOf(
        SentenceTemplate(SentenceTemplate.Pattern.SIMPLE, weight = 1, format = "Ecce {w4}."),
    ),
)

val generator = LoremGenerator(seed = 42L, configuration = configuration)
```

Word-count bounds are authoritative. A template that cannot render inside `maximumWordsPerSentence` is dropped during preparation rather than allowed to overrun the bound; if that leaves nothing, generation raises `LoremException.InvalidTemplate`. `transitionProbability` is clamped into `0.0..1.0`, and a configuration whose bounds are not positive, or whose minimum exceeds its maximum, is rejected with `LoremException.InvalidWeight`.

## The canonical opening

Set `startsWithCanonicalOpening = true` to make generated text begin with `Lorem ipsum dolor sit amet`:

```kotlin
val generator = LoremGenerator(
    seed = 42L,
    configuration = GeneratorConfiguration(startsWithCanonicalOpening = true),
)
generator.generateParagraph()
// Lorem ipsum dolor sit amet adipisci quisquam asperiores aut proident asperiores doloribus. …
```

Three things worth knowing:

- **It is a prefix, not the sentence.** The rest of the first sentence continues with generated words and the selected template's own literals and punctuation.
- **It appears once per call** — on the single sentence, on a paragraph's first sentence, or on the first sentence of a batch's first paragraph. Never later.
- **Bounds still win.** The phrase is kept contiguous, so it is truncated from the end to whatever the selected template's leading word slot can hold. With a generous maximum every built-in template keeps the whole phrase; with a very small one you may see only `Lorem ipsum dolor`.

Enabling the option changes the *entire* text for a given seed, not just its opening words, because the phrase replaces dictionary draws and shifts the random sequence.

## Failure handling

Every failure is a case of the sealed `LoremException`, which extends `IllegalArgumentException`:

| Case | Raised when |
| --- | --- |
| `LoremException.EmptyDictionary` | A dictionary is built from no usable words |
| `LoremException.InvalidTemplate` | A template format is malformed, or configuration and style eliminate every candidate |
| `LoremException.InvalidWeight` | A weight is not positive, or word-count bounds are invalid |

```kotlin
try {
    generator.generateParagraph(ParagraphStyle.Legal)
} catch (exception: LoremException) {
    when (exception) {
        is LoremException.EmptyDictionary -> …
        is LoremException.InvalidTemplate -> …
        is LoremException.InvalidWeight -> …
    }
}
```

## Supported targets

`jvm`, `androidTarget`, `iosArm64`, `iosSimulatorArm64`.

The library is written entirely in `commonMain` — there is no `expect`/`actual` declaration and no platform-specific source set. Whitespace splitting and letter casing are hand-rolled rather than built on `Regex`, whose character classes differ between the JVM, JavaScript, Wasm, and Native, so a given seed produces identical output everywhere.

## Notes for Swift consumers

The library exports to Kotlin/Native, but two things do not survive the Objective-C bridge:

- **Default arguments do not cross.** `generateParagraph(style:)` and friends have Kotlin defaults; from Swift you must pass every parameter explicitly, including `style` and `configuration`.
- **`LoremException` arrives as `NSError`.** Public fallible functions carry `@Throws(LoremException::class)`, so Swift sees `throws` and receives a catchable error rather than a crash. The sealed case is available on the error's `kotlinException` payload; you lose exhaustive `when` matching and get an `NSError` domain instead.

Unannotated Kotlin exceptions terminate the process on Kotlin/Native, so do not remove those annotations.

## Custom dictionaries and character semantics

Word splitting and first-letter casing use Kotlin's `Char` semantics, which are defined per platform. A custom dictionary containing combining marks, surrogate pairs, or locale-sensitive casing (Turkish dotless *i*, for example) may render differently across targets. The built-in Latin dictionary is plain ASCII and is identical everywhere.

## License

Apache License 2.0. See [LICENSE](LICENSE).
