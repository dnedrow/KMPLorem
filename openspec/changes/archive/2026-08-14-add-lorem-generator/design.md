## Context

See `proposal.md` — Why. This is a greenfield Kotlin Multiplatform library that ports the behavior of DSKit's Swift `DSLorem` module (`/Volumes/Repos/AppleDev/DSKit/Sources/DSLorem`, ~773 lines, Foundation-only). Requirements are defined in `specs/lorem-generator/spec.md`. The reference Swift specification is DSKit's `openspec/specs/dslorem-generator/spec.md`; the reference designs are its archived `add-dslorem-module` and `add-dslorem-classic-opening` changes.

Constraints that shape the approach:

- **Greenfield build.** The repository is an empty OpenSpec workspace. The Gradle build, target list, source tree, and CI all arrive with this change, based on the JetBrains multiplatform-library-template layout.
- **Behavioral parity, not byte parity.** The Kotlin library must satisfy the same requirements as the Swift module. It is explicitly *not* required to emit identical text for a given seed, which frees the implementation from Swift standard library internals.
- **No platform code.** Every supported target must run the same common source set, including `wasmJs` and `linuxX64`, neither of which has a resource-bundle concept.
- **Publishing is deferred.** Coordinates stay as placeholders; the release destination is decided separately.

The Swift module has exactly one platform touchpoint in its entire surface — `Bundle.module` in `LoremDictionary.bundled()`. Everything else is pure string and integer arithmetic, which is why a `commonMain`-only port is achievable.

## Goals / Non-Goals

**Goals:**

- A structural port that keeps the Swift pipeline's shape (`Dictionary → WeightedSelector<Template> → TemplateRenderer → PhraseGenerator → LoremGenerator`), so the two implementations stay reviewable against each other.
- A `commonMain`-only library with zero `expect`/`actual` declarations and no runtime dependencies beyond the Kotlin standard library.
- Determinism that is stable against Kotlin toolchain upgrades, so golden tests do not break on a compiler bump.
- Idiomatic Kotlin at the seams — errors, nullability, immutability, and visibility — without redesigning the algorithm.

**Non-Goals (design-level):**

- No byte-for-byte agreement with the Swift module's output. Golden tests are authored fresh.
- No caching layer — determinism makes it unnecessary.
- No streaming, coroutine, or `Flow` API. Generation is pure CPU work and stays synchronous.
- No serialization of public types. The Swift `Pattern` conforms to `Codable`; the Kotlin port does not, because it would pull in `kotlinx.serialization` for no demonstrated need.
- No Compose, Compose Multiplatform, or UI-preview integration in this change.

## Decisions

### D1: `SeededRandom` subclasses `kotlin.random.Random` with a SplitMix64 core

`SeededRandom` extends the abstract `kotlin.random.Random` and overrides `nextBits(bitCount: Int)`, backed by a single `ULong` of SplitMix64 state advanced by the same constants the Swift module uses (`0x9E3779B97F4A7C15`, `0xBF58476D1CE4E5B9`, `0x94D049BB133111EB`). Every derived draw the pipeline needs — `nextInt(range)`, `nextInt(bound)`, `nextDouble()` — comes free from the standard library on top of that core.

- *Why:* This is the direct structural analogue of the Swift design. `SeededRandomNumberGenerator` conforms to `RandomNumberGenerator` by implementing `next() -> UInt64`, which is what makes `Int.random(in:using:)` work at the call sites; subclassing `Random` and overriding `nextBits` gives the identical seam in Kotlin. Call sites port line for line.
- *Alternatives:*
  - `kotlin.random.Random(seed)` directly — zero code, but its KDoc guarantees reproducibility only *within the same version of the Kotlin runtime*. Golden tests would become hostage to toolchain upgrades. Rejected.
  - Hand-rolling the bounded-integer and double derivations as well (~50 extra lines) — freezes goldens against even a standard library change, but re-implements solved, well-tested problems and diverges from the Swift structure. Rejected as unnecessary; the derivations are pure functions of `nextBits` and live in common stdlib code.
  - Porting Swift's standard library derivations exactly (modulo-with-rejection `next(upperBound:)`, the 53-bit significand trick in `Double.random(in: 0..<1)`) to achieve byte parity — rejected with the parity decision, since it pins the library to Swift 6.x internals forever.

### D2: The seed is a public `Long`, `ULong` internally

The public constructor takes `seed: Long`. The SplitMix64 state is `ULong`, converted with `toULong()`, which preserves the bit pattern exactly.

- *Why:* Kotlin unsigned types export poorly through the Objective-C bridge, so a `ULong` parameter would be awkward or invisible to the iOS consumers this library explicitly targets. `Long` covers the same 64 bits.
- *Alternatives:* public `ULong` (hurts the primary consumer); `Int` seed (halves the seed space for no benefit). Both rejected.

### D3: The dictionary is embedded in `commonMain`, not loaded from a resource

The 210 Latin words ship as a compiled-in list in the common source set. `LoremDictionary` retains its custom-word constructor, its trimming, its first-seen-order deduplication, and its empty rejection; only the loading path changes.

- *Why:* `linuxX64` and `wasmJs` have no bundle concept at all, and an `expect`/`actual` resource loader would need four distinct implementations (Android assets, JVM classpath, `NSBundle`, native file I/O) to deliver 1.5 KB of static text that can never change at runtime. Embedding keeps the library platform-free and makes loading infallible.
- *Consequence:* Swift's `dictionaryNotFound` and `resourceLoadFailed` cases become unreachable and are **not** ported. `LoremException` has three cases, not five.
- *Alternatives:* `expect`/`actual` per target (four implementations, fragile, blocks `wasmJs`); a multiplatform resources library such as Compose Resources (adds a dependency to a zero-dependency library). Both rejected.

### D4: `LoremException` is a sealed hierarchy rooted in `IllegalArgumentException`, annotated `@Throws`

```
sealed class LoremException(message: String) : IllegalArgumentException(message)
    class EmptyDictionary : LoremException
    class InvalidTemplate : LoremException
    class InvalidWeight   : LoremException
```

Every public function that can fail carries `@Throws(LoremException::class)`.

- *Why:* Once the dictionary is embedded, every surviving failure is a *programmer* error — a bad literal argument caught at development time (`weight <= 0`, `min > max`, `"{w0}"`, an empty word list). Kotlin convention for that is `IllegalArgumentException`, so rooting the hierarchy there is honest about the nature of the failure. Sealing it preserves the exhaustive `when` that the Swift enum gave callers, satisfying the spec's typed-failure requirement.
- *The `@Throws` annotation is not optional.* Kotlin/Native maps annotated throws to `NSError**`; an *un*annotated exception crossing into Swift is an unrecoverable crash. Since iOS is a first-class target, omitting it would violate the spec requirement that failures are recoverable.
- *Known limitation:* Swift callers receive an `NSError` with the Kotlin exception buried in `userInfo["KotlinException"]`. Case discrimination from Swift is therefore awkward regardless of the chosen model — this is a property of the bridge, not of this design.
- *Alternatives:* a sealed hierarchy rooted in `Exception` (closest mirror of Swift, but dresses programmer errors up as recoverable runtime conditions); plain `require()` throwing `IllegalArgumentException` (most idiomatic, but loses typed discrimination and forces the spec's typed-failure requirement to be dropped); `Result<T>` returns (avoids the crash risk entirely, but `Result` maps badly across the ObjC bridge too and is unidiomatic for argument validation). All rejected.

### D5: The generator stays immutable and idempotent, mirroring Swift

`LoremGenerator` holds an immutable dictionary, seed, and configuration. Each generation call constructs a fresh `SeededRandom(seed)` and threads it through the pipeline. Repeated identical calls therefore return identical text; the batch operation threads **one** generator across all paragraphs so they differ from each other while the call stays reproducible.

- *Why:* Swift adopted this because a `let` binding cannot call `mutating` methods. Kotlin imposes no such constraint, so this is a free choice — and the immutable model buys thread safety with no locks, keeps the determinism guarantee stateable in one sentence, and lets the spec port verbatim.
- *Known trade-off, inherited deliberately:* a caller who invokes `generateParagraph()` five times in a row gets five identical paragraphs. This bites hardest in previews rendering a list. The batch operation is the documented answer and the README must call this out prominently.
- *Alternatives:* a stateful generator whose stream advances across calls (more intuitive, but sacrifices thread safety and weakens "same seed → same output" into a claim about call *sequences* that is far harder to specify and test); an immutable generator plus a separate stateful session type (keeps both properties, but adds a second public type and a new requirement for a problem the batch API already solves). Both rejected.

### D6: A hand-rolled whitespace tokenizer, not `Regex`

Swift's `split(whereSeparator: { $0.isWhitespace })` is used in six places (word counting, normalization, phrase measurement). Kotlin has no stdlib equivalent that takes a `Char` predicate and drops empty results, so the port adds a small internal helper built on `Char.isWhitespace()`.

- *Why:* `Regex("\\s+")` compiles to the host platform's regex engine — Java's on JVM/Android, JavaScript's on `js`/`wasmJs`, and a bundled implementation on Native. Their `\s` classes are not identical for non-ASCII whitespace. That is precisely the failure mode that produces a golden test passing on `jvmTest` and failing on `wasmJsTest`, and it would be diagnosed late and painfully.
- *Alternatives:* `Regex` (platform drift); `split(' ')` (wrong — misses tabs and newlines in custom templates). Both rejected.

### D7: The private unvalidated configuration constructor is dropped

Swift's `GeneratorConfiguration` carries a second, private, validation-skipping initializer solely so `static let default` can exist — `static let x = try ...` is illegal in Swift. Kotlin property initializers can throw, so `val Default = GeneratorConfiguration()` runs the normal validated path and the duplicate constructor has no counterpart.

- *Why:* The Swift construct is a language workaround, not a design decision. Its default values are valid, so nothing is lost by validating them.
- *Note:* `GeneratorConfiguration` is a plain class with a hand-written `copy()` rather than a `data class`. A `data class` cannot clamp a constructor `val` in its `init` block, and `transitionProbability` must clamp to `0.0..1.0`. Routing the hand-written `copy()` through the primary constructor keeps both the clamp and the word-bound validation on every copy, so validation still cannot be bypassed. The generated `componentN` destructuring is the only loss, and it does not export to Swift anyway.

### D8: Kotlin default arguments, with the ObjC gap documented rather than papered over

Generation functions use Kotlin default arguments (`style: ParagraphStyle = ParagraphStyle.Classic`), matching the Swift signatures.

- *Why:* This is the idiomatic Kotlin form and reads correctly for the JVM, Android, JS, and Wasm consumers who make up most of the audience.
- *Known limitation:* Kotlin default arguments do not survive the Objective-C bridge, so Swift callers must always pass `style:` explicitly. Generating explicit overloads would double the API surface to work around a bridge limitation; the README documents it instead. Revisit if iOS adoption makes it painful.

### D9: Explicit API mode enabled

The `:lorem` module sets `explicitApi()`, which the template deliberately omits.

- *Why:* The template is a demonstration; this is a library with a published surface. Explicit visibility and explicit public return types prevent accidental API leakage and make review of the public contract mechanical.

### D10: Target list widened past the template toward Compose Multiplatform

The template declares `jvm`, `androidLibrary`, `iosArm64`, `iosSimulatorArm64`, and `linuxX64`. This change adds `iosX64` and `wasmJs`.

- *Why:* The template's list demonstrates that native works; it is not aligned with a consumer. The obvious consumer for a Lorem Ipsum generator is a Compose Multiplatform app rendering previews and screenshot tests, whose target set is Android, iOS (all three), desktop JVM, and `wasmJs` — of which the template is missing two. Nothing in the code blocks any target once the dictionary is embedded.
- *Restraint:* macOS, watchOS, tvOS, `linuxArm64`, `mingwX64`, and legacy `js` are all technically reachable and are deliberately left out. Adding a target later is additive and non-breaking; removing one is a breaking change for anyone who adopted it. `linuxX64` is retained from the template rather than removed for the same reason it is cheap: it already exists in the CI matrix.
- *Cost:* The template's CI matrix enumerates Gradle task names by hand, so each target is one more matrix line. Note that the template declares `iosArm64` but never builds it in CI — it is only compiled at publish time. This change adds a compile-only matrix entry for it.

### D11: Golden tests are authored, and pinned to `SeededRandom`

Determinism is verified by fixed-seed tests asserting exact strings. Because `SeededRandom` owns its core (D1), those strings are stable across Kotlin toolchain upgrades. Statistical properties (weighted selection proportionality) are asserted over a large fixed-seed sample with generous tolerance, not exact ratios.

- *Why:* Golden tests are the only thing that catches silent determinism drift from a refactor of the token-budget arithmetic, which is the most intricate part of the port.

### D12: Templates that cannot fit the configured maximum are dropped, not allowed to overrun it

`prepareTemplates` discards any template whose `minimumWordCount` exceeds `maximumWordsPerSentence`. The reference implementation instead clamps with `min(bodyTarget, max(minimumWordCount, maximum - reserved))`, letting the template's own minimum win — so with `maximumWordsPerSentence = 4` the default `LIST` template `{w3}: {w2}, {w2}, et {w2}.` still emits five words. When no template survives the filter, `LoremException.InvalidTemplate` is raised, reusing the existing exhausted-templates path.

- *Why:* The spec makes the word-count bounds authoritative. Silently overrunning a caller's stated maximum is worse than reporting that no template fits.
- *Scope:* Only reachable when `maximumWordsPerSentence` is below 5; the default is 20, so the default template set is unaffected. This is the one deliberate behavioral divergence from the Swift reference.

### D13: The canonical opening truncates to the first token's capacity, keeping the phrase contiguous

The phrase is injected as the leading words of a token-leading template's *first* token, so how much of it survives is bounded by that token's share of the word budget, not by `maximumWordsPerSentence` alone. Every other token must keep at least one word, so a multi-token template can hold fewer phrase words than the raw maximum suggests — with `maximumWordsPerSentence = 5` and the emphasis template `{w4}: {w3}!`, the sentence opens `Lorem ipsum dolor sit: …`.

- *Why:* Splitting the phrase across a template's literal punctuation would be worse than truncating it. `reserveLeadingCapacity` already pulls as much capacity forward as the shape allows, and the default templates all keep the full phrase once the maximum reaches 9.
- *Consequence:* Tests assert a non-empty leading prefix of the phrase plus the word bound, not a truncation length derived from the maximum.

## Risks / Trade-offs

- **Platform-dependent character semantics silently diverge** (`Char.isLetter()`, `Char.isWhitespace()`, `uppercaseChar()` behave differently across JVM, JS, Wasm, and Native for non-ASCII input) → the built-in dictionary and templates are pure ASCII Latin, so the default path is unaffected; run the full common test suite on every target in CI rather than only on the JVM, and document that custom dictionaries containing combining marks or surrogate pairs may render differently across targets. Swift operates on grapheme clusters where Kotlin operates on UTF-16 `Char`, so this divergence is inherent, not a bug to fix.
- **Token-budget arithmetic is the highest-risk port** (`distribute` and `reserveLeadingCapacity` do non-obvious rescaling with a bounded correction loop; `makeSentence` layers transition reservation, opening reservation, and clamping on top) → port these three functions first with direct unit tests over their input/output contracts before wiring them into the generator, and cover the canonical-opening truncation boundary explicitly.
- **Golden tests lock in behavior before it is fully reviewed** → author goldens last, after the behavioral tests derived from the spec scenarios pass, so they pin reviewed behavior rather than the first thing that compiled.
- **The idempotent single-call model surprises callers** who expect varied output from repeated calls → documented prominently in the README with the batch operation shown as the answer; accepted deliberately in D5.
- **`wasmJs` is the least mature target** and may hit toolchain issues unrelated to this code → it is additive; if it blocks the change, it can be dropped from the target list without touching a line of library code or a single requirement other than the target-coverage one.
- **`@Throws` omitted on a new public function** would turn a documented failure into an iOS crash → the spec covers it with an explicit scenario; enforce by reviewing every public signature against the annotation when the API surface changes.
- **Rounding differences between Swift and Kotlin** (`Double.rounded()` rounds half away from zero; `roundToInt()` rounds half up) → all rounded values in the pipeline are positive, where the two agree, so this affects nothing today; it is noted only so a future negative-weight feature does not reintroduce it silently.

## Migration Plan

Purely additive to an empty repository; there is no data, API, or consumer migration. Nothing depends on this library yet, so rollback is reverting the commit.

Publishing is deliberately out of scope. The build retains the template's `mavenPublishing` block with placeholder coordinates under group `io.github.dnedrow` and artifact `lorem`, and the template's release workflow is left in place but is not exercised. The release destination — public Maven Central under a personal namespace, public Maven Central under a verified organization namespace, or an internal repository — is decided before the first release, at which point the POM, LICENSE, and signing configuration are completed.

## Open Questions

- **Version number for the first release.** The template ships `1.0.0`. Whether to start at `0.1.0` and reserve `1.0.0` for a committed-stable API can be settled at release time; it changes nothing about the specs, the approach, or the tasks.
- **Whether to add binary-compatibility validation** (`binary-compatibility-validator`) before the first published release. Deferrable — it constrains future changes, not this one.
